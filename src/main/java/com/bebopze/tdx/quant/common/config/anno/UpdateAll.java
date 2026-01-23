package com.bebopze.tdx.quant.common.config.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * topBlock     =>     全量更新 OOM优化  ->  分段执行          // 专门用于  标记 topBlock 需要分段执行的方法
 *
 * @author: bebopze
 * @date: 2026/1/24
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UpdateAll {

}