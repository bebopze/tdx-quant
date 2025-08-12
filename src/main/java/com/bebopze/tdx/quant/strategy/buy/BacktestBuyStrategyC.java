package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.BlockNewIdEnum;
import com.bebopze.tdx.quant.common.constant.ThreadPoolType;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.ParallelCalcUtil;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.strategy.QuickOption;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.util.BoolUtil.bool2Int;


/**
 * 回测 - B策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategyC implements BuyStrategy {


    @Autowired
    private TopBlockService topBlockService;

    @Autowired
    private BacktestBuyStrategyA backtestBuyStrategyA;


    @Override
    public String key() {
        return "C";
    }


    @Override
    public List<String> rule(BacktestCache data, LocalDate tradeDate, Map<String, String> buy_infoMap) {
        return Lists.newArrayList();
    }

    /**
     * 买入策略   =   大盘（70%） +  主线板块（25%） +  个股买点（5%）
     *
     * @param buyConList
     * @param data
     * @param tradeDate
     * @param buy_infoMap
     * @return
     */
    public List<String> rule2(List<String> buyConList,
                              BacktestCache data,
                              LocalDate tradeDate,
                              Map<String, String> buy_infoMap) {


        // -------------------------------------------------------------------------------------------------------------
        //                                                2、主线板块
        // -------------------------------------------------------------------------------------------------------------


        Set<String> topBlockCodeSet = data.topBlockCache.get(tradeDate, k -> {


            // 主线板块
            Map<String, Integer> blockCode_count_Map = topBlockService.topBlockRate(BlockNewIdEnum.百日新高.getBlockNewId(), tradeDate, 2, 10);

            // 主线板块   ->   仅取 TOP1 板块
            Set<String> topBlockCodeSet__db = MapUtils.isEmpty(blockCode_count_Map) ? Sets.newHashSet() :
                    Sets.newHashSet(blockCode_count_Map.keySet().iterator().next().split("-")[0]);

            return topBlockCodeSet__db;
        });


        // -------------------------------------------------------------------------------------------------------------
        //                                                3、（强势）个股
        // -------------------------------------------------------------------------------------------------------------


//        List<String> buy__topStock__CodeList = Collections.synchronizedList(Lists.newArrayList());
//
//        ParallelCalcUtil.forEach(data.stockDOList,
//                                 stockDO -> {


        List<String> buy__topStock__CodeList = Lists.newArrayList();
        data.stockDOList.forEach(stockDO -> {


            String stockCode = stockDO.getCode();


            StockFun fun = data.stockFunCache.get(stockCode, k -> new StockFun(k, stockDO));

            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 当日 - 停牌（003005  ->  2022-10-27）
            Integer idx = dateIndexMap.get(tradeDate);

            // 过滤 停牌/新股
            if (idx == null || idx < 50) {
                return;
            }


            // -----------------------------------------------------------------------------------------


            Map<String, Boolean> conMap = conMap(extDataArrDTO, idx);


            // -----------------------------------------------------------------------------------------


            // 是否买入       =>       conList   ->   全为 true
            boolean signal_B = BuyStrategy__ConCombiner.calcCon(buyConList, conMap);


            if (signal_B) {

                buy__topStock__CodeList.add(stockCode);


                // ----------------------------------------------------- buySingleInfo


                buySingleInfo(buy_infoMap, stockCode, data, idx, conMap);
            }
        });


//                                 },
//                                 ThreadPoolType.CPU_INTENSIVE);


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 个股   ->   IN 主线板块
        List<String> filter__stockCodeList2 = buy__topStock__CodeList
                .stream()
                .filter(stockCode -> {


                    // 个股   -对应->   板块列表
                    Set<String> stock__blockCodeSet = data.stockCode_blockCodeSet_Map.getOrDefault(stockCode, Sets.newHashSet());

                    for (String stock__blockCode : stock__blockCodeSet) {


                        // 个股   ->   IN 主线板块
                        boolean inTopBlock = topBlockCodeSet.contains(stock__blockCode);

                        if (inTopBlock) {

                            log.debug("个股 -> IN 主线板块     >>>     {} , [{}-{}] , [{}-{}]", tradeDate,
                                      stockCode, data.stock__codeNameMap.get(stockCode),
                                      stock__blockCode, data.block__codeNameMap.get(stock__blockCode));

                            return true;
                        }
                    }


                    return false;
                }).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        backtestBuyStrategyA.buyStrategy_ETF(filter__stockCodeList2, data, tradeDate, buy_infoMap);


        // -------------------------------------------------------------------------------------------------------------


        // TODO     按照 规则打分 -> sort
        List<String> filterSort__stockCodeList = scoreSort(filter__stockCodeList2, data, tradeDate, 100);


        return filterSort__stockCodeList;
    }


    private Map<String, Boolean> conMap(ExtDataArrDTO extDataArrDTO, Integer idx) {


        // -------------------------------------------------------------------------------------------------------------


        // double 中期涨幅 = extDataArrDTO.中期涨幅[idx];
        // int 趋势支撑线 = extDataArrDTO.趋势支撑线[idx];


        // boolean 高位爆量上影大阴 = extDataArrDTO.高位爆量上影大阴[idx];


        // -------------------------------------------------------------------------------------------------------------


        boolean SSF多 = extDataArrDTO.SSF多[idx];
        boolean MA20多 = extDataArrDTO.MA20多[idx];


        boolean N60日新高 = extDataArrDTO.N60日新高[idx];
        boolean N100日新高 = extDataArrDTO.N100日新高[idx];
        boolean 历史新高 = extDataArrDTO.历史新高[idx];


        boolean 月多 = extDataArrDTO.月多[idx];
        boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
        boolean 均线萌出 = extDataArrDTO.均线萌出[idx];
        boolean 大均线多头 = extDataArrDTO.大均线多头[idx];


        boolean RPS红 = extDataArrDTO.RPS红[idx];
        boolean RPS一线红 = extDataArrDTO.RPS一线红[idx];
        boolean RPS双线红 = extDataArrDTO.RPS双线红[idx];
        boolean RPS三线红 = extDataArrDTO.RPS三线红[idx];


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        Map<String, Boolean> conMap = Maps.newHashMap();


        conMap.put("SSF多", SSF多);
        conMap.put("MA20多", MA20多);


        conMap.put("N60日新高", N60日新高);
        conMap.put("N100日新高", N100日新高);
        conMap.put("历史新高", 历史新高);


        conMap.put("月多", 月多);
        conMap.put("均线预萌出", 均线预萌出);
        conMap.put("均线萌出", 均线萌出);
        conMap.put("大均线多头", 大均线多头);


        conMap.put("RPS红", RPS红);
        conMap.put("RPS一线红", RPS一线红);
        conMap.put("RPS双线红", RPS双线红);
        conMap.put("RPS三线红", RPS三线红);


        return conMap;
    }


    /**
     * buySingleInfo
     *
     * @param buy_infoMap
     * @param stockCode
     * @param data
     * @param idx
     * @param conMap
     */
    private void buySingleInfo(Map<String, String> buy_infoMap,
                               String stockCode,
                               BacktestCache data,
                               Integer idx,
                               Map<String, Boolean> conMap) {


        // 动态收集所有为 true 的信号名称，按固定顺序拼接
        List<String> singleInfoList = Lists.newArrayList();


        // ---------------------------------------------------------------------------


        // 行业板块
        String pthyLv2 = data.getPthyLv2(stockCode);
        String getYjhyLv1 = data.getYjhyLv1(stockCode);
        singleInfoList.add(pthyLv2);
        singleInfoList.add(getYjhyLv1 + "     ");


        // ---------------------------------------------------------------------------

        // conList
        conMap.forEach((k, v) -> {

            // "N60日新高" - true/false

            if (v) {
                singleInfoList.add(k);
            }
        });


        // ---------------------------------------------------------------------------


        singleInfoList.add("idx-" + idx);


        // ---------------------------------------------------------------------------


        // stockCode - infoList
        buy_infoMap.put(stockCode, String.join(",", singleInfoList));
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
        for (String code : stockCodeList) {
            String stockName = codeNameMap.get(code);


            // -------------------------------------------------------------------------------------------


            // BUY策略   ->   已完成init
            StockFun fun = data.stockFunCache.getIfPresent(code);


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


            scoredStocks.add(new QuickOption.StockScore(code, stockName, RPS和, 中期涨幅, 大均线多头, N日新高, amount, 0));
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


}