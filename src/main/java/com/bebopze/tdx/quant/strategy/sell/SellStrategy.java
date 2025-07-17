package com.bebopze.tdx.quant.strategy.sell;

import com.bebopze.tdx.quant.common.cache.BacktestCache;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * S策略
 *
 * @author: bebopze
 * @date: 2025/5/31
 */
public interface SellStrategy {


    /**
     * 策略 标识
     *
     * @return
     */
    String key();


    /**
     * 根据 S策略     筛选出   ->   待卖出 的 stockCodeList
     *
     * @param data
     * @param tradeDate
     * @param positionStockCodeList
     * @param sell_infoMap
     * @return
     */
    List<String> rule(BacktestCache data,
                      LocalDate tradeDate,
                      List<String> positionStockCodeList,

                      Map<String, String> sell_infoMap);

}