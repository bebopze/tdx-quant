package com.bebopze.tdx.quant.common.config;

import com.bebopze.tdx.quant.common.config.convert.StringToLocalDateConverter;
import com.bebopze.tdx.quant.common.config.convert.StringToLocalDateTimeConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;


/**
 * web config
 *
 * @author: bebopze
 * @date: 2025/5/11
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {


    /**
     * 决定服务端 返回什么格式的响应内容（JSON、XML、HTML等）
     *
     * @param configurer
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                // 禁用基于参数的内容协商
                .favorParameter(false)
                // 忽略 Accept 头（强制 JSON）
                .ignoreAcceptHeader(true)
                // API 默认返回 JSON（response Header   ->   Content-Type: application/json）
                .defaultContentType(MediaType.APPLICATION_JSON);
    }


    /**
     * 静态资源映射配置：解决swagger-ui.html 404无法访问的问题
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 解决静态资源无法访问
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");

        // 解决swagger无法访问
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        // 解决swagger的js文件无法访问
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }


    @Override
    public void addFormatters(FormatterRegistry registry) {
        // web参数   日期格式
        registry.addConverter(new StringToLocalDateConverter("yyyy-MM-dd"));
        // web参数   时间格式
        registry.addConverter(new StringToLocalDateTimeConverter("yyyy-MM-dd HH:mm:ss"));
    }


    /**
     * 处理需要在 异步线程 中访问 请求上下文 的标准方法
     *
     * @return
     */
    @Bean
    public Filter confRequestContextFilter() { // confRequestContextFilter 使用一个不与自动配置冲突的名称
        // 返回类型可以是 Filter 或 RequestContextFilter
        return new RequestContextFilter();
    }


}