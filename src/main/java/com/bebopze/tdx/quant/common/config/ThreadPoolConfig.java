package com.bebopze.tdx.quant.common.config;

import lombok.*;
import lombok.experimental.NonFinal;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 线程池配置类（不可变）
 *
 * @author: bebopze
 * @date: 2025/8/11
 */
@Value
@Builder(toBuilder = true)
@NonFinal  // 允许在 Builder 中使用默认值（可选）
public class ThreadPoolConfig {


    /**
     * 线程池名称（用于线程命名）
     */
    String name;


    /**
     * 核心线程数
     */
    @Builder.Default
    int corePoolSize = Runtime.getRuntime().availableProcessors();


    /**
     * 最大线程数
     */
    @Builder.Default
    int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;


    /**
     * 空闲线程存活时间
     */
    @Builder.Default
    long keepAliveTime = 60L;


    /**
     * 时间单位
     */
    @Builder.Default
    TimeUnit unit = TimeUnit.SECONDS;


    /**
     * 队列容量
     */
    @Builder.Default
    int queueCapacity = 1000;


    /**
     * 拒绝策略
     */
    @Builder.Default
    RejectedExecutionHandler rejectedHandler = new ThreadPoolExecutor.CallerRunsPolicy();

}