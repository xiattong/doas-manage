package com.doas.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * 日期处理工具类
 * @author xiattong
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

    /**
     * 时间是否在指定时间段内
     * @param dateTime
     * @param startDate
     * @param endDate
     * @return
     */
    public static boolean isBetweenDateTime(String dateTime, String startDate, String endDate) {
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

    public static void main(String[] args) {
        System.out.println(isBetweenDateTime("2:00:00", null, "3:00:00"));

        System.out.println(isBetweenDateTime("2:00:00", "1:00:00", null));

        System.out.println(isBetweenDateTime("2:00:00", null, null));

        System.out.println(isBetweenDateTime("2:00:00", "1:00:00", "3:00:00"));



        System.out.println(isBetweenDateTime("2:00:00", null, "1:00:00"));

        System.out.println(isBetweenDateTime("2:00:00", "3:00:00", null));

        System.out.println(isBetweenDateTime("2:00:00", "3:00:00", "1:00:00"));
    }
}
