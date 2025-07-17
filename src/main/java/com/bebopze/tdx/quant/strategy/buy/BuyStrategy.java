package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * B策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
public interface BuyStrategy {


    /**
     * 策略标识
     *
     * @return
     */
    String key();


    /**
     * 根据 B策略     筛选出   ->   待买入 的 stockCodeList
     *
     * @param data
     * @param tradeDate
     * @param buy_infoMap
     * @return
     */
    List<String> rule(BacktestCache data, LocalDate tradeDate, Map<String, String> buy_infoMap);

}