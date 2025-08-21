package com.bebopze.tdx.quant.strategy.sell;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategyC;
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
                            Map<String, String> sell_infoMap) {


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
        Set<String> sell__stockCodeSet = sell__stockCodeSet(positionStockCodeList, data, tradeDate, sell_infoMap);
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
        sellStrategy_ETF(sell__stockCodeSet, data, tradeDate, sell_infoMap);


        // -------------------------------------------------------------------------------------------------------------


        return sell__stockCodeSet;
    }


    private Set<String> sell__stockCodeSet(List<String> positionStockCodeList,
                                           BacktestCache data,
                                           LocalDate tradeDate,
                                           Map<String, String> sell_infoMap) {


        Set<String> sell__stockCodeSet = positionStockCodeList.stream().filter(stockCode -> {
            BaseStockDO stockDO = data.codeStockMap.getOrDefault(stockCode,
                                                                 // ETF
                                                                 data.ETF_stockDOList.stream().filter(e -> e.getCode().equals(stockCode)).findFirst().orElse(null));


            if (stockDO == null) {
                return false;
            }


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


            // 个股   B-single     ->     主线板块     ->     月空/SSF空/MA20空       =>       个股  ->  S / 减半仓

            //        ==>       走弱的   前期 【主线板块】 加速出清     =>     清理  掉出【主线板块】 -  对应的个股     ->     加快 自动 【主线板块 切换】


            boolean S_topBlock = S_topBlock(stockCode, data, tradeDate);

            if (S_topBlock) {
                // log.info("rule - S_topBlock     >>>     [{}] , stockCode : {} , S_topBlock : {}", tradeDate, stockCode, S_topBlock);
                sell_infoMap.put(stockCode, "板块S" + ",idx-" + idx);
                return true;
            }


            // -------------------------------------------


            // -------------------------------------------


            // 是否淘汰
            // boolean flag_S = false;


            // -------------------------------------------


            try {
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
                // double C_SSF_偏离率 = fun.C_SSF_偏离率(idx);
                double C_SSF_偏离率 = extDataArrDTO.C_SSF_偏离率[idx];
                int limit = fun.is20CM() ? 30 : 25;
                if (C_SSF_偏离率 > limit) {
                    sell_infoMap.put(stockCode, "C_SSF_偏离率>" + limit + "%" + ",idx-" + idx);
                    return true;
                }


            } catch (Exception e) {

            }


            // TODO     最大 亏损线  ->  -7% 止损


            return false;

        }).collect(Collectors.toSet());


        return sell__stockCodeSet;
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
        List<String> inTopBlock__stockCodeList = backtestBuyStrategyC.inTopBlock__stockCodeList(topBlockCodeSet, positionStockCodeList, data, tradeDate);


        // 取反     =>     前期 强势个股   ->   not IN 主线板块

        // 走弱的   前期 【主线板块】 加速出清     =>     清理  掉出【主线板块】 -  对应的个股     ->     加快 自动 【主线板块 切换】
        Set<String> notInTopBlock__stockCodeSet = new HashSet<>(positionStockCodeList);
        notInTopBlock__stockCodeSet.removeAll(inTopBlock__stockCodeList);


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
                                            Map<String, String> sell_infoMap) {


        for (String stockCode : notInTopBlock__stockCodeSet) {

            // 排除ETF（无板块关联）
            if (!Objects.equals(StockTypeEnum.getByStockCode(stockCode), StockTypeEnum.ETF)) {
                sell__stockCodeSet.add(stockCode);
                sell_infoMap.put(stockCode, "【主线板块】走弱");
            }
        }
    }


    /**
     * B-single     ->     主线板块     ->     月空/SSF空/MA20空     =>     个股  ->  S / 减半仓
     *
     *
     * ==>       走弱的   前期 【主线板块】 加速出清     ->     加快 自动 【主线板块 切换】
     *
     * @param stockCode
     * @param data
     * @param tradeDate
     * @return
     */
    private boolean S_topBlock(String stockCode, BacktestCache data, LocalDate tradeDate) {


        // 个股   ->   B-single   主线板块
        Set<String> topBlockCodeSet = data.stockCode_topBlockCache.getIfPresent(stockCode);

        if (CollectionUtils.isEmpty(topBlockCodeSet)) {
            return false;
        }


        for (String topBlockCode : topBlockCodeSet) {

            boolean topBlock_S = topBlock_S(topBlockCode, data, tradeDate);
            // 任一 topBlock     ->     还未转空 -> 多
            if (!topBlock_S) {
                return false;
            }
        }


        log.info("rule - S_topBlock     >>>     [{}] , stockCode : {} , topBlockCodeSet : {}",
                 tradeDate, stockCode, JSON.toJSONString(topBlockCodeSet));


        // 个股  ->  全部 topBlock   ->   全 转空
        return true;
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

        BlockFun fun = data.blockFunCache.get(topBlockCode, k -> new BlockFun(k, data.codeBlockMap.get(topBlockCode)));


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
     * @param sellInfoMap
     */
    private void sellStrategy_ETF(Set<String> sell__stockCodeSet,
                                  BacktestCache data,
                                  LocalDate tradeDate,
                                  Map<String, String> sellInfoMap) {

        if (CollectionUtils.isEmpty(sell__stockCodeSet)) {
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
                sell__stockCodeSet.remove(e.getCode());
            });
        }
    }


}
