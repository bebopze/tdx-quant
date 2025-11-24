package com.bebopze.tdx.quant.strategy.backtest;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.BtTradeTypeEnum;
import com.bebopze.tdx.quant.common.constant.SellStrategyEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestCompareDTO;
import com.bebopze.tdx.quant.common.util.*;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.parser.check.TdxFunCheck;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.impl.InitDataServiceImpl;
import com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategyC;
import com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategyD;
import com.bebopze.tdx.quant.strategy.buy.BuyStrategyFactory;
import com.bebopze.tdx.quant.strategy.sell.SellStrategyFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * B/S策略 - 回测                    //     B/S策略 本质       =>       模式成功  🟰 大盘(70) ➕ 主线(25) ➕ 买点(5)
 *
 * @author: bebopze
 * @date: 2025/5/27
 */
@Data
@Slf4j
@Component
public class BacktestStrategy {


    // 共享数据
    private static volatile BacktestCache data = InitDataServiceImpl.data;


    // -----------------------------------------------------------------------------------------------------------------


    // 统计数据
    private static final ThreadLocal<Stat> x = ThreadLocal.withInitial(Stat::new);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * bt_trade_record   -   Cache
     */
    private static final ThreadLocal<Set<Long>> tradeRecord___idSet__cache = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<List<BtTradeRecordDO>> tradeRecordList__cache = ThreadLocal.withInitial(ArrayList::new);


    // -----------------------------------------------------------------------------------------------------------------


    public static final ThreadLocal<BacktestCompareDTO> btCompareDTO = ThreadLocal.withInitial(BacktestCompareDTO::new);


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private IBtTaskService btTaskService;

    @Autowired
    private IBtTradeRecordService btTradeRecordService;

    @Autowired
    private IBtPositionRecordService btPositionRecordService;

    @Autowired
    private IBtDailyReturnService btDailyReturnService;


    @Autowired
    private InitDataService initDataService;


    @Autowired
    private BuyStrategyFactory buyStrategyFactory;

    @Autowired
    private BacktestBuyStrategyC backtestBuyStrategyC;

    @Autowired
    private BacktestBuyStrategyD backtestBuyStrategyD;

    @Autowired
    private SellStrategyFactory sellStrategyFactory;


    @Autowired
    private MarketService marketService;


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    public Long backtest(Integer batchNo,
                         TopBlockStrategyEnum topBlockStrategyEnum, List<String> buyConList, List<String> sellConList,
                         LocalDate startDate, LocalDate endDate, BacktestCompareDTO btCompareDTO) {


        // TODO   回测-对照组 可变参数
        BacktestStrategy.btCompareDTO.set(btCompareDTO);


        try {
            return execBacktest(batchNo, topBlockStrategyEnum, buyConList, sellConList, startDate, endDate);
        } finally {
            clearThreadLocal();
        }
    }


    @TotalTime
    public void backtest_update(Long taskId, LocalDate update__startDate, LocalDate update__endDate) {

        try {
            execBacktest__update(taskId, update__startDate, update__endDate);
        } finally {
            clearThreadLocal();
        }
    }


    private Long execBacktest(Integer batchNo,
                              TopBlockStrategyEnum topBlockStrategyEnum,
                              List<String> buyConList,
                              List<String> sellConList,
                              LocalDate startDate,
                              LocalDate endDate) {

        log.info("execBacktest start     >>>     batchNo : {} , topBlockStrategyEnum : {} , buyConList : {} , sellConList : {} , startDate : {} , endDate : {}",
                 batchNo, topBlockStrategyEnum, buyConList, sellConList, startDate, endDate);


        endDate = DateTimeUtil.min(endDate, LocalDate.now());


        // -------------------------------------------------------------------------------------------------------------
        //                              回测-task   pre   ==>   板块、个股   行情数据 初始化
        // -------------------------------------------------------------------------------------------------------------


        // 数据初始化   ->   加载 全量行情数据
        log.info("--------------------------- " + Thread.currentThread().getName() + "线程 等待🔐     >>>     😴ing");
        long start = System.currentTimeMillis();
        // TODO   优化：一次只加载 3年数据
        initData(startDate, endDate);
        log.info("--------------------------- " + Thread.currentThread().getName() + "线程 释放🔐     >>>     ✅ 耗时：" + DateTimeUtil.formatNow2Hms(start));


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   创建
        // -------------------------------------------------------------------------------------------------------------


        endDate = DateTimeUtil.min(endDate, data.endDate());


        BtTaskDO taskDO = createBacktestTask(batchNo, topBlockStrategyEnum, buyConList, sellConList, startDate, endDate);


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   按日 循环执行
        // -------------------------------------------------------------------------------------------------------------


        LocalDate tradeDate = taskDO.getStartDate().minusDays(1);
        endDate = DateTimeUtil.min(taskDO.getEndDate(), data.endDate());


        // 总资金
        x.get().prevCapital = taskDO.getInitialCapital().doubleValue();
        // 可用金额
        x.get().prevAvlCapital = taskDO.getInitialCapital().doubleValue();


        while (tradeDate.isBefore(endDate)) {

            tradeDate = tradeDateIncr(tradeDate);
            // 备份
            Backup backup = backupThreadLocal();


//            // 数据初始化   ->   加载 全量行情数据
//            // TODO   优化：一次只加载 3年数据
//            if (DateTimeUtil.diff(tradeDate, data.endDate()) > 10) {
//                initData(tradeDate, tradeDate.plusYears(3));
//            }


            try {
                // 每日 - 回测（B/S）
                execBacktestDaily(topBlockStrategyEnum, buyConList, sellConList, tradeDate, taskDO);
            } catch (Exception e) {
                log.error("execBacktestDaily     >>>     taskId : {} , tradeDate : {} , exMsg : {}", taskDO.getId(), tradeDate, e.getMessage(), e);


                // retry
                retryExecBacktestDaily(topBlockStrategyEnum, buyConList, sellConList, tradeDate, taskDO, backup, 5);
            }
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            总收益
        // -------------------------------------------------------------------------------------------------------------


        sumTotalReturn(taskDO);


        log.info("execBacktest end     >>>     taskId : {} , startDate : {} , endDate : {}", taskDO.getId(), startDate, endDate);


        return taskDO.getId();
    }


    private void execBacktest__update(Long taskId,
                                      LocalDate update__startDate,
                                      LocalDate update__endDate) {

        log.info("execBacktest__update start     >>>     taskId : {} , update__startDate : {} , update__endDate : {}",
                 taskId, update__startDate, update__endDate);


        update__endDate = DateTimeUtil.min(update__endDate, LocalDate.now());


        // -------------------------------------------------------------------------------------------------------------
        //                              回测-task   pre   ==>   板块、个股   行情数据 初始化
        // -------------------------------------------------------------------------------------------------------------


        // 数据初始化   ->   加载 全量行情数据
        log.info("--------------------------- " + Thread.currentThread().getName() + "线程 等待🔐     >>>     😴ing");
        long start = System.currentTimeMillis();
        initData(update__startDate, update__endDate);
        log.info("--------------------------- " + Thread.currentThread().getName() + "线程 释放🔐     >>>     ✅ 耗时：" + DateTimeUtil.formatNow2Hms(start));


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   status -> 3
        // -------------------------------------------------------------------------------------------------------------


        BtTaskDO taskDO = btTaskService.getById(taskId);
        Assert.notNull(taskDO, "任务不存在：" + taskId);
        // 3-更新中
        taskDO.setStatus(3);
        btTaskService.updateById(taskDO);


        // -------------------------------------------------------------------------------------------------------------


        List<String> buyConList = Arrays.asList(taskDO.getBuyStrategy().split(","));
        List<String> sellConList = Arrays.asList(taskDO.getSellStrategy().split(","));

        TopBlockStrategyEnum topBlockStrategyEnum = TopBlockStrategyEnum.getByDesc(taskDO.getTopBlockStrategy());


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   按日 循环执行
        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // DEL     ->     待更新区间 old 回测数据
        btTaskService.delBacktestDataByTaskIdAndDate(taskId, update__startDate, update__endDate);


        // -------------------------------------------------------------------------------------------------------------


        // last
        BtDailyReturnDO last_dailyReturnDO = btDailyReturnService.lastByTaskId(taskId);


        LocalDate tradeDate = last_dailyReturnDO.getTradeDate();
        update__endDate = DateTimeUtil.min(update__endDate, data.endDate());


        // -------------------------------------------------------------------------------------------------------------


        // 恢复
        restoreThreadLocal__update(taskId, tradeDate, last_dailyReturnDO);


        // -------------------------------------------------------------------------------------------------------------


        while (tradeDate.isBefore(update__endDate)) {

            tradeDate = tradeDateIncr(tradeDate);
            // 备份
            Backup backup = backupThreadLocal();


            try {
                // 每日 - 回测（B/S）
                execBacktestDaily(topBlockStrategyEnum, buyConList, sellConList, tradeDate, taskDO);
            } catch (Exception e) {
                log.error("execBacktestDaily     >>>     taskId : {} , tradeDate : {} , exMsg : {}", taskDO.getId(), tradeDate, e.getMessage(), e);


                // retry
                retryExecBacktestDaily(topBlockStrategyEnum, buyConList, sellConList, tradeDate, taskDO, backup, 5);
            }
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            总收益
        // -------------------------------------------------------------------------------------------------------------


        taskDO.setEndDate(update__endDate);
        sumTotalReturn(taskDO);


        log.info("execBacktest__update end     >>>     taskId : {} , update__startDate : {} , update__endDate : {}", taskDO.getId(), update__startDate, update__endDate);
    }


    private void retryExecBacktestDaily(TopBlockStrategyEnum topBlockStrategyEnum,
                                        List<String> buyConList,
                                        List<String> sellConList,
                                        LocalDate tradeDate,
                                        BtTaskDO taskDO,
                                        Backup backup,
                                        int retry) {


        log.info("retryExecBacktestDaily - start     >>>     retry : {} , taskId : {} , tradeDate : {} , topBlockStrategyEnum : {} , buyConList : {} , sellConList : {}",
                 retry - 1, taskDO.getId(), tradeDate, topBlockStrategyEnum, buyConList, sellConList);


        // if (--retry < 0) {
        //     return;
        // }


        // 恢复
        restoreThreadLocal(backup);


        try {

            // 每日 - 回测（B/S）
            execBacktestDaily(topBlockStrategyEnum, buyConList, sellConList, tradeDate, taskDO);

            log.info("retryExecBacktestDaily - suc     >>>     retry : {} , taskId : {} , tradeDate : {} , topBlockStrategyEnum : {} , buyConList : {} , sellConList : {}",
                     retry, taskDO.getId(), tradeDate, topBlockStrategyEnum, buyConList, sellConList);


        } catch (Exception e) {

            log.error("retryExecBacktestDaily - fail     >>>     retry : {} , taskId : {} , tradeDate : {} , topBlockStrategyEnum : {} , buyConList : {} , sellConList : {}   ,   exMsg : {}",
                      retry, taskDO.getId(), tradeDate, topBlockStrategyEnum, buyConList, sellConList, e.getMessage(), e);


            // 重试失败   ->   中断 异常task
            if (--retry < 0) {
                throw e;
            }


            retryExecBacktestDaily(topBlockStrategyEnum, buyConList, sellConList, tradeDate, taskDO, backup, retry);
        }
    }


    private BtTaskDO createBacktestTask(Integer batchNo,
                                        TopBlockStrategyEnum topBlockStrategyEnum,
                                        List<String> buyConList,
                                        List<String> sellConList,
                                        LocalDate startDate,
                                        LocalDate endDate) {

        BtTaskDO taskDO = new BtTaskDO();

        // 任务批次号
        taskDO.setBatchNo(batchNo);
        // 任务状态
        taskDO.setStatus(1);

        // B/S策略
        taskDO.setTopBlockStrategy(topBlockStrategyEnum.getDesc());
        taskDO.setBuyStrategy(String.join(",", buyConList));
        taskDO.setSellStrategy(String.join(",", sellConList));


        // 回测 - 时间段
        taskDO.setStartDate(startDate);
        taskDO.setEndDate(endDate);

        // 初始本金
        taskDO.setInitialCapital(BigDecimal.valueOf(100_0000));
        // 初始净值
        taskDO.setInitialNav(BigDecimal.valueOf(1.0000));


        // -------------------------------------------------------------------------------------------------------------


        // extData
        Map<String, Object> extData = Maps.newHashMap();
        BacktestCompareDTO dto = btCompareDTO.get();
        extData.put("scoreSortN", dto.getScoreSortN());
        extData.put("stockPosPctLimit", dto.getStockPosPctLimit());
        taskDO.setExtData(JSON.toJSONString(extData));


        // -------------------------------------------------------------------------------------------------------------


        btTaskService.save(taskDO);


        return taskDO;
    }


    private void execBacktestDaily(TopBlockStrategyEnum topBlockStrategyEnum,
                                   List<String> buyConList,
                                   List<String> sellConList,
                                   LocalDate tradeDate,
                                   BtTaskDO taskDO) {


        Long taskId = taskDO.getId();


        x.get().taskId = taskId;
        x.get().tradeDate = tradeDate;


        x.get().avlCapital = x.get().prevAvlCapital;
        // 总资金     =>     今日 计算前 -> 先取 昨日总资金
        x.get().capital = x.get().prevCapital;


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日持仓（S前）
        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------- 卖出策略（ 先S[淘汰]  =>  空余资金  ->  B[新上榜] ）


        // -------------------------------------------------------------------------------------------------------------
        //                                                1、大盘 -> 仓位
        // -------------------------------------------------------------------------------------------------------------


        // 大盘量化   ->   总仓位 限制
        market__position_limit(tradeDate);


        // S前 -> 账户数据
        sell_before___statData___step1__init();


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略
        // -------------------------------------------------------------------------------------------------------------


        Map<String, SellStrategyEnum> sell_infoMap = Maps.newHashMap();


        // 卖出策略
        Set<String> sell__stockCodeSet = sellStrategyFactory.get("A").rule(topBlockStrategyEnum, data, tradeDate, x.get().positionStockCodeList, sell_infoMap);

        log.info("S策略     >>>     [{}] [{}] , topBlockStrategyEnum : {} , size : {} , sell__stockCodeSet : {} , sell_infoMap : {}",
                 taskId, tradeDate, topBlockStrategyEnum, sell__stockCodeSet.size(), JSON.toJSONString(sell__stockCodeSet), JSON.toJSONString(sell_infoMap));


        // 持仓个股   ->   匹配 淘汰


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）

        // 2.2 每日 淘汰策略（S策略 - 2）[排名]走弱 -> 末位淘汰 ->  stockCodeList（对昨日 持股 -> 末位淘汰[设置末尾淘汰 - 分数线/排名线 ]）


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略 -> 交易 record
        // -------------------------------------------------------------------------------------------------------------


        // S策略   ->   SELL TradeRecord
        createAndSave__SELL_TradeRecord(taskId, tradeDate, sell__stockCodeSet, x.get().stockCode_positionDO_Map, sell_infoMap);


        // S后  ->  账户统计数据
        refresh_statData();


        // ----------------------- S后 仓位校验   =>   是否需要 继续减仓

        // S后 总持仓市值  >  仓位总金额 上限     =>     等比减仓
        if (x.get().marketValue > x.get().positionLimitAmount) {

            // 实际 可用资金 < 0
            x.get().actAvlCapital = 0;

            // 等比减仓
            持仓_大于_持仓上限___等比减仓(taskId, tradeDate);


            // 减仓后（2次 S） ->  账户统计数据
            refresh_statData();
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            B策略
        // -------------------------------------------------------------------------------------------------------------


        Map<String, String> buy_infoMap = Maps.newConcurrentMap();
        // 当前仓位
        double posRate = x.get().getMarketValue() / x.get().getCapital();


        // 买入策略
        // List<String> buy__stockCodeList = buyStrategyFactory.get("A").rule(data, tradeDate, buy_infoMap, posRate);
        // List<String> buy__stockCodeList = backtestBuyStrategyC.rule2(topBlockStrategyEnum, buyConList, data, tradeDate, buy_infoMap, posRate, false);
        List<String> buy__stockCodeList = backtestBuyStrategyD.rule2(topBlockStrategyEnum, buyConList, data, tradeDate, buy_infoMap, posRate, false);

        log.info("B策略     >>>     [{}] [{}] , topBlockStrategyEnum : {} , size : {} , buy__stockCodeList : {} , buy_infoMap : {}",
                 taskId, tradeDate, topBlockStrategyEnum, buy__stockCodeList.size(), JSON.toJSONString(buy__stockCodeList), JSON.toJSONString(buy_infoMap));


        // -------------------------------------------------------------------------------------------------------------


        // TODO     同一日  同时满足       S策略（高位爆量上影大阴）   +   B策略（月多,60日新高,SSF多,RPS三线红,大均线多头）

        // TODO       ==>       S半仓   /   S（清仓） -> 不B


        // B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
        buy_sell__signalConflict(topBlockStrategyEnum, data, tradeDate, buy__stockCodeList);


        // -------------------------------------------------------------------------------------------------------------
        //                                            B策略 -> 交易 record
        // -------------------------------------------------------------------------------------------------------------


        log.debug("B策略 -> 交易 record - start     >>>     [{}] [{}] , prevAvlCapital : {} , sellCapital : {} , avlCapital : {} , prevCapital : {}",
                  taskId, tradeDate, x.get().prevAvlCapital, x.get().sellCapital, x.get().avlCapital, x.get().prevCapital);


        // B策略   ->   BUY TradeRecord
        createAndSave__BUY_TradeRecord(taskId, tradeDate, buy__stockCodeList, buy_infoMap);


        // B后  ->  账户统计数据
        refresh_statData();


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日持仓/清仓 -> record
        // -------------------------------------------------------------------------------------------------------------


        // save -> DB
        // btPositionRecordService.retryBatchSave(x.get().positionRecordDOList);
        // btPositionRecordService.retryBatchSave(x.get().clearPositionRecordDOList);

        List<BtPositionRecordDO> holdAndClearPosList = Lists.newArrayList(x.get().positionRecordDOList);
        holdAndClearPosList.addAll(x.get().clearPositionRecordDOList);
        btPositionRecordService.retryBatchSave(holdAndClearPosList);


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日收益
        // -------------------------------------------------------------------------------------------------------------


        calcDailyReturn(taskId, taskDO.getInitialCapital(), x.get().prevCapital, x.get().avlCapital, x.get().buyCapital, x.get().sellCapital, tradeDate, x.get().positionRecordDOList);


        // -------------------------------------------------------------------------------------------------------------


        // END   ->   prev 赋值
        refresh_statData__prev();
    }


    // ------------------------------------------------------- S -------------------------------------------------------


    /**
     * S策略   ->   SELL TradeRecord
     *
     * @param taskId
     * @param tradeDate
     * @param sell__stockCodeSet
     * @param sell_before__stockCode_positionDO_Map
     * @param sell_infoMap
     * @return
     */
    private void createAndSave__SELL_TradeRecord(Long taskId,
                                                 LocalDate tradeDate,
                                                 Set<String> sell__stockCodeSet,
                                                 Map<String, BtPositionRecordDO> sell_before__stockCode_positionDO_Map,
                                                 Map<String, SellStrategyEnum> sell_infoMap) {


        List<BtTradeRecordDO> sell__tradeRecordDO__List = Lists.newArrayList();


        for (String stockCode : sell__stockCodeSet) {

            BtTradeRecordDO sell_tradeRecordDO = new BtTradeRecordDO();
            sell_tradeRecordDO.setTaskId(taskId);
            sell_tradeRecordDO.setTradeType(BtTradeTypeEnum.SELL.getTradeType());
            sell_tradeRecordDO.setStockId(data.stock__codeIdMap.get(stockCode));
            sell_tradeRecordDO.setStockCode(stockCode);
            sell_tradeRecordDO.setStockName(data.stock__codeNameMap.get(stockCode));
            sell_tradeRecordDO.setTradeDate(tradeDate);


            // 交易信号 -> type分类（  ->  分类统计  【SELL指标】  胜率）
            // sell_tradeRecordDO.setTradeSignal(sell_infoMap.get(stockCode));
            SellStrategyEnum sellStrategyEnum = sell_infoMap.get(stockCode);
            sell_tradeRecordDO.setTradeSignalType(sellStrategyEnum.getType());
            sell_tradeRecordDO.setTradeSignalDesc(sellStrategyEnum.getDesc());


            sell_tradeRecordDO.setPrice(NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate)));
            sell_tradeRecordDO.setQuantity(sell_before__stockCode_positionDO_Map.get(stockCode).getQuantity());

            // 成交额 = 价格 x 数量
            double amount = sell_tradeRecordDO.getPrice().doubleValue() * sell_tradeRecordDO.getQuantity();
            sell_tradeRecordDO.setAmount(of(amount));

            // 仓位占比 = 持仓市值 / 总资金
            double positionPct = amount / x.get().capital * 100;
            sell_tradeRecordDO.setPositionPct(of(positionPct));

            sell_tradeRecordDO.setFee(BigDecimal.ZERO);


            sell__tradeRecordDO__List.add(sell_tradeRecordDO);
        }


        // save
        btTradeRecordService.retryBatchSave(sell__tradeRecordDO__List);
    }


    // ------------------------------------------------------- S -------------------------------------------------------


    /**
     * 持仓 > 持仓上限     =>     等比减仓
     *
     * @param taskId
     * @param tradeDate
     */
    private void 持仓_大于_持仓上限___等比减仓(Long taskId, LocalDate tradeDate) {


        // ----------------------------------------------------------


        // 已清仓
        if (x.get().positionRecordDOList.isEmpty()) {
            return;
        }


        // ---------------------------------------------------------- 等比减仓


        // 减仓总金额  =  S后_持仓总市值 - 仓位总金额_上限
        double total_reduction_amount = x.get().marketValue - x.get().positionLimitAmount;


        // 减仓总金额 市值占比 < 5%       直接略过
        if (total_reduction_amount / x.get().marketValue < 0.05) {
            // 金额太小  ->  略过
            log.debug("[{}] [{}]     >>>     持仓_大于_持仓限制___等比减仓  -  减仓总金额[{}] 市值占比[{}%]太小 -> 略过     >>>     marketValue : {} , positionLimitAmount : {}",
                      taskId, tradeDate,
                      total_reduction_amount, of(total_reduction_amount / x.get().marketValue * 100),
                      x.get().marketValue, x.get().positionLimitAmount);
            return;
        }


        // 持仓总市值
        double totalMarketValue = x.get().marketValue;


        for (BtPositionRecordDO positionRecordDO : x.get().positionRecordDOList) {


            String stockCode = positionRecordDO.getStockCode();
            double marketValue = positionRecordDO.getMarketValue().doubleValue();
            int quantity = positionRecordDO.getQuantity();


            // ---------------------------------------------------------------


            // 个股 减仓金额  =  个股 市值占比  x  减仓总金额
            double stock_reduction_amount = marketValue / totalMarketValue * total_reduction_amount;
//            Assert.isTrue(stock_reduction_amount <= marketValue,
//                          String.format("超卖：个股减仓金额[%s] > 个股市值[%s]", stock_reduction_amount, marketValue));


            BtTradeRecordDO sell_tradeRecordDO = new BtTradeRecordDO();
            sell_tradeRecordDO.setTaskId(taskId);
            sell_tradeRecordDO.setTradeType(BtTradeTypeEnum.SELL.getTradeType());
            sell_tradeRecordDO.setStockId(data.stock__codeIdMap.get(stockCode));
            sell_tradeRecordDO.setStockCode(stockCode);
            sell_tradeRecordDO.setStockName(data.stock__codeNameMap.get(stockCode));

            sell_tradeRecordDO.setTradeDate(tradeDate);
            // 大盘仓位限制->等比减仓
            sell_tradeRecordDO.setTradeSignalType(SellStrategyEnum.S31.getType());
            sell_tradeRecordDO.setTradeSignalDesc(SellStrategyEnum.S31.getDesc());


            double closePrice = getClosePrice(stockCode, tradeDate);
            sell_tradeRecordDO.setPrice(NumUtil.double2Decimal(closePrice));


            int qty = (int) (stock_reduction_amount / closePrice);
            // 不能超卖
            qty = Math.min(qty, quantity);
            sell_tradeRecordDO.setQuantity(qty);


            // 成交额 = 价格 x 数量
            double amount = sell_tradeRecordDO.getPrice().doubleValue() * sell_tradeRecordDO.getQuantity();
            sell_tradeRecordDO.setAmount(of(amount));


            // 仓位占比 = 持仓市值 / 总资金
            double positionPct = amount / x.get().capital * 100;
            sell_tradeRecordDO.setPositionPct(of(positionPct));

            sell_tradeRecordDO.setFee(BigDecimal.ZERO);


            btTradeRecordService.retryBatchSave(Lists.newArrayList(sell_tradeRecordDO));
        }
    }


    // ------------------------------------------------------- B -------------------------------------------------------


    /**
     * B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
     *
     * @param topBlockStrategyEnum
     * @param data
     * @param tradeDate
     * @param buy__stockCodeList
     */
    @TotalTime
    public void buy_sell__signalConflict(TopBlockStrategyEnum topBlockStrategyEnum,
                                         BacktestCache data,
                                         LocalDate tradeDate,
                                         List<String> buy__stockCodeList) {


        Map<String, SellStrategyEnum> sell_infoMap = Maps.newHashMap();


        // 当前 buyList   ->   是否 与 S策略 相互冲突       =>       过滤出 冲突个股（sellList）
        Set<String> sell__stockCodeSet = sellStrategyFactory.get("A").rule(topBlockStrategyEnum, data, tradeDate, buy__stockCodeList, sell_infoMap);


        // buyList   ->   remove  冲突个股（sellList）
        buy__stockCodeList.removeAll(sell__stockCodeSet);


        if (CollectionUtils.isNotEmpty(buy__stockCodeList)) {

            log.warn("buy_sell__signalConflict  -  remove BS冲突个股     >>>     taskId : {} , tradeDate : {} , sell__stockCodeSet : {} , sell_infoMap : {} , new__buy__stockCodeList : {}",
                     x.get().taskId, x.get().tradeDate, JSON.toJSONString(sell__stockCodeSet), JSON.toJSONString(sell_infoMap), JSON.toJSONString(buy__stockCodeList));
        }
    }


    /**
     * B策略   ->   BUY TradeRecord
     *
     * @param taskId
     * @param tradeDate
     * @param buy__stockCodeList
     * @param buy_infoMap
     */
    private void createAndSave__BUY_TradeRecord(Long taskId,
                                                LocalDate tradeDate,
                                                List<String> buy__stockCodeList,
                                                Map<String, String> buy_infoMap) {


        int size_B = buy__stockCodeList.size();
        if (size_B == 0) {
            return;
        }


        log.debug("B策略 -> 交易 record - end     >>>     [{}] [{}] , prevAvlCapital : {} , sellCapital : {} , avlCapital : {} , prevCapital : {} , buyCapital : {}",
                  taskId, tradeDate, x.get().prevAvlCapital, x.get().sellCapital, x.get().avlCapital, x.get().prevCapital, x.get().buyCapital);


        // ------------------------------------------


        // 等比买入
        BigDecimal avg_amount = of(x.get().actAvlCapital / size_B);
        // 单一个股   单次最大买入  剩余资金 x 10%
        // double STOCK_POS__LIMIT = 0.1;
        double STOCK_POS__LIMIT = btCompareDTO.get().getStockPosPctLimit() * 0.01;
        avg_amount = avg_amount.min(of(x.get().actAvlCapital * STOCK_POS__LIMIT));       // 可用资金 * 10%


        // 单一个股   最大仓位限制  =  总资金 x 10%
        double amount_limit = x.get().capital * STOCK_POS__LIMIT;      // 总资金 * 10%


        // ------------------------------------------


        // B策略   ->   BUY TradeRecord
        List<BtTradeRecordDO> buy__tradeRecordDO__List = Lists.newArrayList();


        for (String stockCode : buy__stockCodeList) {


            // 当前  待买入个股  市值（如果 此前已持有 该个股）
            double marketValue = Optional.ofNullable(x.get().stockCode_positionDO_Map.get(stockCode)).map(e -> e.getMarketValue().doubleValue()).orElse(0.0);


            // 可买仓位  =  最大仓位限制 - 个股市值
            double amount = amount_limit - marketValue;
            if (amount <= 0) {
                continue;
            } else {
                amount = Math.min(amount, avg_amount.doubleValue());
            }


            // -----------------------------------------------------------


            BtTradeRecordDO tradeRecordDO = new BtTradeRecordDO();
            tradeRecordDO.setTaskId(taskId);
            tradeRecordDO.setTradeType(BtTradeTypeEnum.BUY.getTradeType());
            tradeRecordDO.setStockId(data.stock__codeIdMap.get(stockCode));
            tradeRecordDO.setStockCode(stockCode);
            tradeRecordDO.setStockName(data.stock__codeNameMap.get(stockCode));
            tradeRecordDO.setTradeDate(tradeDate);
            // tradeRecordDO.setTradeSignal(buy_infoMap.get(stockCode));
            tradeRecordDO.setTradeSignalType(1);
            tradeRecordDO.setTradeSignalDesc(buy_infoMap.get(stockCode));

            // 收盘价
            BigDecimal close = NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate));
            tradeRecordDO.setPrice(close);

            // 买入数量   =   可买仓位 / 收盘价                                  （忽略 🐶💩共产主义特色   ->   100股 bug）
            double qty = amount / close.doubleValue();
            tradeRecordDO.setQuantity((int) qty);

            // 成交额 = 价格 x 数量
            tradeRecordDO.setAmount(of(amount));

            // 仓位占比 = 持仓市值 / 总资金
            double positionPct = amount / x.get().capital * 100;
            tradeRecordDO.setPositionPct(of(positionPct));

            tradeRecordDO.setFee(BigDecimal.ZERO);


            // ---------------------------------------------------------------------------------------------------------


            // 买入0股（     amount -> (0,1)     ）
            if (qty < 1) {
                log.warn("createAndSave__BUY_TradeRecord  -  买入数量<1股     >>>     taskId : {} , tradeDate : {} , stockCode : {} , closePrice : {} , amount : {} , qty : {}",
                         taskId, tradeDate, stockCode, close, amount, qty);
                continue;
            }

            if (close.doubleValue() <= 0.0) {
                log.error("createAndSave__BUY_TradeRecord  -  买入价格<=0     >>>     taskId : {} , tradeDate : {} , stockCode : {} , closePrice : {} , amount : {} , qty : {}",
                          taskId, tradeDate, stockCode, close, amount, qty);

                throw new BizException("买入价格<=0");
            }


            // ---------------------------------------------------------------------------------------------------------


            buy__tradeRecordDO__List.add(tradeRecordDO);
        }


        btTradeRecordService.retryBatchSave(buy__tradeRecordDO__List);
    }


    // ------------------------------------------------------ 大盘 ------------------------------------------------------


    /**
     * 大盘量化   ->   总仓位 限制
     *
     * @param tradeDate
     */
    private void market__position_limit(LocalDate tradeDate) {

        QaMarketMidCycleDO marketInfo = data.marketCache.get(tradeDate, k -> marketService.marketInfo(tradeDate));
        Assert.notNull(marketInfo, "[大盘量化]数据为空：" + tradeDate);


        // 总仓位 - %上限
        double positionPct = marketInfo.getPositionPct().doubleValue();
        x.get().positionLimitRate = positionPct == 0 ? 0 : positionPct / 100;
    }


    // --------------------------------------------------- statData ----------------------------------------------------


    /**
     * SELL - before        =>      计算 总资金
     */
    private void sell_before___statData___step1__init() {


        // 获取 -> 持仓列表
        List<BtPositionRecordDO> positionRecordDOList = getDailyPositions(x.get().taskId, x.get().tradeDate);


        // 计算
        CalcStat calcStat = new CalcStat(positionRecordDOList, null);


        // ------------------------------------------------------------------------

        // copy覆盖
        // BeanUtils.copyProperties(calcStat, x.get());


        x.get().positionRecordDOList = positionRecordDOList;
        x.get().positionStockCodeList = calcStat.getPositionStockCodeList();
        x.get().stockCode_positionDO_Map = calcStat.getStockCode_positionDO_Map();


        // ------------------------------------------------------------------------


        // 当前 总市值   =   S前 总市值
        x.get().marketValue = calcStat.getMarketValue();

        // S前 可用资金   =   昨日 可用资金
        x.get().avlCapital = x.get().prevAvlCapital;


        // ---------------------------------------------------------- 不变


        // S前 总资金   =   S前 总市值  +  S前 可用资金
        x.get().capital = x.get().marketValue + x.get().avlCapital;
        log.debug("init capital   -   [{}] [{}]     >>>     capital : {} , marketValue : {} , avlCapital : {}",
                  x.get().taskId, x.get().tradeDate, x.get().capital, x.get().marketValue, x.get().avlCapital);


        // ---------------------------------------------------------- 不变


        // 仓位总金额 上限   =   总资金  x  仓位百分比 上限
        x.get().positionLimitAmount = x.get().capital * x.get().positionLimitRate;


        // 当前 实际可用资金（策略 -> 仓位限制）  =   仓位总金额_上限   -   持仓总市值
        x.get().actAvlCapital = x.get().positionLimitAmount - x.get().marketValue;
    }


    // --------------------------------------------------- statData ----------------------------------------------------


    /**
     * refresh  ->  statData
     */
    private void refresh_statData() {
        // 获取  ->  当前 持仓列表
        List<BtPositionRecordDO> positionRecordDOList = getDailyPositions(x.get().taskId, x.get().tradeDate);
        // 获取  ->  今日 B/S记录
        List<BtTradeRecordDO> tradeRecordDOList = btTradeRecordService.listByTaskIdAndTradeDate(x.get().taskId, x.get().tradeDate);


        // 计算
        CalcStat calcStat = new CalcStat(positionRecordDOList, tradeRecordDOList);
        // copy覆盖
        BeanUtils.copyProperties(calcStat, x.get());


        // -------------------------------------------------------------------------------------------------------------


        // TODO   优化   ->   清理 早于持仓最小买入日期 的交易记录
        // clear__TradeRecordCache();
    }


    // --------------------------------------------------- statData ----------------------------------------------------


    /**
     * prev 赋值
     */
    private void refresh_statData__prev() {

        Stat x_copy = new Stat();
        BeanUtils.copyProperties(x.get(), x_copy);


        // 1、清空
        x.remove();


        // 2、today -> pre
        x.get().prevCapital = x_copy.capital;
        x.get().prevAvlCapital = x_copy.avlCapital;
        // x.get().prevPositionRecordDOList = x_copy.positionRecordDOList;
        x.get().prev__stockCode_positionDO_Map = x_copy.stockCode_positionDO_Map;


        x.get().taskId = x_copy.taskId;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public LocalDate tradeDateIncr(LocalDate tradeDate) {
        Integer idx = data.dateIndexMap.get(tradeDate);

        // 非交易日
        while (idx == null) {
            // 下一自然日   ->   直至 交易日
            tradeDate = tradeDate.plusDays(1);
            idx = data.dateIndexMap.get(tradeDate);


            if (!DateTimeUtil.between(tradeDate, data.startDate(), data.endDate())) {
                throw new BizException(String.format("[日期：%s]非法，超出有效交易日范围", tradeDate));
            }
        }


        // 下一个
        return data.dateList.get(idx + 1);
    }

    public LocalDate tradeDateDecr(LocalDate tradeDate) {
        Integer idx = data.dateIndexMap.get(tradeDate);

        // 非交易日
        while (idx == null) {
            // 上一自然日   ->   直至 交易日
            tradeDate = tradeDate.minusDays(1);
            idx = data.dateIndexMap.get(tradeDate);


            if (!DateTimeUtil.between(tradeDate, data.startDate(), data.endDate())) {
                throw new BizException(String.format("[日期：%s]非法，超出有效交易日范围", tradeDate));
            }
        }


        // 上一个
        return data.dateList.get(idx - 1);
    }


    /**
     * 计算  ->  每日收益率
     *
     * @param taskId
     * @param initialCapital       本金
     * @param avlCapital
     * @param buyCapital
     * @param sellCapital
     * @param tradeDate            当前 交易日
     * @param positionRecordDOList 当前 持仓列表
     */
    private void calcDailyReturn(Long taskId,
                                 BigDecimal initialCapital,
                                 double prevCapital,
                                 double avlCapital, double buyCapital, double sellCapital,
                                 LocalDate tradeDate,
                                 List<BtPositionRecordDO> positionRecordDOList) {


        // 当日 持仓市值   =   个股市值   汇总
        BigDecimal marketValue = positionRecordDOList.stream()
                                                     .map(BtPositionRecordDO::getMarketValue)
                                                     .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!TdxFunCheck.equals(marketValue, x.get().marketValue)) {
            log.warn("calcDailyReturn - err     >>>     [{}] [{}] , marketValue : {} , x.marketValue : {}",
                     taskId, tradeDate, marketValue, x.get().marketValue);
        }


        // 总资金  =  持仓市值 + 可用资金
        BigDecimal capital = marketValue.add(of(avlCapital));

        if (!TdxFunCheck.equals(capital, x.get().capital)) {
            log.warn("calcDailyReturn - err     >>>     [{}] [{}] , capital : {} , x.capital : {}",
                     taskId, tradeDate, capital, x.get().capital);
        }


        if (!TdxFunCheck.equals(avlCapital, x.get().avlCapital)) {
            log.warn("calcDailyReturn - err     >>>     [{}] [{}] , avlCapital : {} , x.avlCapital : {}",
                     taskId, tradeDate, avlCapital, x.get().avlCapital);
        }


        // 仓位占比 = 持仓市值 / 总资金
        BigDecimal positionPct = marketValue.divide(capital, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));


        // 净值 = 总资金 / 本金
        BigDecimal nav = capital.divide(initialCapital, 8, RoundingMode.HALF_UP);

        // 当日盈亏额 = 当日总资金 - 昨日总资金
        BigDecimal profitLossAmount = capital.subtract(of(prevCapital));

        // 当日收益率 = 当日总资金 / 昨日总资金 - 1
        // BigDecimal dailyReturn = capital.divide(prevCapital, 6, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);

        // 当日收益率 = 当日盈亏额 / 昨日总资金
        BigDecimal dailyReturn = profitLossAmount.divide(of(prevCapital), 6, RoundingMode.HALF_UP);
        log.debug("calcDailyReturn     >>>     [{}] [{}] , marketValue : {} , avlCapital : {} , capital : {} , prevCapital : {} , profitLossAmount : {} , dailyReturn : {} , nav : {}",
                  taskId, tradeDate, marketValue, avlCapital, capital, prevCapital, profitLossAmount, dailyReturn, nav);


        BtDailyReturnDO dailyReturnDO = new BtDailyReturnDO();
        dailyReturnDO.setTaskId(taskId);
        // 日期
        dailyReturnDO.setTradeDate(tradeDate);
        // 当日收益率
        dailyReturnDO.setDailyReturn(dailyReturn);
        // 当日盈亏额
        dailyReturnDO.setProfitLossAmount(profitLossAmount);
        // 净值
        dailyReturnDO.setNav(nav);
        // 总资金
        dailyReturnDO.setCapital(capital);
        // 持仓市值
        dailyReturnDO.setMarketValue(marketValue);
        // 仓位占比（%）
        dailyReturnDO.setPositionPct(of(positionPct));
        // 仓位上限占比（%）
        dailyReturnDO.setPositionLimitPct(of(x.get().positionLimitRate * 100));
        // 可用资金
        dailyReturnDO.setAvlCapital(of(avlCapital));
        // 买入金额
        dailyReturnDO.setBuyCapital(of(buyCapital));
        // 卖出金额
        dailyReturnDO.setSellCapital(of(sellCapital));

        // 基准收益（沪深300）
        dailyReturnDO.setBenchmarkReturn(null);


        btDailyReturnService.retrySave(dailyReturnDO);
    }


    /**
     * 汇总计算 -> 总收益
     *
     * @param taskDO
     */
    private void sumTotalReturn(BtTaskDO taskDO) {


        // 全期汇总：更新 bt_task


        // 全量  每日收益-记录
        List<BtDailyReturnDO> dailyReturnDOList = btDailyReturnService.listByTaskId(x.get().taskId);


        // 最大回撤
        DrawdownResult drawdownResult = calcMaxDrawdown(dailyReturnDOList);


        // 每日收益率 列表
        List<BigDecimal> dailyReturnList = drawdownResult.dailyReturnList;


        // ------------------------------------------------------


        // 交易胜率
        TradePairStat.TradeStatResult tradeStatResult = TradePairStat.calcTradeWinPct(tradeRecordList__cache.get());

        // task 交易胜率
        double winRate = tradeStatResult.getWinPct();
        // 个股 交易胜率
        List<TradePairStat.StockStat> stockStatList = tradeStatResult.getStockStatList();


        // ------------------------------------------------------


        // 总天数（持仓天数）   ->   间隔  N个交易日
        int totalDays = dailyReturnDOList.size();


        // final  ->  Last
        BtDailyReturnDO finalReturn = dailyReturnDOList.get(dailyReturnDOList.size() - 1);
        BigDecimal finalNav = finalReturn.getNav();
        BigDecimal finalCapital = finalReturn.getCapital();


        // 净值增幅 =  期末净值 - 初始净值（1）
        BigDecimal totalReturn = finalNav.subtract(BigDecimal.ONE);
        // 总收益率（%） =  净值增幅 x 100%
        BigDecimal totalReturnPct = totalReturn.multiply(of(100));
        // 年化收益率（%） = （期末净值 / 初始净值）^(252 / 总天数) - 1          x 100%
        BigDecimal annualReturnPct = of(Math.pow(finalNav.doubleValue(), 252.0 / totalDays) - 1).multiply(of(100));


        // 夏普比率 = 日均收益 / 日收益标准差 * sqrt(252)
        double mean = dailyReturnList.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double sd = Math.sqrt(dailyReturnList.stream()
                                             .mapToDouble(r -> Math.pow(r.doubleValue() - mean, 2))
                                             .sum() / dailyReturnList.size());
        double sharpe = mean / sd * Math.sqrt(252);


        // 盈利天数 占比  =  盈利天数 / 总天数
        BigDecimal profitDayPct = of((double) drawdownResult.profitDayCount / totalDays * 100);


        // 盈亏比（日级） =  所有盈利日平均收益 / 所有亏损日平均亏损
        double avgWin = dailyReturnList.stream().filter(r -> r.doubleValue() > 0).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double avgLoss = dailyReturnList.stream().filter(r -> r.doubleValue() < 0).mapToDouble(BigDecimal::doubleValue).map(Math::abs).average().orElse(0);

        BigDecimal profitFactor = avgLoss == 0 ? of(Double.POSITIVE_INFINITY) : of(avgWin / avgLoss);


        // ------------------------------------------------ 更新 bt_task


        taskDO.setTotalTrade(tradeStatResult.total);
        taskDO.setTotalTradeAmount(NumUtil.double2Decimal(tradeStatResult.totalTradeAmount));
        taskDO.setStatus(2);


        taskDO.setFinalCapital(finalCapital);
        taskDO.setFinalNav(finalNav);
        taskDO.setTotalDay(totalDays);
        taskDO.setTotalReturnPct(totalReturnPct);
        taskDO.setAnnualReturnPct(annualReturnPct);
        taskDO.setWinPct(of(winRate));
        taskDO.setProfitFactor(profitFactor);
        taskDO.setMaxDrawdownPct(drawdownResult.drawdownPct);
        taskDO.setProfitDayPct(profitDayPct);
        taskDO.setSharpeRatio(of(sharpe));


        // result - JSON详情
        taskDO.setTradeStatResult(JSON.toJSONString(tradeStatResult));
        taskDO.setDrawdownResult(JSON.toJSONString(drawdownResult));


        btTaskService.updateById(taskDO);
    }


    public DrawdownResult calcMaxDrawdown(List<BtDailyReturnDO> list) {

        DrawdownResult result = new DrawdownResult();
        result.drawdownPct = BigDecimal.ZERO;


        // -------------------------


        // 波峰 tmp
        BigDecimal peakNav = BigDecimal.ZERO;
        LocalDate peakDate = null;


        // --------------------------------------------------


        for (BtDailyReturnDO rec : list) {
            BigDecimal nav = rec.getNav();
            LocalDate date = rec.getTradeDate();


            // 当日创 最大净值   ->   新 波峰
            if (nav.compareTo(peakNav) > 0) {
                // 波峰
                peakNav = nav;
                peakDate = date;
            }


            // 当日回撤  = （净值 - 波峰）/ 波峰
            BigDecimal ddPct = nav.subtract(peakNav).divide(peakNav, 6, RoundingMode.HALF_UP).multiply(of(100));


            // 当日创 最大回撤   ->   新 波谷
            if (ddPct.compareTo(result.drawdownPct) < 0) {

                // 波谷
                result.drawdownPct = ddPct;
                result.troughDate = date;
                result.troughNav = nav;

                // 波峰
                result.peakDate = peakDate;
                result.peakNav = peakNav;
            }


            // 汇总统计 - 指标更新


            // -------------------------


            // 盈利天数
            if (rec.getDailyReturn().doubleValue() > 0) {
                result.profitDayCount++;
            }


            // -------------------------


            // 每日收益率
            result.dailyReturnList.add(rec.getDailyReturn());
        }


        return result;
    }


    /**
     * 获取 某回测任务 在指定日期的   持仓详情
     *
     * @param taskId
     * @param endTradeDate 统计 截止日期
     * @return
     */
    private List<BtPositionRecordDO> getDailyPositions(Long taskId, LocalDate endTradeDate) {


        // -------------------------------------------------------------------------------------------------------------


        // 1、全量 B/S记录     =>     当前B/S（未清仓）   +   历史B/S（已清仓）


        // 每次  ->  增量查询     +     历史记录（cache）


        List<BtTradeRecordDO> incrQuery_tradeRecordList = btTradeRecordService.listByTaskIdAndTradeDate(taskId, endTradeDate);


        incrQuery_tradeRecordList.forEach(e -> {

            if (tradeRecord___idSet__cache.get().add(e.getId())) {
                tradeRecordList__cache.get().add(e);
            }
        });


        // -------------------------------------------------------------------------------------------------------------


        // 2、剔除   ->   历史B/S（已清仓）


        // 当日持仓（买入记录）列表   ->   当前B/S（抵消后 -> 未清仓）
        List<BtTradeRecordDO> todayHoldingList = Lists.newArrayList();
        // 当日清仓列表             ->   清仓stockCode - 清仓（卖出记录）
        Map<String, BtTradeRecordDO> todayClearMap = Maps.newHashMap();
        // 当日S但没一次性卖完（有且仅有一种情况：大盘仓位限制->等比减仓）
        Map<String, BtTradeRecordDO> todaySellAndNotClearMap = Maps.newHashMap();


        // 持仓列表、清仓列表
        holdingList__buyQueues__todayClearedCodes(endTradeDate, tradeRecordList__cache, todayHoldingList, todayClearMap, todaySellAndNotClearMap);


        // todayHoldingList 中即为“当日未清仓”的买入记录（quantity 已是剩余量）


        // -------------------------------------------------------------------------------------------------------------


        // 3. 汇总买卖
        Map<String, Integer> quantityMap = Maps.newHashMap();       // 个股持仓 -   总数量
        Map<String, Integer> avlQuantityMap = Maps.newHashMap();    // 个股持仓 - 可用数量（T+1）
        Map<String, Double> amountMap = Maps.newHashMap();          // 个股持仓 -   总成本（买入价格 x 买入数量   ->   累加）

        Map<String, PositionInfo> codeInfoMap = Maps.newHashMap();  // 个股持仓 - 首次买入Info


        // --------------------------------------------


        // 成本计算
        quantityMap__avlQuantityMap__amountMap__codeInfoMap(endTradeDate, todayHoldingList, quantityMap, avlQuantityMap, amountMap, codeInfoMap);


        // -------------------------------------------------------------------------------------------------------------


        // 4. 构造 当日持仓 对象列表
        List<BtPositionRecordDO> positionRecordDOList = todayPositionRecordList(taskId, endTradeDate, quantityMap, avlQuantityMap, amountMap, codeInfoMap, todaySellAndNotClearMap);


        // -------------------------------------------------------------------------------------------------------------


        // 5. 构造 当日清仓 对象列表
        List<BtPositionRecordDO> todayClearPositionRecordDOList = todayClearPositionRecordList(taskId, endTradeDate, todayClearMap);
        x.get().clearPositionRecordDOList = todayClearPositionRecordDOList;
        // btPositionRecordService.saveBatch(todayClearPositionRecordDOList);


        // -------------------------------------------------------------------------------------------------------------


        return positionRecordDOList;
    }


    /**
     * 优化 -> 清理 早于持仓最小买入日期 的交易记录      =>     DEL配对历史记录（ < 当前持仓 最小BuyDate）
     */
    private void clear__TradeRecordCache() {


        // 当前 持仓个股列表   ->   最小 买入日期
        LocalDate minBuyDate = x.get().getPositionRecordDOList().stream()
                                .map(BtPositionRecordDO::getBuyDate)
                                .min(LocalDate::compareTo)
                                .orElse(null);


        if (null == minBuyDate) {
            return;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 清理 早于持仓最小买入日期 的交易记录
        tradeRecordList__cache.get().removeIf(e -> {

            if (e.getTradeDate().isBefore(minBuyDate)) {

                tradeRecord___idSet__cache.get().remove(e.getId());
                return true;
            }
            return false;
        });
    }


    /**
     * 持仓列表、清仓列表
     *
     * @param endTradeDate
     * @param tradeRecordList__cache
     * @param todayHoldingList        当日持仓（买入记录）列表   ->   当前B/S（抵消后 -> 未清仓）
     * @param todayClearMap           当日清仓列表             ->   清仓stockCode - 清仓（卖出记录）
     * @param todaySellAndNotClearMap 当日S但没一次性卖完（有且仅有一种情况：大盘仓位限制->等比减仓）
     */
    private void holdingList__buyQueues__todayClearedCodes(LocalDate endTradeDate,
                                                           ThreadLocal<List<BtTradeRecordDO>> tradeRecordList__cache,
                                                           List<BtTradeRecordDO> todayHoldingList,
                                                           Map<String, BtTradeRecordDO> todayClearMap,
                                                           Map<String, BtTradeRecordDO> todaySellAndNotClearMap) {


        // 个股code  -  个股 买单队列   （构建 FIFO 队列：队列里存 剩余的买单）
        Map<String, Deque<BuyTradeRecord>> stockCode_buyQueue_map = new HashMap<>();


        // 遍历所有记录，构建/抵销
        for (BtTradeRecordDO tr : tradeRecordList__cache.get()) {

            String stockCode = tr.getStockCode();
            int qty = tr.getQuantity();


            if (Objects.equals(tr.getTradeType(), BtTradeTypeEnum.BUY.getTradeType())) {

                // 买入：入队
                stockCode_buyQueue_map.computeIfAbsent(stockCode, k -> new LinkedList<>()).addLast(new BuyTradeRecord(tr, qty));

            } else {


                // --------------------------------- 卖出：用 FIFO 队头   ->   买单抵销


                // 当前个股   ->   买单queue
                Deque<BuyTradeRecord> b_queue = stockCode_buyQueue_map.get(stockCode);

                // 当前个股   ->   当笔卖单 卖出量
                int s_remaining = qty;


                while (s_remaining > 0 && b_queue != null && !b_queue.isEmpty()) {

                    // 买单 依次出列   ->   抵消卖单
                    BuyTradeRecord b_head = b_queue.peekFirst();


                    // 持有数量 > 当笔卖出量     =>     继续保留 该笔买单（未能1次卖光）
                    if (b_head.remainingQty > s_remaining) {

                        // 买单扣减（有剩余）  ->   卖单数量
                        b_head.remainingQty -= s_remaining;

                        // 该笔卖单 -> 完全抵销
                        s_remaining = 0;
                    }


                    // 持有数量 <= 当笔卖出量     =>     该笔买单 完全抵销（1次卖光）
                    else {

                        // 卖单扣减（可能 有剩余）  ->   买单数量
                        s_remaining -= b_head.remainingQty;

                        // 该笔买单 -> 完全抵销（1次卖光）
                        b_queue.pollFirst();
                    }
                }


                // -----------------------------------------------------------------------------------------------------


                // （可选）如果 s_remaining > 0     ->     说明 超卖，按业务处理
                if (s_remaining > 0) {
                    log.warn("超卖     >>>     股票[{}] 卖出[{}]股，超卖[{}]股", stockCode, qty, s_remaining);
                }


                // -----------------------------------------------------------------------------------------------------


                // 当日卖出  +  卖光（买单全部被抵消 -> 持仓为0）  ->   记录 清仓标记
                if (tr.getTradeDate().isEqual(endTradeDate) && CollectionUtils.isEmpty(b_queue) /* && s_remaining >= 0 */) {
                    todayClearMap.put(stockCode, tr);
                }


                // 当日卖出  +  未卖光（买单有剩余     =>     大盘仓位限制->等比减仓）
                if (tr.getTradeDate().isEqual(endTradeDate) && CollectionUtils.isNotEmpty(b_queue) /* && s_remaining == 0 */) {
                    // 当日S但没一次性卖完（有且仅有一种情况：大盘仓位限制->等比减仓）
                    todaySellAndNotClearMap.put(stockCode, tr);
                }
            }
        }


        // -------------------------------------------------------------------------------------------------------------


        // 当前 持仓个股 列表     ->     各队列所有   剩余的买单（抵消完 全部卖单后）


        for (Deque<BuyTradeRecord> queue : stockCode_buyQueue_map.values()) {

            for (BuyTradeRecord bt : queue) {

                // 转换回原DO 并把 quantity 调成剩余数量
                BtTradeRecordDO b_original = bt.b_original;


                // 当前买单 b_original     ->     剩余持仓量（当前个股 剩余持仓数量）  ->   抵消卖单后的 剩余数量
                b_original.setQuantity(bt.remainingQty);


                // 剩余 持仓总金额  =  买入价格  x  剩余 持仓量
                double amount = b_original.getPrice().doubleValue() * b_original.getQuantity();
                b_original.setAmount(NumUtil.double2Decimal(amount));


                // 当前 持仓个股   ->   买单列表（剩余 数量/金额   ->   持仓 数量/金额）         各买单 价格不变
                todayHoldingList.add(b_original);
            }
        }
    }


    /**
     * 成本计算
     *
     * @param endTradeDate     当前交易日
     * @param todayHoldingList 当日持仓（买入记录）列表
     * @param quantityMap      个股持仓 -   总数量
     * @param avlQuantityMap   个股持仓 - 可用数量（T+1）
     * @param amountMap        个股持仓 -   总成本（买入价格 x 买入数量   ->   累加）
     * @param codeInfoMap      个股持仓 - 首次买入Info
     */
    private void quantityMap__avlQuantityMap__amountMap__codeInfoMap(LocalDate endTradeDate,
                                                                     List<BtTradeRecordDO> todayHoldingList,
                                                                     Map<String, Integer> quantityMap,
                                                                     Map<String, Integer> avlQuantityMap,
                                                                     Map<String, Double> amountMap,
                                                                     Map<String, PositionInfo> codeInfoMap) {


        // 剩余 买单列表（抵扣卖单后 -> 剩余持仓量）
        for (BtTradeRecordDO tr : todayHoldingList) {


            Long stockId = tr.getStockId();
            String stockCode = tr.getStockCode();
            String stockName = tr.getStockName();

            // B/S       ->       实际全为 买单
            Integer tradeType = tr.getTradeType();


            // 买入价格
            double price = tr.getPrice().doubleValue();
            // 抵扣卖单后 -> 剩余持仓量
            int quantity = tr.getQuantity();     // 存在只卖部分的情况：大盘仓位限制->等比减仓

            // double amount = tr.getAmount().doubleValue();
            // 剩余 持仓总金额 = 买入价格 x 剩余持仓量
            double amount = price * quantity;


            // 交易日期
            LocalDate tradeDate = tr.getTradeDate();


            // ---------------------------------------------------------------------------------------------------------


            // 买入累加 / 卖出累减   ->   总数量、总成本
            int sign = Objects.equals(BtTradeTypeEnum.BUY.getTradeType(), tradeType) ? +1 : -1;


            // 个股持仓 - 总数量
            quantityMap.merge(stockCode, sign * quantity, Integer::sum);

            // 个股持仓 - 总成本
            amountMap.merge(stockCode, sign * amount, Double::sum);


            // ---------------------------------------------------------------------------------------------------------


            // T+1（🐶💩共产主义特色）
            if (sign == 1 && tradeDate.isEqual(endTradeDate)) {
                // 今日买入  =>  明日才可卖（今日 不可用  ->  +0 ）
                avlQuantityMap.merge(stockCode, 0, Integer::sum);
            } else {
                // 今日可用   ->   正常累加
                avlQuantityMap.merge(stockCode, sign * quantity, Integer::sum);
            }


            // ---------------------------------------------------------------------------------------------------------


            PositionInfo positionInfo = codeInfoMap.get(stockCode);
            if (positionInfo == null) {

                positionInfo = new PositionInfo(stockId, stockCode, stockName, tradeDate, tr.getPrice());
                codeInfoMap.put(stockCode, positionInfo);

            } else {


                // 更新  ->  最近一次  首次买入日期（用于计算 持仓天数）     =>     最近一次  avlQuantity = 0
                if (avlQuantityMap.get(stockCode) == 0) {
                    // 最近一次
                    LocalDate buyDate = tradeDate.isBefore(positionInfo.buyDate) ? tradeDate : positionInfo.buyDate;
                    positionInfo.setBuyDate(buyDate);
                    positionInfo.setInitBuyPrice(tr.getPrice());
                }
            }


        }
    }

    /**
     * 构造 当日持仓 对象列表
     *
     * @param taskId                  当前任务ID
     * @param endTradeDate            当前交易日
     * @param quantityMap             个股持仓 -   总数量
     * @param avlQuantityMap          个股持仓 - 可用数量（T+1）
     * @param amountMap               个股持仓 -   总成本（买入价格 x 买入数量   ->   累加）
     * @param codeInfoMap             个股持仓 - 首次买入Info
     * @param todaySellAndNotClearMap 当日S但没一次性卖完（有且仅有一种情况：大盘仓位限制->等比减仓）
     * @return
     */
    private List<BtPositionRecordDO> todayPositionRecordList(Long taskId,
                                                             LocalDate endTradeDate,
                                                             Map<String, Integer> quantityMap,
                                                             Map<String, Integer> avlQuantityMap,
                                                             Map<String, Double> amountMap,
                                                             Map<String, PositionInfo> codeInfoMap,
                                                             Map<String, BtTradeRecordDO> todaySellAndNotClearMap) {


        List<BtPositionRecordDO> positionRecordDOList = Lists.newArrayList();


        quantityMap.forEach((stockCode, qty) -> {

            // 当日未持仓 或 已全部卖出
            if (qty <= 0) {
                return;
            }


            int avlQuantity = avlQuantityMap.getOrDefault(stockCode, 0);
            PositionInfo positionInfo = codeInfoMap.get(stockCode);


            // 总成本
            double totalCost = amountMap.getOrDefault(stockCode, 0.0);
            // 平均成本 = 总成本 / 持仓数量
            double avgCost = totalCost / qty;


            // ---------------------------------------------------------------------------------------------------------


            // 每次B/S   ->   成本/收益/收益率   ->   独立事件（边界）     ==>     否则，上次B/S 亏损  ->  合并计入  本次B/S   =>   亏损 -> 负数bug（总成本 负数 -> 平均成本 负数）     =>     市值 爆减bug
            if (avgCost < 0) {
                log.error("getDailyPositions - avgCost err     >>>     [{}] {} {} , totalCost : {} , qty : {} , avgCost : {}",
                          taskId, endTradeDate, stockCode, totalCost, qty, avgCost);
            }


            // ---------------------------------------------------------------------------------------------------------


            // 当日收盘价
            double closePrice = getClosePrice(stockCode, endTradeDate);

            if (closePrice <= 0) {
                log.warn("getDailyPositions - closePrice err     >>>     [{}] {} {} , closePrice : {}",
                         taskId, endTradeDate, stockCode, closePrice);
                return;
            }


            // -------------------------


            // 累计浮动盈亏 = （当日收盘价 - 平均成本）x 持仓数量
            double totalPnl = (closePrice - avgCost) * qty;

            // 累计浮动盈亏率（%）
            double pnlPct = totalPnl / totalCost * 100;


            // ---------------------------------------------------------------------------------------------------------
            //                                              计算当日浮动盈亏
            // ---------------------------------------------------------------------------------------------------------


            double todayPnl = 0;
            double todayPnlPct = 0;


            double closeTodayReturnPct = 0;

            double priceTotalReturnPct = 0;
            double priceMaxReturnPct = 0;
            double priceMaxDrawdownPct = 0;


            // 首次买入价格
            double initBuyPrice = positionInfo.initBuyPrice.doubleValue();


            // 昨日持仓数量、成本
            BtPositionRecordDO prevPos = x.get().prev__stockCode_positionDO_Map.get(stockCode);
            if (prevPos != null) {


                double prevAvgCostPrice = prevPos.getAvgCostPrice().doubleValue();
                double prevClosePrice = prevPos.getClosePrice().doubleValue();
                double prevQty = prevPos.getQuantity();

                double prevTotalCost = prevAvgCostPrice * prevQty;
                double prevMarketValue = prevClosePrice * prevQty;


                // -----------------------------------------------------------------------------------------------------


                // 当日S但没一次性卖完（有且仅有一种情况：大盘仓位限制->等比减仓）
                if (todaySellAndNotClearMap.containsKey(stockCode)) {
                    // 总成本
                    totalCost = prevTotalCost;
                    // 平均成本
                    avgCost = prevAvgCostPrice;
                }


                // -----------------------------------------------------------------------------------------------------


                // 昨日持仓部分的 当日浮动盈亏 = (今日收盘价 - 昨日收盘价) * 昨日持仓数量
                double pnlFromYesterday = (closePrice - prevClosePrice) * prevQty;


                // 今日新增买入部分的当日浮动盈亏 = (今日收盘价 - 今日买入价) * 今日买入数量
                // 由于所有交易都发生在收盘价，因此 今日买入价 = 今日收盘价，当日浮盈=0
                double pnlFromTodayBuy = 0;


                // 当日浮动盈亏总额
                todayPnl = pnlFromYesterday + pnlFromTodayBuy;


                // 当日浮动盈亏率 = 当日盈亏额 / 昨日持仓成本
                // ⚠️ 注意：分母必须是昨日的成本，而不是今日总成本，否则会稀释掉当日盈亏
                // todayPnlPct = (prevTotalCost > 0) ? (todayPnl * 100 / prevTotalCost) : 0;


                // 当日浮动盈亏率 = 当日盈亏额 / 总成本
                // ⚠️ 注意：分母必须是今日的总成本，今日新买入  ->  会等比例 稀释掉当日盈亏
                // todayPnlPct = (totalCost > 0) ? (todayPnl / totalCost * 100) : 0;


                // 当日浮动盈亏率 = (当日持仓市值 - 昨日持仓市值) / 昨日持仓市值 × 100%
                // 当日浮动盈亏率 = 当日浮动盈亏 / 昨日持仓市值 × 100%
                todayPnlPct = todayPnl / prevMarketValue * 100;


                // TODO   此处不是bug（不再深究）    ->     对收益无任何影响，仅是 持仓过程中     =>     加仓/减仓 -> 成本 骤降/升   ->   收益率 几何倍“bug”


                if (todayPnlPct > 31 || todayPnlPct < -31) {

                    // todayPnl : -3791.3999999999664 , totalCost : 107.35 , todayPnlPct : -3531.811830461078 ,


                    // fix版本 :
                    // todayPnl : -3791.3999999999664 , totalCost : 286624.5 , todayPnlPct : -1.32277597 ,


                    // pre       2670
                    // today     2669（等比减仓）    ->     剩余1股
                    //
                    //
                    // pre       "closePrice":"107.350"       "avgCostPrice":"107.350"          "initBuyPrice":"107.350"
                    // today     "closePrice":"105.930"


                    // -------------------------------------------------------------------------------------------------


                    // [ERROR] 2025-09-04 08:16:27.539 [http-nio-7001-exec-9] BacktestStrategy -
                    //
                    // todayPositionRecordList - err     >>>     taskId : 14417 , tradeDate : 2022-07-15 , stockCode : 300073   ,
                    //
                    // todayPnlPct : -3531.811830461078 , todayPnl : -3791.3999999999664 ,totalCost : 107.35 ,
                    //
                    // prevPos : {"avgCostPrice":"107.3500","avlQuantity":"0","blockCodePath":"880987-880440","blockNamePath":"TDX 制造-工业机械","buyDate":"2022-07-14 00:00:00","buyPrice":"107.350","capTodayPnl":"0.0000","capTodayPnlPct":"0.0000","capTotalPnl":"0.0000","capTotalPnlPct":"0.0000","changePct":"0.0000","closePrice":"107.3500","holdingDays":"0","id":"1170275351754964992","marketValue":"286624.5000","positionPct":"9.99705300","positionType":"1","priceMaxDrawdownPct":"0.0000","priceMaxReturnPct":"0.0000","priceTotalReturnPct":"0.0000","quantity":"2670","stockCode":"300073","stockId":"1556","stockName":"当升科技","taskId":"14417","tradeDate":"2022-07-14 00:00:00"} ,
                    //
                    // todayTr : {"amount":"282727.17","blockCodePath":"880987-880440","blockNamePath":"TDX 制造-工业机械","fee":"0.00","gmtCreate":"2025-09-04 08:16:27","gmtModify":"2025-09-04 08:16:27","id":"1170275355315929089","positionPct":"9.70","price":"105.930","quantity":"2669","stockCode":"300073","stockId":"1556","stockName":"当升科技","taskId":"14417","tradeDate":"2022-07-15 00:00:00","tradeSignalDesc":"大盘仓位限制->等比减仓","tradeSignalType":"21","tradeType":"2"}


                    log.error("todayPositionRecordList - err     >>>     taskId : {} , tradeDate : {} , stockCode : {}   ,   todayPnlPct : {} , todayPnl : {} ,totalCost : {} , prevPos : {} , todayTr : {}",
                              taskId, endTradeDate, stockCode, todayPnlPct, todayPnl, totalCost, JSON.toJSONString(prevPos), JSON.toJSONString(todaySellAndNotClearMap.get(stockCode)));

                    // TODO   发现有 S后 剩余1股 bug   ->   已 fix（大盘仓位限制->等比减仓   bug）
                    todayPnlPct = Math.min(todayPnlPct, 9999.99);
                    todayPnlPct = Math.max(todayPnlPct, -9999.99);
                }


                // -----------------------------------------------------------------------------------------------------


                // 当日涨跌幅（%）
                closeTodayReturnPct = (closePrice / prevClosePrice - 1) * 100;


                // 首次买入价格-累计涨幅（%） =  当日收盘价 / initBuyPrice  - 1
                priceTotalReturnPct = (closePrice / initBuyPrice - 1) * 100;


                // 昨日-最大涨幅
                priceMaxReturnPct = prevPos.getPriceMaxReturnPct().doubleValue();
                // 昨日-最大回撤
                priceMaxDrawdownPct = prevPos.getPriceMaxDrawdownPct().doubleValue();


                // 首次买入价格-最大涨幅（%）
                if (priceMaxReturnPct < priceTotalReturnPct) {
                    priceMaxReturnPct = priceTotalReturnPct;
                    // maxDate = tradeDate;
                }


                // 当日回撤（负数）  =  （当日）累计净值 / 最大净值
                double drawdownPct = ((1 + priceTotalReturnPct * 0.01) / (1 + priceMaxReturnPct * 0.01) - 1) * 100;


                // 首次买入价格-最大回撤（%）
                if (priceMaxDrawdownPct > drawdownPct) {
                    priceMaxDrawdownPct = drawdownPct;
                    // minDate = tradeDate;
                    // minNav = nav;
                }


            }

            // 昨日无持仓   ->   今日收盘价 首次买入
            else {
                // 所有 收益/收益率   ->   全为0
            }


            // ---------------------------------------------------------------------------------------------------------


            BtPositionRecordDO positionRecordDO = new BtPositionRecordDO();

            positionRecordDO.setTaskId(taskId);
            positionRecordDO.setTradeDate(endTradeDate);
            positionRecordDO.setStockId(positionInfo.stockId);
            positionRecordDO.setStockCode(stockCode);
            positionRecordDO.setStockName(positionInfo.stockName);
            positionRecordDO.setAvgCostPrice(of(avgCost));
            positionRecordDO.setClosePrice(of(closePrice));
            // 持仓数量
            positionRecordDO.setQuantity(qty);
            positionRecordDO.setAvlQuantity(avlQuantity);


            // 当前市值 = 持仓数量 x 当前收盘价
            positionRecordDO.setMarketValue(of(qty * closePrice));

            // 仓位占比 = 持仓市值 / 总资金
            BigDecimal positionPct = positionRecordDO.getMarketValue().divide(of(x.get().capital), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            positionRecordDO.setPositionPct(of(Math.min(positionPct.doubleValue(), 9999.99)));


            // 当日盈亏额
            positionRecordDO.setCapTodayPnl(of(todayPnl));
            // 当日盈亏率（%）
            positionRecordDO.setCapTodayPnlPct(of(todayPnlPct));

            // 累计盈亏额
            positionRecordDO.setCapTotalPnl(of(totalPnl));
            // 累计盈亏率（%） = 盈亏额 / 总成本  x 100%
            positionRecordDO.setCapTotalPnlPct(of(pnlPct));


            // 当日涨跌幅（%）
            positionRecordDO.setChangePct(of(closeTodayReturnPct));

            // 首次买入价格-累计涨幅（%）
            positionRecordDO.setPriceTotalReturnPct(of(priceTotalReturnPct));
            // 首次买入价格-最大涨幅（%）
            positionRecordDO.setPriceMaxReturnPct(of(priceMaxReturnPct));
            // 首次买入价格-最大回撤（%）
            positionRecordDO.setPriceMaxDrawdownPct(of(priceMaxDrawdownPct));


            positionRecordDO.setBuyDate(positionInfo.buyDate);
            positionRecordDO.setHoldingDays(positionInfo.getHoldingDays(endTradeDate, data.dateIndexMap));
            positionRecordDO.setBuyPrice(positionInfo.initBuyPrice);


            // ---------------------------------------------------------------------------------------------------------


            if (null == positionRecordDO.getPriceMaxDrawdownPct()) {
                log.error("todayPositionRecordList - getPriceMaxDrawdownPct err     >>>     taskId : {} , tradeDate : {} , stockCode : {} , positionRecordDO : {}",
                          taskId, endTradeDate, stockCode, JSON.toJSONString(positionRecordDO));
            }


            // 持仓状态：1-持仓中；2-已清仓；
            positionRecordDO.setPositionType(1);
            positionRecordDOList.add(positionRecordDO);
        });


        return positionRecordDOList;
    }


    /**
     * 构造 当日清仓 对象列表
     *
     * @param taskId        当前任务ID
     * @param endTradeDate  当前交易日
     * @param todayClearMap 当日清仓列表             ->   清仓stockCode - 清仓（卖出记录）
     * @return
     */
    private List<BtPositionRecordDO> todayClearPositionRecordList(Long taskId,
                                                                  LocalDate endTradeDate,
                                                                  Map<String, BtTradeRecordDO> todayClearMap) {


        List<BtPositionRecordDO> clearPositionRecordDOList = Lists.newArrayList();


        todayClearMap.forEach((stockCode, tr) -> {


            // 当日收盘价
            double closePrice = getClosePrice(stockCode, endTradeDate);


            // ---------------------------------------------------------------------------------------------------------
            //                                              计算当日浮动盈亏
            // ---------------------------------------------------------------------------------------------------------


            double todayPnl = 0;
            double todayPnlPct = 0;


            double closeTodayReturnPct = 0;

            double priceTotalReturnPct = 0;
            double priceMaxReturnPct = 0;
            double priceMaxDrawdownPct = 0;


            // 昨日持仓 数量、成本
            BtPositionRecordDO prevPos = x.get().prev__stockCode_positionDO_Map.get(stockCode);

            // 昨日必须 有持仓 -> 今日才能S
            if (prevPos == null) {
                return;
            }


            // 首次买入价格
            double initBuyPrice = prevPos.getBuyPrice().doubleValue();


            double prevAvgCostPrice = prevPos.getAvgCostPrice().doubleValue();
            double prevClosePrice = prevPos.getClosePrice().doubleValue();
            int prevQty = prevPos.getQuantity();
            double prevMarketValue = prevClosePrice * prevQty;
            double prevTotalCost = prevAvgCostPrice * prevQty;


            // ---------------------------------------------------------------------------------------------------------


            // 总成本
            double totalCost = prevTotalCost;
            // 平均成本 = 总成本 / 持仓数量
            double avgCost = prevAvgCostPrice;


            // ---------------------------------------------------------------------------------------------------------


            // 今日卖出 -> 不用特殊处理，因为系统约定“卖出 = 全部清仓”，因此 qty 已经代表当日最终持仓


            // 昨日持仓部分的 当日浮动盈亏 = (今日收盘价 - 昨日收盘价) * 昨日持仓数量
            double pnlFromYesterday = (closePrice - prevClosePrice) * prevQty;


            // 今日新增买入部分的当日浮动盈亏 = (今日收盘价 - 今日买入价) * 今日买入数量
            // 由于所有交易都发生在收盘价，因此 今日买入价 = 今日收盘价，当日浮盈=0
            double pnlFromTodayBuy = 0;


            // 当日浮动盈亏总额
            todayPnl = pnlFromYesterday + pnlFromTodayBuy;


            // 当日浮动盈亏率 = 当日盈亏额 / 昨日持仓成本
            // ⚠️ 注意：分母必须是昨日的成本，而不是今日总成本，否则会稀释掉当日盈亏
            // todayPnlPct = (prevTotalCost > 0) ? (todayPnl * 100 / prevTotalCost) : 0;


            // 当日浮动盈亏率 = 当日盈亏额 / 总成本
            // ⚠️ 注意：分母必须是今日的总成本，今日新买入  ->  会等比例 稀释掉当日盈亏


            // 新买入 -> 累计总成本


            // 当日浮动盈亏率 = (当日持仓市值 - 昨日持仓市值) / 昨日持仓市值 × 100%
            // 当日浮动盈亏率 = 当日浮动盈亏 / 昨日持仓市值 × 100%
            todayPnlPct = todayPnl / prevMarketValue * 100;


            // TODO   此处不是bug（不再深究）    ->     对收益无任何影响，仅是 持仓过程中     =>     加仓/减仓 -> 成本 骤降/升   ->   收益率 几何倍“bug”


            if (todayPnlPct > 31 || todayPnlPct < -31) {


                // TODO     个股 价格数据 异常       ->       北交所（新三板 bug     ->     券商软件 沿用该个股 新三板时期 K线历史数据）


                // stockCode : 832175（上市日期：2023-06-30）
                //
                // preClose  （2021-09-02）    ->     "closePrice":"3.790"
                // todayClose（2021-09-03）    ->     "closePrice":"4.890"               4.890 / 3.790 = 129%


                // -----------------------------------------------------------------------------------------------------


                // [ERROR] 2025-09-04 08:11:04.528 [http-nio-7001-exec-9] BacktestStrategy - todayClearPositionRecordList - err     >>>
                //
                // taskId : 14417 , tradeDate : 2021-09-03 , stockCode : 832175   ,   todayPnlPct : 30.21978021978021 , todayPnl : 4524.299999999998 ,totalCost : 14971.32 ,
                //
                // prevPos : {"avgCostPrice":"3.6400","avlQuantity":"4113","blockCodePath":"880987-880446","blockNamePath":"TDX 制造-电气设备","buyDate":"2021-08-26 00:00:00","buyPrice":"3.460","capTodayPnl":"740.3400","capTodayPnlPct":"4.9440","capTotalPnl":"615.0600","capTotalPnlPct":"4.1080","changePct":"4.9860","closePrice":"3.7900","holdingDays":"5","id":"1170273993840656390","marketValue":"15588.2700","positionPct":"0.68842500","positionType":"1","priceMaxDrawdownPct":"-6.2340","priceMaxReturnPct":"11.2720","priceTotalReturnPct":"9.5380","quantity":"4113","stockCode":"832175","stockId":"5213","stockName":"东方碳素","taskId":"14417","tradeDate":"2021-09-02 00:00:00"} ,
                // preClose     ->     "closePrice":"3.7900"
                // avgCostPrice ->   "avgCostPrice":"3.6400"
                //
                //
                // todayTr : tr
                // todayTr : price : "4.890" , qty : "4113" , amount : "20112.57"
                // todayTr : 1170274000425713667	14417	2021-09-03	2	3	高位爆量上影大阴	5213	832175	东方碳素	4.890	4113	20112.57	0.91	0.00	2025-09-04 08:11:04	2025-09-04 08:11:04

                //
                // todayPos :
                // 1170274003214925828	14417	2021-09-03	2	5213	832175	东方碳素	3.640	4.890	29.02	4113	0	20112.57	0.91	4524.30	30.22	5141.25	34.34	41.33	41.33	-6.23	2021-08-26	3.460	6	2025-09-04 08:11:06	2025-09-04 08:11:06


                log.error("todayClearPositionRecordList - err     >>>     taskId : {} , tradeDate : {} , stockCode : {}   ,   todayPnlPct : {} , todayPnl : {} ,totalCost : {} , prevPos : {} , todayTr : {}",
                          taskId, endTradeDate, stockCode, todayPnlPct, todayPnl, totalCost, JSON.toJSONString(prevPos), JSON.toJSONString(tr));

                // TODO   发现有 S后 剩余1股 bug   ->   已 fix（大盘仓位限制->等比减仓   bug）
                todayPnlPct = Math.min(todayPnlPct, 9999.99);
                todayPnlPct = Math.max(todayPnlPct, -9999.99);
            }


            // ---------------------------------------------------------------------------------------------------------


            // 当日涨跌幅（%）
            closeTodayReturnPct = (closePrice / prevClosePrice - 1) * 100;


            // 首次买入价格-累计涨幅（%） =  当日收盘价 / initBuyPrice  - 1
            priceTotalReturnPct = (closePrice / initBuyPrice - 1) * 100;


            // 昨日-最大涨幅
            priceMaxReturnPct = prevPos.getPriceMaxReturnPct().doubleValue();
            // 昨日-最大回撤
            priceMaxDrawdownPct = prevPos.getPriceMaxDrawdownPct().doubleValue();


            // 首次买入价格-最大涨幅（%）
            if (priceMaxReturnPct < priceTotalReturnPct) {
                priceMaxReturnPct = priceTotalReturnPct;
                // maxDate = tradeDate;
            }


            // 当日回撤（负数）  =  （当日）累计净值 / 最大净值
            double drawdownPct = ((1 + priceTotalReturnPct * 0.01) / (1 + priceMaxReturnPct * 0.01) - 1) * 100;


            // 首次买入价格-最大回撤（%）
            if (priceMaxDrawdownPct > drawdownPct) {
                priceMaxDrawdownPct = drawdownPct;
                // minDate = tradeDate;
                // minNav = nav;
            }


            // ---------------------------------------------------------------------------------------------------------


            // 累计浮动盈亏 = （当日收盘价 - 平均成本）x 持仓数量
            double totalPnl = (closePrice - avgCost) * prevQty;

            // 累计浮动盈亏率（%）
            double pnlPct = totalPnl / totalCost * 100;


            // ---------------------------------------------------------------------------------------------------------


            BtPositionRecordDO positionRecordDO = new BtPositionRecordDO();

            positionRecordDO.setTaskId(taskId);
            positionRecordDO.setTradeDate(endTradeDate);
            positionRecordDO.setStockId(prevPos.getStockId());
            positionRecordDO.setStockCode(stockCode);
            positionRecordDO.setStockName(prevPos.getStockName());

            positionRecordDO.setAvgCostPrice(of(avgCost));
            positionRecordDO.setClosePrice(of(closePrice));
            // 清仓数量
            positionRecordDO.setQuantity(tr.getQuantity());
            positionRecordDO.setAvlQuantity(0);


            // 当前市值 = 清仓数量 x 当前收盘价
            positionRecordDO.setMarketValue(of(tr.getQuantity() * closePrice));

            // 仓位占比 = 清仓市值 / 总资金
            BigDecimal positionPct = positionRecordDO.getMarketValue().divide(of(x.get().capital), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            positionRecordDO.setPositionPct(of(Math.min(positionPct.doubleValue(), 9999.99)));


            // 当日盈亏额
            positionRecordDO.setCapTodayPnl(of(todayPnl));
            // 当日盈亏率（%）
            positionRecordDO.setCapTodayPnlPct(of(todayPnlPct));

            // 累计盈亏额
            positionRecordDO.setCapTotalPnl(of(totalPnl));
            // 累计盈亏率（%） = 盈亏额 / 总成本  x 100%
            positionRecordDO.setCapTotalPnlPct(of(pnlPct));


            // 当日涨跌幅（%）
            positionRecordDO.setChangePct(of(closeTodayReturnPct));

            // 首次买入价格-累计涨幅（%）
            positionRecordDO.setPriceTotalReturnPct(of(priceTotalReturnPct));
            // 首次买入价格-最大涨幅（%）
            positionRecordDO.setPriceMaxReturnPct(of(priceMaxReturnPct));
            // 首次买入价格-最大回撤（%）
            positionRecordDO.setPriceMaxDrawdownPct(of(priceMaxDrawdownPct));


            positionRecordDO.setBuyDate(prevPos.getBuyDate());
            positionRecordDO.setHoldingDays(prevPos.getHoldingDays() + 1);
            positionRecordDO.setBuyPrice(prevPos.getBuyPrice());


            // ---------------------------------------------------------------------------------------------------------


            if (null == positionRecordDO.getPriceMaxDrawdownPct()) {
                log.error("todayClearPositionRecordList - getPriceMaxDrawdownPct err     >>>     taskId : {} , tradeDate : {} , stockCode : {} , positionRecordDO : {}",
                          taskId, endTradeDate, stockCode, JSON.toJSONString(positionRecordDO));
            }


            // 持仓状态：1-持仓中；2-已清仓；
            positionRecordDO.setPositionType(2);
            clearPositionRecordDOList.add(positionRecordDO);
        });


        // -------------------------------------------------------------------------------------------------------------


        // 当日清仓 列表
        // x.get().clearPositionRecordDOList = clearPositionRecordDOList;


        // -------------------------------------------------------------------------------------------------------------


        return clearPositionRecordDOList;
    }


    /**
     * 个股   指定日期 -> 收盘价
     *
     * @param stockCode
     * @param tradeDate
     * @return
     */
    private double getClosePrice(String stockCode, LocalDate tradeDate) {
        if (data.stock__dateCloseMap.get(stockCode) == null) {
            return 0.0;
        }


        Double closePrice = data.stock__dateCloseMap.get(stockCode).get(tradeDate);


        // 停牌（603039 -> 2023-04-03）
        int count = 0;
        while (closePrice == null && count++ < 500) {
            // 交易日 往前一位
            tradeDate = tradeDateDecr(tradeDate);
            closePrice = data.stock__dateCloseMap.get(stockCode).get(tradeDate);
        }


        return closePrice == null ? 0.0 : closePrice;
    }


    @Synchronized
    private void initData(LocalDate startDate, LocalDate endDate) {
        log.info("--------------------------- " + Thread.currentThread().getName() + "线程 竞争到了🔐     >>>     🧑‍💻🏇");


        // 重新初始化   统计数据
        // x = new Stat();
        x.set(new Stat());


        tradeRecord___idSet__cache.set(Sets.newHashSet());
        tradeRecordList__cache.set(Lists.newArrayList());


        log.info("--------------------------- data.stockDOList - before     >>>     size : {} , 线程 : {}",
                 ListUtil.size(data.stockDOList), Thread.currentThread().getName());


        // 全量行情
        data = initDataService.initData(startDate, endDate, false);
        // initDataService.initData(startDate, endDate, false);   // 等价 data = 全局Cache;       data -本身就已全局指向-> 全局Cache


        log.info("--------------------------- data.stockDOList - after      >>>     size : {} , 线程 : {}",
                 ListUtil.size(data.stockDOList), Thread.currentThread().getName());
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    public static class Backup {
        Stat x = new Stat();
        Set<Long> tradeRecord___idSet__cache;
        List<BtTradeRecordDO> tradeRecordList__cache;
    }


    /**
     * 备份
     */
    private Backup backupThreadLocal() {
        Backup backup = new Backup();

        // 深拷贝
        backup.x = DeepCopyUtil.deepCopy(x.get());
        backup.tradeRecord___idSet__cache = Sets.newHashSet(tradeRecord___idSet__cache.get());
        backup.tradeRecordList__cache = Lists.newArrayList(tradeRecordList__cache.get());

        return backup;
    }

    /**
     * 恢复
     */
    private void restoreThreadLocal(Backup backup) {
        x.set(backup.x);
        tradeRecord___idSet__cache.set(backup.tradeRecord___idSet__cache);
        tradeRecordList__cache.set(backup.tradeRecordList__cache);
    }


    /**
     * 清理
     */
    private void clearThreadLocal() {
        x.remove();
        tradeRecord___idSet__cache.remove();
        tradeRecordList__cache.remove();


        btCompareDTO.remove();
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * update   ->   恢复
     *
     * @param taskId
     * @param tradeDate
     * @param last_dailyReturnDO
     */
    private void restoreThreadLocal__update(Long taskId, LocalDate tradeDate, BtDailyReturnDO last_dailyReturnDO) {


        // ------------------------------------ x


        List<BtPositionRecordDO> positionRecordDOList = btPositionRecordService.listByTaskIdAndTradeDateAndPosType(taskId, tradeDate, 1);
        Map<String, BtPositionRecordDO> stockCode_positionDO_Map = positionRecordDOList.stream()
                                                                                       .collect(Collectors.toMap(
                                                                                               BtPositionRecordDO::getStockCode,
                                                                                               Function.identity()
                                                                                       ));


        // ---------------------------


        // 1、清空
        x.remove();


        // 2、today -> pre
        x.get().prevCapital = last_dailyReturnDO.getCapital().doubleValue();
        x.get().prevAvlCapital = last_dailyReturnDO.getAvlCapital().doubleValue();
        x.get().prev__stockCode_positionDO_Map = stockCode_positionDO_Map;


        x.get().taskId = taskId;


        // ------------------------------------ tradeRecord


        List<BtTradeRecordDO> tradeRecordDOList = btTradeRecordService.listByTaskId(taskId);
        Set<Long> tradeRecord___idSet = tradeRecordDOList.stream().map(BtTradeRecordDO::getId).collect(Collectors.toSet());


        tradeRecord___idSet__cache.set(tradeRecord___idSet);
        tradeRecordList__cache.set(tradeRecordDOList);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class CalcStat {


        // ----------------------------------------- 不变


        // 当前 仓位限制（不变）
        double positionLimitRate;       // 仓位百分比 上限     =>     大盘量化 -> 计算
        double positionLimitAmount;     // 仓位总金额 上限     =      总资金  x  仓位百分比 上限


        // 当前 总资金（B/S 任意变换   ->   capital 不变）   =   持仓总市值（跟随BS 变动） +  可用资金（跟随BS 变动）
        double capital;


        // ------------------------------------------------------------


        // ----------------------------------------- 可变


        // 当前 持仓列表
        List<BtPositionRecordDO> positionRecordDOList;
        List<String> positionStockCodeList;
        Map<String, BtPositionRecordDO> stockCode_positionDO_Map = Maps.newHashMap();


        // 当前 持仓总市值   <=   仓位限制
        double marketValue;


        // 当前 可用资金   =   总资金 - 持仓总市值
        double avlCapital;

        // 当前 实际可用资金（大盘 -> 仓位限制）  =   仓位总金额 上限   -   持仓总市值
        double actAvlCapital;


        // ----------------------------------------- 可变


        // 今日 B/S记录
        List<BtTradeRecordDO> tradeRecordDOList;

        // 卖出总金额
        double sellCapital;
        // 买入总金额
        double buyCapital;


        // ------------------------------------------------------------


        public CalcStat(List<BtPositionRecordDO> positionRecordDOList, List<BtTradeRecordDO> tradeRecordDOList) {


            // ------------------------------------------ 不变（已计算）


            this.positionLimitRate = x.get().positionLimitRate;
            this.positionLimitAmount = x.get().positionLimitAmount;
            this.capital = x.get().capital;


            // ------------------------------------------ 可变（B/S记录 -> 实时计算）


            // 今日 B/S记录
            this.tradeRecordDOList = tradeRecordDOList;

            this.sellCapital = getSellCapital();
            this.buyCapital = getBuyCapital();


            // ------------------------------------------ 可变（持仓列表 -> 实时计算）


            // 当前 持仓列表
            this.positionRecordDOList = positionRecordDOList;
            this.positionStockCodeList = getPositionStockCodeList();
            this.stockCode_positionDO_Map = getStockCode_positionDO_Map();


            this.marketValue = getMarketValue();
            this.avlCapital = getAvlCapital();
            this.actAvlCapital = getActAvlCapital();


            // ------------------------------------------


            // check
            checkStatData();
        }


        private void checkStatData() {

            // 总资金  =  总市值 + 可用资金
            double capital_2 = marketValue + avlCapital;
            if (!TdxFunCheck.equals(capital, capital_2)) {
                log.warn("check err     >>>     [{}] [{}] , capital : {} , capital_2 : {}",
                         x.get().taskId, x.get().tradeDate, capital, capital_2);
            }

            // 可用资金  =  prev_可用资金 + 卖出 - 买入
            double avlCapital_2 = x.get().prevAvlCapital + sellCapital - buyCapital;
            if (tradeRecordDOList != null && !TdxFunCheck.equals(avlCapital, avlCapital_2, x.get().capital * 0.001 /*1000*/, 0.01)) {
                log.warn("check err     >>>     [{}] [{}] , avlCapital : {} , avlCapital_2 : {}",
                         x.get().taskId, x.get().tradeDate, avlCapital, avlCapital_2);
            }
        }


        // ------------------------------------------------------------


        // -------------------------- 持仓


        public List<String> getPositionStockCodeList() {
            // 持仓 code列表
            this.positionStockCodeList = positionRecordDOList.stream().map(BtPositionRecordDO::getStockCode).collect(Collectors.toList());
            return this.positionStockCodeList;
        }


        public Map<String, BtPositionRecordDO> getStockCode_positionDO_Map() {
            positionRecordDOList.forEach(e -> stockCode_positionDO_Map.put(e.getStockCode(), e));
            return stockCode_positionDO_Map;
        }

        public double getMarketValue() {
            if (CollectionUtils.isEmpty(positionRecordDOList)) {
                return 0;
            }

            return positionRecordDOList.stream()
                                       .map(BtPositionRecordDO::getMarketValue)
                                       .reduce(BigDecimal.ZERO, BigDecimal::add)
                                       .doubleValue();
        }

        public double getAvlCapital() {
            double avlCapital_1 = capital - marketValue;
            double avlCapital_2 = x.get().prevAvlCapital + sellCapital - buyCapital;

            // 前置init阶段 -> 不校验 （capital -> 还未计算）
            if (tradeRecordDOList != null && !TdxFunCheck.equals(avlCapital_1, avlCapital_2, 1000, 0.01)) {
                log.warn("getAvlCapital err     >>>     [{}] [{}] , {} , {}", x.get().taskId, x.get().tradeDate, avlCapital_1, avlCapital_2);
            }

            return avlCapital_1;
        }

        public double getActAvlCapital() {
            return actAvlCapital = positionLimitAmount - marketValue;
        }


        // -------------------------- B/S


        public double getSellCapital() {
            if (CollectionUtils.isEmpty(tradeRecordDOList)) {
                return 0;
            }

            return tradeRecordDOList.stream()
                                    .filter(e -> e.getTradeType().equals(BtTradeTypeEnum.SELL.getTradeType()))
                                    .map(BtTradeRecordDO::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .doubleValue();
        }

        public double getBuyCapital() {
            if (CollectionUtils.isEmpty(tradeRecordDOList)) {
                return 0;
            }

            return tradeRecordDOList.stream()
                                    .filter(e -> e.getTradeType().equals(BtTradeTypeEnum.BUY.getTradeType()))
                                    .map(BtTradeRecordDO::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .doubleValue();
        }
    }


    /**
     * 统计数据
     */
    @Data
    public static class Stat implements Serializable {


        // -------------------- 每日


        // 总资金
        double prevCapital;
        // 可用资金
        double prevAvlCapital;

        // 持仓列表
        // List<BtPositionRecordDO> prevPositionRecordDOList;
        Map<String, BtPositionRecordDO> prev__stockCode_positionDO_Map = Maps.newHashMap();


        // ----------------------------------------------------------------------------------


        // taskId
        Long taskId;

        // 当前 交易日
        LocalDate tradeDate;


        // ----------------------------------------------------------------------------------


        // S前（昨日持仓） -> S -> S后（减仓前） -> 减仓 -> 减仓后（B前） -> B -> B后


        // ----------------------------------------- 不变


        // 当前 仓位限制（不变）
        double positionLimitRate;       // 仓位百分比 上限     =>     大盘量化 -> 计算
        double positionLimitAmount;     // 仓位总金额 上限     =      总资金  x  仓位百分比 上限


        // 当前 总资金（B/S 任意变换   ->   capital 不变）   =   持仓总市值（跟随BS 变动） +  可用资金（跟随BS 变动）
        double capital;


        // ----------------------------------------- 可变


        // 持仓列表
        List<BtPositionRecordDO> positionRecordDOList;
        List<String> positionStockCodeList;
        Map<String, BtPositionRecordDO> stockCode_positionDO_Map = Maps.newHashMap();


        // 清仓列表
        List<BtPositionRecordDO> clearPositionRecordDOList;


        // --------------------


        // 当前 持仓总市值   <=   仓位限制
        double marketValue;


        // 当前 可用资金   =   总资金 - 持仓总市值
        double avlCapital;

        // 当前 实际可用资金（大盘 -> 仓位限制）  =   仓位总金额 上限   -   持仓总市值
        double actAvlCapital;


        // ----------------------------------------- 可变


        // -------------------- B/S策略

        double sellCapital;
        double buyCapital;
    }


    @Data
    @AllArgsConstructor
    public static class PositionInfo {
        private Long stockId;
        private String stockCode;
        private String stockName;
        private LocalDate buyDate;
        private BigDecimal initBuyPrice;
        // private Integer holdingDays;

        public Integer getHoldingDays(LocalDate endTradeDate, Map<LocalDate, Integer> dateIndexMap) {
            // 持仓天数   ->   间隔  N个交易日
            return between(buyDate, endTradeDate, dateIndexMap);
        }
    }


    /**
     * 两个交易日   间隔天数(交易日)
     *
     * @param start
     * @param end
     * @param dateIndexMap 交易日-idx
     * @return
     */
    public static int between(LocalDate start, LocalDate end, Map<LocalDate, Integer> dateIndexMap) {
        Assert.isTrue(!start.isAfter(end), String.format("start[%s]不能大于end[%s]", start, end));


        Integer idx1 = dateIndexMap.get(start);
        Integer idx2 = dateIndexMap.get(end);

        Assert.notNull(idx1, String.format("start[%s]非交易日", start));
        Assert.notNull(idx2, String.format("end[%s]非交易日", end));

        return idx2 - idx1;
    }


    private static BigDecimal of(Number val) {
        return NumUtil.num2Decimal(val, 4);
    }


    /**
     * 辅助类：包装一条买入记录   及其 剩余可抵销数量
     **/
    @Data
    @AllArgsConstructor
    private static class BuyTradeRecord {
        // 买入记录（原始买单数据）
        final BtTradeRecordDO b_original;
        // b_original  ->  剩余可抵销数量
        int remainingQty;
    }


    public static class DrawdownResult {

        // 波峰
        public LocalDate peakDate;
        public BigDecimal peakNav;

        // 波谷
        public LocalDate troughDate;
        public BigDecimal troughNav;

        // 最大跌幅（负数）
        public BigDecimal drawdownPct;


        // -------------------------

        // 盈利天数
        public int profitDayCount;


        // -------------------------


        // 每日收益率 列表
        List<BigDecimal> dailyReturnList = Lists.newArrayList();
    }


}