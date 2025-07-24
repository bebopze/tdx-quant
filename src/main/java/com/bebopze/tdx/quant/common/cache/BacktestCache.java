package com.bebopze.tdx.quant.common.cache;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;


/**
 * 回测 - 缓存数据
 *
 * @author: bebopze
 * @date: 2025/6/10
 */
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
    public Map<String, BaseStockDO> codeStockMap = Maps.newHashMap();
    public Map<Long, String> stock__idCodeMap = Maps.newHashMap();
    public Map<String, Long> stock__codeIdMap = Maps.newHashMap();
    public Map<String, String> stock__codeNameMap = Maps.newHashMap();
    public Map<String, Map<LocalDate, Double>> stock__dateCloseMap = Maps.newHashMap();


    /**
     * 板块
     */
    public static List<BaseBlockDO> blockDOList;
    public static Map<String, BaseBlockDO> codeBlockMap = Maps.newHashMap();
    public static Map<Long, String> block__idCodeMap = Maps.newHashMap();
    public static Map<String, Long> block__codeIdMap = Maps.newHashMap();
    public static Map<String, String> block__codeNameMap = Maps.newHashMap();
    public static Map<String, Map<LocalDate, Double>> block__dateCloseMap = Maps.newHashMap();


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


    public static final Map<String, StockFun> stockFunMap = Maps.newConcurrentMap();

    public static final Map<String, BlockFun> blockFunMap = Maps.newConcurrentMap();


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public String toString() {
        // toString  ->  OOM
        return "BacktestCache{" +
                "dateIndexMap=" + dateIndexMap.size() +
                ", dateList=" + dateList.size() +
                ", stockDOList=" + stockDOList.size() +
                ", codeStockMap=" + codeStockMap.size() +
                ", stock__idCodeMap=" + stock__idCodeMap.size() +
                ", stock__codeIdMap=" + stock__codeIdMap.size() +
                ", stock__codeNameMap=" + stock__codeNameMap.size() +
                ", stock__dateCloseMap=" + stock__dateCloseMap.size() +
                ", blockDOList=" + blockDOList.size() +
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

        Set<String> blockCodeSet = stockCode_blockCodeSet_Map.get(stockCode);

        return blockCodeSet.stream().map(blockCode -> {
                               BaseBlockDO pBlock = getPBlock(blockCode, pLevel);
                               return pBlock == null || pBlock.getType() != type ? null : pBlock;
                           })
                           .filter(Objects::nonNull)
                           .findFirst().orElse(null);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static BaseBlockDO getPBlock(String blockCode, int pLevel) {
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

    public static BaseBlockDO getPBlock(String blockCode) {
        BaseBlockDO blockDO = codeBlockMap.get(blockCode);
        Assert.notNull(blockDO, String.format("blockCode:[%s]有误", blockCode));

        Long parentId = blockDO.getParentId();
        if (null != parentId) {
            String pCode = block__idCodeMap.get(parentId);
            return codeBlockMap.get(pCode);
        }

        return null;
    }

    public static String getPBlockCode(String blockCode) {
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


}
