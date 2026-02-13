package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;


/**
 * B策略  -  Abstract（rule通用逻辑 模板类）
 *
 * @author: bebopze
 * @date: 2026/2/13
 */
@Slf4j
@Component
public abstract class AbstractBuyStrategy implements BuyStrategy {


    @Autowired
    private MarketStrategy marketStrategy;

    @Autowired
    private TopBlockStrategy topBlockStrategy;


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
        log.info("AbstractBuyStrategy - topBlock     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_1));


        // -------------------------------------------------------------------------------------------------------------
        //                                                3、（强势）个股
        // -------------------------------------------------------------------------------------------------------------


        // B策略   ->   强势个股
        long start_2 = System.currentTimeMillis();
        Set<String> buy__topStock__codeSet = buy__topStock__codeSet(buyConSet, data, tradeDate, buy_infoMap, ztFlag);
        log.info("AbstractBuyStrategy - buy__topStock__codeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 强势个股   ->   IN 主线板块
        long start_3 = System.currentTimeMillis();
        Set<String> inTopBlock__stockCodeSet = topBlockStrategy.inTopBlock__stockCodeSet(topBlockCodeSet, buy__topStock__codeSet, data, tradeDate);
        log.info("AbstractBuyStrategy - inTopBlock__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        long start_4 = System.currentTimeMillis();
        topBlockStrategy.buyStrategy_ETF(inTopBlock__stockCodeSet, data, tradeDate, buy_infoMap, posRate);
        log.info("AbstractBuyStrategy - buyStrategy_ETF     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_4));


        // -------------------------------------------------------------------------------------------------------------


        // 按照 规则打分 -> sort
        long start_5 = System.currentTimeMillis();
        List<String> sort__stockCodeList = ScoreSort.scoreSort__AMO_RPS(inTopBlock__stockCodeSet, data, tradeDate, btCompareDTO.get().getScoreSortN());
        log.info("AbstractBuyStrategy - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));


        return sort__stockCodeList;
    }


}