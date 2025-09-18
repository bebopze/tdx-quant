package com.bebopze.tdx.quant.common.config.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * DB 限流器       ->       DB  read/write 并发限流
 *
 * @author: bebopze
 * @date: 2025/9/18
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DBLimiter {


    /**
     * permits 许可证数量
     *
     * @return
     */
    int value() default 6;

}