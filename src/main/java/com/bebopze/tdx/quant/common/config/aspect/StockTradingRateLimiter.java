package com.bebopze.tdx.quant.common.config.aspect;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


/**
 * A股量化交易限流器
 *
 *
 * 基于上交所和深交所监管规则：
 * -   每秒 同一证券 申报次数≥5次
 * - 每分钟 同一证券 申报次数≥15-20次
 *
 * -   每秒 全部证券 申报次数≥20次
 * - 每分钟 全部证券 申报次数≥1000次
 *
 * @author: bebopze
 * @date: 2025/9/12
 */
public class StockTradingRateLimiter {


    // 上交所重点监控情形：
    //
    //     1秒内对同一证券申报次数≥5次
    //     1分钟内对同一证券申报次数≥20次
    //
    //     连续5个交易日内撤单次数≥1000次
    //     撤单率≥60%且撤单次数≥300次


    // 深交所重点监控情形：
    //
    //     1秒内对同一证券申报次数≥5次
    //     1分钟内对同一证券申报次数≥15次
    //
    //     连续5个交易日内撤单次数≥800次
    //     撤单率≥50%且撤单次数≥200次


    // 全局监控：
    //
    //     1秒内对全部证券申报次数≥20次
    //     1分钟内对全部证券申报次数≥1000次


    // 监管规则配置
    private static final int MAX_REQUESTS_PER_SECOND_PER_STOCK = 4;  // 每秒单股票最大请求数（留有余量）
    private static final int MAX_REQUESTS_PER_MINUTE_PER_STOCK = 15; // 每分钟单股票最大请求数


    private static final int MAX_REQUESTS_PER_SECOND_GLOBAL = 20;    // 每秒全局最大请求数
    private static final int MAX_REQUESTS_PER_MINUTE_GLOBAL = 1000;  // 每分钟全局最大请求数


    // 时间窗口配置
    private static final long SECOND_WINDOW = 1000L;  // 1秒
    private static final long MINUTE_WINDOW = 60000L; // 1分钟


    // 为每只股票维护独立的限流器
    private static final ConcurrentHashMap<String, StockLimiter> stockLimiters = new ConcurrentHashMap<>();

    // 全局限流器
    private static final GlobalLimiter globalLimiter = new GlobalLimiter();


    /**
     * 股票限流器内部类
     */
    private static class StockLimiter {


        // 秒级计数器和时间戳
        private final AtomicInteger secondCounter = new AtomicInteger(0);
        private volatile long lastSecondReset = System.currentTimeMillis();

        // 分钟级计数器和时间戳
        private final AtomicInteger minuteCounter = new AtomicInteger(0);
        private volatile long lastMinuteReset = System.currentTimeMillis();

        private final ReentrantLock lock = new ReentrantLock();


        /**
         * 尝试获取交易许可
         *
         * @return true-允许交易，false-需要等待
         */
        public boolean tryAcquire() {
            long now = System.currentTimeMillis();


            lock.lock();


            try {

                // 检查并重置秒级计数器
                if (now - lastSecondReset >= SECOND_WINDOW) {
                    secondCounter.set(0);
                    lastSecondReset = now;
                }

                // 检查并重置分钟级计数器
                if (now - lastMinuteReset >= MINUTE_WINDOW) {
                    minuteCounter.set(0);
                    lastMinuteReset = now;
                }


                // 检查是否超过限制
                int currentSecondCount = secondCounter.get();
                int currentMinuteCount = minuteCounter.get();


                if (currentSecondCount >= MAX_REQUESTS_PER_SECOND_PER_STOCK ||
                        currentMinuteCount >= MAX_REQUESTS_PER_MINUTE_PER_STOCK) {
                    // 需要等待
                    return false;
                }


                // 增加计数器
                secondCounter.incrementAndGet();
                minuteCounter.incrementAndGet();


                // 允许交易
                return true;


            } finally {
                lock.unlock();
            }
        }


        /**
         * 获取当前秒级剩余配额
         */
        public int getSecondRemaining() {
            long now = System.currentTimeMillis();
            if (now - lastSecondReset >= SECOND_WINDOW) {
                return MAX_REQUESTS_PER_SECOND_PER_STOCK;
            }
            return Math.max(0, MAX_REQUESTS_PER_SECOND_PER_STOCK - secondCounter.get());
        }


        /**
         * 获取当前分钟级剩余配额
         */
        public int getMinuteRemaining() {
            long now = System.currentTimeMillis();
            if (now - lastMinuteReset >= MINUTE_WINDOW) {
                return MAX_REQUESTS_PER_MINUTE_PER_STOCK;
            }
            return Math.max(0, MAX_REQUESTS_PER_MINUTE_PER_STOCK - minuteCounter.get());
        }
    }


    /**
     * 全局限流器内部类
     */
    private static class GlobalLimiter {

        // 全局秒级计数器和时间戳
        private final AtomicInteger globalSecondCounter = new AtomicInteger(0);
        private volatile long lastGlobalSecondReset = System.currentTimeMillis();

        // 全局分钟级计数器和时间戳
        private final AtomicInteger globalMinuteCounter = new AtomicInteger(0);
        private volatile long lastGlobalMinuteReset = System.currentTimeMillis();

        private final ReentrantLock globalLock = new ReentrantLock();


        /**
         * 尝试获取全局交易许可
         *
         * @return true-允许交易，false-需要等待
         */
        public boolean tryAcquire() {
            long now = System.currentTimeMillis();

            globalLock.lock();

            try {
                // 检查并重置全局秒级计数器
                if (now - lastGlobalSecondReset >= SECOND_WINDOW) {
                    globalSecondCounter.set(0);
                    lastGlobalSecondReset = now;
                }

                // 检查并重置全局分钟级计数器
                if (now - lastGlobalMinuteReset >= MINUTE_WINDOW) {
                    globalMinuteCounter.set(0);
                    lastGlobalMinuteReset = now;
                }

                // 检查是否超过全局限制
                int currentGlobalSecondCount = globalSecondCounter.get();
                int currentGlobalMinuteCount = globalMinuteCounter.get();

                if (currentGlobalSecondCount >= MAX_REQUESTS_PER_SECOND_GLOBAL ||
                        currentGlobalMinuteCount >= MAX_REQUESTS_PER_MINUTE_GLOBAL) {
                    // 需要等待
                    return false;
                }

                // 增加全局计数器
                globalSecondCounter.incrementAndGet();
                globalMinuteCounter.incrementAndGet();

                // 允许交易
                return true;

            } finally {
                globalLock.unlock();
            }
        }


        /**
         * 获取当前全局秒级剩余配额
         */
        public int getGlobalSecondRemaining() {
            long now = System.currentTimeMillis();
            if (now - lastGlobalSecondReset >= SECOND_WINDOW) {
                return MAX_REQUESTS_PER_SECOND_GLOBAL;
            }
            return Math.max(0, MAX_REQUESTS_PER_SECOND_GLOBAL - globalSecondCounter.get());
        }


        /**
         * 获取当前全局分钟级剩余配额
         */
        public int getGlobalMinuteRemaining() {
            long now = System.currentTimeMillis();
            if (now - lastGlobalMinuteReset >= MINUTE_WINDOW) {
                return MAX_REQUESTS_PER_MINUTE_GLOBAL;
            }
            return Math.max(0, MAX_REQUESTS_PER_MINUTE_GLOBAL - globalMinuteCounter.get());
        }
    }


    /**
     * 获取指定股票的限流器
     */
    private StockLimiter getStockLimiter(String stockCode) {
        return stockLimiters.computeIfAbsent(stockCode, k -> new StockLimiter());
    }


    /**
     * 等待直到可以进行交易（阻塞等待）
     *
     * @param stockCode 股票代码
     * @throws InterruptedException 如果等待被中断
     */
    public void waitForPermit(String stockCode) throws InterruptedException {
        StockLimiter stockLimiter = getStockLimiter(stockCode);

        while (!stockLimiter.tryAcquire() || !globalLimiter.tryAcquire()) {
            // 计算需要等待的时间
            long now = System.currentTimeMillis();

            // 计算股票级别等待时间
            long stockWaitTime = Math.min(
                    SECOND_WINDOW - (now - stockLimiter.lastSecondReset),
                    MINUTE_WINDOW - (now - stockLimiter.lastMinuteReset)
            );

            // 计算全局级别等待时间
            long globalWaitTime = Math.min(
                    SECOND_WINDOW - (now - globalLimiter.lastGlobalSecondReset),
                    MINUTE_WINDOW - (now - globalLimiter.lastGlobalMinuteReset)
            );

            // 取较小的等待时间
            long waitTime = Math.min(stockWaitTime, globalWaitTime);

            // 等待一小段时间后重试（0.1s ~ 1s）
            Thread.sleep(Math.min(Math.max(waitTime / 4, 100), 1000));
        }
    }


    /**
     * 尝试在指定时间内获取交易许可
     *
     * @param stockCode 股票代码
     * @param timeout   超时时间（毫秒）
     * @return true-成功获取，false-超时
     * @throws InterruptedException 如果等待被中断
     */
    public boolean tryAcquireWithTimeout(String stockCode, long timeout) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        StockLimiter stockLimiter = getStockLimiter(stockCode);

        while (System.currentTimeMillis() - startTime < timeout) {
            if (stockLimiter.tryAcquire() && globalLimiter.tryAcquire()) {
                return true;
            }

            // 计算需要等待的时间
            long now = System.currentTimeMillis();

            // 计算股票级别等待时间
            long stockWaitTime = Math.min(
                    SECOND_WINDOW - (now - stockLimiter.lastSecondReset),
                    MINUTE_WINDOW - (now - stockLimiter.lastMinuteReset)
            );

            // 计算全局级别等待时间
            long globalWaitTime = Math.min(
                    SECOND_WINDOW - (now - globalLimiter.lastGlobalSecondReset),
                    MINUTE_WINDOW - (now - globalLimiter.lastGlobalMinuteReset)
            );

            // 取较小的等待时间
            long waitTime = Math.min(stockWaitTime, globalWaitTime);

            // 等待一小段时间后重试（0.1s ~ 1s）
            Thread.sleep(Math.min(Math.max(waitTime / 4, 50), 500));
        }

        return false;
    }


    /**
     * 检查是否可以立即交易（非阻塞）
     *
     * @param stockCode 股票代码
     * @return true-可以立即交易，false-需要等待
     */
    public boolean canTradeImmediately(String stockCode) {
        StockLimiter stockLimiter = getStockLimiter(stockCode);
        return stockLimiter.tryAcquire() && globalLimiter.tryAcquire();
    }


    /**
     * 获取剩余配额信息（包括股票级别和全局级别）
     */
    public RateInfo getRateInfo(String stockCode) {
        StockLimiter stockLimiter = getStockLimiter(stockCode);

        return new RateInfo(
                stockLimiter.getSecondRemaining(),
                stockLimiter.getMinuteRemaining(),
                globalLimiter.getGlobalSecondRemaining(),
                globalLimiter.getGlobalMinuteRemaining(),
                MAX_REQUESTS_PER_SECOND_PER_STOCK,
                MAX_REQUESTS_PER_MINUTE_PER_STOCK,
                MAX_REQUESTS_PER_SECOND_GLOBAL,
                MAX_REQUESTS_PER_MINUTE_GLOBAL
        );
    }


    /**
     * 速率信息数据类
     */
    @Data
    public static class RateInfo {
        // 股票级别
        private final int stockSecondRemaining;
        private final int stockMinuteRemaining;
        // 全局级别
        private final int globalSecondRemaining;
        private final int globalMinuteRemaining;

        // 最大限制
        private final int maxStockSecondRequests;
        private final int maxStockMinuteRequests;
        private final int maxGlobalSecondRequests;
        private final int maxGlobalMinuteRequests;

        public RateInfo(int stockSecondRemaining, int stockMinuteRemaining,
                        int globalSecondRemaining, int globalMinuteRemaining,
                        int maxStockSecondRequests, int maxStockMinuteRequests,
                        int maxGlobalSecondRequests, int maxGlobalMinuteRequests) {
            this.stockSecondRemaining = stockSecondRemaining;
            this.stockMinuteRemaining = stockMinuteRemaining;
            this.globalSecondRemaining = globalSecondRemaining;
            this.globalMinuteRemaining = globalMinuteRemaining;
            this.maxStockSecondRequests = maxStockSecondRequests;
            this.maxStockMinuteRequests = maxStockMinuteRequests;
            this.maxGlobalSecondRequests = maxGlobalSecondRequests;
            this.maxGlobalMinuteRequests = maxGlobalMinuteRequests;
        }


        public double getStockSecondUtilization() {
            return (double) (maxStockSecondRequests - stockSecondRemaining) / maxStockSecondRequests;
        }

        public double getStockMinuteUtilization() {
            return (double) (maxStockMinuteRequests - stockMinuteRemaining) / maxStockMinuteRequests;
        }

        public double getGlobalSecondUtilization() {
            return (double) (maxGlobalSecondRequests - globalSecondRemaining) / maxGlobalSecondRequests;
        }

        public double getGlobalMinuteUtilization() {
            return (double) (maxGlobalMinuteRequests - globalMinuteRemaining) / maxGlobalMinuteRequests;
        }


        @Override
        public String toString() {
            return String.format("RateInfo{" +
                                         "股票秒级剩余=%d/%d, 股票分钟级剩余=%d/%d, " +
                                         "全局秒级剩余=%d/%d, 全局分钟级剩余=%d/%d, " +
                                         "股票秒级使用率=%.2f%%, 股票分钟级使用率=%.2f%%, " +
                                         "全局秒级使用率=%.2f%%, 全局分钟级使用率=%.2f%%}",
                                 stockSecondRemaining, maxStockSecondRequests,
                                 stockMinuteRemaining, maxStockMinuteRequests,
                                 globalSecondRemaining, maxGlobalSecondRequests,
                                 globalMinuteRemaining, maxGlobalMinuteRequests,
                                 getStockSecondUtilization() * 100,
                                 getStockMinuteUtilization() * 100,
                                 getGlobalSecondUtilization() * 100,
                                 getGlobalMinuteUtilization() * 100);
        }
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 使用示例
     */
    public static void main(String[] args) {


        StockTradingRateLimiter limiter = new StockTradingRateLimiter();
        String stockCode = "600000"; // 浦发银行


        // 创建多个线程模拟高频交易
        ExecutorService executor = Executors.newFixedThreadPool(10);


        for (int i = 0; i < 30; i++) {

            final int taskId = i;
            final String currentStockCode = i % 3 == 0 ? "600000" : (i % 3 == 1 ? "000001" : "600036");

            executor.submit(() -> {
                try {

                    // 获取当前速率信息
                    StockTradingRateLimiter.RateInfo rateInfo = limiter.getRateInfo(currentStockCode);
                    System.out.println("任务" + taskId + "(" + currentStockCode + ") - " + rateInfo);


                    // 等待许可后执行交易
                    limiter.waitForPermit(currentStockCode);


                    System.out.println("任务" + taskId + "(" + currentStockCode + ") 在 " +
                                               java.time.LocalTime.now() + " 执行交易");


                    // 模拟交易处理时间
                    Thread.sleep(10);


                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("任务" + taskId + " 被中断");
                }
            });
        }


        executor.shutdown();


        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static class TradingExample {


        public static void main(String[] args) {

            StockTradingRateLimiter limiter = new StockTradingRateLimiter();


            String stockCode = "000001"; // 平安银行

            try {
                // 方式1：阻塞等待
                limiter.waitForPermit(stockCode);
                executeTrade(stockCode, "买入", 100);


                // 方式2：带超时的获取
                if (limiter.tryAcquireWithTimeout(stockCode, 5000)) {
                    executeTrade(stockCode, "卖出", 100);
                } else {
                    System.out.println("获取交易许可超时");
                }


                // 方式3：检查是否可以立即交易
                if (limiter.canTradeImmediately(stockCode)) {
                    executeTrade(stockCode, "买入", 200);
                } else {
                    System.out.println("当前无法立即交易，需要等待");
                    limiter.waitForPermit(stockCode);
                    executeTrade(stockCode, "买入", 200);
                }


                // 获取速率信息
                StockTradingRateLimiter.RateInfo info = limiter.getRateInfo(stockCode);
                System.out.println("当前速率信息: " + info);


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("交易被中断");
            }
        }


        private static void executeTrade(String stockCode, String action, int quantity) {
            System.out.println(java.time.LocalTime.now() +
                                       " 执行交易: " + stockCode + " " + action + " " + quantity + "股");
        }

    }


    // -----------------------------------------------------------------------------------------------------------------


}