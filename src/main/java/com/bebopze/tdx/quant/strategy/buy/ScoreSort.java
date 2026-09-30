package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.MapUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * B策略 排序规则
 *
 * @author: bebopze
 * @date: 2026/1/8
 */
@Slf4j
public class ScoreSort {


    /**
     * 权重规则   排序
     *
     * @param stockCodeList
     * @param data
     * @param tradeDate
     * @param N
     * @return
     */
    public static List<String> scoreSort__AMO(Collection<String> stockCodeList,
                                              BacktestCache data,
                                              LocalDate tradeDate,
                                              int N) {


        Map<String, Double> code_amo_map = Maps.newHashMap();


        stockCodeList.forEach(stockCode -> {
            String stockName = data.stock__codeNameMap.get(stockCode);
            StockFun fun = data.getOrCreateStockFun(stockCode);


            KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();


            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
            Integer idx = dateIndexMap.get(tradeDate);


            // 停牌（159915 创业板ETF   2021-02-08）
            if (idx == null) {
                log.warn("scoreSort - idx is null     >>>     [{}-{}] , tradeDate : {} , idx : {}", stockCode, stockName, tradeDate, idx);
                return;
            }


            // ---------------------------------------------------------------------------------------------------------


            // AMO
            double amount = klineArrDTO.amo[idx];


            // >5亿   ||   涨停
            if (amount >= 5_0000_0000 || extDataArrDTO.涨停[idx]) {
                code_amo_map.put(stockCode, amount);
            }
        });


        // sort
        Map<String, Double> sortedMap = MapUtil.reverseSortByValue(code_amo_map);
        // limit N
        return sortedMap.keySet().stream().limit(N).collect(Collectors.toList());
    }


    /**
     * 权重规则   排序
     *
     * @param stockCodeList
     * @param data
     * @param tradeDate
     * @param N
     * @return
     */
    public static List<String> scoreSort__AMO_RPS(Collection<String> stockCodeList,
                                                  BacktestCache data,
                                                  LocalDate tradeDate,
                                                  int N) {


        Map<String, Double> code_amo_map = Maps.newHashMap();


        stockCodeList.forEach(stockCode -> {
            String stockName = data.stock__codeNameMap.get(stockCode);
            StockFun fun = data.getOrCreateStockFun(stockCode);


            BaseStockDO stockDO = data.codeStockMap.get(stockCode);


            KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();


            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
            Integer idx = dateIndexMap.get(tradeDate);


            // 停牌（159915 创业板ETF   2021-02-08）
            if (idx == null) {
                log.warn("scoreSort - idx is null     >>>     [{}-{}] , tradeDate : {} , idx : {}", stockCode, stockName, tradeDate, idx);
                return;
            }


            // ---------------------------------------------------------------------------------------------------------


            // AMO
            double amount = klineArrDTO.amo[idx];


            // RPS五线和
            double RPS五线和 = extDataArrDTO.RPS五线和[idx];


            // 个股>5亿   /   ETF>500万
            double MIN_AMO = Objects.equals(stockDO.getType(), StockTypeEnum.A_STOCK.type) ? 5_0000_0000 : 500_0000;
            if ((amount < MIN_AMO && !extDataArrDTO.涨停[idx]) || Double.isNaN(RPS五线和)) {
                return;
            }


            code_amo_map.put(stockCode, RPS五线和);
        });


        // sort
        Map<String, Double> sortedMap = MapUtil.reverseSortByValue(code_amo_map);
        // limit N
        return sortedMap.keySet().stream().limit(N).collect(Collectors.toList());
    }


}