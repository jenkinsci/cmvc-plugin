package hudson.plugins.cmvc;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.plugins.cmvc.CmvcChangeLogSet.CmvcChangeLog;
import hudson.plugins.cmvc.util.CmvcRawParser;
import hudson.plugins.cmvc.util.CommandLineUtil;
import hudson.plugins.cmvc.util.DateUtil;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * This class implements the {@link SCM} methods for a CMVC repository. The call
 * to CMVC is assumed to work without any setup. This implies that either the
 * environment variable <code>BECOME_USER</code> is set or the become user is
 * provided in the project configuration page. Besides that this user must have
 * permissions to access the specified family from within the hudson host.
 * 
 * <p>
 * Checks for changes in a CMVC family (repository). Triggers a build if any
 * integrated track within the monitored releases is detected.
 * </p>
 * 
 * <p>
 * Utilizes CMVCs <code>Report -raw</code> command to query the family for
 * changes. First it looks for all integrated tracks - within the specified
 * releases - between the last build time and the current time (<code>-view TrackView</code>).
 * Then it performs another query to find all files included in these tracks(<code>-view ChangeView</code>).
 * </p>
 * 
 * <p>
 * Changes are detected by running commands similar to the following:
 * </p>
 * 
 * <pre>
 * Report -family family@localhost@6666 
 *  -raw 
 *  -view TrackView 
 *  -where &quot;lastUpdate between &lt;lastBuildTime&gt; 
 *  and &lt;now&gt; 
 *  and state = 'integrate' 
 *  and releaseName in ('RC_123') 
 *  order by defectName&quot;
 * </pre>
 * 
 * <pre>
 * Report -family family@localhost@6666 
 *  -raw 
 *  -view ChangeView 
 *  -where &quot;defectName in ('1', '2') and releaseName in ('RC_123') order by defectName&quot;
 * </pre>
 * 
 * @see <a href="http://www.redbooks.ibm.com/abstracts/gg244178.html">Did You
 *      Say CMVC?</a>
 * 
 * @author <a href="mailto:fuechi@ciandt.com">Fábio Franco Uechi</a>
 * 
 */
public class CmvcSCM extends SCM implements Serializable {

	private static final long serialVersionUID = -6712277029373852186L;

	/** Configuration parameters */

	/**
	 * CMVC family. Syntax: family@host@port
	 */
	private String family;

	/**
	 * Release names separated by comma
	 */
	private String releases;

	/**
	 * User login used to connect to CMVC
	 */
	private String become;

	/**
	 * Absolute fullpath + script name to be used to perform the checkout
	 */
	private String checkoutScript;

	/**
	 * TrackView Report where clause. If defined will be used for polling
	 * changes, otherwise the default condition will be used.
	 */
	private String trackViewReportWhereClause;

	/**
	 * Utility class
	 */
	private CommandLineUtil commandLineUtil = null;

	/**
	 * @param family
	 * @param releases
	 * @param project
	 * @param cleanCopy
	 */
	@DataBoundConstructor
	public CmvcSCM(String family, String become, String releases,
			String checkoutScript, String trackViewReportWhereClause) {
		super();
		this.checkoutScript = checkoutScript;
		this.family = family;
		this.releases = releases;
		this.become = become;
		this.trackViewReportWhereClause = trackViewReportWhereClause;
	}

	private CommandLineUtil getCmvcCommandLineUtil() {
		return commandLineUtil != null ? commandLineUtil : new CommandLineUtil(
				this);
	}

	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher,
			FilePath workspace, BuildListener listener, File changelogFile)
			throws IOException, InterruptedException {

		boolean checkoutResult = false;

		CmvcChangeLogSet cmvcChangeLogSet = null;

		try {

			cmvcChangeLogSet = getCmvcChangeLogSet(build, launcher, workspace,
					listener, changelogFile);

			if (cmvcChangeLogSet.getTrackNames() != null) {
				checkoutResult = doCheckout(build, launcher, workspace,
						listener, changelogFile, cmvcChangeLogSet);
			} else {
				checkoutResult = true;
			}

			writeChangeLogFile(changelogFile, cmvcChangeLogSet);

		} catch (Throwable e) {
			listener.fatalError(e.getMessage(), e);
			checkoutResult = false;
		}

		return checkoutResult;
	}

	private void writeChangeLogFile(File changelogFile,
			CmvcChangeLogSet cmvcChangeLogSet) throws IOException {
		FileWriter fileWriter = new FileWriter(changelogFile);
		try {
			CmvcRawParser.writeChangeLogFile(cmvcChangeLogSet, fileWriter);
		} finally {
			IOUtils.closeQuietly(fileWriter);
		}
	}

	private boolean doCheckout(AbstractBuild build, Launcher launcher,
			FilePath workspace, BuildListener listener, File changelogFile,
			CmvcChangeLogSet cmvcChangeLogSet) throws IOException,
			InterruptedException {

		workspace.deleteContents();

		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(this.checkoutScript);
		cmd.addQuoted(getCmvcCommandLineUtil().convertToUnixQuotedParameter(
				cmvcChangeLogSet.getTrackNames().toArray(new String[0])));

		return run(launcher, cmd, listener, workspace);
	}

	private CmvcChangeLogSet getCmvcChangeLogSet(AbstractBuild build,
			Launcher launcher, FilePath workspace, BuildListener listener,
			File changelogFile) throws IOException, InterruptedException,
			ParseException {

		Date lastBuild = null;
		CmvcChangeLogSet changeLogSet = null;

		if (build.getPreviousBuild() != null) {
			lastBuild = build.getPreviousBuild().getTimestamp().getTime();
		} else {
			listener.getLogger().println("No previous build.");
			// FIXME o q fazer quando é a primeira build?
			lastBuild = new Date();
		}

		ArgumentListBuilder cmd = getCmvcCommandLineUtil()
				.buildReportTrackViewCommand(
						DateUtil.convertToCmvcDate(lastBuild),
						DateUtil.convertToCmvcDate(new Date()));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (run(launcher, cmd, listener, workspace, new ForkOutputStream(baos,
				listener.getLogger()))) {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new ByteArrayInputStream(baos.toByteArray())));
			changeLogSet = new CmvcChangeLogSet(build);
			List<CmvcChangeLog> logs = CmvcRawParser.parseTrackViewReport(in,
					changeLogSet);
			changeLogSet.setLogs(logs);
		}

		cmd = getCmvcCommandLineUtil().buildReportChangeViewCommand(
				changeLogSet);
		baos.reset();

		if (cmd != null) {
			if (run(launcher, cmd, listener, workspace, new ForkOutputStream(
					baos, listener.getLogger()))) {
				BufferedReader in = new BufferedReader(new InputStreamReader(
						new ByteArrayInputStream(baos.toByteArray())));

				CmvcRawParser.parseChangeViewReportAndPopulateChangeLogs(in,
						changeLogSet);
			}
		}

		return changeLogSet;
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return new CmvcChangeLogParser();
	}

	@Override
	public SCMDescriptor<CmvcSCM> getDescriptor() {
		return DESCRIPTOR;
	}

	/**
	 * Polls cmvc repository for integrated tracks within the current family and
	 * release
	 * 
	 * <p>
	 * By default it checks for changes in a CMVC family (repository). Triggers a build if any
	 * integrated track within the monitored releases is detected.
	 * </p>
	 * 
	 * 
	 * @see hudson.scm.SCM#pollChanges(hudson.model.AbstractProject,
	 *      hudson.Launcher, hudson.FilePath, hudson.model.TaskListener)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean pollChanges(AbstractProject project, Launcher launcher,
			FilePath workspace, TaskListener listener) throws IOException,
			InterruptedException {

		Date lastBuild = null;
		if (project.getLastBuild() != null) {
			lastBuild = project.getLastBuild().getTimestamp().getTime();
		} else {
			listener.getLogger().println("No previous build.");
			// FIXME o q fazer quando é a primeira build?
			lastBuild = new Date();
			return false;
		}

		ArgumentListBuilder cmd = getCmvcCommandLineUtil()
				.buildReportTrackViewCommand(
						DateUtil.convertToCmvcDate(lastBuild),
						DateUtil.convertToCmvcDate(new Date()));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (!run(launcher, cmd, listener, workspace, new ForkOutputStream(baos,
				listener.getLogger())))
			return false;
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(baos.toByteArray())));

		return CmvcRawParser.parseTrackViewReport(in);
	}

	/**
	 * Invokes the command with the specified command line option and wait for
	 * its completion.
	 * 
	 * @param dir
	 *            if launching locally this is a local path, otherwise a remote
	 *            path.
	 * @param out
	 *            Receives output from the executed program.
	 */
	protected final boolean run(Launcher launcher, ArgumentListBuilder cmd,
			TaskListener listener, FilePath dir, OutputStream out)
			throws IOException, InterruptedException {
		Map<String, String> env = createEnvVarMap(true);

		int r = launcher.launch(cmd.toCommandArray(), env, out, dir).join();
		if (r != 0)
			listener.fatalError(getDescriptor().getDisplayName()
					+ " failed. exit code=" + r);

		return r == 0;
	}

	protected final boolean run(Launcher launcher, ArgumentListBuilder cmd,
			TaskListener listener, FilePath dir) throws IOException,
			InterruptedException {
		return run(launcher, cmd, listener, dir, listener.getLogger());
	}

	/**
	 * 
	 * @param overrideOnly
	 *            true to indicate that the returned map shall only contain
	 *            properties that need to be overridden. This is for use with
	 *            {@link Launcher}. false to indicate that the map should
	 *            contain complete map. This is to invoke {@link Proc} directly.
	 */
	protected final Map<String, String> createEnvVarMap(boolean overrideOnly) {
		Map<String, String> env = getCmvcEnvVars();
		if (!overrideOnly)
			env.putAll(EnvVars.masterEnvVars);
		buildEnvVars(null/* TODO */, env);
		return env;
	}

	private Map<String, String> getCmvcEnvVars() {
		Map<String, String> env = new HashMap<String, String>();
		env.put("CMVC_FAMILY", this.family);
		env.put("CMVC_RELEASES", this.releases);
		
		if (StringUtils.isNotEmpty(this.become)){
			env.put("CMVC_BECOME", this.become);
		}
		
		return env;
	}

	@Override
	public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
		super.buildEnvVars(build, getCmvcEnvVars());
	}

	/**
	 * Descriptor should be singleton.
	 */
	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public String getReleases() {
		return this.releases;
	}

	public String getFamily() {
		return this.family;
	}

	public String getBecome() {
		return this.become;
	}

	public String getCheckoutScript() {
		return checkoutScript;
	}

	public void setCheckoutScript(String checkoutScript) {
		this.checkoutScript = checkoutScript;
	}

	public String getTrackViewReportWhereClause() {
		return trackViewReportWhereClause;
	}

	public void setTrackViewReportWhereClause(String trackViewReportWhereClause) {
		this.trackViewReportWhereClause = trackViewReportWhereClause;
	}

	public static class DescriptorImpl extends SCMDescriptor<CmvcSCM> implements
			ModelObject {

		/**
		 * CMVC binaries working dir
		 */
		private String cmvcPath;

		/**
		 * CMVC version
		 */
		private String cmvcVersion;

		protected DescriptorImpl() {
			super(CmvcSCM.class, null);
			load();
		}

		@Override
		public String getDisplayName() {
			return "CMVC";
		}

		@Override
		public SCM newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			CmvcSCM scm = req.bindJSON(CmvcSCM.class, formData);
			return scm;
		}

		@Override
		public boolean configure(StaplerRequest req) throws FormException {
			this.cmvcPath = Util.fixEmpty(req.getParameter("cmvc.cmvcPath")
					.trim());
			this.cmvcVersion = Util.fixEmpty(req.getParameter(
					"cmvc.cmvcVersion").trim());
			save();
			return true;
		}

		//
		// web methods
		//

		/**
		 * Checks the correctness of family.
		 */
		public FormValidation doCheckFamily(@QueryParameter
		String value) {
			if (StringUtils.isEmpty(value)) {
				return FormValidation.error(Messages.cmvc_family_mandatory());
			}
			return FormValidation.ok();
		}

		/**
		 * Checks the correctness of releases.
		 */
		public FormValidation doCheckReleases(@QueryParameter
		String value) {
			if (StringUtils.isEmpty(value)) {
				return FormValidation.error(Messages.cmvc_releases_mandatory());
			}
			return FormValidation.ok();
		}

		/**
		 * Checks the correctness of CheckoutScript.
		 */
		public FormValidation doCheckCheckoutScript(@QueryParameter
		String value) {
			if (StringUtils.isNotEmpty(value)) {
				File script = new File(value);
				if (!script.exists()) {
					return FormValidation.error(Messages.cmvc_checkoutScript_filenotexist());
				}
			}
			return FormValidation.ok();
		}


		/**
		 * Checks the correctness of CheckoutScript.
		 */
		public FormValidation doCheckTrackViewReportWhereClause(@QueryParameter
		String value) {
			
			if (StringUtils.isNotEmpty(value)) {
				//TODO test where clause
			}
			
			return FormValidation.ok();
		}

		public String getCmvcPath() {
			if (cmvcPath == null) {
				return "c:/cmvc/exe";
			}
			return cmvcPath;
		}

		public String getCmvcVersion() {
			if (cmvcVersion == null) {
				return "2.0";
			}
			return cmvcVersion;
		}

		public void setCmvcVersion(String cmvcVersion) {
			this.cmvcVersion = cmvcVersion;
		}
	}

}