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

import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.Ostermiller.util.CSVParser;

/**
 * 
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
	 * TODO parametirize condition to trigger build 
	 */
	private String pollChangesCondition;

	private CommandLineUtil commandLineUtil = null;

	/**
	 * @param family
	 * @param releases
	 * @param project
	 * @param cleanCopy
	 */
	@DataBoundConstructor
	public CmvcSCM(String family, String become, String releases,
			String checkoutScript) {
		super();
		this.checkoutScript = checkoutScript;
		this.family = family;
		this.releases = releases;
		this.become = become;
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
			
			if (cmvcChangeLogSet.getTrackNames() != null ) {
				checkoutResult = doCheckout(build, launcher, workspace, listener, changelogFile,
						cmvcChangeLogSet);
			}
			else { 
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

		// The line below is useful for testing purposes
		// lastBuild = DateUtils.addYears(lastBuild, -5);

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

		if ( cmd != null ) {
			if (run(launcher, cmd, listener, workspace, new ForkOutputStream(baos,
					listener.getLogger()))) {
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

		// The line below is useful for testing purposes
		//lastBuild = DateUtils.addYears(lastBuild, -5);

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

		CSVParser parser = new CSVParser(in, '|');
		String[][] integratedTracks = parser.getAllValues();

		return integratedTracks != null && !(integratedTracks.length <= 0);
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
		Map<String, String> env = new HashMap<String, String>();
		env.put("CMVC_FAMILY", this.family);
		env.put("CMVC_RELEASES", this.releases);
		env.put("CMVC_BECOME", this.become);
		if (!overrideOnly)
			env.putAll(EnvVars.masterEnvVars);
		buildEnvVars(null/* TODO */, env);
		return env;
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

	public static class DescriptorImpl extends SCMDescriptor<CmvcSCM> {

		/**
		 * CMVC binaries working dir
		 */
		private String cmvcPath;

		protected DescriptorImpl() {
			super(CmvcSCM.class, null);
			load();
		}

		@Override
		public String getDisplayName() {
			return "CMVC";
		}

		@Override
		public boolean configure(StaplerRequest req) throws FormException {
			cmvcPath = Util.fixEmpty(req.getParameter("cmvc.cmvcPath").trim());
			save();
			return true;
		}

		public String getCmvcPath() {
			if (cmvcPath == null) {
				return "c:/cmvc/exe";
			}
			return cmvcPath;
		}
	}

	public String getCheckoutScript() {
		return checkoutScript;
	}

	public void setCheckoutScript(String checkoutScript) {
		this.checkoutScript = checkoutScript;
	}
}