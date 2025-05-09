package com.bebopze.tdx.quant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


/**
 * 通达信-量化交易
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableFeignClients("com.bebopze.tdx.quant.client")
@MapperScan("com.bebopze.tdx.quant.dal.mapper")
@EnableScheduling
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


//        // 阻止程序启动后停止，如果应用内 存在@Scheduled注解的定时任务，则无需手动阻止程序停止
//        new Thread(() -> {
//            synchronized (TdxQuantApp.class) {
//                try {
//                    TdxQuantApp.class.wait();
//                } catch (Throwable e) {
//
//                }
//            }
//        }).start();
    }

}