package com.bebopze.tdx.quant.common.util;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


/**
 * 日期 / 时间
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
public class DateTimeUtil {


    private static final DateTimeFormatter yyyyMMdd__slash = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter yyyy_MM_dd = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    public static void main(String[] args) {
        // 示例毫秒值
        long milliseconds = 3661000;
        String formattedTime = formatMillis(milliseconds);
        // 输出: 01:01:01
        System.out.println(formattedTime);


        // 时间戳
        long timestamp = System.currentTimeMillis();
        millis2Time(timestamp);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 将毫秒值 格式化为 时分秒
     *
     * @param millis 毫秒值
     * @return 时分秒格式的字符串，例如 "01:02:03"
     */
    public static String formatMillis(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        DecimalFormat df = new DecimalFormat("00");
        return df.format(hours) + ":" + df.format(minutes) + ":" + df.format(seconds);
    }


    /**
     * 将 ms 自动格式化为 可读性强的时间字符串（如：1h 30m, 45s, 123ms）
     *
     * @param millis 毫秒数
     * @return 格式化后的时间字符串
     */
    public static String format2Hms(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }

        long seconds = millis / 1000;
        if (seconds < 60) {
            double secWithDecimals = Math.round(millis / 100.0) / 10.0; // 保留一位小数
            return secWithDecimals + "s";
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes < 60) {
            if (remainingSeconds == 0) {
                return minutes + "min";
            } else {
                return minutes + "min " + remainingSeconds + "s";
            }
        }

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (remainingMinutes == 0) {
            return hours + "h";
        } else {
            return hours + "h " + remainingMinutes + "min";
        }
    }


    /**
     * 时间戳 -> LocalDateTime
     *
     * @param timestamp 时间戳（ms）
     * @return
     */
    public static LocalDateTime millis2Time(long timestamp) {

        // 转换为Instant对象
        Instant instant = Instant.ofEpochMilli(timestamp);

        // 使用系统默认时区
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();

        return localDateTime;
    }


    public static LocalDate parseDate_yyyyMMdd__slash(String dateStr) {
        return LocalDate.parse(dateStr, yyyyMMdd__slash);
    }

    public static LocalDate parseDate_yyyyMMdd(String dateStr) {
        return LocalDate.parse(dateStr, yyyyMMdd);
    }

    public static LocalDate parseDate_yyyy_MM_dd(String dateStr) {
        return LocalDate.parse(dateStr, yyyy_MM_dd);
    }


    public static String format_yyyy_MM_dd(LocalDate date) {
        return date.format(yyyy_MM_dd);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static boolean inDateArr(LocalDate date, LocalDate[] dateArr) {
        return between(date, dateArr[0], dateArr[dateArr.length - 1]);
    }


    public static boolean between(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("起始和结束日期不能为空");
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }


    public static LocalDate min(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("日期不能为空");
        }
        return date1.isBefore(date2) ? date1 : date2;
    }


    public static LocalDate max(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("日期不能为空");
        }
        return date1.isAfter(date2) ? date1 : date2;
    }


}
