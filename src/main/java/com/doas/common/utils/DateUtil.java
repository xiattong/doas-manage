package com.doas.common.utils;

import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期处理工具类
 * @author xiattong
 */
public class DateUtil {

    private static final String FORMAT = "yyyyMMddHHmmss";

    /**
     * 时间格式(yyyy-MM-dd HH:mm:ss)
     */
    public final static String TIME_PATTERN = "HH:mm:ss";

    public static String formatTime(Date date) {
        return format(date, TIME_PATTERN);
    }

    /**
     * 自定义格式化日期
     * @param date
     * @param pattern
     * @return
     */
    public static String format(Date date, String pattern) {
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }

    /**
     * 默认式化日期（yyyy-MM-dd HH:mm:ss）
     * @param date
     * @return
     */
    public static String defaultFormat(Date date) {
        return format(date, FORMAT);
    }


    /**
     * 字符串转datetime
     * @param str
     * @return
     * @throws ParseException
     */
    public static Date parseDateTime(String str) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(FORMAT);
        Date parse = dateFormat.parse(str);
        return parse;
    }

    /**
     * 时间是否在指定时间段内
     * @param dateTime
     * @param startDate
     * @param endDate
     * @return
     */
    public static boolean isBetweenTime(String dateTime, String startDate, String endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_PATTERN);
        try {
            if (!StringUtils.isEmpty(startDate) && sdf.parse(dateTime).before(sdf.parse(startDate))) {
                return false;
            }
            return StringUtils.isEmpty(endDate) || sdf.parse(dateTime).before(sdf.parse(endDate));
        } catch (ParseException e) {
            return true;
        }
    }


    /**
     * 时间是否在指定时间段内
     * @param dateTime
     * @param timeRange
     * @return
     */
    public static boolean isBetweenDateTimeRange(String dateTime, String timeRange) {
        String[] timeRangeArray = timeRange.split("-");
        if (timeRangeArray.length != 2) {
            return true;
        }
        return isBetweenTime(dateTime, timeRangeArray[0].trim(), timeRangeArray[1].trim());
    }

    /**
     * 获取时间间隔，秒
     * @param startDate
     * @param endDate
     * @return
     */
    public static long diffSeconds(String startDate, String endDate) throws ParseException{
        long different = parseDateTime(endDate).getTime() - parseDateTime(startDate).getTime();
        return different / 1000;
    }

    /**
     * 获取时间间隔，分
     * @param startDate
     * @param endDate
     * @return
     */
    public static long diffMinutes(String startDate, String endDate) throws ParseException{
        return diffSeconds(startDate, endDate) / 60;
    }


}
