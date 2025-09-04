package com.bebopze.tdx.quant.common.util;

import lombok.extern.slf4j.Slf4j;


/**
 * sleep
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
@Slf4j
public class SleepUtils {


    public static void sleep(long millis) {
        log.warn("================================ sleep start     >>>     time : {}", DateTimeUtil.format2Hms(millis));


        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }


        log.info("================================ sleep end     >>>     time : {}", DateTimeUtil.format2Hms(millis));
    }


    /**
     * win系统 反应时间
     */
    public static void winSleep() {
        sleep(1000);
    }

    /**
     * win系统 反应时间
     *
     * @param millis
     */
    public static void winSleep(long millis) {
        sleep(millis);
    }


    public static void randomSleep(long millis) {
        long random = (long) (Math.random() * millis);
        sleep(random);
    }


}