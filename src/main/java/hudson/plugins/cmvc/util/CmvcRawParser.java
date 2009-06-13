package hudson.plugins.cmvc.util;

import hudson.model.User;
import hudson.plugins.cmvc.CmvcChangeLogSet;
import hudson.plugins.cmvc.CmvcChangeLogSet.CmvcChangeLog;
import hudson.plugins.cmvc.CmvcChangeLogSet.CmvcChangeLog.ModifiedFile;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.util.Digester2;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.locale.converters.DateLocaleConverter;
import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import com.Ostermiller.util.CSVParser;
import com.thoughtworks.xstream.XStream;

/**
 * 
 * @author <a href="mailto:fuechi@ciandt.com">Fábio Franco Uechi</a>
 * 
 */
public class CmvcRawParser {

	private static CSVParser parser = null;

	/**
	 * Parses TrackView raw report and generate a list of {@link CmvcChangeLog}
	 * 
	 * @param rawResult
	 * @param changeLogSet
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public static List<CmvcChangeLog> parseTrackViewReport(Reader rawResult,
			CmvcChangeLogSet changeLogSet) throws IOException, ParseException {

		String[][] parsedResult = parseCmvcRawReport(rawResult);

		List<CmvcChangeLog> changes = null;
		if (parsedResult == null || parsedResult.length <= 0) {
			changes = new ArrayList<CmvcChangeLog>(0);
		} else {
			int totalChanges = parsedResult.length;
			changes = new ArrayList<CmvcChangeLog>(totalChanges);
			CmvcChangeLog changeLog = null;
			for (int i = 0; i < totalChanges; i++) {
				changeLog = new CmvcChangeLog(changeLogSet);
				changeLog.setDateTime(DateUtil
						.convertFromCmvcDate(parsedResult[i][10]));
				changeLog.setReleaseName(parsedResult[i][0]);
				changeLog.setTrackName(parsedResult[i][1]);
				changeLog.setMsg(parsedResult[i][12]);
				changeLog.setType("f".equals(parsedResult[i][11]) ? "feature"
						: "defect");
				changeLog
						.setUser(StringUtils.isEmpty(parsedResult[i][6]) ? null
								: parsedResult[i][6]);
				changes.add(changeLog);
			}
		}

		return changes;
	}

	public static boolean parseTrackViewReport(Reader rawResult) throws IOException {
		String[][] parsedResult = parseCmvcRawReport(rawResult);
		return parsedResult != null && !(parsedResult.length <= 0);
	}

	private static String[][] parseCmvcRawReport(Reader rawResult)
			throws IOException {
		parser = new CSVParser(rawResult, '|');
		String[][] parsedResult = parser.getAllValues();
		return parsedResult;
	}

	private static Map<String, List<ModifiedFile>> parseReportChangeViewReader(
			Reader rawResult) throws IOException {

		Map<String, List<ModifiedFile>> modifiedFilesByDefectName = new HashMap<String, List<ModifiedFile>>();

		String[][] changes = parseCmvcRawReport(rawResult);

		if (changes == null || changes.length <= 0) {
			// do nothing??
		} else {
			for (int i = 0; i < changes.length; i++) {
				String defectName = changes[i][1];
				List<ModifiedFile> modifiedFiles;
				if (modifiedFilesByDefectName.containsKey(defectName)) {
					modifiedFiles = (List<ModifiedFile>) modifiedFilesByDefectName
							.get(defectName);
				} else {
					modifiedFiles = new ArrayList<ModifiedFile>();
					modifiedFilesByDefectName.put(defectName, modifiedFiles);
				}
				String strFilename = changes[i][4];
				String strVersion = changes[i][3];
				ModifiedFile modifiedFile = new ModifiedFile(strFilename,
						changes[i][5], strVersion);
				modifiedFiles.add(modifiedFile);
			}
		}

		return modifiedFilesByDefectName;
	}

	public static List<ModifiedFile> parseChangeViewReport(Reader rawResult)
			throws IOException, ParseException {

		String[][] changes = parseCmvcRawReport(rawResult);

		if (changes == null || changes.length <= 0) {
			return new ArrayList<ModifiedFile>(0);
		}

		List<ModifiedFile> modifiedFiles = new ArrayList<ModifiedFile>(
				changes.length);

		for (int i = 0; i < changes.length; i++) {
			String strFilename = changes[i][4];
			String strVersion = changes[i][3];
			ModifiedFile modifiedFile = new ModifiedFile(strFilename,
					changes[i][5], strVersion);
			modifiedFiles.add(modifiedFile);
		}

		return modifiedFiles;
	}

	/**
	 * @param rawResult
	 * @param changeLogSet
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void parseChangeViewReportAndPopulateChangeLogs(
			Reader rawResult, CmvcChangeLogSet changeLogSet)
			throws IOException, ParseException {
		Map<String, List<ModifiedFile>> modifiedFilesByDefectName = parseReportChangeViewReader(rawResult);
		
		changeLogSet.setTrackNames(modifiedFilesByDefectName.keySet());
		
		for (CmvcChangeLog log : changeLogSet.getLogs()) {
			if (modifiedFilesByDefectName.containsKey(log.getTrackName()))
				log.setFiles(modifiedFilesByDefectName.get(log.getTrackName()));
		}
	}

	/**
	 * @param changes
	 * @param writer
	 */
	public static void writeChangeLogFile(CmvcChangeLogSet changes,
			Writer writer) {
		XStream xstream = getXStream();
		xstream.toXML(changes, writer);
	}

	private static XStream getXStream() {
		XStream xstream = new XStream();
		xstream.setMode(XStream.NO_REFERENCES);
		xstream.alias("changes", CmvcChangeLogSet.class);
		xstream.alias("change", CmvcChangeLog.class);
		xstream.alias("file", ModifiedFile.class);
		xstream.addImplicitCollection(CmvcChangeLogSet.class, "logs",
				CmvcChangeLog.class);
		xstream.omitField(ChangeLogSet.class, "build");
		xstream.omitField(Entry.class, "parent");
		xstream.omitField(User.class, "properties");
		
		return xstream;
	}

	/**
	 * @param xml
	 * @return
	 */
	public static CmvcChangeLogSet parseChangeLogFile(Reader xml, CmvcChangeLogSet changeLogSet) {
		XStream xstream = getXStream();
		return (CmvcChangeLogSet) xstream.fromXML(xml, changeLogSet);
	}

	/**
	 * @param reader
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 */
	public static List<CmvcChangeLog> parseChangeLogFile(Reader reader) throws IOException, SAXException {
		Digester digester = new Digester2();
		ArrayList<CmvcChangeLog> r = new ArrayList<CmvcChangeLog>();
		
		String pattern = "yyyy-MM-dd HH:mm:ss.0 z";
		Locale locale = Locale.getDefault();
		DateLocaleConverter converter = new DateLocaleConverter(locale, pattern);
		converter.setLenient(true);
		ConvertUtils.register(converter, java.util.Date.class);
		
		digester.push(r);

		digester.addObjectCreate("*/change", CmvcChangeLog.class);
		digester.addBeanPropertySetter("*/change/dateTime");
		digester.addBeanPropertySetter("*/change/author/fullName", "user");
		digester.addBeanPropertySetter("*/change/msg");
		digester.addBeanPropertySetter("*/change/type");
		digester.addBeanPropertySetter("*/change/trackName");

		digester.addObjectCreate("*/change/files/file", ModifiedFile.class);
		digester.addBeanPropertySetter("*/change/files/file/path");
		digester.addBeanPropertySetter("*/change/files/file/action");
		digester.addBeanPropertySetter("*/change/files/file/version");
		digester.addSetNext("*/change/files/file", "addFile");
		
		digester.addSetNext("*/change", "add");

		digester.parse(reader);

		return r;
	}

}