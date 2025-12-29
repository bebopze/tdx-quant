package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * B策略   =   大盘（70%） +  主线板块（25%） +  个股买点（5%）
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
     *
     * -     买入策略   =   大盘（70%） +  主线板块（25%） +  个股买点（5%）
     *
     * @param data
     * @param tradeDate
     * @param buy_infoMap
     * @param posRate     当前 持仓占比
     * @return
     */


    /**
     * 根据 B策略     筛选出   ->   待买入 的 stockCodeList
     *
     * @param topBlockStrategyEnum 主线板块策略
     * @param buyConSet            买入条件列表
     * @param data                 回测数据缓存
     * @param tradeDate            交易日期
     * @param buy_infoMap          买点Info（ext_data）
     * @param posRate              当前 持仓占比
     * @param ztFlag               是否涨停：true-涨停（涨停个股Close 根本买不进去）；false-未涨停（Close 可正常买入）；null-不限；
     * @return
     */
    List<String> rule(TopBlockStrategyEnum topBlockStrategyEnum,
                      Set<String> buyConSet,
                      BacktestCache data,
                      LocalDate tradeDate,
                      Map<String, String> buy_infoMap,
                      double posRate,
                      Boolean ztFlag);
}