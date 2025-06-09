package com.bebopze.tdx.quant.strategy.sell;


import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.google.common.collect.Maps;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * S策略
 *
 * @author: bebopze
 * @date: 2025/5/31
 */
public abstract class SellStrategy {


    /**
     * 交易日 - 基准
     */
    Map<String, Integer> dateIndexMap = Maps.newHashMap();


    List<BaseStockDO> stockDOList;
    Map<String, Map<LocalDate, Double>> stock__dateCloseMap = Maps.newHashMap();


    List<BaseBlockDO> blockDOList;
    Map<String, Map<LocalDate, Double>> block__dateCloseMap = Maps.newHashMap();


    void initData() {

    }


    /**
     * 不指定 -> 默认  全量个股（DB）
     *
     * @return -       符合 S策略 结果列表
     */
    List<String> rule() {
        return null;
    }


    /**
     * 指定 个股
     *
     * @param stockCode
     * @return -          是否符合 S策略
     */
    boolean rule(String stockCode) {
        return false;
    }

    /**
     * 指定 个股列表
     *
     * @param stockCodeList
     * @return -              符合 S策略 结果列表
     */
    List<String> rule(List<String> stockCodeList) {
        return null;
    }
}
