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
    public Map<String, Long> stock__codeIdMap = Maps.newHashMap();
    public Map<String, String> stock__codeNameMap = Maps.newHashMap();
    public Map<String, Map<LocalDate, Double>> stock__dateCloseMap = Maps.newHashMap();


    /**
     * 板块
     */
    public List<BaseBlockDO> blockDOList;
    public Map<String, BaseBlockDO> codeBlockMap = Maps.newHashMap();
    public Map<String, Long> block__codeIdMap = Maps.newHashMap();
    public Map<String, String> block__codeNameMap = Maps.newHashMap();
    public Map<String, Map<LocalDate, Double>> block__dateCloseMap = Maps.newHashMap();


    /**
     * 个股 - 板块
     */
    public Map<Long, List<Long>> stockId_blockIdList_Map = Maps.newHashMap();


    /**
     * 板块 - 个股
     */
    public Map<Long, List<Long>> blockId_stockIdList_Map = Maps.newHashMap();

}
