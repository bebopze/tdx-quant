package com.bebopze.tdx.quant.common.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.connection.ConnectionManager;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.driver.jdbc.core.datasource.ShardingSphereDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;


/**
 * Druid
 *
 * @author: bebopze
 * @date: 2025/8/26
 */
@Slf4j
@Configuration
public class DruidConfig {


    @Autowired
    private ApplicationContext applicationContext;


    @EventListener(ApplicationReadyEvent.class)
    public void enhanceDataSources() {
        log.info("应用完全启动，开始增强数据源配置");

        try {
            Thread.sleep(3000);

            // 获取 ShardingSphere 数据源
            Map<String, DataSource> dataSources = applicationContext.getBeansOfType(DataSource.class);
            DataSource shardingSphereDataSource = dataSources.get("shardingSphereDataSource");

            if (shardingSphereDataSource instanceof ShardingSphereDataSource) {
                log.info("找到 ShardingSphere 数据源: {}", shardingSphereDataSource.getClass().getName());

                // 获取连接
                ShardingSphereConnection connection = (ShardingSphereConnection) shardingSphereDataSource.getConnection();

                // 获取 ConnectionManager
                ConnectionManager connectionManager = connection.getConnectionManager();

                // 通过反射获取 dataSourceMap
                Field dataSourceMapField = ConnectionManager.class.getDeclaredField("dataSourceMap");
                dataSourceMapField.setAccessible(true);

                Map<String, DataSource> dataSourceMap = (Map<String, DataSource>) dataSourceMapField.get(connectionManager);

                log.info("找到 {} 个底层数据源", dataSourceMap.size());


                // 增强每个 DruidDataSource
                for (Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
                    String dsName = entry.getKey();
                    DataSource dataSource = entry.getValue();

                    if (dataSource instanceof DruidDataSource) {
                        log.info("增强底层数据源: {}", dsName);
                        enhanceSingleDruidDataSource(dsName, (DruidDataSource) dataSource);
                    }
                }
            }

            log.info("数据源增强完成");
        } catch (Exception e) {
            log.error("增强数据源失败", e);
        }
    }

    private void enhanceSingleDruidDataSource(String dsName, DruidDataSource dataSource) {
        try {
            if (dataSource.getProxyFilters().isEmpty()) {

                // wall,slf4j 与 ShardingSphere 冲突
                // dataSource.setFilters("stat,wall,slf4j");

                // stat（SQL统计） ->  可用
                dataSource.setFilters("stat");
                log.info("为数据源 {} 添加 Druid 过滤器成功", dsName);


                Properties properties = new Properties();
                properties.setProperty("druid.stat.mergeSql", "true");
                properties.setProperty("druid.stat.slowSqlMillis", "2000");
                dataSource.setConnectProperties(properties);

            } else {
                log.info("数据源 {} 过滤器已存在", dsName);
            }
        } catch (SQLException e) {
            log.error("为数据源 {} 添加过滤器失败", dsName, e);
        }
    }


    /**
     * 注册 Druid Servlet
     */
    @Bean
// @ConditionalOnProperty(name = "druid.stat-view-servlet.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "spring.datasource.druid.stat-view-servlet.enabled", havingValue = "true")
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
        ServletRegistrationBean<StatViewServlet> registrationBean = new ServletRegistrationBean<>(
                new StatViewServlet(), "/druid/*");
        registrationBean.addInitParameter("allow", ""); // 允许所有IP访问
        registrationBean.addInitParameter("deny", ""); // 拒绝访问的IP
        // registrationBean.addInitParameter("loginUsername", "admin");
        // registrationBean.addInitParameter("loginPassword", "admin");
        registrationBean.addInitParameter("resetEnable", "false");
        return registrationBean;
    }


    /**
     * 注册 Druid Filter
     */
    @Bean
// @ConditionalOnProperty(name = "druid.web-stat-filter.enabled", havingValue = "true")
    @ConditionalOnProperty(name = "spring.datasource.druid.web-stat-filter.enabled", havingValue = "true")
    public FilterRegistrationBean<WebStatFilter> druidWebStatFilter() {
        FilterRegistrationBean<WebStatFilter> registrationBean = new FilterRegistrationBean<>();

        WebStatFilter webStatFilter = new WebStatFilter();
        registrationBean.setFilter(webStatFilter);

        registrationBean.addUrlPatterns("/*");
        registrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        registrationBean.addInitParameter("sessionStatEnable", "true");
        registrationBean.addInitParameter("sessionStatMaxCount", "1000");
        registrationBean.addInitParameter("principalSessionName", "user");
        registrationBean.addInitParameter("principalCookieName", "user");
        registrationBean.addInitParameter("profileEnable", "true");

        return registrationBean;
    }


}