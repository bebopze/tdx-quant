package com.bebopze.tdx.quant.common.config.aspect;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Semaphore;


/**
 * - @DBLimiter 拦截器       ->       DB  read/write 并发限流
 *
 * @author: bebopze
 * @date: 2025/9/18
 */
@Slf4j
@Aspect
@Component
public class DBLimiterAspect {


    /**
     * API（class|method|param）    -     Semaphore（N）
     *
     * 定义一个静态的信号量，用于限制 并发read/write 数据库的线程数
     */
    private static final Map<String, Semaphore> dbSemMap = Maps.newConcurrentMap();


    /**
     * 拦截 @DBLimiter       ->       DB  read/write 限流
     *
     * @param point
     */
    @Around("@annotation(com.bebopze.tdx.quant.common.config.anno.DBLimiter)")
    public Object around(ProceedingJoinPoint point) throws Throwable {


        Method method = ((MethodSignature) point.getSignature()).getMethod();

        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String paramName = Arrays.toString(method.getParameters());


        DBLimiter anno = method.getAnnotation(DBLimiter.class);
        int value = anno.value();


        // API（class|method|param）    -     Semaphore（N）
        Semaphore semaphore = dbSemMap.computeIfAbsent(className + "|" + methodName + "|" + paramName, k -> new Semaphore(value));


        // -------------------------------------------------------------------------------------------------------------


        try {
            // 尝试获取信号量许可，获取不到会阻塞等待
            semaphore.acquire();
            log.info("数据库许可[{}] - acquire     >>>     队列中等待的线程数 : {}", methodName, semaphore.getQueueLength());


            // 放行
            return point.proceed();


        } catch (InterruptedException e) {

            // 恢复中断状态
            Thread.currentThread().interrupt();


            String errMsg = String.format("数据库许可[%s] - 被中断     >>>     队列中等待的线程数 : %s", methodName, semaphore.getQueueLength());
            log.error(errMsg, e);


            throw new RuntimeException(errMsg, e);


        } catch (Exception ex) {


            String errMsg = String.format("DB操作[%s] - err     >>>     队列中等待的线程数 : %s", methodName, semaphore.getQueueLength());
            log.error(errMsg, ex);


            throw ex;


        } finally {

            // 确保在任何情况下都释放信号量许可
            semaphore.release();

            log.debug("数据库许可[{}] - release     >>>     队列中等待的线程数 : {}", methodName, semaphore.getQueueLength());
        }
    }


}