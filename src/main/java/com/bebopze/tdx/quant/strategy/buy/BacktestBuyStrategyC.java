package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;


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
    private MarketStrategy marketStrategy;

    @Autowired
    private TopBlockStrategy topBlockStrategy;


    @Override
    public String key() {
        return "C";
    }


    /**
     * 买入策略   =   大盘（70%） +  主线板块（25%） +  个股买点（5%）
     *
     * @param topBlockStrategyEnum 主线策略
     * @param buyConSet            B策略
     * @param data                 全量行情
     * @param tradeDate            交易日期
     * @param buy_infoMap          买入个股-交易信号
     * @param posRate              当前 总仓位
     * @param ztFlag               个股是否涨停： true-是；false-否（默认）；null-不过滤；
     * @return
     */
    @TotalTime
    @Override
    public List<String> rule(TopBlockStrategyEnum topBlockStrategyEnum,
                             Set<String> buyConSet,
                             BacktestCache data,
                             LocalDate tradeDate,
                             Map<String, String> buy_infoMap,
                             double posRate,
                             Boolean ztFlag) {


        // -------------------------------------------------------------------------------------------------------------
        //                                                1、大盘牛熊
        // -------------------------------------------------------------------------------------------------------------


        if (marketStrategy.marketBear(data, tradeDate)) {
            return marketStrategy.bearRule(data, tradeDate, buy_infoMap, posRate);
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                                2、主线板块
        // -------------------------------------------------------------------------------------------------------------


        long start_1 = System.currentTimeMillis();
        Set<String> topBlockCodeSet = topBlockStrategy.topBlock(topBlockStrategyEnum, data, tradeDate, btCompareDTO.get().isTop1TopBlockFlag());
        log.info("BacktestBuyStrategyC - topBlock     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_1));


        // -------------------------------------------------------------------------------------------------------------
        //                                                3、（强势）个股
        // -------------------------------------------------------------------------------------------------------------


        // B策略   ->   强势个股
        long start_2 = System.currentTimeMillis();
        Set<String> buy__topStock__codeSet = buy__topStock__codeList(buyConSet, data, tradeDate, buy_infoMap, ztFlag);
        log.info("BacktestBuyStrategyC - buy__topStock__codeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 强势个股   ->   IN 主线板块
        long start_3 = System.currentTimeMillis();
        Set<String> inTopBlock__stockCodeSet = topBlockStrategy.inTopBlock__stockCodeSet(topBlockCodeSet, buy__topStock__codeSet, data, tradeDate);
        log.info("BacktestBuyStrategyC - inTopBlock__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        long start_4 = System.currentTimeMillis();
        topBlockStrategy.buyStrategy_ETF(inTopBlock__stockCodeSet, data, tradeDate, buy_infoMap, posRate);
        log.info("BacktestBuyStrategyC - buyStrategy_ETF     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_4));


        // -------------------------------------------------------------------------------------------------------------


        // 按照 规则打分 -> sort
        long start_5 = System.currentTimeMillis();
        List<String> sort__stockCodeList = ScoreSort.scoreSort__AMO_RPS(inTopBlock__stockCodeSet, data, tradeDate, btCompareDTO.get().getScoreSortN());
        log.info("BacktestBuyStrategyC - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));


        return sort__stockCodeList;
    }


    /**
     * B策略   ->   强势个股
     *
     * @param buyConSet   B策略
     * @param data
     * @param tradeDate
     * @param buy_infoMap
     * @param ztFlag      个股是否涨停： true-是；false-否（默认）；null-不过滤；
     * @return
     */
    private Set<String> buy__topStock__codeList(Set<String> buyConSet,
                                                BacktestCache data,
                                                LocalDate tradeDate,
                                                Map<String, String> buy_infoMap,
                                                Boolean ztFlag) {


//        List<String> buy__topStock__CodeList = Collections.synchronizedList(Lists.newArrayList());
//
//        ParallelCalcUtil.forEach(data.stockDOList,
//                                 stockDO -> {


        Set<String> buy__topStock__codeSet = Sets.newHashSet();
        data.getStockDOList(btCompareDTO.get().getStockType()).forEach(stockDO -> {

            String stockCode = stockDO.getCode();
            StockFun fun = data.getOrCreateStockFun(stockDO);


            KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 当日 - 停牌（003005  ->  2022-10-27）
            Integer idx = dateIndexMap.get(tradeDate);

            // 过滤 停牌/新股       // TODO 个股行情指标 异常数据bug   688692（达梦数据）     kline 301条   extData 300条（首日 2024-06-12 扩展数据 缺失）
            if (idx == null || Double.isNaN(extDataArrDTO.rps50[idx]) || fun.getKlineDTOList().size() != fun.getExtDataDTOList().size()) {
                return;
            }


            // ---------------------------------------------------------------------------------------------------------


            // 涨停 -> 无法买入
            if (ztFlag != null && !Objects.equals(ztFlag, extDataArrDTO.涨停[idx])) {
                return;
            }


            // ---------------------------------------------------------------------------------------------------------


            // Map<String, Boolean> conMap = conMap(extDataArrDTO, idx);


            Map<String, Boolean> conMap = Maps.newHashMap();

            try {
                conMap = conMap(klineArrDTO, extDataArrDTO, idx);
            } catch (Exception ex) {
                log.error("conMap - err     >>>     stockCode : {} , tradeDate : {} , errMsg : {}", stockCode, tradeDate, ex.getMessage(), ex);
            }


            // ---------------------------------------------------------------------------------------------------------


            // 是否买入       =>       conList   ->   全为 true
            boolean signal_B = BuyStrategy__ConCombiner.calcCon(buyConSet, conMap);


            if (signal_B) {
                buy__topStock__codeSet.add(stockCode);
                buySignalInfo(buy_infoMap, stockCode, data, idx, conMap);
            }
        });


//                                 },
//                                 ThreadPoolType.CPU_INTENSIVE);


        return buy__topStock__codeSet;
    }


    private Map<String, Boolean> conMap(KlineArrDTO klineArrDTO, ExtDataArrDTO extDataArrDTO, Integer idx) {


        // -------------------------------------------------------------------------------------------------------------


        // double 中期涨幅 = extDataArrDTO.中期涨幅N20[idx];
        // int 趋势支撑线 = extDataArrDTO.趋势支撑线[idx];


        // boolean 高位爆量上影大阴 = extDataArrDTO.高位爆量上影大阴[idx];


        // -------------------------------------------------------------------------------------------------------------


        boolean SSF多 = extDataArrDTO.SSF多[idx];
        boolean MA20多 = extDataArrDTO.MA20多[idx];


        boolean N60日新高 = extDataArrDTO.N60日新高[idx];
        boolean N100日新高 = extDataArrDTO.N100日新高[idx];
        boolean 历史新高 = extDataArrDTO.历史新高[idx];
        boolean 百日新高 = extDataArrDTO.百日新高[idx];


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
        conMap.put("百日新高", 百日新高);


        conMap.put("月多", 月多);
        conMap.put("均线预萌出", 均线预萌出);
        conMap.put("均线萌出", 均线萌出);
        conMap.put("大均线多头", 大均线多头);


        conMap.put("RPS红", RPS红);
        conMap.put("RPS一线红", RPS一线红);
        conMap.put("RPS双线红", RPS双线红);
        conMap.put("RPS三线红", RPS三线红);


        // -------------------------------------------------------------------------------------------------------------


        // MA200多  =  C>MA200  &&  MA200>prev_MA200
        boolean MA200多 = klineArrDTO.close[idx] >= extDataArrDTO.MA200[idx]
                && extDataArrDTO.MA200[idx] >= extDataArrDTO.MA200[idx - 1];

        boolean MA250多 = klineArrDTO.close[idx] >= extDataArrDTO.MA250[idx]
                && extDataArrDTO.MA250[idx] >= extDataArrDTO.MA250[idx - 1];


        boolean 上MA200 = klineArrDTO.close[idx] >= extDataArrDTO.MA200[idx];
        boolean 上MA250 = klineArrDTO.close[idx] >= extDataArrDTO.MA250[idx];


        conMap.put("MA200多", MA200多);
        conMap.put("MA250多", MA250多);
        conMap.put("上MA200", 上MA200);
        conMap.put("上MA250", 上MA250);


        // -------------------------------------------------------------------------------------------------------------


        return conMap;
    }


    /**
     * buySignalInfo
     *
     * @param buy_infoMap
     * @param stockCode
     * @param data
     * @param idx
     * @param conMap
     */
    private void buySignalInfo(Map<String, String> buy_infoMap,
                               String stockCode,
                               BacktestCache data,
                               Integer idx,
                               Map<String, Boolean> conMap) {


        // 动态收集所有为 true 的信号名称，按固定顺序拼接
        List<String> signalInfoList = Lists.newArrayList();


        // ---------------------------------------------------------------------------


        // 行业板块
        String pthyLv2 = data.getPthyLv2(stockCode);
        String getYjhyLv1 = data.getYjhyLv1(stockCode);
        signalInfoList.add(pthyLv2);
        signalInfoList.add(getYjhyLv1 + "|");


        // ---------------------------------------------------------------------------

        // conList
        conMap.forEach((k, v) -> {

            // "N60日新高" - true/false

            if (v) {
                signalInfoList.add(k);
            }
        });


        // ---------------------------------------------------------------------------


        signalInfoList.add("|idx-" + idx);


        // ---------------------------------------------------------------------------


        // stockCode - infoList
        buy_infoMap.put(stockCode, String.join(",", signalInfoList));
    }


}