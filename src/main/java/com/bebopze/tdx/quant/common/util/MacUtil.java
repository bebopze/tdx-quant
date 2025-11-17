package com.bebopze.tdx.quant.common.util;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Mac 启动程序
 *
 * @author: bebopze
 * @date: 2025/11/16
 */
@Slf4j
public class MacUtil {


    public static void openChrome(String appName, String url) {
        openMacApp(appName, url);
        // openMacApp2(appName, url);
    }


    public static void openMacApp(String appName, String... args) {

        try {
            // 把 "open" "-a" appName 和 所有参数   拼成一条命令
            List<String> cmd = Lists.newArrayList();
            cmd.add("open");     //   /usr/bin/open
            cmd.add("-a");
            cmd.add(appName);
            if (args != null) {
                cmd.addAll(Arrays.asList(args));
            }


            // 打开App
            Process p = Runtime.getRuntime().exec(cmd.toArray(new String[0]));


            // 等待5s
            p.waitFor(5, TimeUnit.SECONDS);
            log.info("---------------------------- 打开App [{}]", appName);


        } catch (Exception e) {
            log.error("openMacApp - fail     >>>     appName : {} , errMsg : {}", appName, e.getMessage(), e);
        }
    }


    public static void openMacApp2(String appName, String... args) {

        try {
            // 把 "open" "-a" appName 和 所有参数   拼成一条命令
            List<String> cmd = Lists.newArrayList();
            cmd.add("open");     //   /usr/bin/open
            cmd.add("-a");
            cmd.add(appName);
            if (args != null) {
                cmd.addAll(Arrays.asList(args));
            }


            // 打开App
            Process start = new ProcessBuilder(cmd).start();


            // 等待5s
            start.waitFor(5, TimeUnit.SECONDS);
            log.info("---------------------------- 打开App [{}]", appName);


        } catch (Exception e) {
            log.error("openMacApp2 - fail     >>>     appName : {} , errMsg : {}", appName, e.getMessage(), e);
        }
    }

}