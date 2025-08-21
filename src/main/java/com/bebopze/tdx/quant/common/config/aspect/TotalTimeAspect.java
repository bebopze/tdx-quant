package com.bebopze.tdx.quant.common.config.aspect;

import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


/**
 * - @TotalTime 拦截器       ->       统计 Func耗时
 *
 * @author: bebopze
 * @date: 2025/8/8
 */
@Slf4j
@Aspect
@Component
public class TotalTimeAspect {


    /**
     * 拦截 @TotalTime       ->       统计 Func耗时
     *
     * @param point
     */
    @Around("@annotation(com.bebopze.tdx.quant.common.config.anno.TotalTime)")
    public Object around(ProceedingJoinPoint point) throws Throwable {


        Method method = ((MethodSignature) point.getSignature()).getMethod();
        String serviceName = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();


        // TotalTime anno = method.getAnnotation(TotalTime.class);


        // -------------------------------------------------------------------------------------------------------------
        log.info("———————————————————————————————— {} - {}     >>>     start", serviceName, methodName);
        long start = System.currentTimeMillis();


        Object result = point.proceed();


        log.info("———————————————————————————————— {} - {}     >>>     end   -   totalTime : {}",
                 serviceName, methodName, DateTimeUtil.formatNow2Hms(start));
        // -------------------------------------------------------------------------------------------------------------


        return result;
    }


}
