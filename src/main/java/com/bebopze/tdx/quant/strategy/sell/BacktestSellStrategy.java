package com.bebopze.tdx.quant.strategy.sell;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.SellStrategyEnum;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestCompareDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategyC;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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


    /**
     * 涨停S策略        KEY：S策略 + next_date
     * -              VAL：涨停 + S_signal   ->   stockCode 列表
     */
    private static final Map<String, Set<String>> sellConSet_nextDate__dtStockCodeSet__Map = Maps.newConcurrentMap();


    @Autowired
    private MarketService marketService;

    @Autowired
    private BacktestBuyStrategyC backtestBuyStrategyC;


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public String key() {
        return "A";
    }


    @TotalTime
    @Override
    public Set<String> rule(TopBlockStrategyEnum topBlockStrategyEnum,
                            BacktestCache data,
                            LocalDate tradeDate,
                            List<String> positionStockCodeList,
                            Map<String, SellStrategyEnum> sell_infoMap,
                            BacktestCompareDTO btCompareDTO) {


        // -------------------------------------------------------------------------------------------------------------
        //                                                2、主线板块
        // -------------------------------------------------------------------------------------------------------------


        long start_1 = System.currentTimeMillis();
        Set<String> topBlockCodeSet = backtestBuyStrategyC.topBlock(topBlockStrategyEnum, data, tradeDate);
        log.info("BacktestSellStrategy - topBlock     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_1));


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）


        long start_2 = System.currentTimeMillis();
        Set<String> sell__stockCodeSet = sell__stockCodeSet(positionStockCodeList, topBlockStrategyEnum, data, tradeDate, sell_infoMap, btCompareDTO);
        log.info("BacktestSellStrategy - sell__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> NOT IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 取反     =>     前期 强势个股   ->   not IN 主线板块
        long start_3 = System.currentTimeMillis();
        Set<String> notInTopBlock__stockCodeSet = notInTopBlock__stockCodeSet(topBlockCodeSet, positionStockCodeList, data, tradeDate);
        log.info("BacktestSellStrategy - notInTopBlock__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));


        // -------------------------------------------------------------------------------------------------------------


        // merge   ->   个股S + 板块S
        merge___sell__stockCodeSet(sell__stockCodeSet, notInTopBlock__stockCodeSet, sell_infoMap);


        // -------------------------------------------------------------------------------------------------------------


        // 大盘底/大盘底部     ->     ETF策略（持有  ->  暂无视一切 B/S策略）
        sellStrategy_ETF(sell__stockCodeSet, data, tradeDate);


        // -------------------------------------------------------------------------------------------------------------


        return sell__stockCodeSet;
    }


    private Set<String> sell__stockCodeSet(List<String> positionStockCodeList,
                                           TopBlockStrategyEnum topBlockStrategyEnum,
                                           BacktestCache data,
                                           LocalDate tradeDate,
                                           Map<String, SellStrategyEnum> sell_infoMap,
                                           BacktestCompareDTO btCompareDTO) {


        Set<String> sell__stockCodeSet = positionStockCodeList.stream().filter(stockCode -> {
            BaseStockDO stockDO = data.codeStockMap.getOrDefault(
                    stockCode,
                    // ETF
                    data.ETF_stockDOList.stream().filter(e -> e.getCode().equals(stockCode)).findFirst().orElse(null));


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


            boolean signal_S = signal_S(stockCode, topBlockStrategyEnum, data, tradeDate, sell_infoMap, extDataArrDTO, idx, fun, btCompareDTO);


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

    private boolean signal_S(String stockCode,
                             TopBlockStrategyEnum topBlockStrategyEnum,
                             BacktestCache data,
                             LocalDate tradeDate,
                             Map<String, SellStrategyEnum> sell_infoMap,
                             ExtDataArrDTO extDataArrDTO,
                             Integer idx,
                             StockFun fun,
                             BacktestCompareDTO btCompareDTO) {


//        ExtDataDTO extDataDTO = fun.getExtDataDTOList().get(idx);

        KlineArrDTO klineArrDTO = fun.getKlineArrDTO();


        // ---------------------------------------------------------------------------------------------------------


        // -------------------------------------------


        // 个股   B-signal     ->     主线板块     ->     月空/SSF空/MA20空       =>       个股  ->  S / 减半仓

        //        ==>       走弱的   前期 【主线板块】 加速出清     =>     清理  掉出【主线板块】 -  对应的个股     ->     加快 自动 【主线板块 切换】


        boolean S_topBlock = S_topBlock(stockCode, topBlockStrategyEnum, data, tradeDate);

        if (S_topBlock) {
            sell_infoMap.put(stockCode, SellStrategyEnum.S72);
            return true;
        }


        // -------------------------------------------


        // -------------------------------------------


        // 是否淘汰
        // boolean flag_S = false;


        // ------------------------------------------- TODO   涨停（打板） ---------------------------------------------------------


        // if (buyConSet.equals(Sets.newHashSet("月多", "涨停"))) {
        if (btCompareDTO.getZtFlag()) {


            // ---------------------------------------------------------------------------------------------------------


            // prev_跌停__S_signal   ->   S策略 + next_date + 跌停stockCode
            boolean prev_跌停__S_signal = sellConSet_nextDate__dtStockCodeSet__Map
                    .getOrDefault(getKey(btCompareDTO.getSellConSet(), btCompareDTO.getZtFlag(), tradeDate), Sets.newHashSet())
                    .contains(stockCode);


            // 昨日（S + 跌停）  ->   可卖出（今日[open]  ->  直接卖出）
            if (prev_跌停__S_signal) {
                sell_infoMap.put(stockCode, SellStrategyEnum.跌停);
                return true;
            }


            // ---------------------------------------------------------------------------------------------------------


            boolean today_跌停 = extDataArrDTO.跌停[idx];
            boolean 下SSF = klineArrDTO.close[idx] < extDataArrDTO.SSF[idx];
            boolean 下MA5 = klineArrDTO.close[idx] < extDataArrDTO.MA5[idx];
            boolean 下MA10 = klineArrDTO.close[idx] < extDataArrDTO.MA10[idx];


            if (today_跌停) {
                if (idx < fun.getMaxIdx()) {

                    LocalDate next_date = klineArrDTO.date[idx + 1];
                    double today_close = klineArrDTO.close[idx];
                    double next_open = klineArrDTO.open[idx + 1];


                    // S + 跌停  ->  无法卖出（最简化处理：[today_close] = [next_open]，次日开盘 直接卖出）
                    data.stock__dateCloseMap.get(stockCode).put(next_date, next_open);

                    log.info("今日S + [跌停]   ->   无法卖出 - 最简化处理   =>   [today_close]=[next_open]     >>>     [{}-{}] , today_date : {} , next_date : {} , today_close : {} , next_open : {}",
                             stockCode, fun.getName(), tradeDate, next_date, today_close, next_open);


                    // -------------------------------------------------------------------------------------------------


                    // 传递 prev_跌停__S_signal
                    sellConSet_nextDate__dtStockCodeSet__Map.computeIfAbsent(getKey(btCompareDTO.getSellConSet(), btCompareDTO.getZtFlag(), next_date), k -> Sets.newHashSet()).add(stockCode);
                    sellConSet_nextDate__dtStockCodeSet__Map.remove(getKey(btCompareDTO.getSellConSet(), btCompareDTO.getZtFlag(), klineArrDTO.date[idx - 1])); // 清空 prev_date


                    // -------------------------------------------------------------------------------------------------


                    // 跌停  ->  无法卖出
                    return false;
                }
            }


            if (下SSF) {
                sell_infoMap.put(stockCode, SellStrategyEnum.下SSF);
                return true;
            }
            if (下MA5) {
                sell_infoMap.put(stockCode, SellStrategyEnum.下MA5);
                return true;
            }
            if (下MA10) {
                sell_infoMap.put(stockCode, SellStrategyEnum.下MA10);
                return true;
            }


            // SSF空
            if (extDataArrDTO.SSF空[idx]) {
                sell_infoMap.put(stockCode, SellStrategyEnum.SSF空);
                return true;
            }

            // MA20空
            if (extDataArrDTO.MA20空[idx]) {
                sell_infoMap.put(stockCode, SellStrategyEnum.MA20空);
                return true;
            }

            // 高位（中期涨幅_MA20 > 100）   ->   爆天量/长上影/大阴线
            if (extDataArrDTO.高位爆量上影大阴[idx]) {
                sell_infoMap.put(stockCode, SellStrategyEnum.高位爆量上影大阴);
                return true;
            }


            // ------------------------------------------- 高抛 ---------------------------------------------------------


            // 偏离率 > 25%
            double C_SSF_偏离率 = extDataArrDTO.C_SSF_偏离率[idx];
            int limit_C_SSF_偏离率 = fun.is20CM() ? 40 : 35;
            if (C_SSF_偏离率 > limit_C_SSF_偏离率) {
                sell_infoMap.put(stockCode, SellStrategyEnum.C_SSF_偏离率);
                return true;
            }


            double H_SSF_偏离率 = extDataArrDTO.H_SSF_偏离率[idx];
            int limit_H_SSF_偏离率 = fun.is20CM() ? 45 : 40;
            if (H_SSF_偏离率 > limit_H_SSF_偏离率) {
                sell_infoMap.put(stockCode, SellStrategyEnum.H_SSF_偏离率);
                return true;
            }


            return false;
        }


        // ------------------------- TODO test：B_signal（月多,C_MA50_偏离率<5、SSF多,月多,C_MA20_偏离率<5）--------------------------------------------------


        // B_signal -> 月多,C_MA50_偏离率<5
        // B_signal -> SSF多,月多,C_MA20_偏离率<5
        Set<String> buyConSet = btCompareDTO.getBuyConSet();
        Set<String> buyConSet_2 = BacktestStrategy.btCompareDTO.get().getBuyConSet();

        if (!Objects.equals(buyConSet, buyConSet_2)) {
            log.error("signal_S     >>>     buyConSet={}  !=  buyConSet_2={}", buyConSet, buyConSet_2);
        }

        if (buyConSet.equals(Sets.newHashSet("月多", "C_MA50_偏离率<5"))
                || buyConSet.equals(Sets.newHashSet("SSF多", "月多", "C_MA20_偏离率<5"))) {

            // MA20空
            if (extDataArrDTO.MA20空[idx]) {
                sell_infoMap.put(stockCode, SellStrategyEnum.MA20空);
                return true;
            }

            // 高位（中期涨幅_MA20 > 100）   ->   爆天量/长上影/大阴线
            if (extDataArrDTO.高位爆量上影大阴[idx]) {
                sell_infoMap.put(stockCode, SellStrategyEnum.高位爆量上影大阴);
                return true;
            }

            return false;
        }


        // TODO   ->   del   月空_MA20空、SSF空


        // TODO   ->   Sell策略 ： 暂时固定（改为  随机组合 Set）


        // ------------------------------------------- 破位 ---------------------------------------------------------


        // 月空
        boolean 月空 = !extDataArrDTO.月多[idx];
        boolean MA20空 = extDataArrDTO.MA20空[idx];
        if (月空 && MA20空) {
            sell_infoMap.put(stockCode, SellStrategyEnum.月空_MA20空);
            return true;
        }


        // MA20空
        if (MA20空) {
            sell_infoMap.put(stockCode, SellStrategyEnum.MA20空);
            return true;
        }


        // SSF空
        if (extDataArrDTO.SSF空[idx]) {
            sell_infoMap.put(stockCode, SellStrategyEnum.SSF空);
            return true;
        }


        // 高位（中期涨幅_MA20 > 100）   ->   爆天量/长上影/大阴线
        if (extDataArrDTO.高位爆量上影大阴[idx]) {
            sell_infoMap.put(stockCode, SellStrategyEnum.高位爆量上影大阴);
            return true;
        }


        // ------------------------------------------- 破位 + 跌停 --------------------------------------------------


        boolean 跌停 = extDataArrDTO.跌停[idx];
        boolean MA5空 = extDataArrDTO.MA5空[idx];
        boolean MA10空 = extDataArrDTO.MA10空[idx];
        if (跌停 && MA5空 && MA10空) {
            sell_infoMap.put(stockCode, SellStrategyEnum.跌停_MA5空_MA10空); // TODO   跌停 是否必要？？？   ->   MA5空_MA10空
            return true;
        }


        // ------------------------------------------- 高抛 ---------------------------------------------------------


        // 偏离率 > 25%
        double C_SSF_偏离率 = extDataArrDTO.C_SSF_偏离率[idx];
        int limit_C_SSF_偏离率 = fun.is20CM() ? 35 : 30;   // 300346（2021-07-30）
        if (C_SSF_偏离率 > limit_C_SSF_偏离率) {
            sell_infoMap.put(stockCode, SellStrategyEnum.C_SSF_偏离率);
            return true;
        }


        double H_SSF_偏离率 = extDataArrDTO.H_SSF_偏离率[idx];
        int limit_H_SSF_偏离率 = fun.is20CM() ? 40 : 35;   // 300346（2021-07-30）
        if (H_SSF_偏离率 > limit_H_SSF_偏离率) {
            sell_infoMap.put(stockCode, SellStrategyEnum.H_SSF_偏离率);
            return true;
        }


        // ---------------------------------------------------------------------------------------------------------


//            // 偏离率 > 60%
//            double C_短期MA_偏离率 = extDataDTO.getC_短期MA_偏离率();
//            double C_中期MA_偏离率 = extDataDTO.getC_中期MA_偏离率();
//            double C_长期MA_偏离率 = extDataDTO.getC_长期MA_偏离率();
//
//
//            C_MA_Ratio c_ma_ratio = c_ma_ratio(extDataDTO.getKlineType(), fun.chgPctLimit());
//            if (c_ma_ratio == null) {
//                return false;
//            }
//
//
//            if (C_短期MA_偏离率 > c_ma_ratio.short_MA_Ratio) {
//                sell_infoMap.put(stockCode, SellStrategyEnum.C_短期MA_偏离率);
//                return true;
//            }
//
//            if (C_中期MA_偏离率 > c_ma_ratio.medium_MA_Ratio) {
//                sell_infoMap.put(stockCode, SellStrategyEnum.C_中期MA_偏离率);
//                return true;
//            }
//
//            if (C_长期MA_偏离率 > c_ma_ratio.long_MA_Ratio) {
//                sell_infoMap.put(stockCode, SellStrategyEnum.C_长期MA_偏离率);
//                return true;
//            }


        // ---------------------------------------------------------------------------------------------------------


        // TODO     最大 亏损线  ->  -7% 止损


        return false;
    }


    private String getKey(Set<String> sellConSet, Boolean ztFlag, LocalDate next_date) {
        // S策略 + ztFlag + next_date
        return sellConSet + "-" + ztFlag + "-" + next_date;
    }


    private C_MA_Ratio c_ma_ratio(int klineType, Integer chgPctLimit) {
        C_MA_Ratio cMaRatio = new C_MA_Ratio();


        // 慢牛股
        if (klineType == 1) {

            cMaRatio.short_MA_Ratio = chgPctLimit == 5 ? 7 :
                    chgPctLimit == 10 ? 10 :
                            chgPctLimit == 20 ? 20 :
                                    chgPctLimit == 30 ? 30 : 50;


            cMaRatio.medium_MA_Ratio = chgPctLimit == 5 ? 12 :
                    chgPctLimit == 10 ? 15 :
                            chgPctLimit == 20 ? 25 :
                                    chgPctLimit == 30 ? 35 : 50;

            cMaRatio.long_MA_Ratio = chgPctLimit == 5 ? 20 :
                    chgPctLimit == 10 ? 25 :
                            chgPctLimit == 20 ? 35 :
                                    chgPctLimit == 30 ? 45 : 50;

        }
        // 趋势股
        else if (klineType == 2) {


            // 天齐锂业（2025-11）        MA5-MA20-MA50

            cMaRatio.short_MA_Ratio = chgPctLimit == 5 ? 7 :
                    chgPctLimit == 10 ? 10 :
                            chgPctLimit == 20 ? 20 :
                                    chgPctLimit == 30 ? 30 : 50;


            cMaRatio.medium_MA_Ratio = chgPctLimit == 5 ? 12 :
                    chgPctLimit == 10 ? 15 :
                            chgPctLimit == 20 ? 25 :
                                    chgPctLimit == 30 ? 35 : 50;

            cMaRatio.long_MA_Ratio = chgPctLimit == 5 ? 20 :
                    chgPctLimit == 10 ? 25 :
                            chgPctLimit == 20 ? 35 :
                                    chgPctLimit == 30 ? 45 : 50;

        }

        // 动量股
        else if (klineType == 3) {

            cMaRatio.short_MA_Ratio = chgPctLimit == 5 ? 7 :
                    chgPctLimit == 10 ? 10 :
                            chgPctLimit == 20 ? 20 :
                                    chgPctLimit == 30 ? 30 : 50;


            cMaRatio.medium_MA_Ratio = chgPctLimit == 5 ? 12 :
                    chgPctLimit == 10 ? 15 :
                            chgPctLimit == 20 ? 25 :
                                    chgPctLimit == 30 ? 35 : 50;

            cMaRatio.long_MA_Ratio = chgPctLimit == 5 ? 20 :
                    chgPctLimit == 10 ? 25 :
                            chgPctLimit == 20 ? 35 :
                                    chgPctLimit == 30 ? 45 : 50;

        }
        // 妖股
        else if (klineType == 4) {

            cMaRatio.short_MA_Ratio = chgPctLimit == 5 ? 7 :
                    chgPctLimit == 10 ? 10 :
                            chgPctLimit == 20 ? 20 :
                                    chgPctLimit == 30 ? 30 : 50;


            cMaRatio.medium_MA_Ratio = chgPctLimit == 5 ? 12 :
                    chgPctLimit == 10 ? 15 :
                            chgPctLimit == 20 ? 25 :
                                    chgPctLimit == 30 ? 35 : 50;

            cMaRatio.long_MA_Ratio = chgPctLimit == 5 ? 20 :
                    chgPctLimit == 10 ? 25 :
                            chgPctLimit == 20 ? 35 :
                                    chgPctLimit == 30 ? 45 : 50;
        } else {
            return null;
        }


        return cMaRatio;
    }


    /**
     * 取反     =>     前期 强势个股   ->   not IN 主线板块
     *
     * @param topBlockCodeSet       主线板块 列表
     * @param positionStockCodeList 持仓个股 列表
     * @param data                  回测缓存数据
     * @param tradeDate             交易日期
     * @return 不在【主线板块】中的个股集合
     */
    private Set<String> notInTopBlock__stockCodeSet(Set<String> topBlockCodeSet,
                                                    List<String> positionStockCodeList,
                                                    BacktestCache data,
                                                    LocalDate tradeDate) {


        // 强势个股   ->   IN 主线板块
        Set<String> inTopBlock__stockCodeSet = backtestBuyStrategyC.inTopBlock__stockCodeSet(topBlockCodeSet, positionStockCodeList, data, tradeDate);


        // 取反     =>     前期 强势个股   ->   not IN 主线板块

        // 走弱的   前期 【主线板块】 加速出清     =>     清理  掉出【主线板块】 -  对应的个股     ->     加快 自动 【主线板块 切换】
        Set<String> notInTopBlock__stockCodeSet = new HashSet<>(positionStockCodeList);
        notInTopBlock__stockCodeSet.removeAll(inTopBlock__stockCodeSet);


        return notInTopBlock__stockCodeSet;
    }


    /**
     * merge   ->   个股S + 板块S
     *
     * @param sell__stockCodeSet          个股S
     * @param notInTopBlock__stockCodeSet 板块S
     * @param sell_infoMap
     */
    private void merge___sell__stockCodeSet(Set<String> sell__stockCodeSet,
                                            Set<String> notInTopBlock__stockCodeSet,
                                            Map<String, SellStrategyEnum> sell_infoMap) {


        for (String stockCode : notInTopBlock__stockCodeSet) {

            // 排除ETF（无板块关联）
            if (!Objects.equals(StockTypeEnum.getByStockCode(stockCode), StockTypeEnum.ETF)) {
                sell__stockCodeSet.add(stockCode);
                sell_infoMap.put(stockCode, SellStrategyEnum.S71);
            }
        }
    }


    /**
     * B-signal     ->     主线板块     ->     月空/SSF空/MA20空     =>     个股  ->  S / 减半仓
     *
     *
     * ==>       走弱的   前期 【主线板块】 加速出清     ->     加快 自动 【主线板块 切换】
     *
     * @param stockCode
     * @param topBlockStrategyEnum
     * @param data
     * @param tradeDate
     * @return
     */
    private boolean S_topBlock(String stockCode,
                               TopBlockStrategyEnum topBlockStrategyEnum,
                               BacktestCache data,
                               LocalDate tradeDate) {


        // 今日   主线板块 列表
        Set<String> topBlockCodeSet = backtestBuyStrategyC.topBlock(topBlockStrategyEnum, data, tradeDate);
        // 今日   个股 -> IN 主线板块
        Set<String> inTopBlock__stockCodeSet = backtestBuyStrategyC.inTopBlock__stockCodeSet(topBlockCodeSet, Lists.newArrayList(stockCode), data, tradeDate);


        // 个股   ->   NOT IN 主线板块       =>       个股  ->  全部 topBlock   ->   全 转空
        return CollectionUtils.isEmpty(inTopBlock__stockCodeSet);


        // 个股   ->   B-signal   主线板块
        // Set<String> topBlockCodeSet = data.stockCode_topBlockCache.getIfPresent(stockCode);

        // Set<String> all_topBlockCodeSet = data.topBlockCache.getIfPresent(tradeDate).get(topBlockStrategyEnum);


//        if (CollectionUtils.isEmpty(topBlockCodeSet)) {
//            return false;
//        }
//
//
//        for (String topBlockCode : topBlockCodeSet) {
//
//            boolean topBlock_S = topBlock_S(topBlockCode, data, tradeDate);
//            // 任一 topBlock     ->     还未转空 -> 多
//            if (!topBlock_S) {
//                return false;
//            }
//        }
//
//
//        log.info("rule - S_topBlock     >>>     [{}] , stockCode : {} , topBlockCodeSet : {}",
//                 tradeDate, stockCode, JSON.toJSONString(topBlockCodeSet));
//
//
//        // 个股  ->  全部 topBlock   ->   全 转空
//        return true;
    }


    /**
     * 个股  ->  topBlock   ->   多/空
     *
     * @param topBlockCode
     * @param data
     * @param tradeDate
     * @return
     */
    private boolean topBlock_S(String topBlockCode, BacktestCache data, LocalDate tradeDate) {

        BlockFun fun = data.getOrCreateBlockFun(topBlockCode);


        ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


        Integer idx = dateIndexMap.get(tradeDate);
        // 过滤当日  ->  未上市/新板块、非LV3
        if (/*blockDO.getEndLevel() != 1 || */extDataArrDTO.date.length == 0 || idx == null /*|| idx < 50*/) {
            return false;
        }


        boolean 月空 = extDataArrDTO.月空(idx);
        boolean SSF空 = extDataArrDTO.SSF空[idx];
        boolean MA20空 = extDataArrDTO.MA20空[idx];


        return 月空 || SSF空 || MA20空;
    }


    /**
     * 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）
     *
     * @param sell__stockCodeSet
     * @param data
     * @param tradeDate
     */
    private void sellStrategy_ETF(Set<String> sell__stockCodeSet,
                                  BacktestCache data,
                                  LocalDate tradeDate) {

        if (CollectionUtils.isEmpty(sell__stockCodeSet)) {
            return;
        }


        // 大盘量化
        QaMarketMidCycleDO marketInfo = data.marketCache.get(tradeDate, k -> marketService.marketInfo(tradeDate));
        Assert.notNull(marketInfo, "[大盘量化]数据为空：" + tradeDate);


        // 大盘-牛熊：1-牛市；2-熊市；
        Integer marketBullBearStatus = marketInfo.getMarketBullBearStatus();
        // 大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
        Integer marketMidStatus = marketInfo.getMarketMidStatus();
        // MA50占比（%）
        double ma50Pct = marketInfo.getMa50Pct().doubleValue();
        // 底_DAY
        Integer marketLowDay = marketInfo.getMarketLowDay();
        // 个股月多-占比（%）
        double stockMonthBullPct = marketInfo.getStockMonthBullPct().doubleValue();
        // 板块月多-占比（%）
        double blockMonthBullPct = marketInfo.getBlockMonthBullPct().doubleValue();
        // 差值（新高-新低）
        int highLowDiff = marketInfo.getHighLowDiff();
        // 右侧S-占比（%）
        double rightSellPct = marketInfo.getRightSellPct().doubleValue();


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
                sell__stockCodeSet.remove(e.getCode());
            });
        }
    }


}
