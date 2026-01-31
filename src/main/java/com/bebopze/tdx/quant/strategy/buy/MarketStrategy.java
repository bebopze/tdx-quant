package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.service.MarketService;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 大盘策略
 *
 * @author: bebopze
 * @date: 2026/1/31
 */
@Slf4j
@Component
public class MarketStrategy {


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockStrategy topBlockStrategy;


    /**
     * 大盘-熊市：true/false
     *
     * @param data
     * @param tradeDate
     * @return
     */
    public boolean marketBear(BacktestCache data, LocalDate tradeDate) {


        QaMarketMidCycleDO marketInfo = data.marketCache.get(tradeDate, k -> marketService.marketInfo(tradeDate));
        Assert.notNull(marketInfo, "[大盘量化]数据为空：" + tradeDate);


        // 大盘-牛熊：1-牛市；2-熊市；
        Integer marketBullBearStatus = marketInfo.getMarketBullBearStatus();
        // 大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
        Integer marketMidStatus = marketInfo.getMarketMidStatus();


        // 2-熊市
        return marketBullBearStatus == 2;
    }


    public List<String> bearRule(BacktestCache data,
                                 LocalDate tradeDate,
                                 Map<String, String> buy_infoMap,
                                 double posRate) {


        // -------------------------------------------------------------------------------------------------------------
        //                                                1、大盘牛熊
        // -------------------------------------------------------------------------------------------------------------


        // 熊市   =>   唯一买入场景   大盘极限底->ETF策略
        Set<String> inTopBlock__stockCodeSet = Sets.newHashSet();
        topBlockStrategy.buyStrategy_ETF(inTopBlock__stockCodeSet, data, tradeDate, buy_infoMap, posRate);


        // 按照 规则打分 -> sort（取成交额 TOP50   ->   等比 抄底买入）
        long start_5 = System.currentTimeMillis();
        List<String> sort__stockCodeList = ScoreSort.scoreSort__AMO_RPS(inTopBlock__stockCodeSet, data, tradeDate, 50);
        log.info("MarketStrategy - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));


        return sort__stockCodeList;


//        // 1-牛市
//        if (marketBullBearStatus == 1) {
//            // 牛市策略   ->   个股 B/S策略 、 主线板块 策略
//        }

    }


//    public List<String> rule_0(BacktestCache data,
//                               LocalDate tradeDate,
//                               Map<String, String> buy_infoMap,
//                               double posRate) {
//
//
//        // -------------------------------------------------------------------------------------------------------------
//        //                                                1、大盘牛熊
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        QaMarketMidCycleDO marketInfo = data.marketCache.get(tradeDate, k -> marketService.marketInfo(tradeDate));
//        Assert.notNull(marketInfo, "[大盘量化]数据为空：" + tradeDate);
//
//        // 大盘-牛熊：1-牛市；2-熊市；
//        Integer marketBullBearStatus = marketInfo.getMarketBullBearStatus();
//        // 大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
//        Integer marketMidStatus = marketInfo.getMarketMidStatus();
//
//
//        // 1-牛市
//        if (marketBullBearStatus == 1) {
//            // TODO   ->   牛市策略   ->   个股 B/S策略 、 主线板块 策略
//        }
//        // 2-熊市
//        else if (marketBullBearStatus == 2) {
//            // TODO   ->   熊市策略   ->   个股 B/S策略 、 主线板块 策略
//
//
//            // 熊市   =>   唯一买入场景   大盘极限底->ETF策略
//            Set<String> inTopBlock__stockCodeSet = Sets.newHashSet();
//            topBlockStrategy.buyStrategy_ETF(inTopBlock__stockCodeSet, data, tradeDate, buy_infoMap, posRate);
//
//
//            // 按照 规则打分 -> sort（取成交额 TOP50   ->   等比 抄底买入）
//            long start_5 = System.currentTimeMillis();
//            List<String> sort__stockCodeList = ScoreSort.scoreSort__AMO_RPS(inTopBlock__stockCodeSet, data, tradeDate, 50);
//            log.info("BacktestBuyStrategy_ETF - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));
//
//
//            return sort__stockCodeList;
//        }
//
//
//    }


}