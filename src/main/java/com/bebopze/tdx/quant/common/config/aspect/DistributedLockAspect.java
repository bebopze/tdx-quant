package com.bebopze.tdx.quant.common.config.aspect;

import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.config.anno.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;


/**
 * 分布式🔐
 *
 * @author bebopze
 * @date 2025/8/8
 */
@Slf4j
@Aspect
@Component
public class DistributedLockAspect {


    /**
     * 锁KEY前缀
     */
    private static final String LOCK_KEY_PREFIX = "lock:key:";


    private static final ExpressionParser parser = new SpelExpressionParser();


    @Autowired
    private MysqlLockUtils lockUtils;


    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint point, DistributedLock distributedLock) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getSimpleName();

        // 1. 构建锁KEY
        String lockKey = buildLockKey(point, method, distributedLock);
        String lockValue = generateLockValue();

        boolean lockAcquired = false;
        Object result = null;

        try {
            // 2. 获取锁
            lockAcquired = acquireLock(lockKey, lockValue, distributedLock);

            if (!lockAcquired) {
                log.warn("获取分布式锁失败，放弃执行: {}.{} key={}", className, methodName, lockKey);
                return handleLockFailure(method, distributedLock);
            }

            // 3. 启动自动续期（如果需要）
            if (distributedLock.autoRenew()) {
                lockUtils.startAutoRenew(lockKey, lockValue, distributedLock.value());
            }

            // 4. 执行目标方法
            stopWatch.stop();
            long lockWaitTime = stopWatch.getTotalTimeMillis();
            log.info("获取分布式锁成功，等待时间: {}ms, key={}", lockWaitTime, lockKey);

            stopWatch.start();
            result = point.proceed();
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            log.info("方法执行完成，耗时: {}ms, {}.{}", executionTime, className, methodName);

        } catch (Throwable ex) {
            throw ex;
        } finally {
            // 5. 释放锁
            if (lockAcquired) {
                try {
                    boolean released = lockUtils.releaseLock(lockKey, lockValue);
                    if (!released) {
                        log.warn("释放分布式锁失败，可能已被自动清理: key={}", lockKey);
                    }
                } catch (Exception ex) {
                    log.error("释放分布式锁异常: key={}", lockKey, ex);
                }
            }
        }

        return result;
    }

    /**
     * 构建锁KEY
     */
    private String buildLockKey(ProceedingJoinPoint point, Method method, DistributedLock annotation) {
        String keyPrefix = annotation.keyPrefix();

        // 如果指定了 keyPrefix
        if (StringUtils.isNotEmpty(keyPrefix)) {
            try {
                EvaluationContext context = new StandardEvaluationContext();
                String[] parameterNames = ((MethodSignature) point.getSignature()).getParameterNames();
                Object[] args = point.getArgs();

                // 将参数放入上下文，支持 #paramName 引用
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }

                // 尝试作为 SpEL 解析
                Object parsedValue = parser.parseExpression(keyPrefix).getValue(context);
                if (parsedValue != null) {
                    return LOCK_KEY_PREFIX + parsedValue;
                }
            } catch (Exception e) {
                // 【优化点】：解析失败时，不要直接丢弃，而是降级把它当做普通字符串处理
                // 只有当看起来像 SpEL (比如包含 #) 但解析失败时才打印 WARN，否则视为普通字符串
                if (keyPrefix.contains("#") || keyPrefix.contains("'")) {
                    log.warn("SpEL表达式解析异常，降级使用原始字符串作为Key: [{}]", keyPrefix, e);
                }
                return LOCK_KEY_PREFIX + keyPrefix;
            }
        }


        // 默认key生成策略
        String fullMethodName = getFullMethodName(method);
        int methodNameHashCode = fullMethodName.hashCode();
        Serializable sessionId = "default_session"; // 实际项目中替换为真实sessionID
        String keySuffix = method.getName() + ":" + sessionId + ":" + methodNameHashCode;

        return LOCK_KEY_PREFIX + keySuffix;
    }

    /**
     * 生成锁值（唯一标识）
     */
    private String generateLockValue() {
        long randomNum = ThreadLocalRandom.current().nextLong(1000000000L);
        return System.currentTimeMillis() + "_" + randomNum + "_" + Thread.currentThread().getId();
    }

    /**
     * 获取锁，支持重试
     */
    private boolean acquireLock(String lockKey,
                                String lockValue,
                                DistributedLock annotation) throws InterruptedException {

        DistributedLock.RetryStrategy retryStrategy = annotation.retryStrategy();
        int maxRetries = retryStrategy.maxRetries();
        long waitTime = retryStrategy.waitTime();
        long expireSeconds = annotation.value();

        if (maxRetries <= 0) {
            return lockUtils.lock(lockKey, lockValue, expireSeconds);
        }

        return lockUtils.lockWithRetry(lockKey, lockValue, expireSeconds, maxRetries, waitTime);
    }


    /**
     * 处理获取锁失败的情况
     */
    private Object handleLockFailure(Method method, DistributedLock annotation) throws Exception {
        // 企业级处理：可以抛出特定异常、返回默认值、记录监控等
        throw new BizException("获取分布式锁失败，业务方法被拒绝执行: " +
                                       method.getDeclaringClass().getSimpleName() + "." + method.getName());
    }


    /**
     * 获取方法的完整签名
     */
    private static String getFullMethodName(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getName())
          .append(".")
          .append(method.getName())
          .append("(");

        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(parameterTypes[i].getSimpleName());
        }
        sb.append(")");

        return sb.toString();
    }


    // ------------------------------------------- 定时任务 -------------------------------------------------------------


    /**
     * 每30秒清理一次过期锁
     */
    @Scheduled(fixedRate = 30000)
    public void cleanExpiredLocksTask() {
        try {
            log.debug("开始执行分布式锁清理任务...");
            long startTime = System.currentTimeMillis();

            lockUtils.cleanExpiredLocks();

            long costTime = System.currentTimeMillis() - startTime;
            log.debug("分布式锁清理任务完成，耗时: {}ms", costTime);
        } catch (Exception e) {
            log.error("执行分布式锁清理任务异常", e);
        }
    }


    /**
     * 应用关闭时清理资源
     */
    @PreDestroy
    public void shutdownTask() {
        lockUtils.shutdown();
    }


}