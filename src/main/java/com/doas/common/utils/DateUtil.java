package com.doas.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期处理
 */
public class DateUtil {
    /**
     * 时间格式(yyyy-MM-dd)
     */
    public final static String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    /**
     * 时间格式(yyyy-MM-dd HH:mm:ss)
     */
    public final static String TIME_PATTERN = "HH:mm:ss";

    public static String formatTime(Date date) {
        return format(date, TIME_PATTERN);
    }

    public static String format(Date date, String pattern) {
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }
}
