package com.bebopze.tdx.quant.common.util;

import java.text.DecimalFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


/**
 * 日期 / 时间
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
public class DateTimeUtil {


    private static final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter yyyy_MM_dd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter yyyyMMdd__slash = DateTimeFormatter.ofPattern("yyyy/MM/dd");


    private static final DateTimeFormatter HHmmss = DateTimeFormatter.ofPattern("HHmmss");
    public static final DateTimeFormatter HH_mm_ss = DateTimeFormatter.ofPattern("HH:mm:ss");


    private static final DateTimeFormatter yyyy_MM_dd_HHmmss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public static void main(String[] args) {
        // 示例毫秒值
        long milliseconds = 3661000;
        String formattedTime = formatMillis(milliseconds);
        // 输出: 01:01:01
        System.out.println(formattedTime);


        // 时间戳
        long timestamp = System.currentTimeMillis();
        millis2Time(timestamp);


        long diff = diff(LocalDate.now(), LocalDate.of(2025, 9, 1));
        System.out.println(diff);
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


    public static String formatNow2Hms(long start) {
        return format2Hms(System.currentTimeMillis() - start);
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


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  LocalDateTime
    // -----------------------------------------------------------------------------------------------------------------


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


    /**
     * HHmmss -> LocalTime
     *
     * @param timeStr
     * @return
     */
    public static LocalTime parseTime_HHmmss(String timeStr) {
        return LocalTime.parse(timeStr, HHmmss);
    }

    public static LocalTime parseTime__HH_mm_ss(String timeStr) {
        return LocalTime.parse(timeStr, HH_mm_ss);
    }


    public static LocalDateTime parseTime_yyyy_MM_dd(String dateStr) {
        return LocalDateTime.parse(dateStr, yyyy_MM_dd_HHmmss);
    }

    public static String formatTime_yyyy_MM_dd(LocalDateTime dateTime) {
        return dateTime.format(yyyy_MM_dd_HHmmss);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  LocalDate
    // -----------------------------------------------------------------------------------------------------------------


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

    public static String format_yyyyMMdd(LocalDate date) {
        return date.format(yyyyMMdd);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static boolean inDateArr(LocalDate date, LocalDate[] dateArr) {
        return between(date, dateArr[0], dateArr[dateArr.length - 1]);
    }


    /**
     * date   ∈   [start,end]
     *
     * @param date
     * @param start
     * @param end
     * @return
     */
    public static boolean between(LocalDate date, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("起始和结束日期不能为空");
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * time   ∈   [start,end]
     *
     * @param time
     * @param start
     * @param end
     * @return
     */
    public static boolean between(LocalTime time, LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("起始和结束时间不能为空");
        }
        return !time.isBefore(start) && !time.isAfter(end);
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


    /**
     * 天数差  =  date2 - date1
     *
     * @param date1
     * @param date2
     * @return
     */
    public static long diff(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("日期不能为空");
        }
        return ChronoUnit.DAYS.between(date1, date2);
    }


}