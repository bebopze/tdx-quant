package com.bebopze.tdx.quant.indicator;

import com.google.common.cache.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 缓存
 *
 * @author: bebopze
 * @date: 2025/5/23
 */
@Slf4j
public class StockCache {

    // 模拟数据源
    private static final AtomicInteger dataSourceCounter = new AtomicInteger(0);

    // 模拟从数据库加载数据
    private static double[] loadData(String key) throws InterruptedException {
        // 模拟耗时操作
        Thread.sleep(1000);
        // return "value_" + key + "_from_db_" + dataSourceCounter.incrementAndGet();
        return null;
    }

//    public static void get(String stockCode, int N) {
//
//
//        return STOCK_RPS_CACHE.get();
//    }


    // 1. 创建缓存实例
    public static final LoadingCache<String, double[]> STOCK_RPS_CACHE = CacheBuilder.newBuilder()
            // 设置初始容量（可选）
            .initialCapacity(10)
            // 设置最大容量（基于条目数量）
            .maximumSize(6000)
            // 设置写入后过期时间
            .expireAfterWrite(3, TimeUnit.HOURS)
            // 设置访问后过期时间
            .expireAfterAccess(5, TimeUnit.HOURS)
            // 设置刷新策略：写入后 x秒 刷新（异步）
            .refreshAfterWrite(1, TimeUnit.HOURS)
            // 启用缓存统计
            .recordStats()
            // 设置移除监听器（当缓存项被移除时触发）
            .removalListener(notification -> {
                log.info("缓存项被移除: Key={}, Value={}, 原因={}", notification.getKey(), notification.getValue(), notification.getCause());
            })
            // 构建缓存加载器
            .build(new CacheLoader<String, double[]>() {

                // 缓存未命中时调用
                @Override
                public double[] load(String key) throws Exception {
                    System.out.println("加载新数据: " + key);
                    return loadData(key);
                }

                // 异步刷新缓存（可选）
                @Override
                public ListenableFuture<double[]> reload(String key, double[] oldValue) throws Exception {
                    // 使用线程池异步加载
                    ListenableFutureTask<double[]> task = ListenableFutureTask.create(() -> {
                        System.out.println("异步刷新数据: " + key);
                        return loadData(key);
                    });
                    new ThreadPoolExecutor(1, 1,
                                           0L, TimeUnit.MILLISECONDS,
                                           new LinkedBlockingQueue<>())
                            .submit(task);
                    return task;
                }
            });


    public static void main(String[] args) throws Exception {


        // 2. 使用缓存
        String key = "300059";


        // 手动刷新缓存
        STOCK_RPS_CACHE.refresh(key);
        System.out.println("手动刷新后获取值: " + STOCK_RPS_CACHE.get(key));

        // 获取缓存统计信息
        CacheStats stats = STOCK_RPS_CACHE.stats();
        System.out.println("缓存命中次数: " + stats.hitCount());
        System.out.println("缓存未命中次数: " + stats.missCount());
        System.out.println("缓存加载时间（ms）: " + stats.totalLoadTime());
        System.out.println("缓存命中率: " + stats.hitRate());

        // 手动清除缓存
        STOCK_RPS_CACHE.invalidate(key);
        System.out.println("清除缓存后获取值: " + STOCK_RPS_CACHE.get(key));
    }

}