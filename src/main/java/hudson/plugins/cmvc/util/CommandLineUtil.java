package hudson.plugins.cmvc.util;

import hudson.FilePath;
import hudson.plugins.cmvc.CmvcChangeLogSet;
import hudson.plugins.cmvc.CmvcSCM;
import hudson.plugins.cmvc.CmvcChangeLogSet.CmvcChangeLog;
import hudson.util.ArgumentListBuilder;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides cmvc commands to {@link CmvcSCM}.
 * 
 * 
 * @author fuechi
 *
 */
public class CommandLineUtil {
	
	
	private CmvcSCM cmvcSCM = null;
	
	
	public CommandLineUtil(CmvcSCM cmvcSCM) {
		super();
		this.cmvcSCM = cmvcSCM;
	}


	/**
	 * Generates the command line for the Report command.
	 *
	 * For example:
	 *
	 * 'Report -raw -view TrackView -where "1=1"'
	 *
	 * @param strNow
	 *            String representing the current time
	 * @param strLastBuild
	 *            String representing the last build execution time
	 * @return new command line object
	 */
	public ArgumentListBuilder buildReportTrackViewCommand(
			String strNow, String strLastBuild) {
		ArgumentListBuilder command = buildBasicRawReportCommand("TrackView");
		command.add("-where");
		command.add("lastUpdate between " + strNow + " and " + strLastBuild
				+ " and state = 'integrate' and releaseName in "
				+ convertToReleaseInClause(cmvcSCM.getReleases())
				+ " order by defectName");
		return command;
	}
	
	
	/**
	 * Generates the command line for the Report command.
	 *
	 * For example:
	 *
	 * 'Report -family family -raw -view ChangeView -where "defectName in () and
	 * releaseName in () order by defectName"'
	 *
	 * @param trackNames
	 * @return new command line object
	 */
	public ArgumentListBuilder buildReportChangeViewCommand(String[] trackNames) {
		ArgumentListBuilder command = buildBasicRawReportCommand("ChangeView");
		command.add("-where");
		command.add("defectName in " + convertToInClause(trackNames) + " and releaseName in "
				+ convertToReleaseInClause(cmvcSCM.getReleases())
				+ " order by defectName");
		return command;
	}
	
	/**
	 * @param changeLogSet
	 * @return
	 */
	public ArgumentListBuilder buildReportChangeViewCommand(CmvcChangeLogSet changeLogSet) {
		List<CmvcChangeLog> changeLogs = changeLogSet.getLogs();
		if ( changeLogs.size() > 0 ) {
			Set<String> trackNames = new TreeSet<String>(); 
			for( CmvcChangeLog log : changeLogs)
				trackNames.add(log.getTrackName());
			return buildReportChangeViewCommand( trackNames.toArray(new String[0]) );
		}
		return null;
	}

	/**
	 * @param workspace
	 * @return
	 */
	public ArgumentListBuilder buildReleaseExtractCommand(FilePath workspace) {
		ArgumentListBuilder command = new ArgumentListBuilder();
		command.add("Release");
		command.add("-family");
		command.add(cmvcSCM.getFamily());
		
		//FIXME Handle multiple releases
		command.add("-extract");
		command.add(cmvcSCM.getReleases());
		
		command.add("-root");
		command.add(workspace.getName());
		
		//TODO where is the node?
		command.add("-node");
		command.add("");
		
		command.add("-committed");
		
		return command;
	}

	
	
	private ArgumentListBuilder buildBasicRawReportCommand(String viewName) {
		ArgumentListBuilder command = new ArgumentListBuilder();
		command.add("Report");
		command.add("-family");
		command.add(cmvcSCM.getFamily());
		command.add("-raw");
		command.add("-view");
		command.add(viewName);
		return command;
	}

	/**
	 * @param releaseList
	 * @return
	 */
	public String convertToInClause(String[] releaseList) {
		StringBuffer buf = new StringBuffer("(");
		int length = releaseList.length;
		for (int i = 0; i < length; i++) {
			String temp = releaseList[i].trim();
			buf.append("'" + temp + "'");
			buf.append(i != length - 1 ? ", " : ")");
		}
		return buf.toString();
	}
	
	/**
	 * @param rel
	 * @return
	 */
	private String convertToReleaseInClause(String rel) {
		String[] releaseList = rel.split(",");
		return convertToInClause(releaseList);
	}
	

	public String convertToUnixQuotedParameter(String[] trackList) {
		int length = trackList.length;
		
		if ( length < 1 ) {
			return "";
		}
		
		StringBuffer buf = new StringBuffer("");
		for (int i = 0; i < length; i++) {
			String temp = trackList[i].trim();
			buf.append(temp);
			buf.append(i != length - 1 ? " " : "");
		}
		return buf.toString();
	}
}
