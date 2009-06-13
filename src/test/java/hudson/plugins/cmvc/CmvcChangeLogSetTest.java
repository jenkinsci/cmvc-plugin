package hudson.plugins.cmvc;

import static org.junit.Assert.assertEquals;
import hudson.plugins.cmvc.CmvcChangeLogSet.CmvcChangeLog;
import hudson.plugins.cmvc.util.CmvcRawParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;


/**
 * Test for Multi-Release support.
 * 
 * @author Dirceu
 *
 */
public class CmvcChangeLogSetTest {

	@Test
	public void testMultiReleases() throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(trackViewResult.getBytes("UTF-8"));
		InputStreamReader in = new InputStreamReader(bais, "UTF-8");
		BufferedReader reader = new BufferedReader(in);
		
		CmvcChangeLogSet changeLogSet = new CmvcChangeLogSet(null);
		try {
			List<CmvcChangeLog> logs = CmvcRawParser.parseTrackViewReport(reader, changeLogSet);
			changeLogSet.setLogs(logs);
		} finally {
			IOUtils.closeQuietly(reader);
		}

		int i = 0;
		String[] releases = releaseName.split(",");
		for (String release : releases) {
			release = release.trim();
			assertEquals(assertValues[i], changeLogSet.getTracksPerRelease(release).size());
			i++;
		}
	}
	
	private static final String trackViewResult = 
		"COMMON_200902|25289|u15|integrate||109/06/08 13:51:47||Matheus " +
		"Eduardo Ferrari|Development||109/06/11 17:11:25|f|[GASNAT] " +
		"Calcular Volume Corrigido.\n" +
		"GASNAT_200902|25318|u*|integrate||109/06/10 15:24:48||Dirceu " +
		"Silva|developer+||109/06/10 18:39:05|f|[BUILD] Utilizando " +
		"webserver da COMMON\n" +
		"BDEMQ_200902|25217|u*|integrate||109/06/02 13:47:50||Gabriel " +
		"Santos|developer+||109/06/08 08:06:53|f|[BUNDLE] Feature de " +
		"bundle\n" +
		"GASNAT_200902|25319|d|integrate||109/06/10 17:09:32||Heleno" +
		"|developer+||109/06/10 17:21:22|d|[GASNAT] Atributo dinamico " +
		"do componente error-message\n" +
		"BDEMQ_200902|25329|d|integrate||109/06/11 14:11:58||Matheus " +
		"Eduardo Ferrari|Development||109/06/11 17:15:54|d|[GASNAT] " +
		"Atualizar Medicao - falta de Link\n" +
		"GASNAT_200902|25338|d|integrate||109/06/11 15:56:01||Matheus " +
		"Eduardo Ferrari|Development||109/06/11 17:05:12|d|[GASNAT] " +
		"Atualizar Medicao - Erro de usabilidade\n" +
		"COMMON_200902|25332|d|integrate||109/06/11 09:44:33||Matheus " +
		"Eduardo Ferrari|Development||109/06/11 09:45:09|d|[GASNAT] " +
		"Atualizar Medicao - Botao errado\n" +
		"GASNAT_200902|25337|d|integrate||109/06/11 14:48:48||Matheus " +
		"Eduardo Ferrari|Development||109/06/11 15:13:43|d|[GASNAT] " +
		"Atualizar Medicao - Erro no Pdf\n" +
		"GASNAT_200902|25340|u*|integrate||109/06/11 08:50:59||Gabriel " +
		"Santos|developer+||109/06/11 17:01:42|f|[BUNDLE] Feature de " +
		"bundle\n" +
		"BDEMQ_200902|25229|u*|integrate||109/06/03 11:15:07||Thomas " +
		"Pietrafesa|developer+||109/06/11 17:25:46|f|[GASNAT] Atualizar " +
		"Status Medidor\n" +
		"BDEMQ_200902|25353|u*|integrate||109/06/11 17:32:15||Matheus " +
		"Eduardo Ferrari|Development||109/06/11 17:33:10|f|[GASNAT] " +
		"Code Style\n" +
		"COMMON_200902|25358|u*|integrate||109/06/11 19:03:48||Thomas " +
		"Pietrafesa|developer+||109/06/11 20:26:13|f|[GASNAT] Atualizar " +
		"Status Medidor";

	private static final String releaseName = "GASNAT_200902 ,COMMON_200902, BDEMQ_200902,NS34_200902";
	private static final int[] assertValues = {5,3,4,0};	
}


