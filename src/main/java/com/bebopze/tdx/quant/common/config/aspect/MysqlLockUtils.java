package com.bebopze.tdx.quant.common.config.aspect;

import com.bebopze.tdx.quant.common.util.MachineUtil;
import com.bebopze.tdx.quant.dal.entity.ConfDistributedLockDO;
import com.bebopze.tdx.quant.dal.mapper.ConfDistributedLockMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/**
 * Mysql分布式锁工具类
 *
 * @author bebopze
 * @date 2025/8/8
 */
@Slf4j
@Component
public class MysqlLockUtils {


    // 定时（自动续期）任务执行器
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    // 自动续期任务
    private final ConcurrentHashMap<String, ScheduledFuture<?>> renewTasks = new ConcurrentHashMap<>();

    // 本机唯一标识（每次JVM重启都会重新生成）
    private final String machineUniqueId = MachineUtil.generateMachineUniqueId();


    @Autowired
    private ConfDistributedLockMapper lockMapper;


    /**
     * 获取锁
     */
    public boolean lock(String lockKey, String lockValue, long expireSeconds) {
        long expireTimestamp = System.currentTimeMillis() + expireSeconds * 1000;

        try {
            int result = lockMapper.tryAcquireLock(lockKey, lockValue, expireSeconds, expireTimestamp, machineUniqueId);
            if (result > 0) {
                log.debug("获取分布式锁成功: key={}, value={}", lockKey, lockValue);
                return true;
            }
            return false;
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突，说明锁已被其他进程获取
            return false;
        } catch (DataAccessException e) {
            log.error("获取分布式锁失败: key={}, value={}", lockKey, lockValue, e);
            return false;
        }
    }


    /**
     * 释放锁
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        try {
            // 取消自动续期任务
            cancelRenewTask(lockKey);

            int result = lockMapper.releaseLock(lockKey, lockValue);
            if (result > 0) {
                log.debug("释放分布式锁成功: key={}, value={}", lockKey, lockValue);
                return true;
            }
            log.warn("释放分布式锁失败，锁不存在或已被其他进程释放: key={}, value={}", lockKey, lockValue);
            return false;
        } catch (Exception e) {
            log.error("释放分布式锁异常: key={}, value={}", lockKey, lockValue, e);
            return false;
        }
    }


    /**
     * 自动续期
     */
    public void startAutoRenew(String lockKey, String lockValue, long expireSeconds) {
        String taskKey = lockKey + ":" + lockValue;

        // 先取消已存在的任务
        cancelRenewTask(taskKey);


        if (scheduledExecutorService.isShutdown() || scheduledExecutorService.isTerminated()) {
            // log.error("分布式锁续期失败：线程池已被关闭！请检查 shutdownTask 是否被误触发。Key: {}", taskKey);
            throw new RuntimeException("分布式锁续期失败：线程池已被关闭！请检查 shutdownTask 是否被误触发。Key: " + taskKey);
        }


        // 创建新的续期任务
        ScheduledFuture<?> future = scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                long newExpireSeconds = expireSeconds;
                long newExpireTimestamp = System.currentTimeMillis() + newExpireSeconds * 1000;

                int result = lockMapper.renewLock(lockKey, lockValue, newExpireSeconds, newExpireTimestamp);
                if (result > 0) {
                    log.info("分布式锁自动续期成功: key={}, value={}", lockKey, lockValue);
                } else {
                    log.warn("分布式锁自动续期失败，锁可能已被释放: key={}, value={}", lockKey, lockValue);
                    // 续期失败，取消后续任务
                    cancelRenewTask(taskKey);
                }
            } catch (Exception e) {
                log.error("分布式锁自动续期异常: key={}, value={}", lockKey, lockValue, e);
            }
        }, expireSeconds / 2, expireSeconds / 2, TimeUnit.SECONDS);

        renewTasks.put(taskKey, future);
    }


    /**
     * 取消自动续期任务
     */
    public void cancelRenewTask(String lockKey) {
        ScheduledFuture<?> future = renewTasks.remove(lockKey);
        if (future != null) {
            future.cancel(true);
            log.info("已取消分布式锁自动续期任务: key={}", lockKey);
        }
    }


    /**
     * 获取锁，带重试机制
     */
    public boolean lockWithRetry(String lockKey,
                                 String lockValue,
                                 long expireSeconds,
                                 int maxRetries,
                                 long waitTimeMillis) throws InterruptedException {

        for (int i = 0; i <= maxRetries; i++) {
            if (lock(lockKey, lockValue, expireSeconds)) {
                return true;
            }
            if (i < maxRetries) {
                Thread.sleep(waitTimeMillis);
            }
        }
        return false;
    }


    /**
     * 清理过期锁（全局清理）
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredLocks() {
        long currentTime = System.currentTimeMillis();
        List<ConfDistributedLockDO> expiredLocks = lockMapper.selectExpiredLocks(currentTime);

        if (expiredLocks.isEmpty()) {
            return;
        }

        log.info("开始清理过期分布式锁，数量: {}", expiredLocks.size());

        try {
            List<Long> lockIds = expiredLocks.stream().map(ConfDistributedLockDO::getId).collect(Collectors.toList());
            int deletedCount = lockMapper.batchDeleteExpiredLocks(lockIds);
            log.info("清理过期分布式锁完成，清理数量: {}", deletedCount);
        } catch (Exception e) {
            log.error("清理过期分布式锁异常", e);
        }
    }


    /**
     * 清理本机 所有遗留[孤儿锁]分布式锁（应用启动时调用 -> 清理遗留资源）
     */
    @PostConstruct
    @Transactional(rollbackFor = Exception.class)
    public void cleanupOrphanedLocks() {
        log.info("开始清理本机 遗留分布式锁[孤儿锁]，本机唯一标识: {}", machineUniqueId);

        try {
            int cleanedCount = lockMapper.cleanLocalLocks(machineUniqueId);
            log.info("清理本机 遗留分布式锁[孤儿锁] 完成，清理数量: {}", cleanedCount);

        } catch (Exception e) {
            log.error("清理本机 遗留分布式锁[孤儿锁] 异常，本机唯一标识: {}", machineUniqueId, e);
        }
    }


    /**
     * 关闭资源
     */
    public void shutdown() {
        log.info("开始关闭分布式锁资源（定时[自动续期]任务执行器），本机唯一标识: {}", machineUniqueId);

        // 清理本机所有锁
        cleanLocalLocks();

        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    /**
     * 清理本机 所有锁
     */
    public void cleanLocalLocks() {
        log.info("开始清理本机 所有分布式锁，数量: {}, 本机唯一标识: {}", renewTasks.size(), machineUniqueId);

        renewTasks.forEach((key, future) -> {

            // String taskKey = lockKey + ":" + lockValue;
            String[] keyArr = key.split(":");
            if (keyArr.length >= 3) {
                String lockKey = keyArr[0];
                String lockValue = keyArr[2];

                // 释放锁
                releaseLock(lockKey, lockValue);
            }
        });
    }


}