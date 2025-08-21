package com.bebopze.tdx.quant.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;


/**
 * yaml 配置 - 读取
 *
 * @author: bebopze
 * @date: 2025/5/12
 */
@Slf4j
public class PropsUtil {


    private static final Properties props = new Properties();

    static {
        // 加载 公共配置
        loadYamlIntoProps("application.yml");


        // 运行环境
        String activeProfile = props.getProperty("spring.profiles.active");


        // 加载 运行环境-配置
        if (StringUtils.isNotBlank(activeProfile)) {
            loadYamlIntoProps("application-" + activeProfile + ".yml");
        }
    }


    /**
     * 读取单个 YAML 文件，并将其全部键值放入 props 中（覆盖同名 key）
     *
     * @param yamlPath
     */
    private static void loadYamlIntoProps(String yamlPath) {
        Properties p = loadYamlProps(yamlPath);
        props.putAll(p);
    }


    /**
     * 从 ClassPath 读取 YAML 配置文件，转换成 Properties
     *
     * @param yamlPath
     * @return
     */
    private static Properties loadYamlProps(String yamlPath) {

        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource(yamlPath));


        Properties p = yamlFactory.getObject();
        if (p == null) {
            log.warn("未找到或无法解析 YAML：{}", yamlPath);
            return new Properties();
        }
        return p;
    }


    public static String getProperty(String key) {
        String value = props.getProperty(key);
        log.info("getProperty >>> key: {} , value: {}", key, value);
        return value;
    }


    /**
     * 通达信 - 根目录
     */
    public static String getTdxPath() {
        String val = getProperty("tdx-path");
        // MAC  安装  win虚拟机
        return SystemUtils.IS_OS_MAC ? val.replace("C:", "/Volumes/[C] Windows 11") : val;
    }

    public static String getSid() {
        return getProperty("eastmoney.validatekey");
    }

    public static String getCookie() {
        return getProperty("eastmoney.cookie");
    }


    public static void refreshEastmoneySession(String validatekey, String cookie) {
        props.setProperty("eastmoney.validatekey", validatekey);
        props.setProperty("eastmoney.cookie", cookie);
    }


    public static void main(String[] args) {
        String tdxPath = getTdxPath();
        getSid();
        getCookie();
    }

}