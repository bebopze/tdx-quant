package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.MapUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.strategy.QuickOption;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.util.BoolUtil.bool2Int;


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
    public static List<String> scoreSort(Collection<String> stockCodeList,
                                         BacktestCache data,
                                         LocalDate tradeDate,
                                         int N) {


        if (stockCodeList.size() <= N) {
            return Lists.newArrayList(stockCodeList);
        }


        Map<String, String> codeNameMap = data.stock__codeNameMap;


        // ----------------- 规则排名

        // 金额 -> 涨幅榜（近10日） -> ...

        // TODO     RPS（50） -> 大均线多头（20） -> 60日新高（10） -> 涨幅榜（10） -> 成交额-近10日（10） -> ...


        // Step 2: 计算各项指标 & 打分
        List<QuickOption.StockScore> scoredStocks = Lists.newArrayList();


        // 用于归一化处理
        double maxRPS和 = 0;
        double maxAmount = 0;
        double maxMidReturn = 0;
        double max大均线多头 = 0;
        double maxN日新高 = 0;


        // Step 2.1: 遍历所有股票，计算原始值
        for (String stockCode : stockCodeList) {
            String stockName = codeNameMap.get(stockCode);


            // -------------------------------------------------------------------------------------------


            // BUY策略   ->   已完成init
            // StockFun fun = data.stockFunCache.getIfPresent(stockCode);
            // StockFun fun = data.stockFunCache.get(stockCode, k -> new StockFun(data.codeStockMap.get(stockCode)));
            StockFun fun = data.getOrCreateStockFun(stockCode);


            KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();


            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
            Integer idx = dateIndexMap.get(tradeDate);


            // 停牌（159915 创业板ETF   2021-02-08）
            if (idx == null) {
                log.warn("scoreSort - idx is null     >>>     [{}-{}] , tradeDate : {} , idx : {}", stockCode, stockName, tradeDate, idx);
                continue;
            }


            // ---------------------------------------------------------------------------------------------------------


            double[] amoArr = klineArrDTO.amo;


            double[] rps10_arr = extDataArrDTO.rps10;
            double[] rps20_arr = extDataArrDTO.rps20;
            double[] rps50_arr = extDataArrDTO.rps50;
            double[] rps120_arr = extDataArrDTO.rps120;
            double[] rps250_arr = extDataArrDTO.rps250;


            double[] 中期涨幅_arr = extDataArrDTO.中期涨幅N20;


            boolean[] 大均线多头_arr = extDataArrDTO.大均线多头;
            boolean[] N60日新高_arr = extDataArrDTO.N60日新高;


            double rps10 = rps10_arr[idx];
            double rps20 = rps20_arr[idx];
            double rps50 = rps50_arr[idx];
            double rps120 = rps120_arr[idx];
            double rps250 = rps250_arr[idx];


            // RPS和
            double RPS和1 = rps10 + rps20 + rps50;
            double RPS和2 = rps50 + rps120 + NumUtil.NaN_0(rps250);

            double RPS和 = Math.max(RPS和1, RPS和2);


            // AMO
            double amount = amoArr[idx];


            // 中期涨幅
            double 中期涨幅 = 中期涨幅_arr[idx];

            // 新高天数
            // int 新高天数 = 新高天数_arr[idx];


            // 大均线多头
            int 大均线多头 = bool2Int(大均线多头_arr[idx]);
            // 60日新高
            int N日新高 = bool2Int(N60日新高_arr[idx]);


            // -------------------------------------------------------------------------------------------


            // 更新最大值用于归一化
            maxRPS和 = Math.max(maxRPS和, RPS和);
            maxMidReturn = Math.max(maxMidReturn, 中期涨幅);
            max大均线多头 = Math.max(max大均线多头, 大均线多头);
            maxN日新高 = Math.max(maxN日新高, N日新高);
            maxAmount = Math.max(maxAmount, amount);


            scoredStocks.add(new QuickOption.StockScore(stockCode, stockName, RPS和, 中期涨幅, 大均线多头, N日新高, amount, 0));
        }


        // Step 3: 归一化 & 加权打分
        for (QuickOption.StockScore s : scoredStocks) {


            // RPS（50） ->  成交额-近10日（10） ->  大均线多头（10） ->  60日新高（10） ->  涨幅榜（10）  ->   ...


            double rpsScore = NaNor0(maxRPS和) ? 0 : s.RPS和 / maxRPS和 * 50;                         // 权重50%

            // double 新高天数Score = 新高天数 == 0 ? 0 : s.RPS和 / maxRPS和 * 50;                      // 权重30%（新高天数）

            double amountScore = NaNor0(maxAmount) ? 0 : s.amount / maxAmount * 20;                  // 权重20%
            double 大均线Score = NaNor0(max大均线多头) ? 0 : s.大均线多头 / max大均线多头 * 10;            // 权重10%
            double 新高Score = NaNor0(maxN日新高) ? 0 : s.N日新高 / maxN日新高 * 10;                     // 权重10%
            double midScore = NaNor0(maxMidReturn) ? 0 : s.midTermChangePct / maxMidReturn * 10;     // 权重10%


            s.score = NumUtil.of(rpsScore + amountScore + 大均线Score + 新高Score + midScore);


//            if (Double.isNaN(s.score)) {
//                log.debug("scoreSort - NaN     >>>     rpsScore : {} , amountScore : {} , 大均线Score : {} , 新高Score : {} , midScore : {} , score : {}",
//                          rpsScore, amountScore, 大均线Score, 新高Score, midScore, s.score);
//            }

        }


        // Step 4: 按得分排序，取前N名
        List<QuickOption.StockScore> topNStocks = scoredStocks.stream()
                                                              .sorted(Comparator.comparing((QuickOption.StockScore::getScore)).reversed())
                                                              .limit(N)
                                                              .collect(Collectors.toList());


        // 输出结果或进一步操作
        if (topNStocks.size() < scoredStocks.size() /*|| scoredStocks.size() > N / 2*/) {
            log.debug("scoreSort     >>>     前->后 : [{}->{}] , topNStocks : {}",
                      scoredStocks.size(), topNStocks.size(), JSON.toJSONString(topNStocks));
        }


        return topNStocks.stream().map(QuickOption.StockScore::getStockCode).collect(Collectors.toList());
    }


    private static boolean NaNor0(double val) {
        return Double.isNaN(val) || val == 0;
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
    public static List<String> scoreSort__RPS(Collection<String> stockCodeList,
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
            double MIN_AMO = Objects.equals(stockDO.getType(), StockTypeEnum.A_STOCK) ? 5_0000_0000 : 500_0000;
            if (amount < MIN_AMO && !extDataArrDTO.涨停[idx]) {
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