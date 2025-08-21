package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.EastMoneyKlineAPI;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BSStrategyInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.param.QuickBuyPositionParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.indicator.StockFunLast;
import com.bebopze.tdx.quant.parser.writer.TdxBlockNewReaderWriter;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.StrategyService;
import com.bebopze.tdx.quant.service.TradeService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategyC;
import com.bebopze.tdx.quant.strategy.sell.DownMASellStrategy;
import com.bebopze.tdx.quant.strategy.sell.SellStrategyFactory;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.AccountConst.STOCK__POS_PCT_LIMIT;


/**
 * 策略交易
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
@Slf4j
@Service
public class StrategyServiceImpl implements StrategyService {


    BacktestCache data = new BacktestCache();


    @Autowired
    InitDataService initDataService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private BacktestBuyStrategyC backtestBuyStrategyC;

    @Autowired
    private BacktestStrategy backtestStrategy;

    @Autowired
    private SellStrategyFactory sellStrategyFactory;

    @Autowired
    private DownMASellStrategy sellStrategy;


    @Override
    public BSStrategyInfoDTO bsTrade(TopBlockStrategyEnum topBlockStrategyEnum,
                                     List<String> buyConList,
                                     List<String> sellConList,
                                     LocalDate tradeDate) {

        tradeDate = tradeDate == null ? LocalDate.now() : tradeDate;


        // -------------------------------------------------------------------------------------------------------------


        BSStrategyInfoDTO dto = new BSStrategyInfoDTO();
        dto.setDate(tradeDate);
        dto.setBuyConList(buyConList);
        dto.setSellConList(sellConList);
        dto.setTopBlockCon(topBlockStrategyEnum.getDesc());


        // -------------------------------------------------------------------------------------------------------------


        data = initDataService.initData(LocalDate.now().minusYears(2), LocalDate.now(), false);
        // data = initDataService.initData();


        // -------------------------------------------------------------------------------------------------------------


        // 1、我的持仓
        QueryCreditNewPosResp posResp = tradeService.queryCreditNewPosV2();


        // 持仓 - code列表
        List<String> positionStockCodeList = posResp.getStocks().stream().map(CcStockInfo::getStkcode).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // ------------------------- S策略
        Map<String, String> sell_infoMap = Maps.newHashMap();


        // 卖出策略
        Set<String> sell__stockCodeSet = sellStrategyFactory.get("A").rule(topBlockStrategyEnum, data, tradeDate, positionStockCodeList, sell_infoMap);

        log.info("S策略     >>>     date : {} , topBlockStrategyEnum : {} , size : {} , sell__stockCodeSet : {} , sell_infoMap : {}",
                 tradeDate, topBlockStrategyEnum, sell__stockCodeSet.size(), JSON.toJSONString(sell__stockCodeSet), JSON.toJSONString(sell_infoMap));


        // ---------------


        // 一键卖出   ->   指定 个股列表
        // TODO     前期   ->   策略Test阶段     =>     先导出到TDX -> 人工2次审核 -> 再一键BS
        // tradeService.quickSellPosition(sell__stockCodeSet);


        // ---------------
        // S策略（SCL）
        // TODO     前期   ->   策略Test阶段     =>     先导出到TDX -> 人工2次审核 -> 再一键BS
        TdxBlockNewReaderWriter.write("SCL", sell__stockCodeSet);


        // ---------------
        dto.setSell__stockCodeSet(code_name(sell__stockCodeSet));
        dto.setSell_infoMap(code_name(sell_infoMap));


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // ------------------------- B策略
        Map<String, String> buy_infoMap = Maps.newHashMap();


        List<String> buy__stockCodeList = backtestBuyStrategyC.rule2(topBlockStrategyEnum, buyConList, data, tradeDate, buy_infoMap, posResp.getPosratio().doubleValue() / 2);


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


        // 拉取 实时行情（全A/ETF）
        pullStockSnapshotPrice();


        // -------------------------------------------------------------------------------------------------------------

        // newPositionList
        List<QuickBuyPositionParam> newPositionList = convert__newPositionList(buy__stockCodeList);


        // 一键买入     =>     指定 个股列表          ->          当前 剩余资金 买入（不清仓 -> old）
        // TODO     前期   ->   策略Test阶段     =>     先导出到TDX -> 人工2次审核 -> 再一键BS
        // tradeService.quickBuyPosition(newPositionList);


        // -------------------------------------------------------------------------------------------------------------


        // B策略（BCL）
        // TODO     前期   ->   策略Test阶段     =>     先导出到TDX -> 人工2次审核 -> 再一键BS
        TdxBlockNewReaderWriter.write("BCL", buy__stockCodeList);


        return dto;
    }


    @Override
    public BSStrategyInfoDTO bsTradeRead() {


        List<String> sell__stockCodeSet = TdxBlockNewReaderWriter.read("SCL");
        List<String> buy__stockCodeList = TdxBlockNewReaderWriter.read("BCL");


        log.info("bsTradeRead     >>>     sell__stockCodeSet : {}", JSON.toJSONString(sell__stockCodeSet));
        log.info("bsTradeRead     >>>     buy__stockCodeList : {}", JSON.toJSONString(sell__stockCodeSet));


        // -------------------------------------------------------------------------------------------------------------


        // 一键卖出   ->   指定 个股列表
        tradeService.quickSellPosition(Sets.newHashSet(sell__stockCodeSet));


        // -------------------------------------------------------------------------------------------------------------


        // 拉取 实时行情（全A/ETF）
        pullStockSnapshotPrice();


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


    /**
     * 拉取 实时行情（全A/ETF）
     */
    private void pullStockSnapshotPrice() {

        List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS = EastMoneyKlineAPI.allStockETFSnapshotKline();
        stockSnapshotKlineDTOS.forEach(e -> {
            String stockCode = e.getStockCode();

            data.stock__codePriceMap.put(stockCode, NumUtil.of(e.getClose() * 1.005, 5));   // 略有延迟（20s） ->  保险起见：price x 1.005
            data.stock_zt__codePriceMap.put(stockCode, NumUtil.of(e.getZtPrice(), 5));
            data.stock_dt__codePriceMap.put(stockCode, NumUtil.of(e.getDtPrice(), 5));
        });
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
     * buy__stockCodeList   ->   newPositionList
     *
     * @param buy__stockCodeList
     * @return
     */
    private List<QuickBuyPositionParam> convert__newPositionList(List<String> buy__stockCodeList) {


        return buy__stockCodeList.stream().map(stockCode -> {

                                     QuickBuyPositionParam newPosition = new QuickBuyPositionParam();

                                     newPosition.setStockCode(stockCode);
                                     newPosition.setStockName(data.stock__codeNameMap.get(stockCode));


                                     // 单只个股 仓位   ->   最大5%
                                     newPosition.setPositionPct(STOCK__POS_PCT_LIMIT);


                                     // 价格
                                     double price = data.stock__codePriceMap.computeIfAbsent(stockCode,
                                                                                             // 买5/卖5     ->     卖5价（最高价 -> 一键买入）
                                                                                             k -> NumUtil.of(EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode).getFivequote().getSale5()));


                                     // 价格异常   ->   停牌
                                     if (Double.isNaN(price)) {
                                         log.debug("convert__newPositionList     >>>     [{}] , price is NaN", stockCode);
                                         return null;
                                     }


                                     newPosition.setPrice(price);

                                     // 数量（不用设置 -> 后续 自动计算）
                                     newPosition.setQuantity(100);


                                     // --------------------------------------------------------------------------------


                                     // 小账户 -> 禁止买入 百元股
                                     if (price > 100 || (stockCode.startsWith("68") && price > 60)) {
                                         return null;
                                     }


                                     // 涨停股
                                     double zt_price = data.stock_zt__codePriceMap.computeIfAbsent(stockCode,
                                                                                                   // 买5/卖5     ->     涨/跌停价
                                                                                                   k -> NumUtil.of(EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode).getTopprice()));

                                     if (price >= zt_price) {
                                         return null;
                                     }


                                     // --------------------------------------------------------------------------------


                                     return newPosition;


                                 })
                                 .filter(Objects::nonNull)
                                 .collect(Collectors.toList());
    }


    @Deprecated
    @Override
    public void holdingStockRule(String stockCode) {

        sellStrategy.holdingStockRule(stockCode);
    }


    @Override
    public void breakSell() {


        // 持仓
        QueryCreditNewPosResp queryCreditNewPosResp = EastMoneyTradeAPI.queryCreditNewPosV2();

        List<CcStockInfo> stocks = queryCreditNewPosResp.getStocks();


        stocks.forEach(stock -> {

            //
            String stockCode = stock.getStkcode();

            StockFunLast fun = new StockFunLast(stockCode);


        });
    }


}