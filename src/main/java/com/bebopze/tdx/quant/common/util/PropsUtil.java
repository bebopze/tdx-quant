package com.bebopze.tdx.quant.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        String activeProfile = getProperty("spring.profiles.active");


        // 加载 运行环境-配置
        if (StringUtils.isNotBlank(activeProfile)) {
            loadYamlIntoProps("application-" + activeProfile + ".yml");
            loadYamlIntoProps("application-llm-" + activeProfile + ".yml");
            // 需 特殊处理（非正常格式）
            loadYamlIntoProps("shardingsphere-" + activeProfile + ".yml");
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


        Properties p;
        try {
            p = yamlFactory.getObject();
        } catch (Exception e) {
            log.warn("未找到或无法解析 YAML : {}，原因 : {}", yamlPath, e.getMessage());
            // 特殊处理（shardingsphere-prod.yml）
            p = loadLenient(yamlPath);
        }


        if (p == null) {
            log.warn("未找到或无法解析 YAML：{}", yamlPath);
            return new Properties();
        }
        return p;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 特殊处理（shardingsphere-prod.yml）：加载 YAML 全文内容  ->  去除 无法解析的部分
     */
    public static Properties loadLenient(String classpathYaml) {
        // 从 ClassPath 加载 YAML
        try (InputStream in = new ClassPathResource(classpathYaml).getInputStream()) {

            // 读取 YAML 全文内容
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // 去除 无法解析的部分
            String cleaned = stripTags(raw);


            // 解析 YAML 内容为 Properties
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(new ByteArrayResource(cleaned.getBytes(StandardCharsets.UTF_8)));


            Properties p = factory.getObject();
            return p == null ? new Properties() : p;
        } catch (Exception e) {
            log.warn("YAML 加载失败，已舍弃 : {}，原因 : {}", classpathYaml, e.getMessage());
            return new Properties();
        }
    }

    /**
     * 逐行剥掉独立成词的 YAML tag，保留其内容为普通节点
     *
     * <p> 示例：
     * <pre>
     *   - !SINGLE          →  -
     *     tables:               tables:
     *       - "*.*"               - "*.*"
     * </pre>
     */
    private static String stripTags(String yaml) {
        yaml = yaml.split("rules:")[0];
        return yaml;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static String getProperty(String key) {
        String value = System.getenv(key);
        value = StringUtils.isBlank(value) ? props.getProperty(key) : value;

//        if (isSensitiveKey(key)) {
//            log.info("getProperty     >>>     key : {} , configured : {}", key, StringUtils.isNotBlank(value));
//        } else {
        log.info("getProperty     >>>     key : {} , value : {}", key, value);
//        }

        return value;
    }

    private static boolean isSensitiveKey(String key) {
        return StringUtils.containsAnyIgnoreCase(key,
                                                 "username", "account", "password", "passwd", "pwd", "cookie",
                                                 "validatekey", "api-key", "apikey", "secret", "token");
    }


    /**
     * 通达信 - 根目录
     */
    public static String getTdxPath() {
        String val = getProperty("tdx-path");
        // MAC  安装  win虚拟机
        return SystemUtils.IS_OS_MAC ? val.replace("C:", "/Volumes/[C] Windows 11") : val;
        // return SystemUtils.IS_OS_MAC ? val.replace("C:", "smb://Windows 11._smb._tcp.local/[C] Windows 11") : val;
    }

    public static String getSid() {
        return getProperty("eastmoney.validatekey");
    }

    public static String getCookie() {
        return getProperty("eastmoney.cookie");
    }

    public static String getCookie2() {
        return getProperty("eastmoney.cookie2");
    }


    public static void refreshEastmoneySession(String validatekey, String cookie) {
        props.setProperty("eastmoney.validatekey", validatekey);
        props.setProperty("eastmoney.cookie", cookie);

        log.info("refreshEastmoneySession     >>>     validatekey : {} , cookie : {}", validatekey, cookie);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        String tdxPath = getTdxPath();
        getSid();
        getCookie();
    }


}