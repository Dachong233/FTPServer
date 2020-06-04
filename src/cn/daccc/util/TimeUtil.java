package cn.daccc.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public class TimeUtil {
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat FORMATTER_MODIFIED = new SimpleDateFormat("dd HH:ss");
    private static final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    public static String getTime() {
        Date date = new Date();
        return FORMATTER.format(date);
    }

    /**
     * 获取文件最后修改时间的格式字符串
     * May 21 12:02
     * @param time
     * @return
     */
    public static String getLastModifiedTime(long time) {
        Date date = new Date(time);
        Calendar.Builder builder = new Calendar.Builder();
        Calendar calendar = builder.setInstant(date).build();
        return months[calendar.get(Calendar.MONTH)] + " " + FORMATTER_MODIFIED.format(date);
    }
}
