package com.bebopze.tdx.quant.common.util;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


/**
 * @author: bebopze
 * @date: 2025/5/4
 */
public class DateTimeUtil {


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

}