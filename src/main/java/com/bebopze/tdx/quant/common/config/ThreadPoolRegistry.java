package com.bebopze.tdx.quant.common.config;

import com.bebopze.tdx.quant.common.constant.ThreadPoolType;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;


/**
 * 线程池注册中心（全局单例工具类）
 *
 * @author: bebopze
 * @date: 2025/8/11
 */
@Slf4j
@UtilityClass
public class ThreadPoolRegistry {


    private final Map<ThreadPoolType, ThreadPoolExecutor> POOL_MAP = new ConcurrentHashMap<>();

    // 初始化默认线程池
    static {
        initDefaultPools();
    }


    private void initDefaultPools() {

        int processors = Runtime.getRuntime().availableProcessors();


        registerPool(ThreadPoolType.CPU_INTENSIVE,
                     ThreadPoolConfig.builder()
                                     .name("CPU-Intensive-Pool")
                                     .corePoolSize(processors)
                                     .maxPoolSize(processors)
                                     .queueCapacity(200)
                                     .build());

        registerPool(ThreadPoolType.CPU_INTENSIVE_2,
                     ThreadPoolConfig.builder()
                                     .name("CPU-Intensive-Pool2")
                                     .corePoolSize(processors)
                                     .maxPoolSize(processors)
                                     .queueCapacity(200)
                                     .build());


        registerPool(ThreadPoolType.IO_INTENSIVE,
                     ThreadPoolConfig.builder()
                                     .name("IO-Intensive-Pool")
                                     .corePoolSize(processors * 30)
                                     .maxPoolSize(processors * 35)
                                     .queueCapacity(1000)
                                     .build());

        registerPool(ThreadPoolType.IO_INTENSIVE_2,
                     ThreadPoolConfig.builder()
                                     .name("IO-Intensive-Pool2")
                                     .corePoolSize(processors * 2)
                                     .maxPoolSize(processors * 4)
                                     .queueCapacity(1000)
                                     .build());


        registerPool(ThreadPoolType.DATABASE,
                     ThreadPoolConfig.builder()
                                     .name("DB-Pool")
                                     .corePoolSize(10)
                                     .maxPoolSize(20)
                                     .keepAliveTime(30)
                                     .unit(TimeUnit.SECONDS)
                                     .queueCapacity(500)
                                     .build());


        registerPool(ThreadPoolType.FILE_IO,
                     ThreadPoolConfig.builder()
                                     .name("File-IO-Pool")
                                     .corePoolSize(5)
                                     .maxPoolSize(10)
                                     .queueCapacity(200)
                                     .build());


        registerPool(ThreadPoolType.ASYNC_TASK,
                     ThreadPoolConfig.builder()
                                     .name("Async-Task-Pool")
                                     .corePoolSize(5)
                                     .maxPoolSize(10)
                                     .queueCapacity(500)
                                     .build());
    }


    /**
     * 注册线程池
     */
    public void registerPool(ThreadPoolType type, ThreadPoolConfig config) {

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                config.getCorePoolSize(),
                config.getMaxPoolSize(),
                config.getKeepAliveTime(),
                config.getUnit(),
                new LinkedBlockingQueue<>(config.getQueueCapacity()),
                new ThreadFactoryBuilder().setNameFormat(config.getName() + "-%d").build(),
                config.getRejectedHandler()
        );

        POOL_MAP.put(type, pool);
        log.info("✅ 线程池注册成功: {} -> core={}, max={}", type, config.getCorePoolSize(), config.getMaxPoolSize());
    }


    /**
     * 获取线程池
     */
    public ExecutorService getPool(ThreadPoolType type) {
        ThreadPoolExecutor pool = POOL_MAP.get(type);
        if (pool == null) {
            throw new IllegalArgumentException("线程池未注册: " + type);
        }
        return pool;
    }


    /**
     * 关闭所有线程池
     */
    public void shutdownAll() {
        POOL_MAP.forEach((type, pool) -> {

            log.info("CloseOperation: 正在关闭线程池 {}", type);
            pool.shutdown();

            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });

        POOL_MAP.clear();
        log.info("✅ 所有线程池已关闭");
    }


    /**
     * 获取线程池状态快照（用于监控）
     */
    public Map<String, String> getPoolStats() {
        Map<String, String> stats = new LinkedHashMap<>();

        POOL_MAP.forEach((type, pool) -> {
            stats.put(type.name() + ".ActiveCount", String.valueOf(pool.getActiveCount()));
            stats.put(type.name() + ".PoolSize", String.valueOf(pool.getPoolSize()));
            stats.put(type.name() + ".QueueSize", String.valueOf(pool.getQueue().size()));
            stats.put(type.name() + ".CompletedTaskCount", String.valueOf(pool.getCompletedTaskCount()));
        });

        return stats;
    }


}