package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineArrDTO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.strategy.QuickOption;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.parser.check.TdxFunCheck.bool2Int;
import static com.bebopze.tdx.quant.strategy.sell.BacktestSellStrategy.blockFunMap;
import static com.bebopze.tdx.quant.strategy.sell.BacktestSellStrategy.stockFunMap;


/**
 * 回测 - B策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategy extends BuyStrategy {


    public List<String> rule(BacktestCache data, LocalDate tradeDate, Map<String, String> buy_infoMap) {


        // initData(data);


        // -------------------------------------------------------------------------------------------------------------
        //                                                主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 主线板块（月多2   ->   月多 + N日新高/RPS三线红/大均线多头 + SSF多）
        List<String> filter__blockCodeList = Collections.synchronizedList(Lists.newArrayList());
        data.blockDOList/*.parallelStream()*/.forEach(blockDO -> {


            String blockCode = blockDO.getCode();


            // 1、in__板块-月多


            // 2、in__板块-60日新高

            // 3、in__板块-RPS三线红


            // 4、in__板块占比-TOP1


            // 5、xxx


            BlockFun fun = blockFunMap.computeIfAbsent(blockCode, k -> new BlockFun(k, blockDO));


            // KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();


            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 是否买入
            // boolean signal_B = false;


            // -------------------------------------------


            boolean[] 月多_arr = extDataArrDTO.月多;

            boolean[] N日新高_arr = extDataArrDTO.N日新高;
            boolean[] RPS三线红_arr = extDataArrDTO.RPS三线红;

            boolean[] 大均线多头_arr = extDataArrDTO.大均线多头;

            boolean[] SSF多_arr = extDataArrDTO.SSF多;


            boolean 月多 = getByDate(月多_arr, dateIndexMap, tradeDate);
            boolean _60日新高 = getByDate(N日新高_arr, dateIndexMap, tradeDate);
            boolean RPS三线红 = getByDate(RPS三线红_arr, dateIndexMap, tradeDate);
            boolean 大均线多头 = getByDate(大均线多头_arr, dateIndexMap, tradeDate);
            boolean SSF多 = getByDate(SSF多_arr, dateIndexMap, tradeDate);


            boolean signal_B = 月多 /*&& _60日新高*/ && (_60日新高 || RPS三线红 || 大均线多头) && SSF多;
            if (signal_B) {
                filter__blockCodeList.add(blockCode);
            }
        });


        // -------------------------------------------------------------------------------------------------------------
        //                                            （强势）个股
        // -------------------------------------------------------------------------------------------------------------


        List<String> filter__stockCodeList = Collections.synchronizedList(Lists.newArrayList());
        data.stockDOList/*.parallelStream()*/.forEach(stockDO -> {


            String stockCode = stockDO.getCode();


            // 1、in__60日新高

            // 2、in__月多


            // 3、in__RPS三线红

            // 4、in__大均线多头


            // 5、SSF多

            // 6、xxx


            StockFun fun = stockFunMap.computeIfAbsent(stockCode, k -> new StockFun(k, stockDO));


            // KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();


            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 是否买入
            // boolean signal_B = false;


            // -------------------------------------------


            boolean[] 月多_arr = extDataArrDTO.月多;

            boolean[] N日新高_arr = extDataArrDTO.N日新高;
            boolean[] RPS三线红_arr = extDataArrDTO.RPS三线红;

            boolean[] 大均线多头_arr = extDataArrDTO.大均线多头;

            boolean[] SSF多_arr = extDataArrDTO.SSF多;


            boolean 月多 = getByDate(月多_arr, dateIndexMap, tradeDate);
            boolean _60日新高 = getByDate(N日新高_arr, dateIndexMap, tradeDate);
            boolean RPS三线红 = getByDate(RPS三线红_arr, dateIndexMap, tradeDate);
            boolean 大均线多头 = getByDate(大均线多头_arr, dateIndexMap, tradeDate);
            boolean SSF多 = getByDate(SSF多_arr, dateIndexMap, tradeDate);


            boolean signal_B = 月多 && _60日新高 && (RPS三线红 || 大均线多头) && SSF多;
            if (signal_B) {

                filter__stockCodeList.add(stockCode);


                // ----------------------------------------------------- info


                // 动态收集所有为 true 的信号名称，按固定顺序拼接
                List<String> info = Lists.newArrayList();
                if (月多) info.add("月多");
                if (_60日新高) info.add("60日新高");
                if (SSF多) info.add("SSF多");
                if (RPS三线红) info.add("RPS三线红");
                if (大均线多头) info.add("大均线多头");
                info.add("idx-" + dateIndexMap.get(tradeDate));

                buy_infoMap.put(stockCode, String.join(",", info));
            }
        });


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 个股   ->   IN 主线板块
        List<String> filter__stockCodeList2 = filter__stockCodeList/*.parallelStream()*/.stream().filter(stockCode -> {
            List<String> blockCodeList = data.stockCode_blockCodeList_Map.getOrDefault(stockCode, Collections.emptyList());


            // B（主线板块）
            boolean block_B = false;
            for (String blockCode : blockCodeList) {

                block_B = filter__blockCodeList.contains(blockCode);
                if (block_B) {
                    log.debug("个股 -> IN 主线板块     >>>     {} , [{}-{}] , [{}-{}]", tradeDate,
                              stockCode, data.stock__codeNameMap.get(stockCode),
                              blockCode, data.block__codeNameMap.get(blockCode));
                    break;
                }
            }


            return block_B;
        }).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // TODO     按照 规则打分 -> sort
        List<String> filterSort__stockCodeList = scoreSort(filter__stockCodeList2, data, tradeDate, 20);
        // List<String> filterSort__stockCodeList = filter__stockCodeList2.stream().limit(20).collect(Collectors.toList());


        return filterSort__stockCodeList;
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
    public static List<String> scoreSort(Collection<String> stockCodeList,
                                         BacktestCache data,
                                         LocalDate tradeDate,
                                         int N) {


        Map<String, String> codeNameMap = data.stock__codeNameMap;


        // ----------------- 规则排名

        // 金额 -> 涨幅榜（近10日） -> ...

        // TODO     RPS（50） -> 大均线多头（20） -> 60日新高（10） -> 涨幅榜（10） -> 成交额-近10日（10） -> ...


        // Step 2: 计算各项指标 & 打分
        List<QuickOption.StockScore> scoredStocks = Lists.newArrayList();


        // 用于归一化处理
        double maxRPS和 = 0;
        double maxMidReturn = 0;
        double max大均线多头 = 0;
        double maxN日新高 = 0;
        double maxAmount = 0;


        // Step 2.1: 遍历所有股票，计算原始值
        for (String code : stockCodeList) {
            String stockName = codeNameMap.get(code);


            // -------------------------------------------------------------------------------------------


            // BUY策略   ->   已完成init
            // StockFun fun = stockFunMap.computeIfAbsent(stockCode, k -> new StockFun(k, stockDO));
            StockFun fun = stockFunMap.get(code);


            KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();


            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
            Integer idx = dateIndexMap.get(tradeDate);


            double[] amoArr = klineArrDTO.amo;


            double[] rps10_arr = extDataArrDTO.rps10;
            double[] rps20_arr = extDataArrDTO.rps20;
            double[] rps50_arr = extDataArrDTO.rps50;
            double[] rps120_arr = extDataArrDTO.rps120;
            double[] rps250_arr = extDataArrDTO.rps250;


            double[] 中期涨幅_arr = extDataArrDTO.中期涨幅;


            boolean[] 大均线多头_arr = extDataArrDTO.大均线多头;
            boolean[] N日新高_arr = extDataArrDTO.N日新高;


            double rps10 = getByDate(rps10_arr, dateIndexMap, tradeDate);
            double rps20 = getByDate(rps20_arr, dateIndexMap, tradeDate);
            double rps50 = getByDate(rps50_arr, dateIndexMap, tradeDate);
            double rps120 = getByDate(rps120_arr, dateIndexMap, tradeDate);
            double rps250 = getByDate(rps250_arr, dateIndexMap, tradeDate);


            // RPS和
            double RPS和1 = rps10 + rps20 + rps50;
            double RPS和2 = rps50 + rps120 + rps250;

            double RPS和 = Math.max(RPS和1, RPS和2);


            // 中期涨幅
            double 中期涨幅 = getByDate(中期涨幅_arr, dateIndexMap, tradeDate);


            // 大均线多头
            int 大均线多头 = bool2Int(getByDate(大均线多头_arr, dateIndexMap, tradeDate));
            // 60日新高
            int N日新高 = bool2Int(getByDate(N日新高_arr, dateIndexMap, tradeDate));


            // AMO
            double amount = getByDate(amoArr, dateIndexMap, tradeDate);


            // -------------------------------------------------------------------------------------------


//            double[] closes = stockCode_close_map.get(code);
//            if (closes == null || closes.length < 20) continue;

            // 近10日涨幅：(今日收盘价 / 10日前收盘价 - 1)
            // double changePct_d10 = (closes[closes.length - 1] / closes[closes.length - 11]) - 1;

            // 中期涨幅：(今日收盘价 / 20日前收盘价 - 1)
            // double midReturn = (closes[closes.length - 1] / closes[closes.length - 21]) - 1;


            // 更新最大值用于归一化
            maxRPS和 = Math.max(maxRPS和, RPS和);
            maxMidReturn = Math.max(maxMidReturn, 中期涨幅);
            max大均线多头 = Math.max(max大均线多头, 大均线多头);
            maxN日新高 = Math.max(maxN日新高, N日新高);
            maxAmount = Math.max(maxAmount, amount);


            scoredStocks.add(new QuickOption.StockScore(code, stockName, RPS和, 中期涨幅, 大均线多头, N日新高, amount, 0));
        }


        // Step 3: 归一化 & 加权打分
        for (QuickOption.StockScore s : scoredStocks) {


            // RPS（50） -> 大均线多头（20） -> 60日新高（10） -> 涨幅榜（10） -> 成交额-近10日（10） -> ...


            double rpsScore = maxRPS和 == 0 ? 0 : s.RPS和 / maxRPS和 * 50;                         // 权重50%
            double 大均线Score = max大均线多头 == 0 ? 0 : s.大均线多头 / max大均线多头 * 20;            // 权重20%
            double 新高Score = maxN日新高 == 0 ? 0 : s.N日新高 / maxN日新高 * 10;                     // 权重10%
            double midScore = maxMidReturn == 0 ? 0 : s.midTermChangePct / maxMidReturn * 10;     // 权重10%
            double amountScore = maxAmount == 0 ? 0 : s.amount / maxAmount * 10;                  // 权重10%


            s.score = rpsScore + 大均线Score + 新高Score + midScore + amountScore;
        }


        // Step 4: 按得分排序，取前N名
        List<QuickOption.StockScore> topNStocks = scoredStocks.stream()
                                                              .sorted(Comparator.comparingDouble((QuickOption.StockScore s) -> -s.getScore()))
                                                              .limit(N)
                                                              .collect(Collectors.toList());


        // 输出结果或进一步操作
        topNStocks.forEach(JSON::toJSONString);


        return topNStocks.stream().map(QuickOption.StockScore::getStockCode).collect(Collectors.toList());
    }


    public static boolean getByDate(boolean[] arr, Map<LocalDate, Integer> dateIndexMap, LocalDate tradeDate) {
        Integer idx = dateIndexMap.get(tradeDate);

        if (null == idx) {
            // 当前 交易日  ->  未上市/停牌
            return false;
        }

        return arr[idx];
    }


    public static double getByDate(double[] arr, Map<LocalDate, Integer> dateIndexMap, LocalDate tradeDate) {
        Integer idx = dateIndexMap.get(tradeDate);

        if (null == idx) {
            // 当前 交易日  ->  未上市/停牌
            return Double.NaN;
        }

        return arr[idx];
    }


/**
 * 个股   指定日期 -> 收盘价
 *
 * @param blockCode
 * @param tradeDate
 * @return
 */
//    private double getBlockClosePrice(String blockCode, LocalDate tradeDate) {
//        Double closePrice = data.stock__dateCloseMap.get(blockCode).get(DateTimeUtil.format_yyyy_MM_dd(tradeDate));
//        return closePrice == null ? 0.0 : closePrice;
//    }

/**
 * 个股   指定日期 -> 收盘价
 *
 * @param stockCode
 * @param tradeDate
 * @return
 */
//    private double getStockClosePrice(String stockCode, LocalDate tradeDate) {
//        Double closePrice = data.stock__dateCloseMap.get(stockCode).get(DateTimeUtil.format_yyyy_MM_dd(tradeDate));
//        return closePrice == null ? 0.0 : closePrice;
//    }

}
