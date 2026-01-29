package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.bebopze.tdx.quant.strategy.sell.BacktestSellStrategy;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;

import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;
import static com.bebopze.tdx.quant.strategy.buy.ScoreSort.scoreSort__RPS;


/**
 * 回测 - B策略（高抛低吸  ->  C_MA_偏离率）
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategy_Stock implements BuyStrategy {


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockStrategy topBlockStrategy;

    @Autowired
    private BacktestBuyStrategyA backtestBuyStrategyA;

    @Autowired
    private BacktestBuyStrategyD backtestBuyStrategyD;

    @Autowired
    private BacktestBuyStrategyG backtestBuyStrategyG;

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
        return "STOCK";
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
        log.info("BacktestBuyStrategy_Stock - topBlock     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_1));


        // -------------------------------------------------------------------------------------------------------------
        //                                                3、（强势）个股
        // -------------------------------------------------------------------------------------------------------------


//        // B策略   ->   强势个股
//        long start_2 = System.currentTimeMillis();
//        Set<String> buy__topStock__codeSet = buy__topStock__codeSet(buyConSet, data, tradeDate, buy_infoMap, ztFlag);
//        log.info("BacktestBuyStrategy_Stock - buy__topStock__codeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // B策略   ->   主线板块内 成交额TOP25
        long start_2 = System.currentTimeMillis();
        Set<String> buy__topStock__codeSet = buy__topStock__codeSet(topBlockCodeSet, buyConSet, data, tradeDate, buy_infoMap, ztFlag);
        log.info("BacktestBuyStrategy_Stock - buy__topStock__codeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 强势个股   ->   IN 主线板块
        long start_3 = System.currentTimeMillis();
        Set<String> inTopBlock__stockCodeSet = topBlockStrategy.inTopBlock__stockCodeSet(topBlockCodeSet, buy__topStock__codeSet, data, tradeDate);
        log.info("BacktestBuyStrategy_Stock - inTopBlock__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        long start_4 = System.currentTimeMillis();
        if (/*CollectionUtils.isEmpty(inTopBlock__stockCodeSet) || */inTopBlock__stockCodeSet.size() < 25) { // ETF策略  ->  只有在 无个股可买 时 才触发
            topBlockStrategy.buyStrategy_ETF(inTopBlock__stockCodeSet, data, tradeDate, buy_infoMap, posRate);
        }
        log.info("BacktestBuyStrategy_Stock - buyStrategy_ETF     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_4));


        // -------------------------------------------------------------------------------------------------------------


        // B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
        // backtestStrategy.buy_sell__signalConflict(data, tradeDate, inTopBlock__stockCodeList);


        // -------------------------------------------------------------------------------------------------------------


        // 按照 规则打分 -> sort
        long start_5 = System.currentTimeMillis();
        List<String> sort__stockCodeList = scoreSort__RPS(inTopBlock__stockCodeSet, data, tradeDate, btCompareDTO.get().getScoreSortN());
        log.info("BacktestBuyStrategy_Stock - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));


        return sort__stockCodeList;
    }

    private Set<String> buy__topStock__codeSet(Set<String> topBlockCodeSet,
                                               Set<String> buyConSet,
                                               BacktestCache data,
                                               LocalDate tradeDate,
                                               Map<String, String> buy_infoMap,
                                               Boolean ztFlag) {


        Set<String> buy__topStock__codeSet = Sets.newHashSet();


        topBlockCodeSet.forEach(topBlockCode -> {
            Set<String> block__stockCodeSet = data.blockCode_stockCodeSet_Map.get(topBlockCode);


            block__stockCodeSet.stream().map(stockCode -> {
                                   BaseStockDO stockDO = data.codeStockMap.get(stockCode);
                                   if (null == stockDO || !Objects.equals(stockDO.getType(), StockTypeEnum.A_STOCK.type)) {
                                       // 未上市 || 非A股
                                       return null;
                                   }


                                   StockFun fun = data.getOrCreateStockFun(stockDO);


                                   KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
                                   ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
                                   Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                                   // -------------------------------------------


                                   // 当日 - 停牌（003005  ->  2022-10-27）
                                   Integer idx = dateIndexMap.get(tradeDate);

                                   // 过滤 停牌/新股       // TODO 个股行情指标 异常数据bug   688692（达梦数据）     kline 301条   extData 300条（首日 2024-06-12 扩展数据 缺失）
                                   if (idx == null || Double.isNaN(extDataArrDTO.rps50[idx]) || fun.getKlineDTOList().size() != fun.getExtDataDTOList().size()) {
                                       return null;
                                   }


                                   // ----------------------------------------------------------------------------------


                                   //            double rps50 = extDataArrDTO.rps50[idx];
                                   //            double rps120 = extDataArrDTO.rps120[idx];
                                   //            double rps250 = extDataArrDTO.rps250[idx];


                                   boolean 月多 = extDataArrDTO.月多[idx];
                                   boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
                                   boolean RPS红 = extDataArrDTO.RPS红[idx] && extDataArrDTO.rps50[idx] >= 90;
                                   boolean SSF多 = extDataArrDTO.SSF多[idx];


                                   boolean signal_多头 = (月多 || 均线预萌出) && RPS红 && SSF多;
                                   if (!signal_多头) {
                                       return null;
                                   }


                                   // ----------------------------------------------------------------------------------


                                   double amo = klineArrDTO.amo[idx];
                                   if (Double.isNaN(amo)) {
                                       return null;
                                   }


                                   // ----------------------------------------------------------------------------------


                                   boolean 涨停 = extDataArrDTO.涨停[idx];
                                   boolean 一字板 = 涨停 && klineArrDTO.open[idx] == klineArrDTO.high[idx] && klineArrDTO.high[idx] == klineArrDTO.low[idx] && klineArrDTO.low[idx] == klineArrDTO.close[idx];


                                   // 涨停   ->   放大 成交额 权重
                                   if (一字板) {
                                       amo = amo * 50;
                                   } else if (涨停) {
                                       amo = amo * 5;
                                   }


                                   // ----------------------------------------------------------------------------------


                                   // 涨停B 特殊处理
                                   backtestBuyStrategyD.zt_buy(true, Sets.newHashSet(), data, tradeDate, idx, Maps.newHashMap(), buy_infoMap, stockCode, fun, klineArrDTO, extDataArrDTO, ztFlag);


                                   // ----------------------------------------------------------------------------------


                                   return new StockAmoDTO(stockCode, amo);
                               })
                               .filter(Objects::nonNull)
                               // 按照 成交额 降序 排序
                               .sorted(Comparator.comparing(dto -> -dto.amo))
                               // 取 前25名
                               .limit(25)
                               // 提取 stockCode
                               .forEach(dto -> {
                                   String stockCode = dto.stockCode;
                                   buy__topStock__codeSet.add(stockCode);
                                   buy_infoMap.put(stockCode, "IN主线板块+多头（月多2）+成交额TOP");
                               });

        });


        return buy__topStock__codeSet;
    }


    @Data
    @AllArgsConstructor
    public static class StockAmoDTO {
        private String stockCode;
        private double amo;
    }


}