package com.bebopze.tdx.quant.common.cache;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


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
    public List<BaseBlockDO> blockDOList;
    public Map<String, BaseBlockDO> codeBlockMap = Maps.newHashMap();
    public Map<Long, String> block__idCodeMap = Maps.newHashMap();
    public Map<String, Long> block__codeIdMap = Maps.newHashMap();
    public Map<String, String> block__codeNameMap = Maps.newHashMap();
    public Map<String, Map<LocalDate, Double>> block__dateCloseMap = Maps.newHashMap();


    /**
     * 个股 - 板块
     */
    public Map<String, List<String>> stockCode_blockCodeList_Map = Maps.newHashMap();
    // public Map<Long, List<Long>> stockId_blockIdList_Map = Maps.newHashMap();


    /**
     * 板块 - 个股
     */
    public Map<String, List<String>> blockCode_stockCodeList_Map = Maps.newHashMap();
    // public Map<Long, List<Long>> blockId_stockIdList_Map = Maps.newHashMap();


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
                ", stockCode_blockCodeList_Map=" + stockCode_blockCodeList_Map.size() +
                ", blockCode_stockCodeList_Map=" + blockCode_stockCodeList_Map.size() +
                '}';
    }


}
