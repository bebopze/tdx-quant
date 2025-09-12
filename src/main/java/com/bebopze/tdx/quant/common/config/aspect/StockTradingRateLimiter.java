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
 * -     1秒内对同一证券申报次数≥5次
 * -     1分钟内对同一证券申报次数≥15-20次
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


    // 监管规则配置
    private static final int MAX_REQUESTS_PER_SECOND = 4;  // 每秒最大请求数（留有余量）
    private static final int MAX_REQUESTS_PER_MINUTE = 15; // 每分钟最大请求数


    // 时间窗口配置
    private static final long SECOND_WINDOW = 1000L;  // 1秒
    private static final long MINUTE_WINDOW = 60000L; // 1分钟


    // 为每只股票维护独立的限流器
    private static final ConcurrentHashMap<String, StockLimiter> stockLimiters = new ConcurrentHashMap<>();


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


                if (currentSecondCount >= MAX_REQUESTS_PER_SECOND || currentMinuteCount >= MAX_REQUESTS_PER_MINUTE) {
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
                return MAX_REQUESTS_PER_SECOND;
            }
            return Math.max(0, MAX_REQUESTS_PER_SECOND - secondCounter.get());
        }


        /**
         * 获取当前分钟级剩余配额
         */
        public int getMinuteRemaining() {
            long now = System.currentTimeMillis();
            if (now - lastMinuteReset >= MINUTE_WINDOW) {
                return MAX_REQUESTS_PER_MINUTE;
            }
            return Math.max(0, MAX_REQUESTS_PER_MINUTE - minuteCounter.get());
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
        StockLimiter limiter = getStockLimiter(stockCode);

        while (!limiter.tryAcquire()) {
            // 计算需要等待的时间
            long now = System.currentTimeMillis();
            long waitTime = Math.min(
                    SECOND_WINDOW - (now - limiter.lastSecondReset),
                    MINUTE_WINDOW - (now - limiter.lastMinuteReset)
            );

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


        StockLimiter limiter = getStockLimiter(stockCode);

        while (System.currentTimeMillis() - startTime < timeout) {
            if (limiter.tryAcquire()) {
                return true;
            }

            // 计算需要等待的时间
            long now = System.currentTimeMillis();
            long waitTime = Math.min(
                    SECOND_WINDOW - (now - limiter.lastSecondReset),
                    MINUTE_WINDOW - (now - limiter.lastMinuteReset)
            );

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
        return getStockLimiter(stockCode).tryAcquire();
    }


    /**
     * 获取剩余配额信息
     */
    public RateInfo getRateInfo(String stockCode) {
        StockLimiter limiter = getStockLimiter(stockCode);
        return new RateInfo(
                limiter.getSecondRemaining(),
                limiter.getMinuteRemaining(),
                MAX_REQUESTS_PER_SECOND,
                MAX_REQUESTS_PER_MINUTE
        );
    }


    /**
     * 速率信息数据类
     */
    @Data
    public static class RateInfo {
        private final int secondRemaining;
        private final int minuteRemaining;
        private final int maxSecondRequests;
        private final int maxMinuteRequests;

        public RateInfo(int secondRemaining, int minuteRemaining, int maxSecondRequests, int maxMinuteRequests) {
            this.secondRemaining = secondRemaining;
            this.minuteRemaining = minuteRemaining;
            this.maxSecondRequests = maxSecondRequests;
            this.maxMinuteRequests = maxMinuteRequests;
        }


        public double getSecondUtilization() {
            return (double) (maxSecondRequests - secondRemaining) / maxSecondRequests;
        }

        public double getMinuteUtilization() {
            return (double) (maxMinuteRequests - minuteRemaining) / maxMinuteRequests;
        }


        @Override
        public String toString() {
            return String.format("RateInfo{秒级剩余=%d/%d, 分钟级剩余=%d/%d, 秒级使用率=%.2f%%, 分钟级使用率=%.2f%%}",
                                 secondRemaining, maxSecondRequests,
                                 minuteRemaining, maxMinuteRequests,
                                 getSecondUtilization() * 100,
                                 getMinuteUtilization() * 100);
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

            executor.submit(() -> {
                try {

                    // 获取当前速率信息
                    StockTradingRateLimiter.RateInfo rateInfo = limiter.getRateInfo(stockCode);
                    System.out.println("任务" + taskId + " - " + rateInfo);


                    // 等待许可后执行交易
                    limiter.waitForPermit(stockCode);


                    System.out.println("任务" + taskId + " 在 " + java.time.LocalTime.now() + " 执行交易");


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