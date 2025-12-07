package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.*;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BSStrategyInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestCompareDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopPoolAvgPctDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopStockDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopStockPoolDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.param.QuickBuyPositionParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.bebopze.tdx.quant.parser.writer.TdxBlockNewReaderWriter;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.StrategyService;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.service.TradeService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategyD;
import com.bebopze.tdx.quant.strategy.sell.SellStrategyFactory;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.AccountConst.STOCK__POS_PCT_LIMIT;
import static com.bebopze.tdx.quant.service.impl.InitDataServiceImpl.data;
import static com.bebopze.tdx.quant.service.impl.TradeServiceImpl.of;


/**
 * 策略交易
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
@Slf4j
@Service
public class StrategyServiceImpl implements StrategyService {


    // private static final BacktestCache data = InitDataServiceImpl.data;


    @Autowired
    InitDataService initDataService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private BacktestBuyStrategyD backtestBuyStrategyD;

    @Autowired
    private BacktestStrategy backtestStrategy;

    @Autowired
    private SellStrategyFactory sellStrategyFactory;

    @Autowired
    private TopBlockService topBlockService;


    @TotalTime
    @Override
    public BSStrategyInfoDTO bsTrade(TopBlockStrategyEnum topBlockStrategyEnum,
                                     List<String> buyConList,
                                     List<String> sellConList,
                                     LocalDate tradeDate) {

        log.info("---------------------------- 策略交易[bsTrade]   start   >>>     topBlockStrategyEnum : {} , buyConList : {} , sellConList : {} , tradeDate : {}",
                 topBlockStrategyEnum.getDesc(), buyConList, sellConList, tradeDate);


        tradeDate = tradeDate == null ? LocalDate.now() : tradeDate;


        // -------------------------------------------------------------------------------------------------------------


        BSStrategyInfoDTO dto = new BSStrategyInfoDTO();
        dto.setDate(tradeDate);
        dto.setBuyConList(buyConList);
        dto.setSellConList(sellConList);
        dto.setTopBlockCon(topBlockStrategyEnum.getDesc());


        // -------------------------------------------------------------------------------------------------------------


        BacktestCompareDTO btCompareDTO = new BacktestCompareDTO();
        btCompareDTO.setBuyConSet(Sets.newHashSet(buyConList));


        // -------------------------------------------------------------------------------------------------------------


        initDataService.initData(LocalDate.now().minusYears(2), LocalDate.now(), false);
//        initDataService.initData();


        // -------------------------------------------------------------------------------------------------------------


        // 1、我的持仓
        QueryCreditNewPosResp posResp = tradeService.queryCreditNewPosV2();


        // 持仓 - code列表
        List<String> positionStockCodeList = posResp.getStocks().stream().map(CcStockInfo::getStkcode).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // ------------------------- S策略
        Map<String, SellStrategyEnum> sell_infoMap = Maps.newHashMap();


        // 卖出策略
        Set<String> sell__stockCodeSet = sellStrategyFactory.get("A").rule(topBlockStrategyEnum, data, tradeDate, positionStockCodeList, sell_infoMap, btCompareDTO);

        log.info("S策略     >>>     date : {} , topBlockStrategyEnum : {} , size : {} , sell__stockCodeSet : {} , sell_infoMap : {}",
                 tradeDate, topBlockStrategyEnum, sell__stockCodeSet.size(), JSON.toJSONString(sell__stockCodeSet), JSON.toJSONString(sell_infoMap));


        // ---------------


        // 一键清仓   ->   指定 个股列表
        tradeService.quickClearPosition(sell__stockCodeSet);


        // ---------------
        // S策略（SCL）
        try {
            TdxBlockNewReaderWriter.write("SCL", sell__stockCodeSet);
        } catch (Exception e) {
            log.error("S策略（SCL）    >>>     write   ->   TDX（策略-等比买入）    errMsg : {}", e.getMessage());
        }


        // ---------------
        dto.setSell__stockCodeSet(code_name(sell__stockCodeSet));
        dto.setSell_infoMap(code_name2(sell_infoMap));


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // ------------------------- B策略
        Map<String, String> buy_infoMap = Maps.newHashMap();


        List<String> buy__stockCodeList = backtestBuyStrategyD.rule2(topBlockStrategyEnum, buyConList, data, tradeDate, buy_infoMap, posResp.getPosratio().doubleValue() / 2, false);


        // ---------------------------------------------------------


        // B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
        backtestStrategy.buy_sell__signalConflict(topBlockStrategyEnum, data, tradeDate, buy__stockCodeList);

        log.info("B策略     >>>     [{}] , topBlockStrategyEnum : {} , size : {} , buy__stockCodeList : {} , buy_infoMap : {}",
                 tradeDate, topBlockStrategyEnum, buy__stockCodeList.size(), JSON.toJSONString(buy__stockCodeList), JSON.toJSONString(buy_infoMap));


        // --------------------------------
        dto.setBuy__stockCodeSet(code_name(buy__stockCodeList));

        buy_infoMap.keySet().removeIf(k -> !buy__stockCodeList.contains(k));
        dto.setBuy_infoMap(code_name(buy_infoMap));


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------

        // newPositionList
        List<QuickBuyPositionParam> newPositionList = convert__newPositionList(buy__stockCodeList);


        // 一键买入     =>     指定 个股列表          ->          当前 剩余资金 买入（不清仓 -> old）
        tradeService.quickBuyPosition(newPositionList);


        // -------------------------------------------------------------------------------------------------------------


        // B策略（BCL）
        try {
            TdxBlockNewReaderWriter.write("BCL", buy__stockCodeList);
        } catch (Exception e) {
            log.error("B策略（BCL）    >>>     write   ->   TDX（策略-等比买入）    errMsg : {}", e.getMessage());
        }


        return dto;
    }


    @Override
    public BSStrategyInfoDTO bsTradeRead() {


        List<String> sell__stockCodeSet = TdxBlockNewReaderWriter.read("SCL");
        List<String> buy__stockCodeList = TdxBlockNewReaderWriter.read("BCL");


        log.info("bsTradeRead     >>>     sell__stockCodeSet : {}", JSON.toJSONString(sell__stockCodeSet));
        log.info("bsTradeRead     >>>     buy__stockCodeList : {}", JSON.toJSONString(sell__stockCodeSet));


        // -------------------------------------------------------------------------------------------------------------


        // 一键清仓   ->   指定 个股列表
        tradeService.quickClearPosition(Sets.newHashSet(sell__stockCodeSet));


        // -------------------------------------------------------------------------------------------------------------


        // newPositionList
        List<QuickBuyPositionParam> newPositionList = convert__newPositionList(buy__stockCodeList);


        // 一键买入     =>     指定 个股列表          ->          当前 剩余资金 买入（不清仓 -> old）
        tradeService.quickBuyPosition(newPositionList);


        // -------------------------------------------------------------------------------------------------------------


        BSStrategyInfoDTO dto = new BSStrategyInfoDTO();
        dto.setDate(LocalDate.now());
        dto.setBuyConList(null);
        dto.setSellConList(null);
        dto.setTopBlockCon(null);


        dto.setSell__stockCodeSet(code_name(sell__stockCodeSet));
        // dto.setSell_infoMap(code_name(sell_infoMap));


        dto.setBuy__stockCodeSet(code_name(buy__stockCodeList));
        // dto.setBuy_infoMap(code_name(buy_infoMap));


        return dto;
    }


    @Override
    public void bsTopStockList() {


        // 我的持仓（昨日买入）
        QueryCreditNewPosResp posResp = tradeService.queryCreditNewPosV2();
        List<CcStockInfo> old_posStockList = posResp.getStocks();


        // 今日 主线个股列表 （待调仓换股）
        LocalDate today = LocalDate.now();
        today = LocalDate.of(2025, 10, 31);
        TopStockPoolDTO new_dto = topBlockService.topStockList(today, TopTypeEnum.AUTO.type);


        // ------------------------------------------------- check -----------------------------------------------------


        TopPoolAvgPctDTO avgPctDTO = new_dto.getTopStockAvgPctDTO();
        Assert.isTrue(avgPctDTO.getDate().isEqual(today), "今日主线个股列表 数据为空，请检查是否[未刷新]今日主线数据");

        List<TopStockDTO> topStockDTOList = new_dto.getTopStockDTOList();
        if (CollectionUtils.isEmpty(topStockDTOList)) {
            log.warn("bsTopStockList - 今日主线数据为空（上榜个股=0） ->  将清仓     >>>     [{}] , topStockDTOList : {}",
                     today, JSON.toJSONString(topStockDTOList));
        }


        // -------------------------------------------------------------------------------------------------------------


        Set<String> new__stockCodeSet = topStockDTOList.stream().map(TopStockDTO::getStockCode).collect(Collectors.toSet());


        Set<String> clearStockCodeSet = Sets.newHashSet();
        Set<String> posStockCodeSet = Sets.newHashSet();
        Set<String> buyStockCodeSet = Sets.newHashSet(new__stockCodeSet);


        old_posStockList.forEach(e -> {
            String stockCode = e.getStkcode();


            // not_in_new   ->   sell
            if (!new__stockCodeSet.contains(stockCode)) {
                clearStockCodeSet.add(stockCode);
            } else {
                // in_new   ->   继续持有/加仓
                posStockCodeSet.add(stockCode);
                // new 买入列表剔除 已持仓股票
                buyStockCodeSet.remove(stockCode);
            }
        });


        // ----------------------------------------------- 特殊处理 -----------------------------------------------------


        // ETF（略过 -> sell）
        clearStockCodeSet.removeIf(StockTypeEnum::isETF);


        // 手动主动买入（略过 -> sell）


        // ...


        // -------------------------------------------------------------------------------------------------------------


        // 一键清仓   ->   指定 个股列表
        tradeService.quickClearPosition(clearStockCodeSet);


        // 一键买入     =>     指定 个股列表          ->          当前 剩余资金 买入（不清仓 -> old）
        tradeService.quickBuyPosition(convert__newPositionList(buyStockCodeSet));
    }


    /**
     * code-name     List
     *
     * @param sell__stockCodeSet
     * @return
     */
    private Set<String> code_name(Collection<String> sell__stockCodeSet) {
        return sell__stockCodeSet.stream()
                                 // code - name
                                 .map(stockCode -> stockCode + "-" + data.stock__codeNameMap.get(stockCode))
                                 .collect(Collectors.toSet());
    }


    /**
     * code-name     Map
     *
     * @param buy_infoMap
     * @return
     */
    private Map<String, String> code_name(Map<String, String> buy_infoMap) {

        return buy_infoMap.entrySet().stream()
                          .collect(Collectors.toMap(
                                  // code - name
                                  entry -> entry.getKey() + "-" + data.stock__codeNameMap.get(entry.getKey()),
                                  Map.Entry::getValue
                          ));
    }

    /**
     * code-name     Map
     *
     * @param buy_infoMap
     * @return
     */
    private Map<String, String> code_name2(Map<String, SellStrategyEnum> buy_infoMap) {

        return buy_infoMap.entrySet().stream()
                          .collect(Collectors.toMap(
                                  // code - name
                                  entry -> entry.getKey() + "-" + data.stock__codeNameMap.get(entry.getKey()),
                                  entry -> entry.getValue().getDesc()
                          ));
    }


    /**
     * buy__stockCodeList   ->   newPositionList
     *
     * @param buy__stockCodeList
     * @return
     */
    public static List<QuickBuyPositionParam> convert__newPositionList(Collection<String> buy__stockCodeList) {
        return convert__newPositionList(buy__stockCodeList, 0.0, 0.0);
    }

    public static List<QuickBuyPositionParam> convert__newPositionList(Collection<String> buy__stockCodeList,
                                                                       double currPricePct,
                                                                       double prevPricePct) {


        // 拉取 实时行情（全A/ETF）
        pullStockSnapshotPrice(buy__stockCodeList);


        return buy__stockCodeList.stream().map(stockCode -> {

                                     String stockName = data.stock__codeNameMap.get(stockCode);


                                     QuickBuyPositionParam newPosition = new QuickBuyPositionParam();
                                     newPosition.setStockCode(stockCode);
                                     newPosition.setStockName(stockName);


                                     // 单只个股 仓位   ->   最大5%
                                     newPosition.setPosPct(STOCK__POS_PCT_LIMIT);


//                                     // 价格
//                                     double price = data.rt_stock__codePriceMap.computeIfAbsent(stockCode,
//                                                                                                // 买5/卖5     ->     卖5价（最高价 -> 一键买入）
//                                                                                                k -> NumUtil.of(EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode).getFivequote().getSale5()));


                                     // 价格
                                     double price = data.rt_stock__codePriceMap.computeIfAbsent(stockCode,
                                                                                                k -> {
                                                                                                    // 拉取实时行情
                                                                                                    pullStockSnapshotPrice(Sets.newHashSet(stockCode));
                                                                                                    // 买5/卖5     ->     卖5价（最高价 -> 一键买入）
                                                                                                    return data.rt_stock__codePriceMap.get(stockCode) * 1.001;
                                                                                                });


                                     // 价格异常   ->   停牌
                                     if (Double.isNaN(price)) {
                                         log.warn("convert__newPositionList - err     >>>     [{} {}]   ->   price is NaN", stockCode, stockName);
                                         return null;
                                     }


                                     // -----------------------------------------------------------------


                                     // B价格策略
                                     price = buyPriceStrategyPct(stockCode, currPricePct, prevPricePct);


                                     // -----------------------------------------------------------------


                                     newPosition.setPrice(price);

                                     // 数量（不用设置 -> 后续 自动计算）
                                     newPosition.setQuantity(100);


                                     // --------------------------------------------------------------------------------


                                     // ST（暂未进入退市）、*ST（退市中）
                                     if (stockName.contains("*ST")) {
                                         log.warn("买入[{} {}]   -   [退市中] -> 略过     >>>     close : {} , 涨跌幅 : {}%",
                                                  stockCode, stockName, of(price), data.rt_stock__codePctMap.get(stockCode));

                                         // 限制买入 -> 极少仓位
                                         // newPosition.setPositionPct(Math.min(0.1 * STOCK__POS_PCT_LIMIT, 1));

                                         // 略过
                                         return null;
                                     }


                                     // 小账户 -> 禁止买入 百元股
                                     if (price > 300 || (stockCode.startsWith("68") && price > 200)) {
                                         log.warn("买入[{} {}]   -   [高价股] -> 略过     >>>     close : {} , 涨跌幅 : {}%",
                                                  stockCode, stockName, of(price), data.rt_stock__codePctMap.get(stockCode));
                                         return null;
                                     }


                                     // 涨停股
                                     double zt_price = data.rt_stock_zt__codePriceMap.computeIfAbsent(stockCode,
                                                                                                      // 买5/卖5     ->     涨/跌停价
                                                                                                      k -> KlineAPI.kline(stockCode).getZtPrice());

                                     if (price >= zt_price) {
                                         log.warn("买入[{} {}]   -   [已涨停] -> 略过     >>>     close : {} , 涨跌幅 : {}%",
                                                  stockCode, stockName, of(price), data.rt_stock__codePctMap.get(stockCode));
                                         return null;
                                     }


                                     // --------------------------------------------------------------------------------


                                     return newPosition;
                                 })
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList());
    }


    /**
     * 拉取 实时行情（指定 个股/ETF 列表）
     */
    private static void pullStockSnapshotPrice(Collection<String> buy__stockCodeList) {


//        try {
//            List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS = EastMoneyKlineAPI.pullAllStockETFSnapshotKline();
//
//
//            stockSnapshotKlineDTOS.forEach(e -> {
//                String stockCode = e.getStockCode();
//
//
//                data.stock__codeNameMap.put(stockCode, e.getStockName());
//                data.rt_stock__codePctMap.put(stockCode, e.getChange_pct());
//
//
//                data.rt_stock__codePriceMap.put(stockCode, NumUtil.of(e.getClose() * 1.003, 5));   // 略有延迟（20s） ->  保险起见：price x 1.003
//                data.rt_stock_zt__codePriceMap.put(stockCode, NumUtil.of(e.getZtPrice(), 5));
//                data.rt_stock_dt__codePriceMap.put(stockCode, NumUtil.of(e.getDtPrice(), 5));
//            });
//
//        } catch (Exception e) {
//
//            // 封禁IP
//            log.error(e.getMessage(), e);
//        }


        // ------------------------------------


        buy__stockCodeList.forEach(stockCode -> {


//            SHSZQuoteSnapshotResp resp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
//            SleepUtils.randomSleep(50, 100);
//
//
//            String stockName = resp.getName();
//            double change_pct = resp.getRealtimequote().getZdf();
//            double currPrice = resp.getRealtimequote().getCurrentPrice();
//            double ztPrice = resp.getTopprice();
//            double dtPrice = resp.getBottomprice();


            StockSnapshotKlineDTO dto = KlineAPI.kline(stockCode);
            SleepUtils.randomSleep(0, 10);


            String stockName = dto.getStockName();
            double change_pct = dto.getChange_pct();
            double currPrice = dto.getClose();
            double ztPrice = dto.getZtPrice();
            double dtPrice = dto.getDtPrice();


            data.stock__codeNameMap.put(stockCode, stockName);
            data.rt_stock__codePctMap.put(stockCode, change_pct);


            data.rt_stock__codePriceMap.put(stockCode, NumUtil.of(currPrice * 1.001, 5));   // 略有延迟（1s） ->  保险起见：price x 1.001
            data.rt_stock_zt__codePriceMap.put(stockCode, ztPrice);
            data.rt_stock_dt__codePriceMap.put(stockCode, dtPrice);
        });
    }


    /**
     * B价格策略
     *
     * @param stockCode
     * @param currPricePct 按当前价格涨跌幅%
     * @param prevPricePct 按昨日收盘价涨跌幅%
     * @return
     */
    private static double buyPriceStrategyPct(String stockCode, double currPricePct, double prevPricePct) {

        // 今日 涨跌幅
        double pct = data.rt_stock__codePctMap.get(stockCode);
        // 当前 实时价格
        double price = data.rt_stock__codePriceMap.get(stockCode);


        // 昨日收盘价 = 当前实时价格 / (1 + 涨跌幅)
        double preClose = price / (1 + pct * 0.01);


        // ------------------------------------------- 特殊处理 ---------------------------------------------------------


        Integer changePctLimit = StockLimitEnum.getChgPctLimit(stockCode, data.stock__codeNameMap.get(stockCode));

        // 涨停/近停   ->   挂 更低的价格（91% -> 1个跌停板） 买入
        if (changePctLimit == 10 && pct >= changePctLimit * 0.9) {
            price *= 0.97;
            preClose *= 0.97;
        } else if (changePctLimit == 20 && pct >= changePctLimit * 0.75) {
            price *= 0.95;
            preClose *= 0.95;
        } else if (changePctLimit == 30 && pct >= changePctLimit * 0.7) {
            price *= 0.93;
            preClose *= 0.93;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 按昨日收盘价涨跌幅%
        if (prevPricePct != 0) {
            return preClose * (1 + prevPricePct * 0.01);
        }
        // 按当前价格涨跌幅%
        return price * (1 + currPricePct * 0.01);
    }


}