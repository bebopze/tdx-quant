package com.bebopze.tdx.quant.common.cache;

import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.github.benmanes.caffeine.cache.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


/**
 * 回测 - 全量行情Cache
 *
 * @author: bebopze
 * @date: 2025/6/10
 */
@Slf4j
@Data
public class BacktestCache {


    /**
     * 缓存 时间段
     */
    public LocalDate startDate;
    public LocalDate endDate;


    // -----------------------------------------------------------------------------------------------------------------


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


    public Map<String, Double> stock_zt__codePriceMap = Maps.newHashMap(); // 涨停价   -> 东财API
    public Map<String, Double> stock_dt__codePriceMap = Maps.newHashMap(); // 跌停价   -> 东财API
    public Map<String, Double> stock__codePriceMap = Maps.newHashMap();    // 实时行情 -> 东财API


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


    // ----------------------------- （Java 内存管理 非常“垃圾”（黑盒无感->极易失控）   =>   只要涉及大对象  ->  一律卡死）


    // 凡是系统   运行一段时间 直接卡死！   每次重启后 正常运行！     ->     一律为 GC bug！！！
    // 凡是系统   运行一段时间 直接卡死！   每次重启后 正常运行！     ->     一律为 GC bug！！！
    // 凡是系统   运行一段时间 直接卡死！   每次重启后 正常运行！     ->     一律为 GC bug！！！


    // --------- 内存托管 的代价     ->     黑箱操作！！！


    // 凡事涉及 Java大对象     ->     1、JVM启动参数 调优     8G -> 16G   ✅✅✅
    //
    //                                                   -Xms8g
    //                                                   -Xmx16g
    //                                                   -XX:+UseG1GC
    //                                                   -XX:MaxGCPauseMillis=100
    //                                                   -XX:+AlwaysPreTouch
    //                                                   -XX:InitiatingHeapOccupancyPercent=45
    //                                                   -XX:G1ReservePercent=20
    //                                                   -XX:MaxDirectMemorySize=6g
    //
    //
    //                              2、限制缓存   数量 + 时间（极速失效  ->  1-5分钟）
    //
    //                              3、一律禁用缓存！！！❌❌❌


    public static final Cache<String, StockFun> stockFunCache = Caffeine.newBuilder()
                                                                        .maximumSize(6_000)                                // 内存容量控制（可根据对象大小调整）
                                                                        // .expireAfterWrite(30, TimeUnit.MINUTES)         // 写入后 30分钟过期（TTL）
                                                                        .expireAfterAccess(5, TimeUnit.MINUTES)    // 最近访问后 5分钟过期（TTI）
                                                                        .recordStats()                                     // 开启统计（命中率等）
                                                                        .removalListener(createStatsRemovalListener("stockFunCache", () -> BacktestCache.stockFunCache)) // 可选：清理时回调
                                                                        .scheduler(Scheduler.systemScheduler())            // 使用系统调度器（更精准）
                                                                        .build();


    public static final Cache<String, BlockFun> blockFunCache = Caffeine.newBuilder()
                                                                        .maximumSize(1_000)
                                                                        // .expireAfterWrite(30, TimeUnit.MINUTES)
                                                                        .expireAfterAccess(5, TimeUnit.MINUTES)
                                                                        .recordStats()
                                                                        .removalListener(createStatsRemovalListener("blockFunCache", () -> BacktestCache.blockFunCache))
                                                                        // .removalListener((key, value, cause) -> log.info("Cache entry {} was removed due to {}     >>>     stats : {}", key, cause, BacktestCache.blockFunCache.stats()))
                                                                        .scheduler(Scheduler.systemScheduler())
                                                                        .build();


    public StockFun getOrCreateStockFun(String stockCode) {
        return getOrCreateStockFun(codeStockMap.get(stockCode));
    }

    public StockFun getOrCreateStockFun(BaseStockDO stockDO) {
        return stockFunCache.get(stockDO.getCode(), k -> new StockFun(stockDO));
    }


    public BlockFun getOrCreateBlockFun(String blockCode) {
        return getOrCreateBlockFun(codeBlockMap.get(blockCode));
    }

    public BlockFun getOrCreateBlockFun(BaseBlockDO blockDO) {
        return blockFunCache.get(blockDO.getCode(), k -> new BlockFun(k, blockDO));
    }


    // ====== 可选：移除监听器（用于调试/监控）======
    @NotNull
    private static <K, V> RemovalListener<K, V> createStatsRemovalListener(String cacheName,
                                                                           Supplier<Cache<K, V>> cacheSupplier) {

        // String _cacheName = cacheSupplier.get().getClass().getSimpleName();
        // Cache<K, V> kvCache = cacheSupplier.get();

        // 可记录日志、监控、或资源释放
        return (key, value, cause) -> log.info("{} entry [{}] was removed due to {}     >>>     stats : {}", cacheName, key, cause, cacheSupplier.get().stats());
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 大盘量化 Cache
     *
     * date - QaMarketMidCycleDO
     */
    public static final Cache<LocalDate, QaMarketMidCycleDO> marketCache = Caffeine.newBuilder()
                                                                                   .maximumSize(2_000)
                                                                                   // .expireAfterWrite(10, TimeUnit.MINUTES)
                                                                                   .expireAfterAccess(5, TimeUnit.MINUTES)
                                                                                   .recordStats()
                                                                                   .removalListener(createStatsRemovalListener("marketCache", () -> BacktestCache.marketCache))
                                                                                   .scheduler(Scheduler.systemScheduler())
                                                                                   .build();


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 主线板块 Cache
     *
     * date     /     TopBlockStrategyEnum - topBlockCodeSet
     */
    public static final Cache<LocalDate, Map<TopBlockStrategyEnum, Set<String>>> topBlockCache = Caffeine.newBuilder()
                                                                                                         .maximumSize(1_000)
                                                                                                         // .expireAfterWrite(10, TimeUnit.MINUTES)
                                                                                                         .expireAfterAccess(5, TimeUnit.MINUTES)
                                                                                                         .recordStats()
                                                                                                         .removalListener(createStatsRemovalListener("topBlockCache", () -> BacktestCache.topBlockCache))
                                                                                                         .scheduler(Scheduler.systemScheduler())
                                                                                                         .build();


    public static final Cache<String, Set<String>> stockCode_topBlockCache = Caffeine.newBuilder()
                                                                                     .maximumSize(5_000)
                                                                                     .expireAfterWrite(60, TimeUnit.MINUTES)
                                                                                     .expireAfterAccess(30, TimeUnit.MINUTES)
                                                                                     .recordStats()
                                                                                     .removalListener(createStatsRemovalListener("stockCode_topBlockCache", () -> BacktestCache.stockCode_topBlockCache))
                                                                                     .scheduler(Scheduler.systemScheduler())
                                                                                     .build();


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
    //                               获取 指定日期  ->  昨日 / 今日（指定日期）/ 明日   Kline
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 今日（指定日期）  Kline
     *
     * @param stockCode
     * @param tradeDate 指定日期
     * @return
     */
    public KlineDTO getStockKlineDTO(String stockCode, LocalDate tradeDate) {
        StockFun fun = getOrCreateStockFun(stockCode);

        Integer idx = fun.getDateIndexMap().get(tradeDate);
        return fun.getKlineDTOList().get(idx);
    }


    /**
     * 昨日（指定日期-prev）  Kline
     *
     * @param stockCode
     * @param tradeDate 指定日期
     * @return
     */
    public KlineDTO getPrevStockKlineDTO(String stockCode, LocalDate tradeDate) {
        StockFun fun = getOrCreateStockFun(stockCode);

        Integer idx = fun.getDateIndexMap().get(tradeDate);
        Assert.isTrue(idx != null && idx - 1 >= 0, "[idx=" + idx + "]异常");

        return fun.getKlineDTOList().get(idx - 1);
    }

    /**
     * 明日（指定日期-next）  Kline
     *
     * @param stockCode
     * @param tradeDate 指定日期
     * @return
     */
    public KlineDTO getNextStockKlineDTO(String stockCode, LocalDate tradeDate) {
        StockFun fun = getOrCreateStockFun(stockCode);

        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
        Integer idx = dateIndexMap.get(tradeDate);
        Assert.isTrue(idx != null && idx + 1 <= dateIndexMap.size() - 1, "[idx=" + idx + "]异常");

        return fun.getKlineDTOList().get(idx + 1);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                    大盘交易日 基准（非 Cache  ->  ❌startDate ~ endDate❌）
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