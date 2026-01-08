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
import com.bebopze.tdx.quant.common.util.ConvertUtil;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.AccountConst.ACCOUNT__POS_PCT_LIMIT;
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


    public static final ThreadLocal<BacktestStrategy.Stat> x = BacktestStrategy.x;

    public static final ThreadLocal<BacktestCompareDTO> btCompareDTO = BacktestStrategy.btCompareDTO;


    @Autowired
    InitDataService initDataService;

    @Lazy
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


    @Override
    public BSStrategyInfoDTO bsTradeMultiStrategy(TopBlockStrategyEnum topBlockStrategyEnum,
                                                  Set<String> buyConSet,
                                                  Set<String> sellConSet,
                                                  LocalDate tradeDate) {


        // {
        //    "marketPosLimitFlag": "false",
        //    "scoreSortN": "20",
        //    "singleStockMaxBuyAvlPct": "100.0",
        //    "singleStockMaxPosPct": "25.0",
        //    "singleStockMinBuyPosPct": "0.0",
        //    "top1TopBlockFlag": "true",
        //    "ztFlag": "true"
        // }

        BacktestCompareDTO btCompareDTO_1 = new BacktestCompareDTO();
        btCompareDTO_1.setMarketPosLimitFlag(false);
        btCompareDTO_1.setScoreSortN(20);
        btCompareDTO_1.setSingleStockMaxBuyAvlPct(100.0);
        btCompareDTO_1.setSingleStockMaxPosPct(25.0);
        btCompareDTO_1.setSingleStockMinBuyPosPct(0.0);
        btCompareDTO_1.setTop1TopBlockFlag(true);
        btCompareDTO_1.setZtFlag(true);


        // 策略1：打板A
        // 大均线多头,RPS红,C_MA5_偏离率<5,中期涨幅<100,SSF多,MA20多,MA200多（29625）
        TopBlockStrategyEnum topBlockStrategyEnum_1 = TopBlockStrategyEnum.LV3;
        Set<String> buyConSet_1 = ConvertUtil.str2Set("大均线多头,RPS红,C_MA5_偏离率<5,中期涨幅<100,SSF多,MA20多,MA200多");
        Set<String> sellConSet_1 = ConvertUtil.str2Set("个股S,板块S,主线S");


        btCompareDTO_1.setBuyStrategyKey("D");
        btCompareDTO_1.setTopBlockStrategyEnum(topBlockStrategyEnum_1);
        btCompareDTO_1.setBuyConSet(buyConSet_1);
        btCompareDTO_1.setSellConSet(sellConSet_1);
        btCompareDTO_1.setStrategyPosRatio(0.20 * 2);   // 融资账户 x2
        this.btCompareDTO.set(btCompareDTO_1);


        try {
            BSStrategyInfoDTO bsStrategyInfoDTO = bsTrade(topBlockStrategyEnum_1, buyConSet_1, sellConSet_1, tradeDate);
        } catch (Exception e) {
            log.error("bsTrade error     >>>     topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , tradeDate : {}",
                      topBlockStrategyEnum_1, buyConSet_1, sellConSet_1, tradeDate, e);
        } finally {
            this.btCompareDTO.remove();
            this.x.remove();
        }


        // -------------------------------------------------------------------------------------------------------------


        // {
        //    "marketPosLimitFlag": "true",
        //    "scoreSortN": "100",
        //    "singleStockMaxBuyAvlPct": "100.0",
        //    "singleStockMaxPosPct": "20.0",
        //    "singleStockMinBuyPosPct": "0.0",
        //    "top1TopBlockFlag": "false",
        //    "ztFlag": "true"
        // }

        BacktestCompareDTO btCompareDTO_2 = new BacktestCompareDTO();
        btCompareDTO_2.setMarketPosLimitFlag(true);
        btCompareDTO_2.setScoreSortN(100);
        btCompareDTO_2.setSingleStockMaxBuyAvlPct(100.0);
        btCompareDTO_2.setSingleStockMaxPosPct(20.0);
        btCompareDTO_2.setSingleStockMinBuyPosPct(0.0);
        btCompareDTO_2.setTop1TopBlockFlag(false);
        btCompareDTO_2.setZtFlag(true);


        // 策略2：打板B
        // RPS双线红,C_MA5_偏离率<5,中期涨幅<100,SSF多,MA20多,MA200多（86430）
        TopBlockStrategyEnum topBlockStrategyEnum_2 = TopBlockStrategyEnum.LV3;
        Set<String> buyConSet_2 = ConvertUtil.str2Set("RPS双线红,C_MA5_偏离率<5,中期涨幅<100,SSF多,MA20多,MA200多");
        Set<String> sellConSet_2 = ConvertUtil.str2Set("个股S,板块S,主线S");


        btCompareDTO_2.setBuyStrategyKey("D");
        btCompareDTO_2.setTopBlockStrategyEnum(topBlockStrategyEnum_2);
        btCompareDTO_2.setBuyConSet(buyConSet_2);
        btCompareDTO_2.setSellConSet(sellConSet_2);
        btCompareDTO_2.setStrategyPosRatio(0.10 * 2);   // 融资账户 x2
        this.btCompareDTO.set(btCompareDTO_2);


        try {
            BSStrategyInfoDTO bsStrategyInfoDTO = bsTrade(topBlockStrategyEnum_2, buyConSet_2, sellConSet_2, tradeDate);
        } catch (Exception e) {
            log.error("bsTrade error     >>>     topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , tradeDate : {}",
                      topBlockStrategyEnum_2, buyConSet_2, sellConSet_2, tradeDate, e);
        } finally {
            this.btCompareDTO.remove();
            this.x.remove();
        }


        // -------------------------------------------------------------------------------------------------------------


        // {
        //    "marketPosLimitFlag": "true",
        //    "scoreSortN": "100",
        //    "singleStockMaxBuyAvlPct": "100.0",
        //    "singleStockMaxPosPct": "20.0",
        //    "singleStockMinBuyPosPct": "0.0",
        //    "top1TopBlockFlag": "true"
        //    "ztFlag": "true"
        // }

        BacktestCompareDTO btCompareDTO_3 = new BacktestCompareDTO();
        btCompareDTO_3.setMarketPosLimitFlag(true);
        btCompareDTO_3.setScoreSortN(100);
        btCompareDTO_3.setSingleStockMaxBuyAvlPct(100.0);
        btCompareDTO_3.setSingleStockMaxPosPct(20.0);
        btCompareDTO_3.setSingleStockMinBuyPosPct(0.0);
        btCompareDTO_3.setTop1TopBlockFlag(true);
        btCompareDTO_3.setZtFlag(true);


        // 策略3：打板C
        // 小均线多头,RPS红,中期涨幅<100,SSF多,MA20多,MA200多（84373）
        TopBlockStrategyEnum topBlockStrategyEnum_3 = TopBlockStrategyEnum.LV3;
        Set<String> buyConSet_3 = ConvertUtil.str2Set("小均线多头,RPS红,中期涨幅<100,SSF多,MA20多,MA200多");
        Set<String> sellConSet_3 = ConvertUtil.str2Set("个股S,板块S,主线S");

        btCompareDTO_3.setBuyStrategyKey("D");
        btCompareDTO_3.setTopBlockStrategyEnum(topBlockStrategyEnum_3);
        btCompareDTO_3.setBuyConSet(buyConSet_3);
        btCompareDTO_3.setSellConSet(sellConSet_3);
        btCompareDTO_3.setStrategyPosRatio(0.10 * 2);   // 融资账户 x2
        this.btCompareDTO.set(btCompareDTO_3);


        try {
            BSStrategyInfoDTO bsStrategyInfoDTO = bsTrade(topBlockStrategyEnum_3, buyConSet_3, sellConSet_3, tradeDate);
        } catch (Exception e) {
            log.error("bsTrade error     >>>     topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , tradeDate : {}",
                      topBlockStrategyEnum_3, buyConSet_3, sellConSet_3, tradeDate, e);
        } finally {
            this.btCompareDTO.remove();
            this.x.remove();
        }


        // -------------------------------------------------------------------------------------------------------------


        // {
        //    "marketPosLimitFlag": "true",
        //    "scoreSortN": "100",
        //    "singleStockMaxBuyAvlPct": "100.0",
        //    "singleStockMaxPosPct": "20.0",
        //    "singleStockMinBuyPosPct": "0.0",
        //    "top1TopBlockFlag": "false"
        //    "ztFlag": "null"
        // }


        BacktestCompareDTO btCompareDTO_4 = new BacktestCompareDTO();
        btCompareDTO_4.setMarketPosLimitFlag(true);
        btCompareDTO_4.setScoreSortN(100);
        btCompareDTO_4.setSingleStockMaxBuyAvlPct(100.0);
        btCompareDTO_4.setSingleStockMaxPosPct(20.0);
        btCompareDTO_4.setSingleStockMinBuyPosPct(0.0);
        btCompareDTO_4.setTop1TopBlockFlag(false);
        btCompareDTO_4.setZtFlag(null);


        // 策略4：趋势A
        // RPS一线红,C_SSF_偏离率<5,中期涨幅<35,XZZB,SSF多,MA200多（74992）
        TopBlockStrategyEnum topBlockStrategyEnum_4 = TopBlockStrategyEnum.LV3;
        Set<String> buyConSet_4 = ConvertUtil.str2Set("RPS一线红,C_SSF_偏离率<5,中期涨幅<35,XZZB,SSF多,MA200多");
        Set<String> sellConSet_4 = ConvertUtil.str2Set("个股S,板块S,主线S");


        btCompareDTO_4.setBuyStrategyKey("D");
        btCompareDTO_4.setTopBlockStrategyEnum(topBlockStrategyEnum_4);
        btCompareDTO_4.setBuyConSet(buyConSet_4);
        btCompareDTO_4.setSellConSet(sellConSet_4);
        btCompareDTO_4.setStrategyPosRatio(0.20 * 2);   // 融资账户 x2
        this.btCompareDTO.set(btCompareDTO_4);

        try {
            BSStrategyInfoDTO bsStrategyInfoDTO = bsTrade(topBlockStrategyEnum_4, buyConSet_4, sellConSet_4, tradeDate);
        } catch (Exception e) {
            log.error("bsTrade error     >>>     topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , tradeDate : {}",
                      topBlockStrategyEnum_4, buyConSet_4, sellConSet_4, tradeDate, e);
        } finally {
            this.btCompareDTO.remove();
            this.x.remove();
        }


        // -------------------------------------------------------------------------------------------------------------


        // {
        //    "marketPosLimitFlag": "true",
        //    "scoreSortN": "100",
        //    "singleStockMaxBuyAvlPct": "100.0",
        //    "singleStockMaxPosPct": "20.0",
        //    "singleStockMinBuyPosPct": "0.0",
        //    "ztFlag": null
        // }


        BacktestCompareDTO btCompareDTO_5 = new BacktestCompareDTO();
        btCompareDTO_5.setMarketPosLimitFlag(true);
        btCompareDTO_5.setScoreSortN(100);
        btCompareDTO_5.setSingleStockMaxBuyAvlPct(100.0);
        btCompareDTO_5.setSingleStockMaxPosPct(20.0);
        btCompareDTO_5.setSingleStockMinBuyPosPct(0.0);
        btCompareDTO_5.setTop1TopBlockFlag(false);
        btCompareDTO_5.setZtFlag(null);


        // 策略5：趋势B
        // 百日新高,C_MA20_偏离率<5,中期涨幅<50,中期涨幅N250<150,XZZB,SSF多,MA200多（79069）
        TopBlockStrategyEnum topBlockStrategyEnum_5 = TopBlockStrategyEnum.LV3;
        Set<String> buyConSet_5 = ConvertUtil.str2Set("百日新高,C_MA20_偏离率<5,中期涨幅<50,中期涨幅N250<150,XZZB,SSF多,MA200多");
        Set<String> sellConSet_5 = ConvertUtil.str2Set("个股S,板块S,主线S");


        btCompareDTO_5.setBuyStrategyKey("D");
        btCompareDTO_5.setTopBlockStrategyEnum(topBlockStrategyEnum_5);
        btCompareDTO_5.setBuyConSet(buyConSet_5);
        btCompareDTO_5.setSellConSet(sellConSet_5);
        btCompareDTO_5.setStrategyPosRatio(0.20 * 2);   // 融资账户 x2
        this.btCompareDTO.set(btCompareDTO_5);


        try {
            BSStrategyInfoDTO bsStrategyInfoDTO = bsTrade(topBlockStrategyEnum_5, buyConSet_5, sellConSet_5, tradeDate);
        } catch (Exception e) {
            log.error("bsTrade error     >>>     topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , tradeDate : {}",
                      topBlockStrategyEnum_5, buyConSet_5, sellConSet_5, tradeDate, e);
        } finally {
            this.btCompareDTO.remove();
            this.x.remove();
        }


        // -------------------------------------------------------------------------------------------------------------


        // {
        //    "marketPosLimitFlag": "true",
        //    "scoreSortN": "100",
        //    "singleStockMaxBuyAvlPct": "100.0",
        //    "singleStockMaxPosPct": "25.0",
        //    "singleStockMinBuyPosPct": "0.0",
        //    "top1TopBlockFlag": "true",
        //    "ztFlag": null
        // }


        BacktestCompareDTO btCompareDTO_6 = new BacktestCompareDTO();
        btCompareDTO_6.setMarketPosLimitFlag(true);
        btCompareDTO_6.setScoreSortN(100);
        btCompareDTO_6.setSingleStockMaxBuyAvlPct(100.0);
        btCompareDTO_6.setSingleStockMaxPosPct(25.0);
        btCompareDTO_6.setSingleStockMinBuyPosPct(0.0);
        btCompareDTO_6.setTop1TopBlockFlag(true);
        btCompareDTO_6.setZtFlag(null);


        // 策略6：趋势C
        // SSF多,百日新高,月多,C_MA20_偏离率<5,中期涨幅<50（31209/31203/110698）
        TopBlockStrategyEnum topBlockStrategyEnum_6 = TopBlockStrategyEnum.LV3;
        Set<String> buyConSet_6 = ConvertUtil.str2Set("SSF多,百日新高,月多,C_MA20_偏离率<5,中期涨幅<50");
        Set<String> sellConSet_6 = ConvertUtil.str2Set("个股S,板块S,主线S");

        btCompareDTO_6.setBuyStrategyKey("D");
        btCompareDTO_6.setTopBlockStrategyEnum(topBlockStrategyEnum_6);
        btCompareDTO_6.setBuyConSet(buyConSet_6);
        btCompareDTO_6.setSellConSet(sellConSet_6);
        btCompareDTO_6.setStrategyPosRatio(0.25 * 2);   // 融资账户 x2
        this.btCompareDTO.set(btCompareDTO_6);


        try {
            BSStrategyInfoDTO bsStrategyInfoDTO = bsTrade(topBlockStrategyEnum_6, buyConSet_6, sellConSet_6, tradeDate);
        } catch (Exception e) {
            log.error("bsTrade error     >>>     topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , tradeDate : {}",
                      topBlockStrategyEnum_6, buyConSet_6, sellConSet_6, tradeDate, e);
        } finally {
            this.btCompareDTO.remove();
            this.x.remove();
        }


        return null;
    }


    @TotalTime
    @Override
    public BSStrategyInfoDTO bsTrade(TopBlockStrategyEnum topBlockStrategyEnum,
                                     Set<String> buyConSet,
                                     Set<String> sellConSet,
                                     LocalDate tradeDate) {

        log.info("---------------------------- 策略交易[bsTrade]   start   >>>     topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , tradeDate : {}",
                 topBlockStrategyEnum.getDesc(), buyConSet, sellConSet, tradeDate);


        tradeDate = tradeDate == null ? LocalDate.now() : tradeDate;


        // -------------------------------------------------------------------------------------------------------------


        BSStrategyInfoDTO dto = new BSStrategyInfoDTO();
        dto.setDate(tradeDate);
        dto.setBuyConSet(buyConSet);
        dto.setSellConSet(sellConSet);
        dto.setTopBlockCon(topBlockStrategyEnum.getDesc());


        // -------------------------------------------------------------------------------------------------------------


//        BacktestCompareDTO btCompareDTO = new BacktestCompareDTO();
//        btCompareDTO.setBuyConSet(Sets.newHashSet(buyConList));


        // -------------------------------------------------------------------------------------------------------------


        initDataService.initData(LocalDate.now().minusYears(2), LocalDate.now(), false, 1);
//        initDataService.initData();


        // -------------------------------------------------------------------------------------------------------------


        // 1、我的持仓
        QueryCreditNewPosResp posResp = tradeService.queryCreditNewPosV2();


        // 持仓 - code列表
        Set<String> positionStockCodeSet = posResp.getStocks().stream().map(CcStockInfo::getStkcode).collect(Collectors.toSet());


        // -------------------------------------------------------------------------------------------------------------


        // ------------------------- S策略
        Map<String, SellStrategyEnum> sell_infoMap = Maps.newHashMap();


        // ------------------------- 持仓成本（avgCostPrice）


//        // 持仓成本（avgCostPrice）
//        Map<String, Double> posCostMap_0 = x.get().getPositionRecordDOList().stream()
//                                            .collect(Collectors.toMap(BtPositionRecordDO::getStockCode, e -> e.getAvgCostPrice().doubleValue()));
//
//        Map<String, Double> posCostMap = posResp.getStocks().stream().collect(Collectors.toMap(CcStockInfo::getStkcode, CcStockInfo::getCostprice));
        List<BtPositionRecordDO> positionRecordDOList = posResp.getStocks().stream()
                                                               .map(e -> {
                                                                   BtPositionRecordDO positionRecordDO = new BtPositionRecordDO();
                                                                   positionRecordDO.setStockCode(e.getStkcode());
                                                                   positionRecordDO.setAvgCostPrice(e.getCostprice());
                                                                   return positionRecordDO;
                                                               })
                                                               .collect(Collectors.toList());


        BacktestStrategy.Stat stat = new BacktestStrategy.Stat();
        stat.setPositionRecordDOList(positionRecordDOList);
        x.set(stat);


        // -------------------------------------------------------------------------------------------------------------


        // 卖出策略
        Set<String> sell__stockCodeSet = sellStrategyFactory.get("A").rule(topBlockStrategyEnum, data, tradeDate, positionStockCodeSet, sell_infoMap, btCompareDTO.get());

        log.info("S策略     >>>     date : {} , topBlockStrategyEnum : {} , size : {} , sell__stockCodeSet : {} , sell_infoMap : {}",
                 tradeDate, topBlockStrategyEnum, sell__stockCodeSet.size(), JSON.toJSONString(sell__stockCodeSet), JSON.toJSONString(sell_infoMap));


        // ---------------


        // TODO   一键清仓   ->   指定 个股列表
// TODO        tradeService.quickClearPosition(sell__stockCodeSet);


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


        List<String> buy__stockCodeList = backtestBuyStrategyD.rule(topBlockStrategyEnum, buyConSet, data, tradeDate, buy_infoMap, posResp.getPosratio().doubleValue() / 2, btCompareDTO.get().getZtFlag());


        // ---------------------------------------------------------


        // B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
        backtestStrategy.buy_sell__signalConflict(topBlockStrategyEnum, data, tradeDate, buy__stockCodeList, buy_infoMap);

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


        double singleStockMaxPosPct = btCompareDTO.get().getSingleStockMaxPosPct();
        double strategyPosRatio = btCompareDTO.get().getStrategyPosRatio();


        // newPositionList
//        List<QuickBuyPositionParam> newPositionList = convert__newPositionList(buy__stockCodeList);
        List<QuickBuyPositionParam> newPositionList = convert__newPositionList(buy__stockCodeList, strategyPosRatio, singleStockMaxPosPct, 0, 0);


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
        dto.setBuyConSet(null);
        dto.setSellConSet(null);
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
        TopStockPoolDTO new_dto = topBlockService.topStockList(today, TopTypeEnum.AUTO.type, 1);


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


    @Override
    public List<String> sellCodeList() {
        // 持仓S
        List<String> sellCodeList = TdxBlockNewReaderWriter.read("CCS");
        return sellCodeList;
    }

    @Override
    public List<StockSnapshotKlineDTO> sellList() {

        // 持仓S
        List<String> sellCodeList = sellCodeList();

        // kline
        List<StockSnapshotKlineDTO> klineList = KlineAPI.kline(sellCodeList);
        return klineList;
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


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * buy__stockCodeList   ->   newPositionList
     *
     * @param buy__stockCodeList
     * @return
     */
    public static List<QuickBuyPositionParam> convert__newPositionList(Collection<String> buy__stockCodeList) {
        return convert__newPositionList(buy__stockCodeList, ACCOUNT__POS_PCT_LIMIT, STOCK__POS_PCT_LIMIT, 0.0, 0.0);
    }

    public static List<QuickBuyPositionParam> convert__newPositionList(Collection<String> buy__stockCodeList,
                                                                       double singleStockMaxPosPct,
                                                                       double currPricePct,
                                                                       double prevPricePct) {

        return convert__newPositionList(buy__stockCodeList, ACCOUNT__POS_PCT_LIMIT, singleStockMaxPosPct, currPricePct, prevPricePct);
    }


    /**
     * @param buy__stockCodeList   买入个股列表
     * @param buyPosRatio          买入总仓位 比例限制（占账户总资金比例：0~1）
     * @param singleStockMaxPosPct 单只个股 最大仓位限制（%）
     * @param currPricePct         当前价格 涨跌幅（%）
     * @param prevPricePct         上一价格 涨跌幅（%）
     * @return
     */
    public static List<QuickBuyPositionParam> convert__newPositionList(Collection<String> buy__stockCodeList,
                                                                       double buyPosRatio,
                                                                       double singleStockMaxPosPct,
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
                                     newPosition.setPosPct(singleStockMaxPosPct > 0 ? singleStockMaxPosPct * buyPosRatio : STOCK__POS_PCT_LIMIT * buyPosRatio);


                                     // 价格
                                     double price = data.rt_stock__codePriceMap.computeIfAbsent(stockCode,
                                                                                                k -> {
                                                                                                    // 拉取实时行情
                                                                                                    pullStockSnapshotPrice(Sets.newHashSet(stockCode));
                                                                                                    // 买5/卖5     ->     卖5价（最高价 -> 一键买入）    // 这里不要乱改价！！！
                                                                                                    return data.rt_stock__codePriceMap.get(stockCode) /** 1.001*/;
                                                                                                });


                                     // 价格异常   ->   停牌
                                     if (Double.isNaN(price)) {
                                         log.warn("convert__newPositionList - err     >>>     [{} {}]   ->   price is NaN", stockCode, stockName);
                                         return null;
                                     }


                                     // --------------------------------- B价格策略 -------------------------------------


                                     // B价格策略
                                     price = buyPriceStrategyPct(stockCode, currPricePct, prevPricePct);


                                     // ---------------------------------


                                     newPosition.setPrice(price);

                                     // 数量（不用设置 -> 后续 自动计算）
                                     newPosition.setQuantity(100);


                                     // --------------------------------- 过滤 ------------------------------------------


                                     // ST（暂未进入退市）、*ST（退市中）
                                     if (stockName.contains("*ST")) {
                                         log.warn("买入[{}-{}]   -   [退市中] -> 略过     >>>     close : {} , 涨跌幅 : {}%",
                                                  stockCode, stockName, of(price), data.rt_stock__codePctMap.get(stockCode));

                                         // 限制买入 -> 极少仓位
                                         // newPosition.setPositionPct(Math.min(0.1 * STOCK__POS_PCT_LIMIT, 1));

                                         // 略过
                                         return null;
                                     }


                                     // 小账户 -> 禁止买入 百元股
                                     if (price > 300 || (stockCode.startsWith("68") && price > 200)) {
                                         log.warn("买入[{}-{}]   -   [高价股] -> 略过     >>>     close : {} , 涨跌幅 : {}%",
                                                  stockCode, stockName, of(price), data.rt_stock__codePctMap.get(stockCode));
                                         return null;
                                     }


                                     // 涨停股
                                     double zt_price = data.rt_stock_zt__codePriceMap.computeIfAbsent(stockCode,
                                                                                                      // 买5/卖5     ->     涨/跌停价
                                                                                                      k -> KlineAPI.kline(stockCode).getZtPrice());

                                     // 盘中（不含 集合竞价时间） ->  过滤 涨停股（无法买入）
                                     if (isTradingTime() && price >= zt_price) {
                                         log.warn("[盘中]买入[{}-{}]   -   [已涨停] -> 略过     >>>     close : {} , 涨跌幅 : {}%",
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


        buy__stockCodeList.forEach(stockCode -> {


            StockSnapshotKlineDTO dto = KlineAPI.kline(stockCode);
            SleepUtils.randomSleep(0, 10);


            String stockName = dto.getStockName();
            double change_pct = dto.getChange_pct();
            double currPrice = dto.getClose();
            double ztPrice = dto.getZtPrice();
            double dtPrice = dto.getDtPrice();


            data.stock__codeNameMap.put(stockCode, stockName);
            data.rt_stock__codePctMap.put(stockCode, change_pct);


//            data.rt_stock__codePriceMap.put(stockCode, NumUtil.of(currPrice * 1.001, 5));   // 略有延迟（1s） ->  保险起见：price x 1.001
            data.rt_stock__codePriceMap.put(stockCode, currPrice);   // 这里不要乱改价！！！
            data.rt_stock_zt__codePriceMap.put(stockCode, ztPrice);
            data.rt_stock_dt__codePriceMap.put(stockCode, dtPrice);
        });
    }


    /**
     * B价格策略（适配  盘中/盘后~盘前）
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
        double prevClose = price / (1 + pct * 0.01);


        // ------------------------------------------- 特殊处理（盘中） ---------------------------------------------------


        LocalTime time = LocalTime.now();


        // 盘中（9:15-15:00）
        if (time.isAfter(LocalTime.of(9, 15)) && time.isBefore(LocalTime.of(15, 0))) {


            Integer changePctLimit = StockLimitEnum.getChgPctLimit(stockCode, data.stock__codeNameMap.get(stockCode));

            // 涨停/近停   ->   挂 更低的价格（91% -> 1个跌停板） 买入
            if (changePctLimit == 10 && pct >= changePctLimit * 0.9) {
                price *= 0.97;
                prevClose *= 0.97;
            } else if (changePctLimit == 20 && pct >= changePctLimit * 0.75) {
                price *= 0.95;
                prevClose *= 0.95;
            } else if (changePctLimit == 30 && pct >= changePctLimit * 0.7) {
                price *= 0.93;
                prevClose *= 0.93;
            }
        }


        // ------------------------------ 盘后~盘前（15:00-9:15）  ->   更新prevClose -------------------------------------


        // 9:15        可向系统提交             ->   买卖委托
        // 9:15-9:25   只接受申报，不成交

        // 9:25        撮合[成交]时刻（按 [成交量] 最大原则   ->   确定当日 [开盘价]）

        // 9:25-9:30   静默期（提交的买卖委托    ->   暂存在系统，不处理）
        // 9:30        开盘  =>  竞价期间 订单  ->   按连续竞价规则处理


        // 盘后~盘前（15:00-9:15）
        if (time.isAfter(LocalTime.of(15, 0)) || time.isBefore(LocalTime.of(9, 15))) {


            // 盘前 提前挂单（盘后 无法挂单）  ->   打板（涨停_SSF多_月多）
            prevClose = price;   // 盘前 昨日收盘价（prevClose）  ->   当前最后一个交易日 收盘价（price）
            //                      否则，prevClose             ->   实际为 前2日的 收盘价（prev_2_Close）
            //                      因为，盘前挂单               ->   实际挂的是  次日的单


            // close = close
            // price  = price;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 按昨日收盘价涨跌幅%
        if (prevPricePct != 0) {
            return prevClose * (1 + prevPricePct * 0.01);
        }
        // 按当前价格涨跌幅%
        return price * (1 + currPricePct * 0.01);
    }


    /**
     * 是否 交易时间（盘中[不含 集合竞价时间]：9:30-15:00）
     *
     * @return
     */
    private static boolean isTradingTime() {
        LocalTime time = LocalTime.now();
        return time.isAfter(LocalTime.of(9, 30)) && time.isBefore(LocalTime.of(15, 0));
    }


    // -----------------------------------------------------------------------------------------------------------------


}