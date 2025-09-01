package com.bebopze.tdx.quant;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


/**
 * 通达信-量化交易
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
@SpringBootApplication(exclude = {DruidDataSourceAutoConfigure.class, DataSourceAutoConfiguration.class})
@EnableTransactionManagement
@EnableFeignClients("com.bebopze.tdx.quant.client")
@MapperScan("com.bebopze.tdx.quant.dal.mapper")
@EnableScheduling
@EnableCaching
@EnableRetry
// @EnableAdminServer
public class TdxQuantApp {


    public static void main(String[] args) {


        SpringApplication application = new SpringApplicationBuilder(TdxQuantApp.class)
                // .web(WebApplicationType.NONE)

                // 问题描述：在使用Robot来模拟键盘事件时，启动报错java.awt.AWTException: headless environment
                // https://blog.csdn.net/weixin_44216706/article/details/107138556
                // https://blog.csdn.net/qq_35607651/article/details/106055160
                .headless(false)

                .build(args);


        application.run(args);
    }


}