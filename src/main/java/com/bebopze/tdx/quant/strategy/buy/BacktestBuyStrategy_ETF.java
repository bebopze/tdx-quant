package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.bebopze.tdx.quant.strategy.sell.BacktestSellStrategy;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;

import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;


/**
 * 回测 - B策略（高抛低吸  ->  C_MA_偏离率）
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategy_ETF implements BuyStrategy {


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockStrategy topBlockStrategy;

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
        return "ETF";
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
        log.info("BacktestBuyStrategy_ETF - topBlock     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_1));


        // -------------------------------------------------------------------------------------------------------------
        //                                                3、（强势）个股
        // -------------------------------------------------------------------------------------------------------------


        // B策略   ->   强势ETF
        long start_2 = System.currentTimeMillis();
        Set<String> buy__topStock__codeSet = buy__topETF__codeSet(buyConSet, data, tradeDate, buy_infoMap, ztFlag);
        log.info("BacktestBuyStrategy_ETF - buy__topETF__codeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 强势ETF   ->   IN 主线板块
        long start_3 = System.currentTimeMillis();
        Set<String> inTopBlock__stockCodeSet = topBlockStrategy.inTopBlock__stockCodeSet(topBlockCodeSet, buy__topStock__codeSet, data, tradeDate);
        log.info("BacktestBuyStrategy_ETF - inTopBlock__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        long start_4 = System.currentTimeMillis();
        if (CollectionUtils.isEmpty(inTopBlock__stockCodeSet)) { // ETF策略  ->  只有在 无ETF可买 时 才触发
            topBlockStrategy.buyStrategy_ETF(inTopBlock__stockCodeSet, data, tradeDate, buy_infoMap, posRate);
        }
        log.info("BacktestBuyStrategy_ETF - buyStrategy_ETF     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_4));


        // -------------------------------------------------------------------------------------------------------------


        // B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
        // backtestStrategy.buy_sell__signalConflict(data, tradeDate, inTopBlock__stockCodeList);


        // -------------------------------------------------------------------------------------------------------------


        // 按照 规则打分 -> sort
        long start_5 = System.currentTimeMillis();
        List<String> sort__stockCodeList = ScoreSort.scoreSort__RPS(inTopBlock__stockCodeSet, data, tradeDate, btCompareDTO.get().getScoreSortN());
        log.info("BacktestBuyStrategy_ETF - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));


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
    private Set<String> buy__topETF__codeSet(Set<String> buyConSet,
                                             BacktestCache data,
                                             LocalDate tradeDate,
                                             Map<String, String> buy_infoMap,
                                             Boolean ztFlag) {


        Set<String> buy__topStock__codeSet = Sets.newHashSet();
        data.ETF_stockDOList.forEach(stockDO -> {

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


//            // ----------------------------------------- ztFlag 过滤策略 ------------------------------------------------
//
//
//            // ztFlag 策略   ->   是否过滤 涨停（true/false/不过滤）
//            boolean today_涨停 = extDataArrDTO.涨停[idx];
//            if (ztFlag != null && !Objects.equals(ztFlag, today_涨停)) {
//                return;
//            }
//
//
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            ExtDataDTO extDataDTO = fun.getExtDataDTOList().get(idx);
//
//
            // ---------------------------------------------------------------------------------------------------------


//            double rps50 = extDataArrDTO.rps50[idx];
//            double rps120 = extDataArrDTO.rps120[idx];
//            double rps250 = extDataArrDTO.rps250[idx];


            boolean 月多 = extDataArrDTO.月多[idx];
            boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
            boolean RPS红 = extDataArrDTO.RPS红[idx] && extDataArrDTO.rps50[idx] >= 90;
            boolean SSF多 = extDataArrDTO.SSF多[idx];


            boolean signal_B = (月多 || 均线预萌出) && RPS红 && SSF多;
            if (signal_B) {
                buy__topStock__codeSet.add(stockCode);
                buy_infoMap.put(stockCode, "主线ETF（月多2）");
            }


            // ---------------------------------------------------------------------------------------------------------


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
//
//
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // B   +   anyMatch__buyStrategy          // ❌❌❌ 年收益率 从100%  ->  10% ❌❌❌（废弃！）
//            // signal_B = signal_B && buyStrategy__conCombiner__topStock.anyMatch__buyStrategy(extDataDTO);
//
//
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // B + 未涨停  ->  可买入（今日[close]  ->  直接买入）
//            if (signal_B && !today_涨停) {
//                buy__topStock__codeSet.add(stockCode);
//                buySignalInfo(buy_infoMap, stockCode, data, idx, conMap);
//            }
//
//
//            // B + 涨停  ->  无法买入（最简化处理：[next_close] = [next_open]，次日开盘 直接买入）
//            if (signal_B && today_涨停) {
//
//                if (idx < fun.getMaxIdx()) {
//
//                    LocalDate next_date = klineArrDTO.date[idx + 1];
//
//
//                    // 次日S  ->  不能B（提前预知 -> 次日收盘价？？？❌❌❌）
//
//                    // 今日B + 涨停     =>     今日S  ->  次日不能B
//                    Set<String> nextDate__sellStockCodeSet = backtestSellStrategy.rule(btCompareDTO.get().getTopBlockStrategyEnum(), data, tradeDate, Sets.newHashSet(stockCode), Maps.newHashMap(), btCompareDTO.get());
//                    boolean next_date_S = nextDate__sellStockCodeSet.contains(stockCode);
//                    if (next_date_S /*|| nextDate__大盘仓位限制->等比减仓*/) {
//                        return;
//                    }
//
//
//                    // -------------------------------------------------------------------------------------------------
//
//
//                    // 今日B + 涨停     =>     次日 -> 开盘B
//                    btOpenBSDTO.get().today_date = tradeDate;
//                    btOpenBSDTO.get().next_date = next_date;
//                    btOpenBSDTO.get().open_B___stockCodeSet.add(stockCode);
//                    btOpenBSDTO.get().open_B___buy_infoMap.put(stockCode, 涨停_SSF多_月多.getDesc());
//                }
//            }
        });


        return buy__topStock__codeSet;
    }


}