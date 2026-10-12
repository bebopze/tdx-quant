package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.MapUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.strategy.MarketStrategy;
import com.bebopze.tdx.quant.strategy.TopBlockStrategy;
import com.bebopze.tdx.quant.strategy.sell.BacktestSellStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

import static com.bebopze.tdx.quant.common.constant.BuyStrategyEnum.涨停_SSF多_月多;
import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;
import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btOpenBSDTO;


/**
 * 回测 - 微盘股（每天取AB股 总市值排名 后400 的上市公司，剔除掉ST、*ST、暂停上市、新股未开板 及 北交所的股票）
 *
 *
 * 总市值   = 当日收盘价 × 总股本
 * 流通市值 = 当日收盘价 × 流通股本
 *
 * @author: bebopze
 * @date: 2026/9/11
 */
@Slf4j
@Component
public class BacktestBuyStrategy_WPG /* extends AbstractBuyStrategy */ implements BuyStrategy {


    @Autowired
    private MarketStrategy marketStrategy;

    @Autowired
    private TopBlockStrategy topBlockStrategy;


    @Lazy
    @Autowired
    private BacktestSellStrategy backtestSellStrategy;


    @Autowired
    private BuyStrategy__ConCombiner__TopStock buyStrategy__conCombiner__topStock;


    @Override
    public String key() {
        return "微盘股";
    }


    /**
     * 微盘股  ->  无 主线策略（关闭通用 主线策略   ->   extends AbstractBuyStrategy）
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


//        // -------------------------------------------------------------------------------------------------------------
//        //                                                2、主线板块
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        long start_1 = System.currentTimeMillis();
//        Set<String> topBlockCodeSet = topBlockStrategy.topBlock(topBlockStrategyEnum, data, tradeDate, btCompareDTO.get().isTop1TopBlockFlag());
//        log.info("AbstractBuyStrategy - topBlock     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_1));


        // -------------------------------------------------------------------------------------------------------------
        //                                                3、（强势）个股
        // -------------------------------------------------------------------------------------------------------------


        // B策略   ->   强势个股
        long start_2 = System.currentTimeMillis();
        Set<String> buy__topStock__codeSet = buy__topStock__codeSet(buyConSet, data, tradeDate, buy_infoMap, ztFlag);
        log.info("WeiPanGu - buy__topStock__codeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


//        // -------------------------------------------------------------------------------------------------------------
//        //                                                4、个股 -> IN 主线板块
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        // 强势个股   ->   IN 主线板块
//        long start_3 = System.currentTimeMillis();
//        Set<String> inTopBlock__stockCodeSet = topBlockStrategy.inTopBlock__stockCodeSet(topBlockCodeSet, buy__topStock__codeSet, data, tradeDate);
//        log.info("AbstractBuyStrategy - inTopBlock__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));


        // -------------------------------------------------------------------------------------------------------------


        Set<String> inTopBlock__stockCodeSet = buy__topStock__codeSet;


        // -------------------------------------------------------------------------------------------------------------
        //                                                5、大盘极限底 -> 指数ETF 策略
        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        long start_4 = System.currentTimeMillis();
        topBlockStrategy.buyStrategy_ETF(inTopBlock__stockCodeSet, data, tradeDate, buy_infoMap, posRate);
        log.info("WeiPanGu - buyStrategy_ETF     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_4));


//        // -------------------------------------------------------------------------------------------------------------
//        //                                                6、规则打分
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        // 按照 规则打分 -> sort
//        long start_5 = System.currentTimeMillis();
//        List<String> sort__stockCodeList = ScoreSort.scoreSort__AMO_RPS(inTopBlock__stockCodeSet, data, tradeDate, btCompareDTO.get().getScoreSortN());
//        log.info("WeiPanGu - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));
//
//
//        return sort__stockCodeList;


        List<String> sort__stockCodeList = Lists.newArrayList(inTopBlock__stockCodeSet);
        log.info("WeiPanGu - scoreSort     >>>     sort__stockCodeList : {} , totalTime : {}", sort__stockCodeList.size(), DateTimeUtil.formatNow2Hms(start_4));
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
    @Override
    public Set<String> buy__topStock__codeSet(Set<String> buyConSet,
                                              BacktestCache data,
                                              LocalDate tradeDate,
                                              Map<String, String> buy_infoMap,
                                              Boolean ztFlag) {


        // -------------------------------------------------------------------------------------------------------------
        // ----------------------------------------- 微盘股（全A总市值排名 倒数400） ---------------------------------------
        // -------------------------------------------------------------------------------------------------------------


        // 微盘股 - 总市值
        Map<String, Double> wpgCode_zsz_Map = Maps.newHashMap();


        data.A_stockDOList.forEach(stockDO -> {

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


            ExtDataDTO extDataDTO = fun.getExtDataDTOList().get(idx);


            // ---------------------------------------------------------------------------------------------------------


            // 剔除掉ST、*ST、暂停上市、新股未开板 及 北交所的股票

            // ST
            // stockDO.getName().contains("ST") // TODO   暂忽略（需要：ST 历史快照表）

            // 新股（已过滤）

            // 过滤  ->  北交所
            if (StockMarketEnum.isBJ(stockCode)) {
                return;
            }


            // ---------------------------------------------------------------------------------------------------------


            // 总市值（元）
            double zsz = extDataDTO.getZsz();
            if (!Double.isNaN(zsz) && zsz > 0) {
                // log.info("wpgCode_zsz_Map     >>>     date : {} , [{}-{}] , zsz : {} 亿", tradeDate, stockCode, stockDO.getName(), NumUtil.of(zsz / 1_0000_0000, 2));
                wpgCode_zsz_Map.put(stockCode, zsz);
            }
        });


        // 总市值 排名（小 -> 大）
        Map<String, Double> zsz__sortedMap = MapUtil.sortByValue(wpgCode_zsz_Map);


        // 微盘股 列表  ->  取Top400
        List<String> wpgCodeList = zsz__sortedMap.keySet().stream().limit(btCompareDTO.get().getScoreSortN()).toList();
        log.info("wpgCodeList     >>>     date : {} , zsz__sortedMap__size : {} , 微盘股_size : {} , min_微盘股 : {} , max_微盘股 : {}", tradeDate, zsz__sortedMap.size(), wpgCodeList.size(),
                 NumUtil.of(zsz__sortedMap.get(wpgCodeList.getFirst()) / 1_0000_0000, 2),
                 NumUtil.of(zsz__sortedMap.get(wpgCodeList.getLast()) / 1_0000_0000, 2));


//        // ---------------- DEBUG
//        int[] top = {0};
//        zsz__sortedMap.forEach((stockCode, zsz) -> {
//            if (top[0] < 400) {
//                log.info("wpgCodeSet     >>>     date : {} , [{}-{}] , zsz : {} 亿 , 排名 : {}", tradeDate, stockCode, data.getOrCreateStockFun(stockCode).getName(), NumUtil.of(zsz / 1_0000_0000, 2), top[0]++);
//            }
//        });


        // -------------------------------------------------------------------------------------------------------------
        // -------------------------------------------------------------------------------------------------------------
        // -------------------------------------------------------------------------------------------------------------


        Set<String> buy__topStock__codeSet = Sets.newHashSet();


        // 只遍历 微盘股列表
        wpgCodeList.forEach(stockCode -> {
//      data.getAllType_stockDOList(btCompareDTO.get().getBtStockType()).forEach(stockDO -> {


            BaseStockDO stockDO = data.codeStockMap.get(stockCode);


            // String stockCode = stockDO.getCode();
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


//            ExtDataDTO extDataDTO = fun.getExtDataDTOList().get(idx);


            // ---------------------------------------------------------------------------------------------------------


            // 微盘股  ->  B（当前个股100%为 微盘股）
            boolean signal_B = true;


            // ---------------------------------------------------------------------------------------------------------


            Map<String, Boolean> conMap = Map.of("微盘股_B", true);


//            Map<String, Boolean> conMap = Maps.newHashMap();
//            try {
//                conMap = conMap(klineArrDTO, extDataArrDTO, extDataDTO, idx);
//            } catch (Exception ex) {
//                log.error("conMap - err     >>>     stockCode : {} , tradeDate : {} , errMsg : {}", stockCode, tradeDate, ex.getMessage(), ex);
//            }
//
//
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // 是否买入       =>       conList   ->   全为 true
//            boolean signal_B = BuyStrategy__ConCombiner.calcCon(buyConSet, conMap);


            // ---------------------------------------------------------------------------------------------------------


            // B   +   anyMatch__buyStrategy          // ❌❌❌ 年收益率 从100%  ->  10% ❌❌❌（废弃！）
            // signal_B = signal_B && buyStrategy__conCombiner__topStock.anyMatch__buyStrategy(extDataDTO);


            // ---------------------------------------------------------------------------------------------------------
            zt_buy(signal_B, buy__topStock__codeSet, data, tradeDate, idx, conMap, buy_infoMap, stockCode, fun, klineArrDTO, extDataArrDTO, ztFlag);
        });


        return buy__topStock__codeSet;
    }


    /**
     * 涨停B 特殊处理
     *
     * @param signal_B
     * @param buy__topStock__codeSet
     * @param data
     * @param tradeDate
     * @param idx
     * @param conMap
     * @param buy_infoMap
     * @param stockCode
     * @param fun
     * @param klineArrDTO
     * @param extDataArrDTO
     * @param ztFlag
     */
    public void zt_buy(boolean signal_B,
                       Set<String> buy__topStock__codeSet,
                       BacktestCache data,
                       LocalDate tradeDate,
                       Integer idx,
                       Map<String, Boolean> conMap,
                       Map<String, String> buy_infoMap,
                       String stockCode,
                       StockFun fun,
                       KlineArrDTO klineArrDTO,
                       ExtDataArrDTO extDataArrDTO,
                       Boolean ztFlag) {


        // ----------------------------------------- ztFlag 过滤策略 ----------------------------------------------------


        // ztFlag 策略   ->   是否过滤 涨停（true/false/不过滤）
        boolean today_涨停 = extDataArrDTO.涨停[idx];
        if (ztFlag != null && !Objects.equals(ztFlag, today_涨停)) {
            return;
        }


        // -------------------------------------------------------------------------------------------------------------


        // B + 未涨停  ->  可买入（今日[close]  ->  直接买入）
        if (signal_B && !today_涨停) {
            buy__topStock__codeSet.add(stockCode);
            buySignalInfo(buy_infoMap, stockCode, data, idx, conMap);
        }


        // B + 涨停  ->  无法买入（最简化处理：[next_close] = [next_open]，次日开盘 直接买入）
        if (signal_B && today_涨停) {

            if (idx < fun.getMaxIdx()) {

                LocalDate next_date = klineArrDTO.date[idx + 1];


                // 次日S  ->  不能B（提前预知 -> 次日收盘价？？？❌❌❌）

                // 今日B + 涨停     =>     今日S  ->  次日不能B
                Set<String> nextDate__sellStockCodeSet = backtestSellStrategy.rule(btCompareDTO.get().getTopBlockStrategyEnum(), data, tradeDate, Sets.newHashSet(stockCode), Maps.newHashMap(), btCompareDTO.get());
                boolean next_date_S = nextDate__sellStockCodeSet.contains(stockCode);
                if (next_date_S /*|| nextDate__大盘仓位限制->等比减仓*/) {
                    return;
                }


                // -------------------------------------------------------------------------------------------------


                // 今日B + 涨停     =>     次日 -> 开盘B
                btOpenBSDTO.get().today_date = tradeDate;
                btOpenBSDTO.get().next_date = next_date;
                btOpenBSDTO.get().open_B___stockCodeSet.add(stockCode);
                btOpenBSDTO.get().open_B___buy_infoMap.put(stockCode, 涨停_SSF多_月多.getDesc());
            }
        }
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