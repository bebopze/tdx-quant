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
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
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
        log.info("sleep : {}s", millis / 1000);
        sleep(millis);
    }


    public static void randomSleep(long millis) {
        long random = (long) (Math.random() * millis);
        sleep(random);
    }


}
