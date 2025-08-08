package com.bebopze.tdx.quant.common.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * 并行计算工具类（工业级）
 * <p>
 * 特性：
 * - 自定义线程池（避免 ForkJoinPool 竞争）
 * - 支持分片批处理（chunk）
 * - 支持进度回调
 * - 支持超时控制
 * - 线程安全、异常安全
 *
 * @author bebopze
 * @date 2025/8/8
 */
@Slf4j
public class ParallelCalcUtil {


    // CPU 核心数
    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_PARALLELISM = Math.max(2, AVAILABLE_PROCESSORS);


    /**
     * 自定义线程池：避免占用 ForkJoinPool.commonPool()
     * 适用于 CPU 密集型任务（如量化计算、技术指标）
     */
    private static final ThreadPoolExecutor SHARED_POOL = new ThreadPoolExecutor(
            DEFAULT_PARALLELISM,
            DEFAULT_PARALLELISM,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            r -> new Thread(r, "ParallelCalcPool-" + Thread.currentThread().getId()),
            new ThreadPoolExecutor.CallerRunsPolicy() // 防止拒绝
    );


    // ======================= 基础并行 =======================


    /**
     * 并行处理集合（无返回值）
     */
    public static <T> void forEach(List<T> dataList, ThrowingConsumer<T> processor) {
        if (dataList == null || dataList.isEmpty()) return;

        List<CompletableFuture<Void>> futures = new ArrayList<>(dataList.size());
        for (T data : dataList) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    processor.accept(data);
                } catch (Exception e) {
                    log.error("ParallelCalcUtil.forEach error: {}", e.getMessage(), e);
                    throw new CompletionException(e);
                }
            }, SHARED_POOL);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }


    /**
     * 并行映射（有返回值，顺序一致）
     */
    public static <T, R> List<R> map(List<T> dataList, ThrowingFunction<T, R> mapper) {
        if (dataList == null || dataList.isEmpty()) return Collections.emptyList();

        @SuppressWarnings("unchecked")
        CompletableFuture<R>[] futures = new CompletableFuture[dataList.size()];
        for (int i = 0; i < dataList.size(); i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    return mapper.apply(dataList.get(index));
                } catch (Exception e) {
                    log.error("ParallelCalcUtil.map error index={}: {}", index, e.getMessage(), e);
                    throw new CompletionException(e);
                }
            }, SHARED_POOL);
        }
        return Arrays.stream(futures).map(CompletableFuture::join).collect(Collectors.toList());
    }


    // ======================= 分片并行 =======================


    /**
     * 分片并行处理（无返回值）
     */
    public static <T> void chunkForEach(List<T> dataList, int chunkSize, ThrowingConsumer<List<T>> processor) {
        if (dataList == null || dataList.isEmpty()) return;

        int total = dataList.size();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < total; i += chunkSize) {
            int end = Math.min(i + chunkSize, total);
            List<T> chunk = dataList.subList(i, end);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    processor.accept(chunk);
                } catch (Exception e) {
                    log.error("ParallelCalcUtil.chunkForEach error: {}", e.getMessage(), e);
                    throw new CompletionException(e);
                }
            }, SHARED_POOL);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }


    /**
     * 分片并行处理 + 进度回调
     */
    public static <T> void chunkForEachWithProgress(
            List<T> dataList,
            int chunkSize,
            ThrowingConsumer<List<T>> processor,
            ProgressCallback callback) {

        if (dataList == null || dataList.isEmpty()) return;

        int total = dataList.size();
        int numChunks = (total + chunkSize - 1) / chunkSize;
        AtomicInteger completedChunks = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < total; i += chunkSize) {
            int end = Math.min(i + chunkSize, total);
            List<T> chunk = dataList.subList(i, end);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    processor.accept(chunk);
                    int completed = completedChunks.incrementAndGet();
                    if (callback != null) {
                        callback.onProgress(completed, numChunks, "Completed " + completed + "/" + numChunks + " chunks");
                    }
                } catch (Exception e) {
                    log.error("ParallelCalcUtil.chunkForEachWithProgress error: {}", e.getMessage(), e);
                    throw new CompletionException(e);
                }
            }, SHARED_POOL);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }


    /**
     * 分片并行处理 + 超时控制
     */
    public static <T> void chunkForEachWithTimeout(
            List<T> dataList,
            int chunkSize,
            ThrowingConsumer<List<T>> processor,
            long timeout,
            TimeUnit unit) throws TimeoutException {

        if (dataList == null || dataList.isEmpty()) return;

        int total = dataList.size();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < total; i += chunkSize) {
            int end = Math.min(i + chunkSize, total);
            List<T> chunk = dataList.subList(i, end);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    processor.accept(chunk);
                } catch (Exception e) {
                    log.error("ParallelCalcUtil.chunkForEachWithTimeout error: {}", e.getMessage(), e);
                    throw new CompletionException(e);
                }
            }, SHARED_POOL);

            futures.add(future);
        }

        try {
            CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]))
                    .get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Task execution failed", e.getCause());
        } catch (TimeoutException e) {
            futures.forEach(f -> f.cancel(true)); // 尝试取消
            throw e;
        }
    }


    // ======================= 工具方法 =======================


    /**
     * 获取底层线程池（用于监控或关闭）
     */
    public static ExecutorService getPool() {
        return SHARED_POOL;
    }

    /**
     * 关闭线程池（应用退出时调用）
     */
    public static void shutdown() {
        SHARED_POOL.shutdown();
        try {
            if (!SHARED_POOL.awaitTermination(5, TimeUnit.SECONDS)) {
                SHARED_POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            SHARED_POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }


    // ======================= 函数式接口 =======================


    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    @FunctionalInterface
    public interface ProgressCallback {
        /**
         * 进度回调
         *
         * @param current 当前完成数
         * @param total   总数
         * @param message 自定义消息
         */
        void onProgress(int current, int total, String message);
    }


}