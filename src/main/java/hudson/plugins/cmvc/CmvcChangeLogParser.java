package hudson.plugins.cmvc;

import hudson.model.AbstractBuild;
import hudson.plugins.cmvc.util.CmvcRawParser;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

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
		Reader reader = new InputStreamReader(new FileInputStream(changelogFile), StandardCharsets.UTF_8);
		
		return new CmvcChangeLogSet(build, CmvcRawParser
				.parseChangeLogFile(reader));
	}

}