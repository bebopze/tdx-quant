package com.bebopze.tdx.quant.common.cache;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


/**
 * 回测 - 缓存数据
 *
 * @author: bebopze
 * @date: 2025/6/10
 */
@Slf4j
@Data
public class BacktestCache {


    /**
     * 交易日 - 基准
     */
    public Map<LocalDate, Integer> dateIndexMap = Maps.newHashMap();
    public List<LocalDate> dateList = Lists.newArrayList();


    /**
     * 个股
     */
    public List<BaseStockDO> stockDOList;
    public List<BaseStockDO> ETF_stockDOList;
    public Map<String, BaseStockDO> codeStockMap = Maps.newHashMap();
    public Map<Long, String> stock__idCodeMap = Maps.newHashMap();
    public Map<String, Long> stock__codeIdMap = Maps.newHashMap();
    public Map<String, String> stock__codeNameMap = Maps.newHashMap();
    public Map<String, Map<LocalDate, Double>> stock__dateCloseMap = Maps.newHashMap();


    /**
     * 板块
     */
    public List<BaseBlockDO> blockDOList;
    public Map<String, BaseBlockDO> codeBlockMap = Maps.newHashMap();
    public Map<Long, String> block__idCodeMap = Maps.newHashMap();
    public Map<String, Long> block__codeIdMap = Maps.newHashMap();
    public Map<String, String> block__codeNameMap = Maps.newHashMap();
    public Map<String, Map<LocalDate, Double>> block__dateCloseMap = Maps.newHashMap();


    /**
     * 个股 - 板块
     */
    public Map<String, Set<String>> stockCode_blockCodeSet_Map = Maps.newHashMap();
    // public Map<Long, List<Long>> stockId_blockIdList_Map = Maps.newHashMap();


    /**
     * 板块 - 个股
     */
    public Map<String, Set<String>> blockCode_stockCodeSet_Map = Maps.newHashMap();
    // public Map<Long, List<Long>> blockId_stockIdList_Map = Maps.newHashMap();


    /**
     * 板块 - 子板块       【2-普通行业 / 12-研究行业】
     */
    public Map<String, Set<String>> bk2_level1__blockCode_stockCodeSet_Map = Maps.newHashMap();
    public Map<String, Set<String>> bk12_level1__blockCode_stockCodeSet_Map = Maps.newHashMap();


    // -----------------------------------------------------------------------------------------------------------------


//    /**
//     * 仅适用 回测（每日 -> 复用1次）   ->     其他 一次性计算 一律禁用🚫（Java 内存管理 非常垃圾   =>   只要涉及大对象  ->  一律卡死）
//     */
//    public static final Map<String, StockFun> stockFunMap = Maps.newConcurrentMap();
//
//    public static final Map<String, BlockFun> blockFunMap = Maps.newConcurrentMap();


    // ====== 优化后的缓存 Caffeine ======


    public final Cache<String, StockFun> stockFunCache = Caffeine.newBuilder()
                                                                 .maximumSize(5_000)                               // 内存容量控制（可根据对象大小调整）
                                                                 .expireAfterWrite(10, TimeUnit.MINUTES)   // 写入后 10分钟过期（TTL）
                                                                 .expireAfterAccess(5, TimeUnit.MINUTES)   // 最近访问后 5分钟过期（TTI）
                                                                 .recordStats()                                    // 开启统计（命中率等）
                                                                 .removalListener(createRemovalListener())         // 可选：清理时回调
                                                                 .scheduler(Scheduler.systemScheduler())           // 使用系统调度器（更精准）
                                                                 .build();


    public final Cache<String, BlockFun> blockFunCache = Caffeine.newBuilder()
                                                                 .maximumSize(1_000)
                                                                 .expireAfterWrite(10, TimeUnit.MINUTES)
                                                                 .expireAfterAccess(5, TimeUnit.MINUTES)
                                                                 .recordStats()
                                                                 .removalListener(createRemovalListener())
                                                                 .scheduler(Scheduler.systemScheduler())
                                                                 .build();


    // 支持 computeIfAbsent 模式（带加载逻辑）
    public StockFun getOrCreateStockFun(String key, Function<String, StockFun> loader) {
        return stockFunCache.get(key, loader);
    }

    public BlockFun getOrCreateBlockFun(String key, Function<String, BlockFun> loader) {
        return blockFunCache.get(key, loader);
    }


    // 获取统计信息（可用于监控）
    public CacheStats getStockFunStats() {
        return stockFunCache.stats();
    }

    public CacheStats getBlockFunStats() {
        return blockFunCache.stats();
    }


    // ====== 可选：移除监听器（用于调试/监控）======
    private RemovalListener<String, StockFun> createRemovalListener() {

        return (key, value, cause) -> {
            // 可记录日志、监控、或资源释放
            CacheStats stats = getStockFunStats();
            log.info("Cache entry {} was removed due to {}     >>>     stats : {}", key, cause, JSON.toJSONString(stats));
        };
    }


//    // ====== 优化后的缓存 Guava ======
//
//
//    public static final com.google.common.cache.Cache<String, StockFun> stockFunCache2 =
//            com.google.common.cache.CacheBuilder.newBuilder()
//                                                .maximumSize(5_000)   // 可配置：最大个股数量
//                                                .expireAfterWrite(10, TimeUnit.MINUTES)
//                                                .expireAfterAccess(5, TimeUnit.MINUTES)   // 可配置：5分钟未访问即回收
//                                                .recordStats()
//                                                .removalListener(createRemovalListener2())
//                                                .build();
//
//    public static final com.google.common.cache.Cache<String, BlockFun> blockFunCache2 =
//            com.google.common.cache.CacheBuilder.newBuilder()
//                                                .maximumSize(1_000)
//                                                .expireAfterWrite(10, TimeUnit.MINUTES)
//                                                .expireAfterAccess(5, TimeUnit.MINUTES)
//                                                .recordStats()
//                                                .removalListener(createRemovalListener2())
//                                                .build();
//
//
//    private static com.google.common.cache.RemovalListener<String, StockFun> createRemovalListener2() {
//
//        return (notification) -> {
//
//            // 可记录日志、监控、或资源释放
//            log.info("Cache entry {} was removed due to {}", notification.getKey(), notification.getCause());
//        };
//    }


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public String toString() {
        // toString  ->  OOM
        return "BacktestCache{" +
                "dateIndexMap=" + dateIndexMap.size() +
                ", dateList=" + dateList.size() +
                ", stockDOList=" + (stockDOList == null ? 0 : stockDOList.size()) +
                ", ETF_stockDOList=" + (ETF_stockDOList == null ? 0 : ETF_stockDOList.size()) +
                ", codeStockMap=" + codeStockMap.size() +
                ", stock__idCodeMap=" + stock__idCodeMap.size() +
                ", stock__codeIdMap=" + stock__codeIdMap.size() +
                ", stock__codeNameMap=" + stock__codeNameMap.size() +
                ", stock__dateCloseMap=" + stock__dateCloseMap.size() +
                ", blockDOList=" + (blockDOList == null ? 0 : blockDOList.size()) +
                ", codeBlockMap=" + codeBlockMap.size() +
                ", block__idCodeMap=" + block__idCodeMap.size() +
                ", block__codeIdMap=" + block__codeIdMap.size() +
                ", block__codeNameMap=" + block__codeNameMap.size() +
                ", block__dateCloseMap=" + block__dateCloseMap.size() +
                ", stockCode_blockCodeSet_Map=" + stockCode_blockCodeSet_Map.size() +
                ", blockCode_stockCodeSet_Map=" + blockCode_stockCodeSet_Map.size() +
                '}';
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static boolean getByDate(boolean[] arr, Map<LocalDate, Integer> dateIndexMap, LocalDate tradeDate) {
        Integer idx = dateIndexMap.get(tradeDate);

        if (null == idx) {
            // 当前 交易日  ->  未上市/停牌
            return false;
        }

        return arr[idx];
    }


    public static double getByDate(double[] arr, Map<LocalDate, Integer> dateIndexMap, LocalDate tradeDate) {
        Integer idx = dateIndexMap.get(tradeDate);

        if (null == idx) {
            // 当前 交易日  ->  未上市/停牌
            return Double.NaN;
        }

        return arr[idx];
    }


    public static int getByDate(int[] arr, Map<LocalDate, Integer> dateIndexMap, LocalDate tradeDate) {
        Integer idx = dateIndexMap.get(tradeDate);

        if (null == idx) {
            // 当前 交易日  ->  未上市/停牌
            return 0;
        }

        return arr[idx];
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 1级 - 研究行业
     *
     * @param stockCode
     * @return
     */
    public String getYjhyLv1(String stockCode) {
        BaseBlockDO block = getBlock(stockCode, 12, 1);
        return null == block ? null : block.getCode() + "-" + block.getName();
    }

    /**
     * 2级 - 普通行业
     *
     * @param stockCode
     * @return
     */
    public String getPthyLv2(String stockCode) {
        BaseBlockDO block = getBlock(stockCode, 2, 2);
        return null == block ? null : block.getCode() + "-" + block.getName();
    }


    public BaseBlockDO getBlock(String stockCode, int type, int pLevel) {

        Set<String> blockCodeSet = stockCode_blockCodeSet_Map.getOrDefault(stockCode, Sets.newHashSet());

        return blockCodeSet.stream().map(blockCode -> {
                               BaseBlockDO pBlock = getPBlock(blockCode, pLevel);
                               return pBlock == null || pBlock.getType() != type ? null : pBlock;
                           })
                           .filter(Objects::nonNull)
                           .findFirst().orElse(null);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public BaseBlockDO getPBlock(String blockCode, int pLevel) {
        Assert.isTrue(1 <= pLevel && pLevel <= 3, String.format("[pLevel:%s]有误", pLevel));


        BaseBlockDO blockDO = codeBlockMap.get(blockCode);
        Assert.notNull(blockDO, String.format("blockCode:[%s]有误", blockCode));


//        if (blockDO.getType() == 4) {
//            return null;
//        }


        Integer level = blockDO.getLevel();
//        Assert.isTrue(blockDO.getType() == 4 || pLevel <= level,
//                      String.format("当前[blockCode:%s] 的 [level:%s] < [pLevel:%s]，", blockCode, level, pLevel));

        if (level == pLevel) {
            return blockDO;
        }


        Long parentId = blockDO.getParentId();
        if (null != parentId && parentId != 0) {
            String pCode = block__idCodeMap.get(parentId);
            BaseBlockDO pBlockDO = codeBlockMap.get(pCode);
            assert pBlockDO != null;

            if (pBlockDO.getLevel() == pLevel) {
                return pBlockDO;
            }

            return getPBlock(pBlockDO.getCode(), pLevel);
        }

        return null;
    }

    public BaseBlockDO getPBlock(String blockCode) {
        BaseBlockDO blockDO = codeBlockMap.get(blockCode);
        Assert.notNull(blockDO, String.format("blockCode:[%s]有误", blockCode));

        Long parentId = blockDO.getParentId();
        if (null != parentId) {
            String pCode = block__idCodeMap.get(parentId);
            return codeBlockMap.get(pCode);
        }

        return null;
    }

    public String getPBlockCode(String blockCode) {
        BaseBlockDO blockDO = codeBlockMap.get(blockCode);
        Assert.notNull(blockDO, String.format("blockCode:[%s]有误", blockCode));

        Long parentId = blockDO.getParentId();
        return parentId == null ? null : block__idCodeMap.get(parentId);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public LocalDate startDate() {
        return dateList.get(0);
    }

    public LocalDate endDate() {
        return dateList.get(dateList.size() - 1);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 手动 clear Cache     =>     Java大对象   ->   直接卡死（已优化 -> 支持 TTL）
     */
    public void clear() {
        stockFunCache.invalidateAll();
        blockFunCache.invalidateAll();
    }


}