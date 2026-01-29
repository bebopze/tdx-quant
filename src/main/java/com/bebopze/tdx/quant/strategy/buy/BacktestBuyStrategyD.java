package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.bebopze.tdx.quant.strategy.sell.BacktestSellStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;

import static com.bebopze.tdx.quant.common.constant.BuyStrategyEnum.涨停_SSF多_月多;
import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;
import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btOpenBSDTO;


/**
 * 回测 - B策略（高抛低吸  ->  C_MA_偏离率）
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategyD implements BuyStrategy {


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockStrategy topBlockStrategy;

    @Autowired
    private BacktestBuyStrategyA backtestBuyStrategyA;

    @Lazy
    @Autowired
    private BacktestStrategy backtestStrategy;

    @Lazy
    @Autowired
    private BacktestSellStrategy backtestSellStrategy;


    @Autowired
    private BuyStrategy__ConCombiner__TopStock buyStrategy__conCombiner__topStock;


    @Override
    public String key() {
        return "D";
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
     * @param ztFlag               是否涨停（打板）
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


        QaMarketMidCycleDO marketInfo = data.marketCache.get(tradeDate, k -> marketService.marketInfo(tradeDate));
        Assert.notNull(marketInfo, "[大盘量化]数据为空：" + tradeDate);

        // 大盘-牛熊：1-牛市；2-熊市；
        Integer marketBullBearStatus = marketInfo.getMarketBullBearStatus();
        // 大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
        Integer marketMidStatus = marketInfo.getMarketMidStatus();


        // 1-牛市
        if (marketBullBearStatus == 1) {
            // TODO   ->   牛市策略   ->   个股 B/S策略 、 主线板块 策略
        }
        // 2-熊市
        else if (marketMidStatus == 2) {
            // TODO   ->   熊市策略   ->   个股 B/S策略 、 主线板块 策略
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
        Set<String> buy__topStock__codeSet = buy__topStock__codeSet(buyConSet, data, tradeDate, buy_infoMap, ztFlag);
        log.info("BacktestBuyStrategyD - buy__topStock__codeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 强势个股   ->   IN 主线板块
        long start_3 = System.currentTimeMillis();
        Set<String> inTopBlock__stockCodeSet = topBlockStrategy.inTopBlock__stockCodeSet(topBlockCodeSet, buy__topStock__codeSet, data, tradeDate);
        log.info("BacktestBuyStrategyD - inTopBlock__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        long start_4 = System.currentTimeMillis();
        topBlockStrategy.buyStrategy_ETF(inTopBlock__stockCodeSet, data, tradeDate, buy_infoMap, posRate);
        log.info("BacktestBuyStrategyD - buyStrategy_ETF     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_4));


        // -------------------------------------------------------------------------------------------------------------


        // B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
        // backtestStrategy.buy_sell__signalConflict(data, tradeDate, inTopBlock__stockCodeList);


        // -------------------------------------------------------------------------------------------------------------


        // 按照 规则打分 -> sort
        long start_5 = System.currentTimeMillis();
        List<String> sort__stockCodeList = ScoreSort.scoreSort(inTopBlock__stockCodeSet, data, tradeDate, btCompareDTO.get().getScoreSortN());
        log.info("BacktestBuyStrategyD - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));


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
    private Set<String> buy__topStock__codeSet(Set<String> buyConSet,
                                               BacktestCache data,
                                               LocalDate tradeDate,
                                               Map<String, String> buy_infoMap,
                                               Boolean ztFlag) {


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


            ExtDataDTO extDataDTO = fun.getExtDataDTOList().get(idx);


            // ---------------------------------------------------------------------------------------------------------


            Map<String, Boolean> conMap = Maps.newHashMap();
            try {
                conMap = conMap(klineArrDTO, extDataArrDTO, extDataDTO, idx);
            } catch (Exception ex) {
                log.error("conMap - err     >>>     stockCode : {} , tradeDate : {} , errMsg : {}", stockCode, tradeDate, ex.getMessage(), ex);
            }


            // ---------------------------------------------------------------------------------------------------------


            // 是否买入       =>       conList   ->   全为 true
            boolean signal_B = BuyStrategy__ConCombiner.calcCon(buyConSet, conMap);


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


    private Map<String, Boolean> conMap(KlineArrDTO klineArrDTO,
                                        ExtDataArrDTO extDataArrDTO,
                                        ExtDataDTO extDataDTO,
                                        Integer idx) {


        // -------------------------------------------------------------------------------------------------------------


        // double 中期涨幅 = extDataArrDTO.中期涨幅N20[idx];
        // int 趋势支撑线 = extDataArrDTO.趋势支撑线[idx];


        // boolean 高位爆量上影大阴 = extDataArrDTO.高位爆量上影大阴[idx];


        // -------------------------------------------------------------------------------------------------------------


//        Map<String, Boolean> conMap = ConvertStockExtData.toBooleanMap(extDataDTO);
        Map<String, Boolean> conMap = BuyStrategy__ConCombiner__TopStock.convertExtDataDTO(extDataDTO);


        // -------------------------------------------------------------------------------------------------------------


        double 中期涨幅N250 = extDataArrDTO.中期涨幅N250[idx];
        conMap.put("中期涨幅N250<150", 中期涨幅N250 < 150);
        conMap.put("中期涨幅N250<350", 中期涨幅N250 < 350);
        conMap.put("中期涨幅N250<700", 中期涨幅N250 < 700);
        conMap.put("中期涨幅N250<1500", 中期涨幅N250 < 1500);


        // -------------------------------------------------------------------------------------------------------------


//        boolean XZZB = extDataArrDTO.XZZB[idx];
//
//
//        boolean SSF多 = extDataArrDTO.SSF多[idx];
//        boolean MA20多 = extDataArrDTO.MA20多[idx];
//
//
//        boolean N60日新高 = extDataArrDTO.N60日新高[idx];
//        boolean N100日新高 = extDataArrDTO.N100日新高[idx];
//        boolean 历史新高 = extDataArrDTO.历史新高[idx];
//
//
//        boolean 百日新高 = extDataArrDTO.百日新高[idx];
//
//
//        boolean 月多 = extDataArrDTO.月多[idx];
//        boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
//        boolean 均线萌出 = extDataArrDTO.均线萌出[idx];
//        boolean 小均线多头 = extDataArrDTO.小均线多头[idx];
//        boolean 大均线多头 = extDataArrDTO.大均线多头[idx];
//        boolean 均线大多头 = extDataArrDTO.均线大多头[idx];
//        boolean 均线极多头 = extDataArrDTO.均线极多头[idx];
//
//
//        boolean RPS红 = extDataArrDTO.RPS红[idx];
//        boolean RPS一线红 = extDataArrDTO.RPS一线红[idx];
//        boolean RPS双线红 = extDataArrDTO.RPS双线红[idx];
//        boolean RPS三线红 = extDataArrDTO.RPS三线红[idx];


        // -------------------------------------------------------------------------------------------------------------


//        boolean 百日新高 = false;
//        for (int i = 0; i < 10; i++) {
//            int n_Idx = idx - i;
//            if (n_Idx < 0) {
//                break;
//            }
//
//            boolean _N100日新高 = extDataArrDTO.N100日新高[n_Idx];   // 近10日内 N100日新高
//            if (_N100日新高 && (SSF多 || MA20多)) {
//                百日新高 = true;
//                break;
//            }
//        }


        // -------------------------------------------------------------------------------------------------------------


//        Map<String, Boolean> conMap = Maps.newHashMap();
//
//
//        conMap.put("XZZB", XZZB);
//
//
//        conMap.put("SSF多", SSF多);
//        conMap.put("MA20多", MA20多);
//
//
//        conMap.put("N60日新高", N60日新高);
//        conMap.put("N100日新高", N100日新高);
//        conMap.put("历史新高", 历史新高);
//
//
//        conMap.put("百日新高", 百日新高);
//
//
//        conMap.put("月多", 月多);
//        conMap.put("均线预萌出", 均线预萌出);
//        conMap.put("均线萌出", 均线萌出);
//        conMap.put("小均线多头", 小均线多头);
//        conMap.put("大均线多头", 大均线多头);
//        conMap.put("均线大多头", 均线大多头);
//        conMap.put("均线极多头", 均线极多头);
//
//
//        conMap.put("RPS红", RPS红);
//        conMap.put("RPS一线红", RPS一线红);
//        conMap.put("RPS双线红", RPS双线红);
//        conMap.put("RPS三线红", RPS三线红);


        // -------------------------------------------------------------------------------------------------------------


        // MA200多  =  C>MA200  &&  MA200>prev_MA200
        boolean MA200多 = idx > 0 && klineArrDTO.close[idx] >= extDataArrDTO.MA200[idx]
                && extDataArrDTO.MA200[idx] >= extDataArrDTO.MA200[idx - 1];

        boolean MA250多 = idx > 0 && klineArrDTO.close[idx] >= extDataArrDTO.MA250[idx]
                && extDataArrDTO.MA250[idx] >= extDataArrDTO.MA250[idx - 1];


        boolean 上MA200 = klineArrDTO.close[idx] >= extDataArrDTO.MA200[idx];
        boolean 上MA250 = klineArrDTO.close[idx] >= extDataArrDTO.MA250[idx];


        conMap.put("MA200多", MA200多);
        conMap.put("MA250多", MA250多);
        conMap.put("上MA200", 上MA200);
        conMap.put("上MA250", 上MA250);


        // -------------------------------------------------------------------------------------------------------------


        // ------------------------------------------- 低吸 -------------------------------------------------------------


        double C_SSF_偏离率 = extDataArrDTO.C_SSF_偏离率[idx];
        conMap.put("C_SSF_偏离率<5", C_SSF_偏离率 < 5);


        // ------------------------------------------- 低吸 -------------------------------------------------------------


//        double C_MA_偏离率 = extDataDTO.getC_MA5_偏离率();
        double C_MA_短期偏离率 = extDataDTO.getC_短期MA_偏离率();
//        double C_MA_中期偏离率 = extDataDTO.getC_中期MA_偏离率();
//        double C_MA_长期偏离率 = extDataDTO.getC_长期MA_偏离率();
        conMap.put("C_MA_偏离率<3", C_MA_短期偏离率 < 3);
        conMap.put("C_MA_偏离率<5", C_MA_短期偏离率 < 5);
        conMap.put("C_MA_偏离率<7", C_MA_短期偏离率 < 7);


//        int 短期趋势支撑线 = extDataDTO.get短期支撑线();
//        int 中期趋势支撑线 = extDataDTO.get中期支撑线();
//
//        conMap.put("短期趋势支撑线", 短期趋势支撑线);
//        conMap.put("中期趋势支撑线", 中期趋势支撑线);


        // ------------------------------------------- 低吸 -------------------------------------------------------------


        double C_MA5_偏离率 = extDataDTO.getC_MA5_偏离率();
        double C_MA10_偏离率 = extDataDTO.getC_MA10_偏离率();
        double C_MA20_偏离率 = extDataDTO.getC_MA20_偏离率();
        double C_MA30_偏离率 = extDataDTO.getC_MA30_偏离率();
        double C_MA50_偏离率 = extDataDTO.getC_MA50_偏离率();
        double C_MA60_偏离率 = extDataDTO.getC_MA60_偏离率();
        double C_MA100_偏离率 = extDataDTO.getC_MA100_偏离率();


        conMap.put("C_MA5_偏离率<5", C_MA5_偏离率 < 5);
        conMap.put("C_MA10_偏离率<5", C_MA10_偏离率 < 5);
        conMap.put("C_MA20_偏离率<5", C_MA20_偏离率 < 5);
        conMap.put("C_MA30_偏离率<5", C_MA30_偏离率 < 5);
        conMap.put("C_MA50_偏离率<5", C_MA50_偏离率 < 5);
        conMap.put("C_MA60_偏离率<5", C_MA60_偏离率 < 5);
        conMap.put("C_MA100_偏离率<5", C_MA100_偏离率 < 5);


        // ------------------------------------------- 限高 -------------------------------------------------------------


        double 中期涨幅 = extDataArrDTO.中期涨幅N20[idx];


        conMap.put("中期涨幅<35", 中期涨幅 < 35);
        conMap.put("中期涨幅<50", 中期涨幅 < 50);
        conMap.put("中期涨幅<100", 中期涨幅 < 100);


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