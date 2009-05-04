package hudson.plugins.cmvc;

import hudson.model.AbstractBuild;
import hudson.plugins.cmvc.util.CmvcRawParser;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.xml.sax.SAXException;

/**
 * Parses the changelog.xml file.
 * 
 * @author <a href="mailto:fuechi@ciandt.com">Fábio Franco Uechi</a>
 *
 */
public class CmvcChangeLogParser extends ChangeLogParser {

	public CmvcChangeLogParser() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ChangeLogSet<? extends Entry> parse(AbstractBuild build,
			File changelogFile) throws IOException, SAXException {
		FileReader fileReader = new FileReader(changelogFile);
		return new CmvcChangeLogSet(build, CmvcRawParser
				.parseChangeLogFile(fileReader));
	}

}