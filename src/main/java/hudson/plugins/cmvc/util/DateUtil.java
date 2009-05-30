package hudson.plugins.cmvc.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper methods for date convertion
 *  
 * @author <a href="mailto:fuechi@ciandt.com">Fábio Franco Uechi</a>
 *
 */
public class DateUtil {

    
    /** Date format expected by CMVC */
    private static final String CMVC_DATE_FORMAT_INOUT = "yy/MM/dd HH:mm:ss";

    private static final SimpleDateFormat SDF = new SimpleDateFormat(CMVC_DATE_FORMAT_INOUT);

    /**
     * @param cmvcDate
     * @return
     * @throws ParseException
     */
    public static Date convertFromCmvcDate(String cmvcDate) throws ParseException {
        if (cmvcDate != null && !"".equals(cmvcDate)) {
            return SDF.parse(cmvcDate.substring(1));
        }
        return null;
    }

    /**
     * @param lastBuild
     * @return
     */
    public static String convertToCmvcDate(Date lastBuild) {
        return "'1" + SDF.format(lastBuild) + "'";
    }

    
    
}
