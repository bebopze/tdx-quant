package com.bebopze.tdx.quant.common.config.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @author bebopze
 * @date 2025/8/8
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DistributedLock {


    /**
     * 锁超时时间（秒）
     */
    long value() default 10L;

    /**
     * 锁key前缀，支持SpEL表达式
     */
    String keyPrefix() default "";

    /**
     * 是否自动续期
     */
    boolean autoRenew() default false;

    /**
     * 续期间隔（秒），仅当autoRenew=true时生效
     */
    long renewInterval() default 5L;


    /**
     * 获取锁失败时的重试策略
     */
    RetryStrategy retryStrategy() default @RetryStrategy(
            maxRetries = 3,
            waitTime = 100
    );

    @interface RetryStrategy {
        int maxRetries() default 3;

        long waitTime() default 100; // 毫秒
    }

}