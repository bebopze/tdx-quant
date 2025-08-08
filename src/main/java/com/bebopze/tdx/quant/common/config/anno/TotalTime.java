package com.bebopze.tdx.quant.common.config.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 统计 Func耗时
 *
 * @author: bebopze
 * @date: 2025/8/8
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TotalTime {

}