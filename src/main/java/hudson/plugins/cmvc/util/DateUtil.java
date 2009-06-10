package hudson.plugins.cmvc.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
    /** A working calendar. */
    
    private static final Calendar CALENDAR = Calendar.getInstance();
    
    public static final Date MIN_DATE;
    
    static {
    	CALENDAR.clear();
    	CALENDAR.set(CALENDAR.getMinimum(Calendar.YEAR),
    			CALENDAR.getMinimum(Calendar.MONTH),
    			CALENDAR.getMinimum(Calendar.DAY_OF_MONTH),
    			CALENDAR.getMinimum(Calendar.HOUR_OF_DAY),
    			CALENDAR.getMinimum(Calendar.MINUTE),
    			CALENDAR.getMinimum(Calendar.SECOND));
    	MIN_DATE = CALENDAR.getTime();
    }

    /**
     * Private constructor to prevent object creation.
     */
    private DateUtil() {
    }
    
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
    	if (lastBuild != null) {
    		CALENDAR.setTime(lastBuild);
    		String sufix = CALENDAR.get(Calendar.YEAR) < 2000 ? "0" : "1";
    		return "'" + sufix + SDF.format(lastBuild) + "'";
    	}
    	return null;
    }
}