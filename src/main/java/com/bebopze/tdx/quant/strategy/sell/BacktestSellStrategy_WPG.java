package com.bebopze.tdx.quant.strategy.sell;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.SellStrategyEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestCompareDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategy_WPG;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.*;


/**
 * 回测 - S策略 -> 微盘股（每天取AB股 总市值排名 后400 的上市公司，剔除掉ST、*ST、暂停上市、新股未开板及北交所的股票）
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestSellStrategy_WPG implements SellStrategy {


    @Autowired
    private BacktestBuyStrategy_WPG backtestBuyStrategy_WPG;

    @Autowired
    private BacktestSellStrategy backtestSellStrategy;


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public String key() {
        return "微盘股";
    }


    @TotalTime
    @Override
    public Set<String> rule(TopBlockStrategyEnum topBlockStrategyEnum,
                            BacktestCache data,
                            LocalDate tradeDate,
                            Set<String> positionStockCodeSet,
                            Map<String, SellStrategyEnum> sell_infoMap,
                            BacktestCompareDTO btCompareDTO) {


        // -------------------------------------------------------------------------------------------------------------
        //                                                2、主线板块
        // -------------------------------------------------------------------------------------------------------------


//        long start_1 = System.currentTimeMillis();
//        Set<String> topN__topBlockCodeSet = topBlockStrategy.topBlock(topBlockStrategyEnum, data, tradeDate, false);
//        log.info("BacktestSellStrategy - topBlock     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_1));


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）


        long start_2 = System.currentTimeMillis();
        Set<String> sell__stockCodeSet = sell__stockCodeSet(positionStockCodeSet, topBlockStrategyEnum, data, tradeDate, sell_infoMap, btCompareDTO);
        log.info("BacktestSellStrategy_WPG - sell__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> NOT IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // TODO   ->   重复校验   =>   sell__stockCodeSet 中每个 个股已经执行过   ->   S_topBlock （已包含  stockCode  ->  NOT IN 主线板块）

//        // 取反     =>     前期 强势个股   ->   not IN 主线板块
//        long start_3 = System.currentTimeMillis();
//        Set<String> notInTopBlock__stockCodeSet = notInBaseTopBlock__stockCodeSet(topBlockStrategyEnum/*, topN__topBlockCodeSet*/, positionStockCodeSet, data, tradeDate);
//        log.info("BacktestSellStrategy - notInBaseTopBlock__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));
//
//
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        // merge   ->   个股S + 板块S
//        merge___sell__stockCodeSet(sell__stockCodeSet, notInTopBlock__stockCodeSet, sell_infoMap);


        // -------------------------------------------------------------------------------------------------------------


        // 大盘底/大盘底部     ->     ETF策略（持有  ->  暂无视一切 B/S策略）
        backtestSellStrategy.sellStrategy_ETF(sell__stockCodeSet, data, tradeDate);


        // -------------------------------------------------------------------------------------------------------------


        return sell__stockCodeSet;
    }


    private Set<String> sell__stockCodeSet(Set<String> positionStockCodeSet,
                                           TopBlockStrategyEnum topBlockStrategyEnum,
                                           BacktestCache data,
                                           LocalDate tradeDate,
                                           Map<String, SellStrategyEnum> sell_infoMap,
                                           BacktestCompareDTO btCompareDTO) {


        // -------------------------------------------------------------------------------------------------------------


//        // 持仓成本（avgCostPrice）
//        Map<String, Double> posCostMap = x.get().getPositionRecordDOList().stream()
//                                          .collect(Collectors.toMap(BtPositionRecordDO::getStockCode, e -> e.getAvgCostPrice().doubleValue()));


        // -------------------------------------------------------------------------------------------------------------


        // S  ->  当日 非 微盘股（当日 总市值排名  ->  非 倒数400）
        Set<String> tradeDate__wpgCodeSet = backtestBuyStrategy_WPG.buy__topStock__codeSet(Set.of(), data, tradeDate, Maps.newHashMap(), btCompareDTO.getZtFlag());


        // -------------------------------------------------------------------------------------------------------------


        Set<String> sell__stockCodeSet = positionStockCodeSet.stream().filter(stockCode -> {
            BaseStockDO stockDO = data.codeStockMap.get(stockCode);


            if (stockDO == null) {
                return false;
            }


            StockFun fun = data.getOrCreateStockFun(stockDO);


            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 当日 - 停牌（600446  ->  2023-05-10）
            Integer idx = dateIndexMap.get(tradeDate);
            if (idx == null) {
                return false;
            }


            // ---------------------------------------------------------------------------------------------------------


            // S  ->  当日 非 微盘股（当日 总市值排名  ->  非 倒数400）
            boolean signal_S = !tradeDate__wpgCodeSet.contains(stockCode);


            sell_infoMap.put(stockCode, SellStrategyEnum.微盘股_S);


            // boolean signal_S = signal_S(stockCode, topBlockStrategyEnum, data, tradeDate, sell_infoMap, extDataArrDTO, idx, fun, btCompareDTO, posCostMap);


            if (signal_S) {


//                // ------------------------------- S + 跌停  ->  无法卖出（特殊处理【最简化处理】）❌❌❌---------------------
//
//
//                // 当日跌停 -> 无法卖出
//                boolean today_跌停 = extDataArrDTO.跌停[idx];
//
//
//                if (today_跌停 && idx < fun.getMaxIdx()) {
//                    KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
//
//                    double today_close = klineArrDTO.close[idx];
//                    double next_open = klineArrDTO.open[idx + 1];
//
//
//                    // today_close = next_open   ❌❌❌
//                    data.stock__dateCloseMap.get(stockCode).put(tradeDate, next_open);
//                    // BUG：S->B阶段 改价     =>     S前阶段 用于计算 [总资金/S前_持仓市值] 的 close   与   SB阶段 的 close（next_open）前后不一致❗❗❗
//                    //                  today_close  >  next_open       =>       S前_总资金（全程不变）  >   SB_持仓市值  +  SB_可用资金
//                    //                  today_close  <  next_open       =>       S前_总资金（全程不变）  <   SB_持仓市值  +  SB_可用资金
//
//
//                    log.info("今日S + [跌停]   ->   无法卖出 - 最简化处理   =>   [today_close]=[next_open]     >>>     [{}-{}] , {} , today_close : {} , next_open : {}",
//                             stockCode, stockDO.getName(), tradeDate, today_close, next_open);
//                }
            }


            return signal_S;


        }).collect(Collectors.toSet());


        return sell__stockCodeSet;
    }


}