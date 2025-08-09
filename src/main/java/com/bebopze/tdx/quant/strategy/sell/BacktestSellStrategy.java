package com.bebopze.tdx.quant.strategy.sell;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 回测 - S策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestSellStrategy implements SellStrategy {


    @Autowired
    private MarketService marketService;


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public String key() {
        return "A";
    }


    @Override
    public List<String> rule(BacktestCache data,
                             LocalDate tradeDate,
                             List<String> positionStockCodeList,
                             Map<String, String> sell_infoMap) {


        // -------------------------------------------------------------------------------------------------------------


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）


        List<String> sell__stockCodeList = positionStockCodeList.stream().filter(stockCode -> {
            BaseStockDO stockDO = data.codeStockMap.get(stockCode);


            StockFun fun = data.stockFunCache.get(stockCode, k -> new StockFun(k, stockDO));


            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 当日 - 停牌（600446  ->  2023-05-10）
            Integer idx = dateIndexMap.get(tradeDate);
            if (idx == null) {
                return false;
            }


            // -------------------------------------------


            // 是否淘汰
            // boolean flag_S = false;


            // -------------------------------------------


            // 月空
            boolean 月多 = extDataArrDTO.月多[idx];
            boolean MA20空 = extDataArrDTO.MA20空[idx];
            if (!月多 && MA20空) {
                sell_infoMap.put(stockCode, "月空" + ",idx-" + idx);
                sell_infoMap.put(stockCode, "MA20空" + ",idx-" + idx);
                return true;
            }


            // SSF空
            boolean SSF空 = extDataArrDTO.SSF空[idx];
            if (SSF空) {
                sell_infoMap.put(stockCode, "SSF空" + ",idx-" + idx);
                return true;
            }


            // 高位（中期涨幅_MA20 > 100）   ->   爆天量/长上影/大阴线
            boolean 高位爆量上影大阴 = extDataArrDTO.高位爆量上影大阴[idx];
            if (高位爆量上影大阴) {
                sell_infoMap.put(stockCode, "高位爆量上影大阴" + ",idx-" + idx);
                return true;
            }


            // 偏离率 > 25%
            double C_SSF_偏离率 = fun.C_SSF_偏离率(idx);
            if (C_SSF_偏离率 > 25) {
                sell_infoMap.put(stockCode, "C_SSF_偏离率>25%" + ",idx-" + idx);
                return true;
            }


            // TODO     最大 亏损线  ->  -7% 止损


            return false;

        }).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // 大盘底/大盘底部     ->     ETF策略（持有  ->  暂无视一切 B/S策略）
        sellStrategy_ETF(sell__stockCodeList, data, tradeDate, sell_infoMap);


        // -------------------------------------------------------------------------------------------------------------


        return sell__stockCodeList;
    }


    /**
     * 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）
     *
     * @param sell__stockCodeList
     * @param data
     * @param tradeDate
     * @param sellInfoMap
     */
    private void sellStrategy_ETF(List<String> sell__stockCodeList,
                                  BacktestCache data,
                                  LocalDate tradeDate,
                                  Map<String, String> sellInfoMap) {

        if (CollectionUtils.isEmpty(sell__stockCodeList)) {
            return;
        }


        // 大盘量化
        QaMarketMidCycleDO qaMarketMidCycleDO = marketService.marketInfo(tradeDate);
        Assert.notNull(qaMarketMidCycleDO, "[大盘量化]数据为空：" + tradeDate);


        // 大盘-牛熊：1-牛市；2-熊市；
        Integer marketBullBearStatus = qaMarketMidCycleDO.getMarketBullBearStatus();
        // 大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
        Integer marketMidStatus = qaMarketMidCycleDO.getMarketMidStatus();
        // MA50占比（%）
        double ma50Pct = qaMarketMidCycleDO.getMa50Pct().doubleValue();
        // 底_DAY
        Integer marketLowDay = qaMarketMidCycleDO.getMarketLowDay();
        // 个股月多-占比（%）
        double stockMonthBullPct = qaMarketMidCycleDO.getStockMonthBullPct().doubleValue();
        // 板块月多-占比（%）
        double blockMonthBullPct = qaMarketMidCycleDO.getBlockMonthBullPct().doubleValue();
        // 差值（新高-新低）
        int highLowDiff = qaMarketMidCycleDO.getHighLowDiff();
        // 右侧S-占比（%）
        double rightSellPct = qaMarketMidCycleDO.getRightSellPct().doubleValue();


        // -------------------------------------------------------------------------------------------------------------


        // 大盘底
        boolean con_1 = marketMidStatus == 1;


        // -----------------------------------------------
        boolean con_2_1 = false;
        boolean con_2_2 = false;


        // 1-牛市
        if (marketBullBearStatus == 1) {
            con_2_1 = highLowDiff < 0 || stockMonthBullPct < 10 || blockMonthBullPct < 3 || rightSellPct > 85;
        }
        // 2-熊市
        else if (marketBullBearStatus == 2) {
            con_2_2 = highLowDiff < -500 || stockMonthBullPct < 5 || blockMonthBullPct < 2 || rightSellPct > 90;
        }


        // 大盘底部
        boolean con_2 = marketMidStatus == 2 && (marketLowDay < 10 || ma50Pct < 25) && (con_2_1 || con_2_2);


        // -------------------------------------------------------------------------------------------------------------


        // 大盘底/大盘底部
        if (con_1 || con_2) {

            // ETF
            data.ETF_stockDOList.forEach(e -> {

                // 大盘底   ->   ETF   不卖出
                sell__stockCodeList.remove(e.getCode());
            });
        }
    }


}
