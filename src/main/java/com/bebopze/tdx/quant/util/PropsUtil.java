package com.bebopze.tdx.quant.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;


/**
 * @author: bebopze
 * @date: 2025/5/4
 */
@Slf4j
public class PropsUtil {


    private static Properties props = null;

    static {

        try {
            Properties root_props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
            String active_profile = root_props.getProperty("spring.profiles.active");

            props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-" + active_profile + ".properties"));

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }


    @SneakyThrows
    public static String getProperty(String key) {

//        Properties root_props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
//        String active_profile = root_props.getProperty("spring.profiles.active");
//
//
//        Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-" + active_profile + ".properties"));
        String value = props.getProperty(key);


        log.info("getProperty     >>>     key : {} , value : {}", key, value);
        return value;
    }


    /**
     * 通达信 - 根目录
     *
     * @return
     */
    public static String getTdxPath() {
        return getProperty("tdx-path");
    }


    public static String getSid() {
        return getProperty("eastmoney.validatekey");
    }

    public static String getCookie() {
        return getProperty("eastmoney.cookie");
    }


    public static void main(String[] args) {
        getTdxPath();
        getSid();
    }

}
