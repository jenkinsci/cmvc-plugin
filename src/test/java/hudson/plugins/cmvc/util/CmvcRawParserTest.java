package hudson.plugins.cmvc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import hudson.plugins.cmvc.CmvcChangeLogSet;
import hudson.plugins.cmvc.CmvcChangeLogSet.CmvcChangeLog;
import hudson.plugins.cmvc.CmvcChangeLogSet.CmvcChangeLog.ModifiedFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class CmvcRawParserTest {

	final String testTrackViewResultNoLogin = "1.0|236|d|complete||107/11/08 12:41:"
			+ "37||Leandro Goncalves||LVL0711091728|107/11/10 00"
			+ ":34:27|d|[UC15] Alteracao de pergunta combo dependente de s"
			+ "im/nao\n1.0|290|u21|complete||107/11/08 15:01:48||"
			+ "|root|LVL0711091728|107/11/10 00:34:34|f|UC 21\n1.0|239|d|"
			+ "complete||107/11/08 15:39:47||Leandro Goncalves||"
			+ "LVL0711091728|107/11/10 00:34:28|d|[UC15] Apos inclusao de "
			+ "dependente deve-se limpar a tela\n1.0|250|d|complete||107/1"
			+ "1/08 15:59:54||Leandro Goncalves||LVL0711091728|1"
			+ "07/11/10 00:34:28|d|[UC15] Campo \"Filtro Relatorio\" nao p"
			+ "ode ser editavel\n1.0|305|u*|complete||107/11/08 16:18:43|"
			+ "|Leandro Goncalves||LVL0711091728|107/11/10 00:34:"
			+ "35|f|Ajustes\n1.0|311|u24|complete||107/11/08 16:54:39|"
			+ "|Andrew Henrique da Rosa||LVL0711091728|107/11/10 00:34"
			+ ":35|f|UC24 - Coletar Auditoria - Incluir - Integragco e sto"
			+ "red procs.\n1.0|213|d|complete||107/11/08 17:29:24|"
			+ "|Leandro Goncalves||LVL0711091728|107/11/10 00:34:26|d|[U"
			+ "C15] Apresentar posicao da pergunta na lista de perguntas\n"
			+ "1.0|316|u29|complete||107/11/08 17:51:29|||root|LVL"
			+ "0711091728|107/11/10 00:34:36|f|UC 29 Solicitar Relatorios"
			+ "\n1.0|322|u*|complete||107/11/08 19:28:37||Leandr"
			+ "o Goncalves||LVL0711091728|107/11/10 00:34:36|f|RM - Render"
			+ "izar campo transplante\n1.0|323|u29|complete||107/11/09 08:"
			+ "03:31|||root|LVL0711142025|107/11/15 01:19:51|f|UC"
			+ " 29 Solicitar Relatorios\n1.0|331|u24|complete||107/11/09 "
			+ "14:34:22||Andrew Henrique da Rosa||LVL0711091728|10"
			+ "7/11/10 00:34:36|f|UC24 - Atualizagco de procs\n1.0|332|u*|"
			+ "complete||107/11/09 14:42:52||Fabio||LVL0711091728|10"
			+ "7/11/10 00:34:37|f|criacao de classes utilitarias\n1.0|333|"
			+ "u*|complete||107/11/09 15:13:00||Fabio||LVL0711091728"
			+ "|107/11/10 00:34:37|f|ajustes testes HIAE\n1.0|334|u*|compl"
			+ "ete||107/11/09 15:20:13||Fabio||LVL0711091728|107/11/"
			+ "10 00:34:37|f|ajustes build";

	@SuppressWarnings("unchecked")
	@Test
	public void parsingTheResultsOfExecutingReportInTrackView()
			throws Exception {

		List<CmvcChangeLog> result = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(testTrackViewResultNoLogin
						.getBytes("UTF-8")), "UTF-8"));
		try {
			result = CmvcRawParser.parseTrackViewReport(reader, new CmvcChangeLogSet(null));
		} finally {
			IOUtils.closeQuietly(reader);
		}

		assertEquals(14, result.size());

	}

	@Test
	public void parsingTheResultsOfExecutingReportInChangeView()
			throws Exception {
		String testChangeViewResult = "1.0|279|LVL0711091728|1.1|SRC/databas"
				+ "e/SGAU_SP_CONSULTA_FORMULARIOS_FILHOS.sql|create|u*|Procedu"
				+ "res|f|lgoncalves|Leandro Goncalves|\n"
				+ "1.0|279|LVL0711091728|1.1|SRC/database/SGAU_SP_CONSULTA_I"
				+ "D_PRIMEIRA_VERSAO_FORM.sql|create|u*|Procedures|f|lgoncalve"
				+ "s|Leandro Goncalves|\n1.0|280|LVL0711091728|1.33|SRC/Gerenc"
				+ "iamentoDeAuditoria/Formulario/Servicos/DALC/cFormularioDALC"
				+ ".cs|delta|u*|Ajustes |f|lgoncalves|Leandro Goncalves|\n1.0|"
				+ "261|LVL0711091728|1.1|SRC/GerenciamentoDeAuditoria/Formular"
				+ "io/Controlador/cPopuladorFiltroHIS.cs|create|u*|Renderizar "
				+ "campo HIS|f|gandrade|Guilherme|\n1.0|261|LVL0711091728|1.1|"
				+ "SRC/GerenciamentoDeAuditoria/Formulario/Controlador/cRemove"
				+ "RespostaHIS.cs|create|u*|Renderizar campo HIS|f|gandrade|Gu"
				+ "ilherme|\n1.0|261|LVL0711091728|1.1|SRC/GerenciamentoDeAudi"
				+ "toria/Formulario/Formulario/Coleta/cRespostaHIS.cs|create|"
				+ "u*|Renderizar campo HIS|f|gandrade|Guilherme|\n1.0|261|LVL0"
				+ "711091728|1.1|SRC/GerenciamentoDeAuditoria/Formulario/Ui/Wi"
				+ "dget/cHISWidget.cs|create|u*|Renderizar campo HIS|f|gandrad"
				+ "e|Guilherme|";

		List<ModifiedFile> changes = null;
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(
						testChangeViewResult.getBytes("UTF-8")), "UTF-8"));
		try {
			changes = CmvcRawParser.parseChangeViewReport(reader);
		} finally {
			IOUtils.closeQuietly(reader);
		}

		assertEquals(7, changes.size());
	}

	@Test
	public void testParseChangeViewReport() throws Throwable {
		List<CmvcChangeLog> result = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new ByteArrayInputStream(testTrackViewResultNoLogin
						.getBytes("UTF-8")), "UTF-8"));
		try {

			File tempFile = File.createTempFile("changes", "xml");
			tempFile.deleteOnExit();
			result = CmvcRawParser.parseTrackViewReport(reader, new CmvcChangeLogSet(null));
			StringWriter stringWriter = new StringWriter();

			CmvcChangeLogSet changeLogSet = new CmvcChangeLogSet(null, result);
			CmvcRawParser.writeChangeLogFile(changeLogSet, stringWriter);

			System.out.println(stringWriter.getBuffer().toString());

		} finally {
			IOUtils.closeQuietly(reader);
		}
	}

	@Test
	public void parsingChangeLogFile() throws Throwable {
		List<CmvcChangeLog> result = null;
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				ClassLoader.getSystemResourceAsStream("changelog.xml")));
		
		result = CmvcRawParser.parseChangeLogFile(reader);
		assertNotNull(result);
		assertEquals(4, result.size());
		
		assertEquals(1, result.get(0).getFiles().size());
		assertEquals(1, result.get(1).getFiles().size());
		assertEquals(1, result.get(2).getFiles().size());
		assertEquals(7, result.get(3).getFiles().size());
		
	}

}
