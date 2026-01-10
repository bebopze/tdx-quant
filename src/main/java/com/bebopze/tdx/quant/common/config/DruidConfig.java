package com.bebopze.tdx.quant.common.config;

import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Druid 监控配置
 *
 * 注意：ShardingSphere 5.5.x 中底层 DruidDataSource 的 stat 配置
 * 需要在 shardingsphere.yml 中直接配置，而不是运行时增强
 *
 * @author: bebopze
 * @date: 2025/8/26
 */
@Slf4j
@Configuration
public class DruidConfig {


    /**
     * 注册 Druid Servlet
     */
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.druid.stat-view-servlet.enabled", havingValue = "true")
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
        ServletRegistrationBean<StatViewServlet> registrationBean = new ServletRegistrationBean<>(
                new StatViewServlet(), "/druid/*");
        registrationBean.addInitParameter("allow", ""); // 允许所有IP访问
        registrationBean.addInitParameter("deny", ""); // 拒绝访问的IP
        registrationBean.addInitParameter("resetEnable", "false");
        return registrationBean;
    }


    /**
     * 注册 Druid Filter
     */
    @Bean
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