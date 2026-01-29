package com.bebopze.tdx.quant.strategy.backtest;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.BtTradeTypeEnum;
import com.bebopze.tdx.quant.common.constant.SellStrategyEnum;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestOpenBSDTO;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestCompareDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.*;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.parser.check.TdxFunCheck;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.impl.InitDataServiceImpl;
import com.bebopze.tdx.quant.strategy.buy.BuyStrategyFactory;
import com.bebopze.tdx.quant.strategy.buy.ScoreSort;
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

import static com.bebopze.tdx.quant.common.constant.BuyStrategyEnum.涨停_SSF多_月多;


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
    public static final ThreadLocal<Stat> x = ThreadLocal.withInitial(Stat::new);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * bt_trade_record   -   Cache
     */
    private static final ThreadLocal<Set<Long>> tradeRecord___idSet__cache = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<List<BtTradeRecordDO>> tradeRecordList__cache = ThreadLocal.withInitial(ArrayList::new);


    /**
     * 前一日 日收益率   -   Cache（check、fail-fast）
     */
    private static final ThreadLocal<BtDailyReturnDO> prev_dailyReturnDO__cache = new ThreadLocal<>(); // 首日为null


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 可变参数
     */
    public static final ThreadLocal<BacktestCompareDTO> btCompareDTO = ThreadLocal.withInitial(BacktestCompareDTO::new);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 开盘BS
     */
    public static final ThreadLocal<BacktestOpenBSDTO> btOpenBSDTO = ThreadLocal.withInitial(BacktestOpenBSDTO::new);


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
    private SellStrategyFactory sellStrategyFactory;


    @Autowired
    private MarketService marketService;


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    public Long backtest(Integer batchNo,
                         TopBlockStrategyEnum topBlockStrategyEnum,
                         Set<String> buyConSet, Set<String> sellConSet,
                         LocalDate startDate, LocalDate endDate,
                         BacktestCompareDTO btCompareDTO) {


        BacktestCompareDTO copy_btCompareDTO = new BacktestCompareDTO();
        BeanUtils.copyProperties(btCompareDTO, copy_btCompareDTO);
        copy_btCompareDTO.setBatchNo(batchNo);
        copy_btCompareDTO.setTopBlockStrategyEnum(topBlockStrategyEnum);
        copy_btCompareDTO.setBuyConSet(buyConSet);
        copy_btCompareDTO.setSellConSet(sellConSet);
        copy_btCompareDTO.setStartDate(startDate);
        copy_btCompareDTO.setEndDate(endDate);


        // 回测-对照组 可变参数
        BacktestStrategy.btCompareDTO.set(copy_btCompareDTO);


        try {
            return execBacktest(batchNo, topBlockStrategyEnum, buyConSet, sellConSet, startDate, endDate);
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
                              Set<String> buyConSet,
                              Set<String> sellConSet,
                              LocalDate startDate,
                              LocalDate endDate) {


        log.info("execBacktest start     >>>     batchNo : {} , topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , startDate : {} , endDate : {} , btCompareDTO : {}",
                 batchNo, topBlockStrategyEnum, buyConSet, sellConSet, startDate, endDate, JSON.toJSONString(btCompareDTO.get()));


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


        BtTaskDO taskDO = createBacktestTask(batchNo, topBlockStrategyEnum, buyConSet, sellConSet, startDate, endDate);


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


            // ------------------ fail-fast（BS策略 快速淘汰）
            if (failFast(taskDO, tradeDate)) {
                taskDO.setFailFastFlag(1);
                taskDO.setEndDate(tradeDate);
                break;
            }


            tradeDate = tradeDateIncr(tradeDate);
//            // 备份
//            Backup backup = backupThreadLocal();


//            // 数据初始化   ->   加载 全量行情数据
//            // TODO   优化：一次只加载 3年数据
//            if (DateTimeUtil.diff(tradeDate, data.endDate()) > 10) {
//                initData(tradeDate, tradeDate.plusYears(3));
//            }


            try {
                // 每日 - 回测（B/S）
                execBacktestDaily(topBlockStrategyEnum, buyConSet, sellConSet, tradeDate, taskDO);
            } catch (Exception e) {
                log.error("execBacktestDaily     >>>     taskId : {} , tradeDate : {} , exMsg : {}", taskDO.getId(), tradeDate, e.getMessage(), e);


                throw e;


                // retry（TODO   暂时 先禁止重试   ->   直接抛出异常，终止执行！）
//                retryExecBacktestDaily(topBlockStrategyEnum, buyConList, sellConList, tradeDate, taskDO, backup, 5);
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


        Set<String> buyConSet = Sets.newHashSet(taskDO.getBuyStrategy().split(","));
        Set<String> sellConSet = Sets.newHashSet(taskDO.getSellStrategy().split(","));

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


        // 回测-对照组 可变参数
        BacktestCompareDTO btCompareDTO = JSON.parseObject(taskDO.getExtData(), BacktestCompareDTO.class);

        btCompareDTO.setBatchNo(taskDO.getBatchNo());
        btCompareDTO.setTopBlockStrategyEnum(topBlockStrategyEnum);
        btCompareDTO.setBuyConSet(buyConSet);
        btCompareDTO.setSellConSet(sellConSet);
        btCompareDTO.setStartDate(taskDO.getStartDate());
        btCompareDTO.setEndDate(taskDO.getEndDate());

        BacktestStrategy.btCompareDTO.set(btCompareDTO);


        // -------------------------------------------------------------------------------------------------------------


        while (tradeDate.isBefore(update__endDate)) {


            // ------------------ fail-fast（BS策略 快速淘汰）
            if (failFast(taskDO, tradeDate)) {
                taskDO.setFailFastFlag(1);
                taskDO.setEndDate(tradeDate);
                break;
            }


            tradeDate = tradeDateIncr(tradeDate);
            // 备份
            Backup backup = backupThreadLocal();


            try {
                // 每日 - 回测（B/S）
                execBacktestDaily(topBlockStrategyEnum, buyConSet, sellConSet, tradeDate, taskDO);
            } catch (Exception e) {
                log.error("execBacktestDaily     >>>     taskId : {} , tradeDate : {} , exMsg : {}", taskDO.getId(), tradeDate, e.getMessage(), e);


                // retry
                retryExecBacktestDaily(topBlockStrategyEnum, buyConSet, sellConSet, tradeDate, taskDO, backup, 5);
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
                                        Set<String> buyConSet,
                                        Set<String> sellConSet,
                                        LocalDate tradeDate,
                                        BtTaskDO taskDO,
                                        Backup backup,
                                        int retry) {


        log.info("retryExecBacktestDaily - start     >>>     retry : {} , taskId : {} , tradeDate : {} , topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {}",
                 retry - 1, taskDO.getId(), tradeDate, topBlockStrategyEnum, buyConSet, sellConSet);


        // if (--retry < 0) {
        //     return;
        // }


        // 恢复
        restoreThreadLocal(backup);


        try {

            // 每日 - 回测（B/S）
            execBacktestDaily(topBlockStrategyEnum, buyConSet, sellConSet, tradeDate, taskDO);

            log.info("retryExecBacktestDaily - suc     >>>     retry : {} , taskId : {} , tradeDate : {} , topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {}",
                     retry, taskDO.getId(), tradeDate, topBlockStrategyEnum, buyConSet, sellConSet);


        } catch (Exception e) {

            log.error("retryExecBacktestDaily - fail     >>>     retry : {} , taskId : {} , tradeDate : {} , topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {}   ,   exMsg : {}",
                      retry, taskDO.getId(), tradeDate, topBlockStrategyEnum, buyConSet, sellConSet, e.getMessage(), e);


            // 重试失败   ->   中断 异常task
            if (--retry < 0) {
                throw e;
            }


            retryExecBacktestDaily(topBlockStrategyEnum, buyConSet, sellConSet, tradeDate, taskDO, backup, retry);
        }
    }


    private BtTaskDO createBacktestTask(Integer batchNo,
                                        TopBlockStrategyEnum topBlockStrategyEnum,
                                        Set<String> buyConSet,
                                        Set<String> sellConSet,
                                        LocalDate startDate,
                                        LocalDate endDate) {

        BtTaskDO taskDO = new BtTaskDO();

        // 任务批次号
        taskDO.setBatchNo(batchNo);
        // 回测股票类型
        taskDO.setStockType(btCompareDTO.get().getStockType());
        // 任务状态
        taskDO.setStatus(1);

        // B/S策略
        taskDO.setTopBlockStrategy(topBlockStrategyEnum.getDesc());
        taskDO.setBuyStrategy(String.join(",", buyConSet));
        taskDO.setSellStrategy(String.join(",", sellConSet));


        // 回测 - 时间段
        taskDO.setStartDate(startDate);
        taskDO.setEndDate(endDate);

        // 初始本金
        taskDO.setInitialCapital(BigDecimal.valueOf(100_0000));
        // 初始净值
        taskDO.setInitialNav(BigDecimal.valueOf(1.0000));


        // extData
        taskDO.setExtData(JSON.toJSONString(btCompareDTO.get()));


        // -------------------------------------------------------------------------------------------------------------


        btTaskService.save(taskDO);


        return taskDO;
    }


    private void execBacktestDaily(TopBlockStrategyEnum topBlockStrategyEnum,
                                   Set<String> buyConSet,
                                   Set<String> sellConSet,
                                   LocalDate tradeDate,
                                   BtTaskDO taskDO) {


        Long taskId = taskDO.getId();


        x.get().taskId = taskId;
        x.get().tradeDate = tradeDate;


        x.get().avlCapital = x.get().prevAvlCapital;
        // 总资金     =>     今日 计算前 -> 先取 昨日总资金（ = 昨日市值 + 昨日可用资金）
        x.get().capital = x.get().prevCapital;


        // -------------------------------------------------------------------------------------------------------------
        //                                            B策略（开盘B）   ->  这里将 open_B 放在 open_S 前  ->  开盘BS  发生在 集合竞价阶段（9:25） =>  open_S 竞价S的资金 不能用来 open_B
        // -------------------------------------------------------------------------------------------------------------


        // open_BS前 -> 账户数据
        open_BS_before___statData___step1__init(tradeDate);


        open_B(tradeDate);


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略（开盘S）
        // -------------------------------------------------------------------------------------------------------------


        open_S(tradeDate);


        // -------------------------------------------------------------------------------------------------------------


        // 收盘价 还原     +     清空  ->  prev_date__btOpenBSDTO
        clear__prevDate__btOpenBSDTO();


        // -------------------------------------------------------------------------------------------------------------


        print__open_BS__close_BS("close_BS");


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
        sell_before___statData___step2__init();


        // -------------------------------------------------------------------------------------------------------------


        // 1、开盘BS     =>     先B -> 再S（开盘S   S资金 -> 不能用来B）       同一时刻（竞价9:15） 同时挂 B单 + S单
        // 2、收盘BS     =>     先S -> 再B（收盘S   S资金 -> 可以用来B）       不同时刻（盘中14:55）先挂S单 -> 再挂B单


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略（收盘S）
        // -------------------------------------------------------------------------------------------------------------


        Map<String, SellStrategyEnum> sell_infoMap = Maps.newHashMap();


        // 卖出策略
        String sellStrategyKey = btCompareDTO.get().getSellStrategyKey();
        Set<String> sell__stockCodeSet = sellStrategyFactory.get(sellStrategyKey).rule(topBlockStrategyEnum, data, tradeDate, x.get().positionStockCodeSet, sell_infoMap, btCompareDTO.get());

        log.info("S策略     >>>     [{}] [{}] , topBlockStrategyEnum : {} , size : {} , sell__stockCodeSet : {} , sell_infoMap : {}",
                 taskId, tradeDate, topBlockStrategyEnum, sell__stockCodeSet.size(), JSON.toJSONString(sell__stockCodeSet), JSON.toJSONString(sell_infoMap));


        // 持仓个股   ->   匹配 淘汰


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）

        // 2.2 每日 淘汰策略（S策略 - 2）[排名]走弱 -> 末位淘汰 ->  stockCodeList（对昨日 持股 -> 末位淘汰[设置末尾淘汰 - 分数线/排名线 ]）


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略（收盘S）-> 交易 record
        // -------------------------------------------------------------------------------------------------------------


        // S策略   ->   SELL TradeRecord
        createAndSave__SELL_TradeRecord(taskId, tradeDate, sell__stockCodeSet, x.get().prev__stockCode_positionDO_Map, sell_infoMap);


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
        //                                            B策略（收盘B）
        // -------------------------------------------------------------------------------------------------------------


        Map<String, String> buy_infoMap = Maps.newConcurrentMap();
        // 当前仓位
        double posRate = x.get().getMarketValue() / x.get().getCapital();


        // 买入策略
        String buyStrategyKey = btCompareDTO.get().getBuyStrategyKey();
        List<String> buy__stockCodeList = buyStrategyFactory.get(buyStrategyKey).rule(topBlockStrategyEnum, buyConSet, data, tradeDate, buy_infoMap, posRate, btCompareDTO.get().getZtFlag());

        log.info("B策略     >>>     [{}] [{}] , topBlockStrategyEnum : {} , size : {} , buy__stockCodeList : {} , buy_infoMap : {}",
                 taskId, tradeDate, topBlockStrategyEnum, buy__stockCodeList.size(), JSON.toJSONString(buy__stockCodeList), JSON.toJSONString(buy_infoMap));


        // -------------------------------------------------------------------------------------------------------------


        // TODO   B策略 - S策略   相互冲突bug       ==>       S半仓   /   S（清仓） -> 不B


        // B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
        buy_sell__signalConflict(topBlockStrategyEnum, data, tradeDate, buy__stockCodeList, buy_infoMap);


        // -------------------------------------------------------------------------------------------------------------
        //                                            B策略（收盘B）-> 交易 record
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


        // 强制更新 最新价（closePrice）  ->   市值（marketValue）  ->   总资金（capital）


        // save -> DB
        List<BtPositionRecordDO> holdAndClearPosList = Lists.newArrayList(x.get().positionRecordDOList);
        holdAndClearPosList.addAll(x.get().clearPositionRecordDOList);
        btPositionRecordService.retryBatchSave(holdAndClearPosList);


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日收益
        // -------------------------------------------------------------------------------------------------------------


        calcDailyReturn(taskId, taskDO.getInitialCapital().doubleValue(), tradeDate);


        // -------------------------------------------------------------------------------------------------------------


        // 清理 非持仓个股 的交易记录
        clear__tradeRecordCache();


        // END   ->   prev 赋值
        refresh_statData__prev();


        // -------------------------------------------------------------------------------------------------------------
        checkTrade(taskId, tradeDate);
    }


    /**
     * BS策略 快速淘汰：月均收益<5%            // 按月淘汰
     *
     * @param taskDO
     * @param tradeDate
     * @return
     */
    private boolean failFast(BtTaskDO taskDO, LocalDate tradeDate) {
        if (!btCompareDTO.get().isFailFastFlag()) {
            return false;
        }


        // double ztRatio = btCompareDTO.get().ztFlag_true() || btCompareDTO.get().getStockType() == StockTypeEnum.ETF.type ? 0.7 : 1.0;
        double ztRatio = btCompareDTO.get().ztFlag_true() ? 0.7 : 1.0;
        ztRatio = btCompareDTO.get().getStockType() == StockTypeEnum.ETF.type ? 0.5 : ztRatio;
        int min_diff = btCompareDTO.get().getStockType() == StockTypeEnum.ETF.type ? 500 : 70;


        LocalDate startDate = taskDO.getStartDate();
        LocalDate endDate = taskDO.getEndDate();


        BtDailyReturnDO prev_dailyReturnDO = prev_dailyReturnDO__cache.get();
        if (prev_dailyReturnDO != null) {

            // 净值
            double nav = prev_dailyReturnDO.getNav().doubleValue();


            // 执行天数（自然日）
            long diff = DateTimeUtil.diff(startDate, tradeDate);
            if (diff > min_diff) {

                long N = diff / 30;
                if (N >= 2 && N <= 3 && nav < 1 + (0.015 * N * ztRatio)) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }
                if (N >= 4 && N <= 6 && nav < 1 + (0.025 * N * ztRatio)) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }
                if (N >= 7 && N <= 12 && nav < 1 + (0.035 * N * ztRatio)) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }
                if (N >= 13 && N <= 24 && nav < 1 + (0.045 * N * ztRatio)) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }
                if (N >= 25 && nav < 1 + (0.055 * N * ztRatio)) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }


                // 提前显示 TOP策略
                if (N >= 6 && diff % 10 == 0) {
                    taskDO.setEndDate(tradeDate);
                    sumTotalReturn(taskDO);
                }


            } else {

                if (nav < 0.95) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }
            }
        }


        return false;
    }


    /**
     * BS策略 快速淘汰：月均收益<5%            // 按月淘汰
     *
     * @param taskDO
     * @param tradeDate
     * @return
     */
    private boolean failFast2(BtTaskDO taskDO, LocalDate tradeDate) {
        if (!btCompareDTO.get().isFailFastFlag()) {
            return false;
        }


        LocalDate startDate = taskDO.getStartDate();
        LocalDate endDate = taskDO.getEndDate();


        BtDailyReturnDO prev_dailyReturnDO = prev_dailyReturnDO__cache.get();
        if (prev_dailyReturnDO != null) {

            // 净值
            double nav = prev_dailyReturnDO.getNav().doubleValue();


            // 执行天数（自然日）
            long diff = DateTimeUtil.diff(startDate, tradeDate);
            if (diff > 30) {

                long N = diff / 30;
                if (N >= 1 && N <= 2 && nav < Math.pow(1.03, N)) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }
                if (N >= 3 && N <= 4 && nav < Math.pow(1.035, N)) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }
                if (N >= 5 && nav < Math.pow(1.05, N)) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }


                // 提前显示 TOP策略
                if (N >= 6 && diff % 10 == 0) {
                    taskDO.setEndDate(tradeDate);
                    sumTotalReturn(taskDO);
                }


            } else {

                if (nav < 0.95) {
                    log.warn("failFast     >>>     taskId : {} , startDate : {} , endDate : {} , tradeDate : {} , nav : {}",
                             taskDO.getId(), startDate, endDate, tradeDate, nav);
                    return true;
                }
            }
        }


        return false;
    }


    /**
     * 检测 每日交易记录 数据完整性
     */
    private void checkTrade(Long taskId, LocalDate today) {
        if (!btCompareDTO.get().isCheckTradeFlag()) {
            return;
        }


//        if (today.isEqual(LocalDate.of(2019, 3, 5))) {
//            log.debug("checkTrade     >>>     taskId : {} , tradeDate : {}", taskId, today);
//        }


        LocalDate prev_date = tradeDateDecr(today);


        // BtTaskDO taskDO = btTaskService.getById(taskId);

        BtDailyReturnDO dailyReturnDO = btDailyReturnService.getByTaskIdAndTradeDate(taskId, today);
        BtDailyReturnDO prev_dailyReturnDO = btDailyReturnService.getByTaskIdAndTradeDate(taskId, prev_date);
        if (null == prev_dailyReturnDO) {
            // 首日  ->  无 昨日数据
            return;
        }


        List<BtTradeRecordDO> tradeRecordDOList = btTradeRecordService.listByTaskIdAndTradeDate(taskId, today);

        List<BtPositionRecordDO> prev_positionRecordDOList = btPositionRecordService.listByTaskIdAndTradeDateAndPosType(taskId, prev_date, 1);
        List<BtPositionRecordDO> positionRecordDOList = btPositionRecordService.listByTaskIdAndTradeDateAndPosType(taskId, today, 1);


        double prev_capital = prev_dailyReturnDO.getCapital().doubleValue();
        double prev_marketValue = prev_dailyReturnDO.getMarketValue().doubleValue();
        double prev_avlCapital = prev_dailyReturnDO.getAvlCapital().doubleValue();
        double prev_buyCapital = prev_dailyReturnDO.getBuyCapital().doubleValue();
        double prev_sellCapital = prev_dailyReturnDO.getSellCapital().doubleValue();


        double capital = dailyReturnDO.getCapital().doubleValue();
        double marketValue = dailyReturnDO.getMarketValue().doubleValue();
        double avlCapital = dailyReturnDO.getAvlCapital().doubleValue();
        double buyCapital = dailyReturnDO.getBuyCapital().doubleValue();
        double sellCapital = dailyReturnDO.getSellCapital().doubleValue();
        double profitLossAmount = dailyReturnDO.getProfitLossAmount().doubleValue();


        // prev__pos  +  today__trade_record     ->     today__pos
        Map<String, BtPositionRecordDO> prevPos__codeEntityMap = Maps.newHashMap();
        for (BtPositionRecordDO entity : prev_positionRecordDOList) {
            String stockCode = entity.getStockCode();
            int qty = entity.getQuantity();


            double db_closePrice = entity.getClosePrice().doubleValue();   // 可能为开盘价
            double act_closePrice = getActClosePrice(stockCode, prev_date);

            if (act_closePrice == 0) {
                log.debug("当日停牌   -   [{}] [{}]   >>>   db_closePrice[{}] == 0", stockCode, prev_date, db_closePrice);
            }


            Assert.isTrue(TdxFunCheck.equals(db_closePrice, act_closePrice, 1, 0.001), String.format("[%s]   >>>   db_closePrice[%s] != act_closePrice[%s]", stockCode, db_closePrice, act_closePrice));
            entity.setAct_closePrice(act_closePrice);

            prevPos__codeEntityMap.put(stockCode, entity);
        }


        // prev__return  +  today__trade_record     ->     today__return
        // today__pos   ->     today__marketValue   ->   today__capital


        // today__trade_record   ->     today__buyCapital / today__sellCapital   ->   today__avlCapital
        // 今日BS数量  =  +B数量 - S数量
        Map<String, Integer> todayBS__codeQtyMap = Maps.newHashMap();
        // 今日B数量
        Map<String, Integer> todayB__codeQtyMap = Maps.newHashMap();
        // 今日S数量
        Map<String, Integer> todayS__codeQtyMap = Maps.newHashMap();

        double x_buyCapital = 0;
        double x_sellCapital = 0;

        // BS收益（变动仓位）
        double x_profitLossAmount__todayBS = 0; // 每日收益额  =  BS收益（变动仓位） +  持仓收益（不变仓位）


        for (BtTradeRecordDO t : tradeRecordDOList) {
            String stockCode = t.getStockCode();


            double price = t.getPrice().doubleValue();
            int todayBS_qty = t.getQuantity();
            int sign = t.getTradeType() == 1 ? 1 : -1;


            double amount = t.getAmount().doubleValue();
            double x_amount = todayBS_qty * price;
            Assert.isTrue(TdxFunCheck.equals(amount, x_amount, 0.1, 0.001), String.format("[%s]   >>>   amount[%s] != x_amount[%s]", stockCode, amount, x_amount));


            // 1-买入
            if (t.getTradeType() == 1) {
                todayB__codeQtyMap.merge(stockCode, todayBS_qty, Integer::sum);
                x_buyCapital += amount;

                double today__act_closePrice = getActClosePrice(stockCode, today);
                if (today__act_closePrice == 0) {
                    log.debug("当日停牌   -   [{}] [{}]   >>>   today__act_closePrice[{}] == 0", stockCode, today, today__act_closePrice);
                }


                x_profitLossAmount__todayBS += todayBS_qty * (today__act_closePrice - price);  // B仓位 -> 今日收益额 = 买入数量 * (今日收盘价 - 今日B价格)
            }
            // 2-卖出
            else if (t.getTradeType() == 2) {
                todayS__codeQtyMap.merge(stockCode, todayBS_qty, Integer::sum);
                x_sellCapital += amount;


                BtPositionRecordDO prevPos = prevPos__codeEntityMap.get(stockCode);
                Assert.isTrue(null != prevPos, String.format("[%s]   >>>   prev_pos_not_found[昨日无持仓，今日却在S！]", stockCode));

                int prevPos__qty = prevPos.getQuantity();
                // 昨日持仓数量 >= 今日S数量
                Assert.isTrue(prevPos__qty >= todayBS_qty, String.format("[%s]   >>>   prevPos__qty[%s] < today_S_qty[%s]", stockCode, prevPos__qty, todayBS_qty)); // 每次S -> 清仓（除非大盘仓位限制 -> 减仓）


                double prevPos__act_closePrice = prevPos.getAct_closePrice();
                x_profitLossAmount__todayBS += todayBS_qty * (price - prevPos__act_closePrice);  // S仓位 -> 今日收益额 = 卖出数量 * (今日S价格 - 昨日收盘价)
            }


            todayBS__codeQtyMap.merge(stockCode, todayBS_qty * sign, Integer::sum);
        }
        Assert.isTrue(TdxFunCheck.equals(buyCapital, x_buyCapital, 1, 0.001), String.format("buyCapital[%s] != x_buyCapital[%s]", buyCapital, x_buyCapital));
        Assert.isTrue(TdxFunCheck.equals(sellCapital, x_sellCapital, 1, 0.001), String.format("sellCapital[%s] != x_sellCapital[%s]", sellCapital, x_sellCapital));


        Map<String, BtPositionRecordDO> todayPos__codeEntityMap = Maps.newHashMap();
        double x_marketValue = 0;
        // 持仓收益（不变仓位）
        double x_profitLossAmount__todayPos = 0; // 每日收益额  =  BS收益（变动仓位） +  持仓收益（不变仓位）
        for (BtPositionRecordDO entity : positionRecordDOList) {
            String stockCode = entity.getStockCode();
            int qty = entity.getQuantity();


            double db__marketValue = entity.getMarketValue().doubleValue();


            double db_closePrice = entity.getClosePrice().doubleValue();   // 可能为开盘价
            double act_closePrice = getActClosePrice(stockCode, today);
            if (act_closePrice == 0) {
                log.debug("当日停牌   -   [{}] [{}]   >>>   act_closePrice[{}] == 0", stockCode, today, act_closePrice);
            }
            Assert.isTrue(db_closePrice == act_closePrice, String.format("[%s]   >>>   db_closePrice[%s] != act_closePrice[%s]", stockCode, db_closePrice, act_closePrice));
            entity.setAct_closePrice(act_closePrice);


            double act__marketValue = act_closePrice * qty;
            Assert.isTrue(TdxFunCheck.equals(act__marketValue, db__marketValue, 1, 0.001), String.format("[%s]   >>>   act__marketValue[%s] != db__marketValue[%s]", stockCode, act__marketValue, db__marketValue));


            x_marketValue += act__marketValue;


            BtPositionRecordDO prevPos = prevPos__codeEntityMap.get(stockCode);
            int prevPos__qty = 0;
            double prevPos__act_closePrice = 0;
            if (null != prevPos) {
                prevPos__qty = prevPos.getQuantity();
                prevPos__act_closePrice = prevPos.getAct_closePrice();

                double prevPos__act_closePrice_2 = getActClosePrice(stockCode, prev_date);
                if (prevPos__act_closePrice_2 == 0) {
                    log.debug("当日停牌   -   [{}] [{}]   >>>   prevPos__act_closePrice_2[{}] == 0", stockCode, prev_date, prevPos__act_closePrice_2);
                }
                Assert.isTrue(prevPos__act_closePrice == prevPos__act_closePrice_2, String.format("[%s]   >>>   prevPos__act_closePrice[%s] != prevPos__act_closePrice_2[%s]", stockCode, prevPos__act_closePrice, prevPos__act_closePrice_2));
            }
            // 今日持仓数量  =  昨日持仓数量  ±  今日BS数量
            int todayBS_qty = todayBS__codeQtyMap.getOrDefault(stockCode, 0);
            int todayBS_qty__2 = todayB__codeQtyMap.getOrDefault(stockCode, 0) - todayS__codeQtyMap.getOrDefault(stockCode, 0);
            Assert.isTrue(todayBS_qty == todayBS_qty__2, String.format("[%s]   >>>   todayBS_qty[%s] != todayBS_qty__2[%s]", stockCode, todayBS_qty, todayBS_qty__2));

            int todayPos__qty = prevPos__qty + todayBS_qty;
            Assert.isTrue(todayPos__qty == qty, String.format("[%s]   >>>   todayPos__qty[%s] != qty[%s]", stockCode, todayPos__qty, qty));


            // 昨日_今日__持仓数量  =  今日持仓数量 - 今日B数量
            int prev_today__qty = todayPos__qty - todayB__codeQtyMap.getOrDefault(stockCode, 0);
            // 持有收益  =  昨日_今日__持仓数量 * (今日收盘价 - 昨日收盘价)
            x_profitLossAmount__todayPos += prev_today__qty * (act_closePrice - prevPos__act_closePrice);


            todayPos__codeEntityMap.put(stockCode, entity);
        }
        Assert.isTrue(TdxFunCheck.equals(marketValue, x_marketValue, 1, 0.001), String.format("marketValue[%s] != x_marketValue[%s]", marketValue, x_marketValue));


        // 可用资金  =  昨日可用资金 + 今日卖出金额 - 今日买入金额
        double x_avlCapital = prev_avlCapital + sellCapital - buyCapital;
        Assert.isTrue(TdxFunCheck.equals(avlCapital, x_avlCapital, 1, 0.001), String.format("avlCapital[%s] != x_avlCapital[%s]", avlCapital, x_avlCapital));


        // 每日收益额  =  BS收益（变动仓位） +  持仓收益（不变仓位）
        double x_profitLossAmount = x_profitLossAmount__todayBS + x_profitLossAmount__todayPos;
        // profitLossAmount[1698.32] != x_profitLossAmount[1677.79]   ,   x_profitLossAmount__todayBS=[1677.79]   ,   x_profitLossAmount__todayPos=[0.0]
        Assert.isTrue(TdxFunCheck.equals(profitLossAmount, x_profitLossAmount, 50, 0.001),
                      String.format("profitLossAmount[%s] != x_profitLossAmount[%s]   ,   x_profitLossAmount__todayBS=[%s]   ,   x_profitLossAmount__todayPos=[%s]",
                                    profitLossAmount, x_profitLossAmount, x_profitLossAmount__todayBS, x_profitLossAmount__todayPos));


        // 账户资金  =  昨日资金 + 今日收益额
        double x_capital = prev_capital + x_profitLossAmount;
        Assert.isTrue(TdxFunCheck.equals(capital, x_capital, 1, 0.001), String.format("capital[%s] != x_capital[%s]", capital, x_capital));
    }


    private void open_BS_before___statData___step1__init(LocalDate tradeDate) {


        print__open_BS__close_BS("open_BS");


        BacktestOpenBSDTO bt_openBSDTO = btOpenBSDTO.get();
        if (is_openBS(tradeDate)) {
            Stat stat = x.get();
            log.info("open_BS -> 账户统计数据 - start     >>>     [{}] [{}] , stat : {}", stat.taskId, tradeDate, JSON.toJSONString(stat));
        }


        // 大盘量化   ->   总仓位 限制
        LocalDate prev_date = bt_openBSDTO.today_date == null ? tradeDateDecr(tradeDate) : bt_openBSDTO.today_date;
        market__position_limit(prev_date);   // 开盘BS  ->  用 prev_date（ =  btOpenBSDTO.today_date）


        //  open_BS前 -> 账户数据（每日 -> 必须执行1次）
        sell_before___statData___step1__init();
    }


    // ------------------------------------------------------- open_B --------------------------------------------------


    private void open_B(LocalDate tradeDate) {
        BacktestOpenBSDTO bt_OpenBSDTO = btOpenBSDTO.get();
        if (!is_openBS(tradeDate)) {
            return;
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            B策略（开盘B）-> 交易 record
        // -------------------------------------------------------------------------------------------------------------


        Long taskId = x.get().taskId;
        List<String> buy__stockCodeList = Lists.newArrayList(bt_OpenBSDTO.open_B___stockCodeSet);
        Map<String, String> openB___buyInfoMap = bt_OpenBSDTO.open_B___buy_infoMap;


        // 按照 规则打分 -> sort
        List<String> sort__stockCodeList = ScoreSort.scoreSort(buy__stockCodeList, data, tradeDate, btCompareDTO.get().getScoreSortN());


        log.debug("B策略 -> 交易 record - start     >>>     [{}] [{}] , prevAvlCapital : {} , sellCapital : {} , avlCapital : {} , prevCapital : {}",
                  taskId, tradeDate, x.get().prevAvlCapital, x.get().sellCapital, x.get().avlCapital, x.get().prevCapital);


        // B策略   ->   BUY TradeRecord
        createAndSave__BUY_TradeRecord(taskId, tradeDate, sort__stockCodeList, openB___buyInfoMap);


//        List<BtTradeRecordDO> tradeRecordDOList = btTradeRecordService.listByTaskIdAndTradeDate(x.get().taskId, x.get().tradeDate);
//        if (tradeRecordDOList.size() > 0) {
//            Stat stat = x.get();
//            log.debug("B策略 -> 账户统计数据 - start     >>>     [{}] [{}] , stat : {}", stat.taskId, tradeDate, JSON.toJSONString(stat));
//        }


        // B后  ->  账户统计数据
        refresh_statData();
    }


    // ------------------------------------------------------- open_S --------------------------------------------------

    private void open_S(LocalDate tradeDate) {

        BacktestOpenBSDTO bt_OpenBSDTO = btOpenBSDTO.get();
        if (!is_openBS(tradeDate)) {
            return;
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略（开盘S）-> 交易 record
        // -------------------------------------------------------------------------------------------------------------


        Long taskId = x.get().taskId;
        Set<String> sell__stockCodeSet = bt_OpenBSDTO.open_S___stockCodeSet;
        Map<String, SellStrategyEnum> openS___sellInfoMap = bt_OpenBSDTO.open_S___sell_infoMap;


//        if (/*tradeDate.isEqual(LocalDate.of(2025, 12, 3)) &&*/ sell__stockCodeSet.contains("301136")) {
//            log.debug("open_S debug   -   S策略 -> 交易 record - start     >>>     [{}] [{}] , sell__stockCodeSet : {}", taskId, tradeDate, sell__stockCodeSet);
//        }


        // S策略   ->   SELL TradeRecord
        createAndSave__SELL_TradeRecord(taskId, tradeDate, sell__stockCodeSet, x.get().prev__stockCode_positionDO_Map, openS___sellInfoMap);


//        List<BtTradeRecordDO> tradeRecordDOList = btTradeRecordService.listByTaskIdAndTradeDate(x.get().taskId, x.get().tradeDate);
//        if (tradeRecordDOList.size() > 0) {
//            Stat stat = x.get();
//            log.debug("S策略 -> 账户统计数据 - start     >>>     [{}] [{}] , stat : {}", stat.taskId, tradeDate, JSON.toJSONString(stat));
//        }


        // S后  ->  账户统计数据
        refresh_statData();
    }


    private void clear__prevDate__btOpenBSDTO() {
        // clear  ->  prev_date__开盘BS
        BacktestStrategy.btOpenBSDTO.set(new BacktestOpenBSDTO());
    }


    // ------------------------------------------------------- S -------------------------------------------------------


    /**
     * S策略   ->   SELL TradeRecord
     *
     * @param taskId
     * @param tradeDate
     * @param sell__stockCodeSet
     * @param prev__stockCode_positionDO_Map 昨日持仓（T+1  ->  只能卖出 昨日持仓）
     * @param sell_infoMap
     * @return
     */
    private void createAndSave__SELL_TradeRecord(Long taskId,
                                                 LocalDate tradeDate,
                                                 Set<String> sell__stockCodeSet,
                                                 Map<String, BtPositionRecordDO> prev__stockCode_positionDO_Map,
                                                 Map<String, SellStrategyEnum> sell_infoMap) {


        List<BtTradeRecordDO> sell__tradeRecordDO__List = Lists.newArrayList();


        // 只能卖出 昨日持仓（减去  ->  今日开盘B 买入数量）


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

            // 主线板块
            Set<String> stock__blockCodeNameSet__inTopBlock = data.stock__inTopBlockCache.get(tradeDate, k -> Maps.newConcurrentMap()).get(stockCode);
            sell_tradeRecordDO.setTopBlockSet(JSON.toJSONString(stock__blockCodeNameSet__inTopBlock));


            sell_tradeRecordDO.setPrice(NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate)));
            BtPositionRecordDO prevPos = prev__stockCode_positionDO_Map.get(stockCode);
            if (null == prevPos) {
                log.error("[SELL] 持仓记录不存在  ->  stockCode : {} , prevPos : {} , taskId : {} , tradeDate : {} , btOpenBSDTO : {}",
                          stockCode, JSON.toJSONString(prevPos), taskId, tradeDate, JSON.toJSONString(btOpenBSDTO.get()));
                continue;
            }
            sell_tradeRecordDO.setQuantity(prevPos.getQuantity());

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
            sell_tradeRecordDO.setTradeSignalType(SellStrategyEnum.S91.getType());
            sell_tradeRecordDO.setTradeSignalDesc(SellStrategyEnum.S91.getDesc());


            // 主线板块
            Set<String> stock__blockCodeNameSet__inTopBlock = data.stock__inTopBlockCache.get(tradeDate, k -> Maps.newConcurrentMap()).get(stockCode);
            sell_tradeRecordDO.setTopBlockSet(JSON.toJSONString(stock__blockCodeNameSet__inTopBlock));


            double closePrice = getClosePrice(stockCode, tradeDate);
            sell_tradeRecordDO.setPrice(NumUtil.double2Decimal(closePrice));


            int qty = (int) (stock_reduction_amount / closePrice);   // 减仓金额过小时，可能会为0.1234  ->  取整后为：0
            // 不能超卖
            qty = Math.min(qty, quantity);
            sell_tradeRecordDO.setQuantity(qty);

            // 减仓=0（减仓金额太小）  ->   略过（TODO   暂不略过  ->  依然记录：大盘仓位限制->等比减仓  0股）
//            if (qty <= 0) {
//                continue;
//            }


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
     * @param buy_infoMap
     */
    @TotalTime
    public void buy_sell__signalConflict(TopBlockStrategyEnum topBlockStrategyEnum,
                                         BacktestCache data,
                                         LocalDate tradeDate,
                                         List<String> buy__stockCodeList,
                                         Map<String, String> buy_infoMap) {

        if (CollectionUtils.isEmpty(buy__stockCodeList)) {
            return;
        }


        Map<String, SellStrategyEnum> sell_infoMap = Maps.newHashMap();


        // 当前 buyList   ->   是否 与 S策略 相互冲突       =>       过滤出 冲突个股（sellList）
        String sellStrategyKey = btCompareDTO.get().getSellStrategyKey();
        Set<String> sell__stockCodeSet = sellStrategyFactory.get(sellStrategyKey).rule(topBlockStrategyEnum, data, tradeDate, Sets.newHashSet(buy__stockCodeList), sell_infoMap, btCompareDTO.get());


        // 今日开盘买入[涨停_SSF多_月多]   ->   今日无法卖出     =>     不参与 BS冲突过滤
        if (CollectionUtils.isNotEmpty(sell__stockCodeSet)) {
            buy_infoMap.forEach((stockCode, strategyInfo) -> {
                if (strategyInfo.contains(涨停_SSF多_月多.getDesc())) {
                    sell__stockCodeSet.remove(stockCode);
                }
            });
        }


        // buyList   ->   remove  冲突个股（sellSet）
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


        if (CollectionUtils.isEmpty(buy__stockCodeList)) {
            return;
        }


        log.debug("B策略 -> 交易 record - end     >>>     [{}] [{}] , prevAvlCapital : {} , sellCapital : {} , avlCapital : {} , prevCapital : {} , buyCapital : {}",
                  taskId, tradeDate, x.get().prevAvlCapital, x.get().sellCapital, x.get().avlCapital, x.get().prevCapital, x.get().buyCapital);


        // ------------------------------------------


        // 等比买入
        double avg_amount = x.get().actAvlCapital / buy__stockCodeList.size();


        // 单一个股 单次买入下限  >=  账户总资金 x 0.5%
        double STOCK_BUY__LIMIT_min = btCompareDTO.get().getSingleStockMinBuyPosPct() * 0.01;
        double min_buy_amount = x.get().capital * STOCK_BUY__LIMIT_min;                      // 账户总资金 x 0.5%


        while (avg_amount < min_buy_amount && buy__stockCodeList.size() >= 2) {
            // buy__stockCodeList   ->   减半（自动淘汰 排名后50%）
            buy__stockCodeList = buy__stockCodeList.subList(0, buy__stockCodeList.size() / 2);


            // 等比买入
            avg_amount = x.get().actAvlCapital / buy__stockCodeList.size();
        }


        // ------------------------------------------


        // 单一个股 单次买入上限  <=  剩余资金 x 10%
        double STOCK_BUY__LIMIT_max = btCompareDTO.get().getSingleStockMaxBuyAvlPct() * 0.01;
        avg_amount = Math.min(avg_amount, x.get().actAvlCapital * STOCK_BUY__LIMIT_max);     // 实际 可用资金 * 10%


        // 单一个股 总持仓上限  <=  账户总资金 x 5%
        double STOCK_POS__LIMIT = btCompareDTO.get().getSingleStockMaxPosPct() * 0.01;
        double amount_limit = x.get().capital * STOCK_POS__LIMIT;     // 总资金 * 5%


        // ------------------------------------------


        // B策略   ->   BUY TradeRecord
        List<BtTradeRecordDO> buy__tradeRecordDO__List = Lists.newArrayList();


        for (String stockCode : buy__stockCodeList) {


            // 当前  待买入个股  市值（如果 此前已持有 该个股）
            double marketValue = Optional.ofNullable(x.get().stockCode_positionDO_Map.get(stockCode)).map(e -> e.getMarketValue().doubleValue()).orElse(0.0);


            // 可买仓位  =  最大仓位限制 - 个股市值
            double amount = amount_limit - marketValue;
            if (amount <= 0 || avg_amount <= 0) {
                continue;
            } else {
                amount = Math.min(amount, avg_amount);
            }


            // -----------------------------------------------------------


            BtTradeRecordDO tradeRecordDO = new BtTradeRecordDO();
            tradeRecordDO.setTaskId(taskId);
            tradeRecordDO.setTradeType(BtTradeTypeEnum.BUY.getTradeType());
            tradeRecordDO.setStockId(data.stock__codeIdMap.get(stockCode));
            tradeRecordDO.setStockCode(stockCode);
            tradeRecordDO.setStockName(data.stock__codeNameMap.get(stockCode));
            tradeRecordDO.setTradeDate(tradeDate);
            tradeRecordDO.setTradeSignalType(1);
            tradeRecordDO.setTradeSignalDesc(buy_infoMap.get(stockCode));


            // 主线板块
            Set<String> stock__blockCodeNameSet__inTopBlock = data.stock__inTopBlockCache.get(tradeDate, k -> Maps.newConcurrentMap()).get(stockCode);
            tradeRecordDO.setTopBlockSet(JSON.toJSONString(stock__blockCodeNameSet__inTopBlock));


            // 收盘价
            BigDecimal close = NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate));
            tradeRecordDO.setPrice(close);

            // 买入数量   =   可买仓位 / 收盘价                                  （忽略 🐶💩共产主义特色   ->   100股 bug）
            double qty = amount / close.doubleValue();
            tradeRecordDO.setQuantity((int) qty);

            // 成交额 = 价格 x 数量
            // 不能用 amount（double qty  ->  int qty  =>  会少买1股）  ->   必须用 price x qty           // 如果 price = 100， amount -> 100 x 1 = 100   =>   bug：B金额 -> 凭空 多计入 100 ❗❗❗
            double act_amount = tradeRecordDO.getPrice().doubleValue() * tradeRecordDO.getQuantity(); // BUG：每天  100多只个股   =>   每天 B金额 -> 凭空 多计入 1000 ~ 10000 ❗❗❗
            tradeRecordDO.setAmount(of(act_amount));     // BUG：导致  实际市值（持仓个股 【price x qty ↓】 累加） <  计算市值（持仓个股 【amount ↑】 累加）
            //                                              BUG：导致  实际可用资金（+S -B【price x qty ↓ 累加】）>  计算可用资金（+S -B【amount ↑ 累加】）  =>   计算可用资金（甚至会 出现负数）❗❗❗


            // 仓位占比 = 持仓市值 / 总资金
            double positionPct = act_amount / x.get().capital * 100;
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


        // 是否开启 大盘持仓限制：true/false
        boolean marketPosFlag = btCompareDTO.get().isMarketPosLimitFlag();
        if (!marketPosFlag) {
            // 否  ->  100% 仓位上限
            x.get().positionLimitRate = 1.0;
            return;
        }


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


        // 获取 -> 持仓列表（今日收盘时刻 持仓个股 涨跌变化列表  =>  今日 [市值变化] -> 今日 [账户总资金变化]）
        List<BtPositionRecordDO> positionRecordDOList = getDailyPositions(x.get().taskId, x.get().tradeDate, null);


        // 计算
        CalcStat calcStat = new CalcStat(positionRecordDOList, null);


        // ------------------------------------------------------------------------

        // copy覆盖
        // BeanUtils.copyProperties(calcStat, x.get());


        x.get().positionRecordDOList = positionRecordDOList;
        x.get().positionStockCodeSet = calcStat.positionStockCodeSet;
        x.get().stockCode_positionDO_Map = calcStat.stockCode_positionDO_Map;


        // ------------------------------------------------------------------------


        // 当前 总市值   =   S前 总市值
        x.get().marketValue = calcStat.marketValue;

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


    /**
     * SELL - before        =>      计算 总资金
     */
    private void sell_before___statData___step2__init() {


        // -------- open_BS之后   ->   close_BS之前


        // 强制更新 最新价（closePrice）  ->   市值（marketValue）  ->   总资金（capital）
        Stat stat = x.get();
        double openBS_after__marketValue = stat.marketValue;
        double openBS_after__avlCapital = stat.avlCapital;
        double openBS_after__capital = stat.capital;

        // =  buy_price/sell_price * qty     仅跟   买入/卖出价格 * 数量   相关     ->     买入后生成 交易快照（bt_trade_record）  ->   永不再变
        double openBS_after__buyCapital = stat.buyCapital;
        double openBS_after__sellCapital = stat.sellCapital;


        if (openBS_after__avlCapital < 0) {
            log.error("sell_before___statData___step2__init  -  err     >>>     [{}] [{}] , openBS_after__avlCapital : {}",
                      x.get().taskId, x.get().tradeDate, JSON.toJSONString(stat));
        }


        // -------- close_BS（收盘时刻）


        // 刷新 收盘价（openBS[open]   ->   closeBS[close]）

        // 实时价格（closePrice）  ->   实时市值（marketValue）  ->   实时总资金（capital）
        refresh_statData();   // 重计算 收盘时刻   closePrice   ->   marketValue   ->   capital


        stat.getPositionRecordDOList().forEach(p -> {
            String stockCode = p.getStockCode();

            double db_close = p.getClosePrice().doubleValue();

            double openPrice = getOpenPrice(stockCode, stat.tradeDate);
            double closePrice_1 = getClosePrice(stockCode, stat.tradeDate);
            double closePrice_2 = getActClosePrice(stockCode, stat.tradeDate);

            if (openPrice != closePrice_2 && db_close == openPrice) {
                if (log.isDebugEnabled()) {
                    log.error("sell_before___statData___step2__init  -  err     >>>     [{}] [{}] , stockCode : {} , openPrice : {} , closePrice_1 : {} , closePrice_2 : {}",
                              stat.taskId, stat.tradeDate, stockCode, openPrice, closePrice_1, closePrice_2);
                }
                p.setClosePrice(of(closePrice_2));
            }
        });


        // ---------------------------------------------------------- open_BS之后   ->   close_BS之前     未再发生过BS   ->   avlCapital 始终保持不变
        // S前 可用资金   =   openBS之后 可用资金
        x.get().avlCapital = openBS_after__avlCapital;


        // ------------------------------------------------------------------------

        // open_BS之后   ->   close_BS之前          总资金 跟随 实时市值（个股 实时价格） 实时变动

        // 当前 总市值   =   S前 总市值（更新个股 最新实时价格（price = close 收盘价）  ->   重新计算 实时市值[收盘市值]）
        x.get().marketValue = x.get().marketValue;


        // ---------------------------------------------------------- 不变（close_BS 开始，个股收盘价 不再变动   ->   个股市值不再变动   ->   总市值不再变动   ->   总资金 不再变动）
        x.get().capital = x.get().marketValue + openBS_after__avlCapital;


        // ---------------------------------------------------------- 不变


        // 仓位总金额 上限   =   总资金  x  仓位百分比 上限
        x.get().positionLimitAmount = x.get().capital * x.get().positionLimitRate;


        // 当前 实际可用资金（策略 -> 仓位限制）  =   仓位总金额_上限   -   持仓总市值
        x.get().actAvlCapital = x.get().positionLimitAmount - x.get().marketValue;


        // ---------------------------------------------------------- debug


        // =  buy_price/sell_price * qty     仅跟   买入/卖出价格 * 数量   相关     ->     买入后生成 交易快照（bt_trade_record）  ->   永不再变
        double closeBS_before__buyCapital = stat.buyCapital;
        double closeBS_before__sellCapital = stat.sellCapital;


        if (openBS_after__marketValue == stat.marketValue
                || openBS_after__avlCapital != stat.avlCapital
                || openBS_after__capital == stat.capital
                || openBS_after__buyCapital != closeBS_before__buyCapital
                || openBS_after__sellCapital != closeBS_before__sellCapital) {


            if (log.isDebugEnabled()) {
                log.error("sell_before___statData___step2__init  -  err     >>>     [{}] [{}] , x : {}",
                          x.get().taskId, x.get().tradeDate, JSON.toJSONString(stat));
            }
        }
    }


    // --------------------------------------------------- statData ----------------------------------------------------


    /**
     * refresh  ->  statData
     */
    private void refresh_statData() {

        // 获取  ->  今日 B/S记录
        List<BtTradeRecordDO> today_tradeRecordDOList = btTradeRecordService.listByTaskIdAndTradeDate(x.get().taskId, x.get().tradeDate);
        // 获取  ->  当前 持仓列表
        List<BtPositionRecordDO> positionRecordDOList = getDailyPositions(x.get().taskId, x.get().tradeDate, today_tradeRecordDOList);


        // 计算
        CalcStat calcStat = new CalcStat(positionRecordDOList, today_tradeRecordDOList);
        // copy覆盖
        BeanUtils.copyProperties(calcStat, x.get());
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


        // 2、today -> prev
        x.get().prevCapital = x_copy.capital;
        x.get().prevAvlCapital = x_copy.avlCapital;
        x.get().prev__stockCode_positionDO_Map = x_copy.stockCode_positionDO_Map;


        // 不清空
        x.get().taskId = x_copy.taskId;
        x.get().tradeDate = x_copy.tradeDate;   // date 不清空
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
     * @param initialCapital 本金
     * @param tradeDate      当前 交易日
     */
    private void calcDailyReturn(Long taskId, double initialCapital, LocalDate tradeDate) {


        Stat stat = x.get();
        double prevCapital = stat.prevCapital;
        double avlCapital = stat.avlCapital;
        double marketValue = stat.marketValue;
        double capital = stat.capital;


        // -------------------------------------------------------------------------------------------------------------


        BtDailyReturnDO prev_dailyReturnDO = prev_dailyReturnDO__cache.get();
        if (prev_dailyReturnDO != null) {
            double db__prev_capital = prev_dailyReturnDO.getCapital().doubleValue();
            Assert.isTrue(TdxFunCheck.equals(prevCapital, db__prev_capital, 0.01, 0.001), String.format("[%s] [%s] , x.prevCapital[%s] != db__prev_capital[%s]", taskId, tradeDate, prevCapital, db__prev_capital));
        }


        Assert.isTrue(avlCapital >= 0, String.format("[%s] [%s] , avlCapital[%s] < 0", taskId, tradeDate, avlCapital));


        // -------------------------------------------------------------------------------------------------------------


        // 仓位占比 = 持仓市值 / 总资金
        double positionPct = marketValue / capital * 100;


        // 净值 = 总资金 / 本金
        double nav = capital / initialCapital;

        // 当日盈亏额 = 当日总资金 - 昨日总资金
        double profitLossAmount = capital - prevCapital;


        // 当日收益率 = 当日盈亏额 / 昨日总资金
        double dailyReturn = profitLossAmount / prevCapital;
        log.debug("calcDailyReturn     >>>     [{}] [{}] , marketValue : {} , avlCapital : {} , capital : {} , prevCapital : {} , profitLossAmount : {} , dailyReturn : {} , nav : {}",
                  taskId, tradeDate, marketValue, avlCapital, capital, prevCapital, profitLossAmount, dailyReturn, nav);


        BtDailyReturnDO dailyReturnDO = new BtDailyReturnDO();
        dailyReturnDO.setTaskId(taskId);
        // 日期
        dailyReturnDO.setTradeDate(tradeDate);
        // 当日收益率
        dailyReturnDO.setDailyReturn(of(dailyReturn));
        // 当日盈亏额
        dailyReturnDO.setProfitLossAmount(of(profitLossAmount));
        // 净值
        dailyReturnDO.setNav(of(nav));
        // 总资金
        dailyReturnDO.setCapital(of(capital));
        // 持仓市值
        dailyReturnDO.setMarketValue(of(marketValue));
        // 持仓数量
        dailyReturnDO.setPosCount(stat.positionRecordDOList.size());
        // 仓位占比（%）
        dailyReturnDO.setPositionPct(of(positionPct));
        // 仓位上限占比（%）
        dailyReturnDO.setPositionLimitPct(of(stat.positionLimitRate * 100));
        // 可用资金
        dailyReturnDO.setAvlCapital(of(avlCapital));
        // 买入金额
        dailyReturnDO.setBuyCapital(of(stat.buyCapital));
        // 卖出金额
        dailyReturnDO.setSellCapital(of(stat.sellCapital));

        // 基准收益（沪深300）
        dailyReturnDO.setBenchmarkReturn(null);


        btDailyReturnService.retrySave(dailyReturnDO);


        // --------------------------------------------- prev
        prev_dailyReturnDO__cache.set(dailyReturnDO);
    }


    /**
     * 汇总计算 -> 总收益
     *
     * @param taskDO
     */
    private void sumTotalReturn(BtTaskDO taskDO) {


        // 全期汇总：更新 bt_task


        // 实时 结束日期
        taskDO.setEndDate(x.get().tradeDate);


        // 全量  每日收益-记录
        List<BtDailyReturnDO> dailyReturnDOList = btDailyReturnService.listByTaskId(x.get().taskId);
        // 全量  交易记录
        List<BtTradeRecordDO> tradeRecordDOList = btTradeRecordService.listByTaskId(x.get().taskId);


        // -------------------------------------------------------------------------------------------------------------


        // 最大回撤
        DrawdownResult drawdownResult = calcMaxDrawdown(dailyReturnDOList);


        // 每日收益率 列表
        List<BigDecimal> dailyReturnList = drawdownResult.dailyReturnList;


        // ------------------------------------------------------


        // 交易胜率
        TradePairStat.TradeStatResult tradeStatResult = TradePairStat.calcTradeWinPct(tradeRecordDOList);

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


        // 盈利/平局/亏损 天数占比   =   盈利/平局/亏损天数 / 总天数
        BigDecimal profitDayPct = of((double) drawdownResult.profitDayCount / totalDays * 100);
        BigDecimal drawDayPct = of((double) drawdownResult.drawDayCount / totalDays * 100);
        BigDecimal lossDayPct = of((double) drawdownResult.lossDayCount / totalDays * 100);


        // 盈亏比（日级） =  所有盈利日平均收益 / 所有亏损日平均亏损
        double avgWin = dailyReturnList.stream().filter(r -> r.doubleValue() > 0).mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double avgLoss = dailyReturnList.stream().filter(r -> r.doubleValue() < 0).mapToDouble(BigDecimal::doubleValue).map(Math::abs).average().orElse(0);

        BigDecimal profitFactor = avgLoss == 0 ? of(Double.POSITIVE_INFINITY) : of(avgWin / avgLoss);


        // ------------------------------------------------ 更新 bt_task


        taskDO.setTotalTrades(tradeStatResult.total);
        taskDO.setTotalTradeAmount(NumUtil.double2Decimal(tradeStatResult.totalTradeAmount));
        taskDO.setStatus(2);


        taskDO.setFinalCapital(finalCapital);
        taskDO.setFinalNav(finalNav);
        taskDO.setTotalDays(totalDays);
        taskDO.setTotalReturnPct(totalReturnPct);
        taskDO.setAnnualReturnPct(annualReturnPct);
        taskDO.setWinPct(of(winRate));
        taskDO.setProfitFactor(profitFactor);
        taskDO.setMaxDrawdownPct(drawdownResult.drawdownPct);
        taskDO.setProfitDaysPct(profitDayPct);
        taskDO.setDrawDaysPct(drawDayPct);
        taskDO.setLossDaysPct(lossDayPct);
        taskDO.setHoldPosDaysPct(of((double) drawdownResult.holdPosDayCount / totalDays * 100));
        taskDO.setClearPosDaysPct(of((double) drawdownResult.clearPosDayCount / totalDays * 100));
        taskDO.setAvgPosWhenHoldPct(of(drawdownResult.avgPosWhenHoldPct));
        taskDO.setAvgPosPct(of(drawdownResult.avgPosPct));
        taskDO.setAvgPosWhenHoldCount(of(drawdownResult.avgPosWhenHoldCount));
        taskDO.setAvgPosCount(of(drawdownResult.avgPosCount));
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
            // 平局天数
            else if (rec.getDailyReturn().doubleValue() == 0) {
                result.drawDayCount++;
            }
            // 亏损天数
            else if (rec.getDailyReturn().doubleValue() < 0) {
                result.lossDayCount++;
            }


            // -------------------------


            // 持仓天数
            if (rec.getPositionLimitPct().doubleValue() > 10 && rec.getPositionPct().doubleValue() > 0) {
                result.holdPosDayCount++;
            }
            // 空仓天数（收盘空仓 + 盘中空仓）
            else if ((rec.getPositionPct().doubleValue() == 0 && rec.getSellCapital().doubleValue() == 0)
                    // 仓位上限占比（%）< 5%   +   仓位占比（%）< 5%
                    || (rec.getPositionLimitPct().doubleValue() < 5 && rec.getPositionPct().doubleValue() < 5)) {
                result.clearPosDayCount++;
            } else {
                result.holdPosDayCount++;
            }


            // 总仓位（%）
            result.avgPosPct += rec.getPositionPct().doubleValue();


            // 持仓总数量
            result.avgPosCount += rec.getPosCount();


            // -------------------------


            // 每日收益率
            result.dailyReturnList.add(rec.getDailyReturn());
        }


        // 持仓平均仓位（%） =  持仓总仓位 /  持仓天数
        result.avgPosWhenHoldPct = result.avgPosPct / result.holdPosDayCount;
        // 总平均仓位（%） =  持仓总仓位 / 总天数
        result.avgPosPct = result.avgPosPct / list.size();


        // 持仓日均持股数量  =  持仓总数量 / 持仓天数
        result.avgPosWhenHoldCount = result.avgPosCount / result.holdPosDayCount;
        // 总日均持股数量  =  持仓总数量 / 总天数
        result.avgPosCount = result.avgPosCount / list.size();


        return result;
    }


    /**
     * 获取 某回测任务 在指定日期的   持仓详情
     *
     * @param taskId
     * @param tradeDate               统计 截止日期
     * @param today_tradeRecordDOList 当日 BS交易记录列表
     * @return
     */
    private List<BtPositionRecordDO> getDailyPositions(Long taskId,
                                                       LocalDate tradeDate,
                                                       List<BtTradeRecordDO> today_tradeRecordDOList) {


        // -------------------------------------------------------------------------------------------------------------


        // 1、全量 B/S记录     =>     当前B/S（未清仓）   +   历史B/S（已清仓）


        // 每次  ->  增量查询     +     历史记录（cache）


        today_tradeRecordDOList = today_tradeRecordDOList == null ? btTradeRecordService.listByTaskIdAndTradeDate(taskId, tradeDate) : today_tradeRecordDOList;


        today_tradeRecordDOList.forEach(e -> {

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
        holdingList__buyQueues__todayClearedCodes(tradeDate, tradeRecordList__cache.get(), todayHoldingList, todayClearMap, todaySellAndNotClearMap);


        // todayHoldingList 中即为“当日未清仓”的买入记录（quantity 已是剩余量）


        // -------------------------------------------------------------------------------------------------------------


        // 3. 汇总买卖
        Map<String, Integer> quantityMap = Maps.newHashMap();       // 个股持仓 -   总数量
        Map<String, Integer> avlQuantityMap = Maps.newHashMap();    // 个股持仓 - 可用数量（T+1）
        Map<String, Double> amountMap = Maps.newHashMap();          // 个股持仓 -   总成本（买入价格 x 买入数量   ->   累加）

        Map<String, PositionInfo> codeInfoMap = Maps.newHashMap();  // 个股持仓 - 首次买入Info

        Map<String, List<BtTradeRecordDO>> code_todayTradeRecords_Map = Maps.newHashMap(); // 个股 - 今日BS详情（今日b_price/s_price、qty）


        // --------------------------------------------


        // 成本计算
        quantityMap__avlQuantityMap__amountMap__codeInfoMap(tradeDate, todayHoldingList, quantityMap, avlQuantityMap, amountMap, codeInfoMap, code_todayTradeRecords_Map);


        // -------------------------------------------------------------------------------------------------------------


        // 4. 构造 当日持仓 对象列表
        List<BtPositionRecordDO> positionRecordDOList = todayPositionRecordList(taskId, tradeDate, quantityMap, avlQuantityMap, amountMap, codeInfoMap, todaySellAndNotClearMap, code_todayTradeRecords_Map);


        // -------------------------------------------------------------------------------------------------------------


        // 5. 构造 当日清仓 对象列表
        List<BtPositionRecordDO> todayClearPositionRecordDOList = todayClearPositionRecordList(taskId, tradeDate, todayClearMap);
        x.get().clearPositionRecordDOList = todayClearPositionRecordDOList;


        // -------------------------------------------------------------------------------------------------------------


        return positionRecordDOList;
    }


    /**
     * 优化 -> 清理 非持仓个股 的交易记录
     *
     *
     * 当 个股持仓 完全S时，移除个股（此日期之前的） 全部BS记录   ->   1、减少内存占用（1~5W条 历史BS记录）
     * -                                                      2、提升计算效率（refresh_statData   ->   getDailyPositions
     * -                                                                    每次匹配 遍历当前整个task 历史BS记录[已被抵消的BS记录]，完全多余[优化后：匹配遍历 1W -> 100条]）
     */
    private void clear__tradeRecordCache() {


        // 当前 持仓个股列表
        Set<String> positionStockCodeSet = x.get().positionStockCodeSet;

        if (CollectionUtils.isEmpty(positionStockCodeSet)) {
            return;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 清理 非持仓个股 的交易记录
        tradeRecordList__cache.get().removeIf(e -> {

            if (!positionStockCodeSet.contains(e.getStockCode())) {

                tradeRecord___idSet__cache.get().remove(e.getId());
                return true;
            }
            return false;
        });
    }


    /**
     * 持仓列表、清仓列表
     *
     * @param tradeDate               当前交易日
     * @param tradeRecordList         BS记录列表（按日期 有序：B单一定在 S单之前）
     * @param todayHoldingList        当日持仓（买入记录）列表   ->   当前B/S（抵消后 -> 未清仓）
     * @param todayClearMap           当日清仓列表             ->   清仓stockCode - 清仓（卖出记录）
     * @param todaySellAndNotClearMap 当日S但没一次性卖完（有且仅有一种情况：大盘仓位限制->等比减仓）
     */
    private void holdingList__buyQueues__todayClearedCodes(LocalDate tradeDate,
                                                           List<BtTradeRecordDO> tradeRecordList,
                                                           List<BtTradeRecordDO> todayHoldingList,
                                                           Map<String, BtTradeRecordDO> todayClearMap,
                                                           Map<String, BtTradeRecordDO> todaySellAndNotClearMap) {


        // 个股code  -  个股 买单队列   （构建 FIFO 队列：队列里存 剩余的买单）
        Map<String, Deque<BuyTradeRecord>> stockCode_buyQueue_map = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------


        // 遍历所有 BS记录，构建/抵销
        for (BtTradeRecordDO tr : tradeRecordList) {

            String stockCode = tr.getStockCode();
            int qty = tr.getQuantity();


            // BS记录列表（按日期 有序：B单一定在 S单之前）           个股S时  ->  其对应 B单 100%已经 先入队


            // 买入：入队
            if (Objects.equals(tr.getTradeType(), BtTradeTypeEnum.BUY.getTradeType())) {
                stockCode_buyQueue_map.computeIfAbsent(stockCode, k -> new LinkedList<>()).addLast(new BuyTradeRecord(tr, qty));
            }


            // 卖出：出队抵销
            else if (Objects.equals(tr.getTradeType(), BtTradeTypeEnum.SELL.getTradeType())) {


                // --------------------------------- 卖出：用 FIFO 队头   ->   买单抵销（tradeRecordList 有序：个股S时  ->  其对应的[S日期之前] B单 100%已经 先入队）


                // 当前个股   ->   买单queue
                Deque<BuyTradeRecord> b_queue = stockCode_buyQueue_map.get(stockCode);


                // 当前个股   ->   总持仓数量（当前 S日期之前  ->  全部B单 数量累加）
                int totalPosQty = 0;
                // 当前个股   ->   当笔卖单 卖出量
                int sell_qty = qty;


                // 遍历抵销 买单队列
                while (sell_qty > 0 && b_queue != null && !b_queue.isEmpty()) {


                    // 买单 依次出列   ->   抵消卖单
                    BuyTradeRecord b_head = b_queue.peekFirst();


                    // 总持仓数量
                    totalPosQty += b_head.posQty;


                    // 持有数量 > 当笔卖出量     =>     继续保留 该笔买单（未能1次卖光）
                    if (b_head.posQty > sell_qty) {

                        // 买单扣减（有剩余）    ->     剩余数量 = 持有数量 - 卖单数量
                        b_head.posQty -= sell_qty;

                        // 该笔卖单 -> 完全抵销
                        sell_qty = 0;
                    }


                    // 持有数量 <= 当笔卖出量     =>     该笔买单 完全抵销（1次卖光）
                    else {

                        // 卖单扣减（可能 有剩余）    ->     剩余数量 = 卖单数量 - 持有数量
                        sell_qty -= b_head.posQty;

                        // 该笔买单 -> 完全抵销（1次卖光）
                        b_queue.pollFirst();
                    }
                }


                // -----------------------------------------------------------------------------------------------------


                // 遍历抵销 全部B单后，sell_qty > 0     ->     说明 超卖
                Assert.isTrue(sell_qty == 0, String.format("超卖     >>>     股票[%s]   总持仓[%s]股   卖出[%d]股   ->   超卖[%d]股", stockCode, totalPosQty, sell_qty, sell_qty - totalPosQty));


                // -----------------------------------------------------------------------------------------------------


                // 当日卖出  +  卖光（买单全部被抵消 -> 持仓为0）  ->   记录 清仓标记
                if (tr.getTradeDate().isEqual(tradeDate) && CollectionUtils.isEmpty(b_queue)) {
                    todayClearMap.put(stockCode, tr);
                }


                // 当日卖出  +  未卖光（买单有剩余     =>     大盘仓位限制->等比减仓）
                if (tr.getTradeDate().isEqual(tradeDate) && CollectionUtils.isNotEmpty(b_queue)) {
                    // 当日S但没一次性卖完（有且仅有一种情况：大盘仓位限制->等比减仓）
                    todaySellAndNotClearMap.put(stockCode, tr);
                }
            }
        }


        // -------------------------------------------------------------------------------------------------------------


        // 当前 持仓个股 列表     ->     各队列所有   剩余的买单（抵消完 全部卖单后）


        for (Deque<BuyTradeRecord> queue : stockCode_buyQueue_map.values()) {

            for (BuyTradeRecord b_tr : queue) {


                // 转换回原DO，并把 quantity 调成剩余数量

                BtTradeRecordDO b_tr__original = new BtTradeRecordDO();
                BeanUtils.copyProperties(b_tr.original, b_tr__original);


                // “随意引入” Cache 带来的 指数级复杂性❗❗❗：
                //
                // 严禁 直接修改   b_tr.original  （b_tr  来源于  tradeRecordList__cache  缓存数据   ->   并非每次 从DB拉取
                //                                 =>   此处 setQuantity 修改后，tradeRecordList__cache 中的数据 被篡改❗❗❗  ->   后续计算会基于 此修改后的数据❗❗❗）
                //
                //       否则，❌bug：每次执行 refresh_statData   ->   getDailyPositions   ->   holdingList__buyQueues__todayClearedCodes
                //       都会修改1次   b_tr.original.quantity，导致后续计算错误   ->   ❌ 每执行1次，都要修改 抵扣1次 Quantity   ->   每次重复 抵扣 ❌


                // -----------------------------------------------------------------------------------------------------


                // 当前买单 b_tr__original     ->     剩余持仓量（当前个股 剩余持仓数量）  ->   抵消卖单后的 剩余数量
                b_tr__original.setQuantity(b_tr.posQty);


                // 剩余 持仓总金额  =  买入价格  x  剩余 持仓量
                double amount = b_tr__original.getPrice().doubleValue() * b_tr__original.getQuantity();
                b_tr__original.setAmount(NumUtil.double2Decimal(amount));


                // 当前 持仓个股   ->   买单列表（剩余 数量/金额   ->   持仓 数量/金额）         各买单 价格不变
                todayHoldingList.add(b_tr__original);
            }
        }
    }


    /**
     * 成本计算
     *
     * @param tradeDate                  当前交易日
     * @param todayHoldingList           当日持仓（买入记录）列表
     * @param quantityMap                个股持仓 -   总数量
     * @param avlQuantityMap             个股持仓 - 可用数量（T+1）
     * @param amountMap                  个股持仓 -   总成本（买入价格 x 买入数量   ->   累加）
     * @param codeInfoMap                个股持仓 - 首次买入Info
     * @param code_todayTradeRecords_Map 个股 - 当日交易记录列表
     */
    private void quantityMap__avlQuantityMap__amountMap__codeInfoMap(LocalDate tradeDate,
                                                                     List<BtTradeRecordDO> todayHoldingList,
                                                                     Map<String, Integer> quantityMap,
                                                                     Map<String, Integer> avlQuantityMap,
                                                                     Map<String, Double> amountMap,
                                                                     Map<String, PositionInfo> codeInfoMap,
                                                                     Map<String, List<BtTradeRecordDO>> code_todayTradeRecords_Map) {


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
            LocalDate tr_tradeDate = tr.getTradeDate();


            // ---------------------------------------------------------------------------------------------------------


            // 买入累加 / 卖出累减   ->   总数量、总成本
            int sign = Objects.equals(BtTradeTypeEnum.BUY.getTradeType(), tradeType) ? +1 : -1;


            // 个股持仓 - 总数量
            quantityMap.merge(stockCode, sign * quantity, Integer::sum);

            // 个股持仓 - 总成本
            amountMap.merge(stockCode, sign * amount, Double::sum);


            // ---------------------------------------------------------------------------------------------------------


            // T+1（🐶💩共产主义特色）
            if (sign == 1 && tr_tradeDate.isEqual(tradeDate)) {
                // 今日买入  =>  明日才可卖（今日 不可用  ->  +0 ）
                avlQuantityMap.merge(stockCode, 0, Integer::sum);
            } else {
                // 今日可用   ->   正常累加
                avlQuantityMap.merge(stockCode, sign * quantity, Integer::sum);
            }


            // ---------------------------------------------------------------------------------------------------------


            // 个股 - 当日交易记录列表
            if (tr_tradeDate.isEqual(tradeDate)) {
                code_todayTradeRecords_Map.computeIfAbsent(stockCode, k -> Lists.newArrayList()).add(tr);
            }


            // ---------------------------------------------------------------------------------------------------------


            PositionInfo positionInfo = codeInfoMap.get(stockCode);
            if (positionInfo == null) {

                positionInfo = new PositionInfo(stockId, stockCode, stockName, tr_tradeDate, tr.getPrice());
                codeInfoMap.put(stockCode, positionInfo);

            } else {


                // 更新  ->  最近一次  首次买入日期（用于计算 持仓天数）     =>     最近一次  avlQuantity = 0
                if (avlQuantityMap.get(stockCode) == 0) {
                    // 最近一次
                    LocalDate buyDate = tr_tradeDate.isBefore(positionInfo.buyDate) ? tr_tradeDate : positionInfo.buyDate;
                    positionInfo.setBuyDate(buyDate);
                    positionInfo.setInitBuyPrice(tr.getPrice());
                }
            }


        }
    }

    /**
     * 构造 当日持仓 对象列表
     *
     * @param taskId                     当前任务ID
     * @param tradeDate                  当前交易日
     * @param quantityMap                个股持仓 -   总数量
     * @param avlQuantityMap             个股持仓 - 可用数量（T+1）
     * @param amountMap                  个股持仓 -   总成本（买入价格 x 买入数量   ->   累加）
     * @param codeInfoMap                个股持仓 - 首次买入Info
     * @param todaySellAndNotClearMap    当日S但没一次性卖完（有且仅有一种情况：大盘仓位限制->等比减仓）
     * @param code_todayTradeRecords_Map 个股 - 当日交易记录列表
     * @return
     */
    private List<BtPositionRecordDO> todayPositionRecordList(Long taskId,
                                                             LocalDate tradeDate,
                                                             Map<String, Integer> quantityMap,
                                                             Map<String, Integer> avlQuantityMap,
                                                             Map<String, Double> amountMap,
                                                             Map<String, PositionInfo> codeInfoMap,
                                                             Map<String, BtTradeRecordDO> todaySellAndNotClearMap,
                                                             Map<String, List<BtTradeRecordDO>> code_todayTradeRecords_Map) {


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
                          taskId, tradeDate, stockCode, totalCost, qty, avgCost);
            }


            // ---------------------------------------------------------------------------------------------------------


            // 当日收盘价（实时价格   =>   开盘BS -> 开盘价 / 收盘BS -> 收盘价）
            double closePrice = getClosePrice(stockCode, tradeDate);
            // 昨日收盘价
            double prevClosePrice = getPrevClosePrice(stockCode, tradeDate);


            if (closePrice <= 0 || prevClosePrice <= 0) {
                log.error("getDailyPositions - closePrice err     >>>     [{}] {} {} , closePrice : {} , prevClosePrice : {}",
                          taskId, tradeDate, stockCode, closePrice, prevClosePrice);
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


            // 当日浮动盈亏
            double todayPnl = 0;
            // 当日浮动盈亏率（%）
            double todayPnlPct = 0;


            // 当日涨跌幅（%）
            double changePct = getChangePct(stockCode, tradeDate);


            double priceTotalReturnPct = 0;
            double priceMaxReturnPct = 0;
            double priceMaxDrawdownPct = 0;


            // 首次买入价格
            double initBuyPrice = positionInfo.initBuyPrice.doubleValue();


            // 个股 当日交易记录列表
            List<BtTradeRecordDO> today_tradeRecords = code_todayTradeRecords_Map.getOrDefault(stockCode, Lists.newArrayList());


            // ---------------------------------------------------------------------------------------------------------


            // 今日BS 持仓变动数量
            int todayBsQty = 0;


            // 今日BS变动
            for (BtTradeRecordDO tr : today_tradeRecords) {
                double price = tr.getPrice().doubleValue();
                int quantity = tr.getQuantity();


                // 当日买入
                if (tr.getTradeType() == 1) {
                    // 当日浮动盈亏 = 实时价 - 今日买入价
                    todayPnl += (closePrice - price) * quantity;
                    todayBsQty += quantity;
                }

                // 当日卖出（实际不会有S  ->  约定：B/S 互斥）
                if (tr.getTradeType() == 2) {
                    // 当日浮动盈亏 = 今日卖出价 - 昨日收盘价
                    todayPnl += (price - prevClosePrice) * quantity;
                    todayBsQty -= quantity;
                }
            }

            // 昨日->今日   未变持仓数量  =  当前持仓数量 - 今日BS变动数量
            double prevQty = qty - todayBsQty;
            if (prevQty < 0) {
                log.error("getDailyPositions - prevQty err     >>>     [{}] {} {} , qty : {} , todayBsQty : {} , prevQty : {}",
                          taskId, tradeDate, stockCode, qty, todayBsQty, prevQty);
            }
            // 昨日->今日   未变持仓盈亏 =  (今日收盘价 - 昨日收盘价) * 未变持仓数量
            todayPnl += (closePrice - prevClosePrice) * prevQty;


            // 当日浮动盈亏率 = 当日浮动盈亏 / 总成本 × 100%
            todayPnlPct = todayPnl / totalCost * 100;


            // ---------------------------------------------------------------------------------------------------------


            // 昨日持仓数量、成本
            BtPositionRecordDO prevPos = x.get().prev__stockCode_positionDO_Map.get(stockCode);
            if (prevPos != null) {


//                double prevAvgCostPrice = prevPos.getAvgCostPrice().doubleValue();
//                double prevClosePrice = prevPos.getClosePrice().doubleValue();
//                double prevQty = prevPos.getQuantity();
//
//                double prevTotalCost = prevAvgCostPrice * prevQty;
//                double prevMarketValue = prevClosePrice * prevQty;


                // -----------------------------------------------------------------------------------------------------


//                // 当日S但没一次性卖完（有且仅有一种情况：大盘仓位限制->等比减仓）
//                if (todaySellAndNotClearMap.containsKey(stockCode)) {
//                    // 总成本
//                    totalCost = prevTotalCost;
//                    // 平均成本
//                    avgCost = prevAvgCostPrice;
//                }


                // -----------------------------------------------------------------------------------------------------


//                // 昨日持仓部分的 当日浮动盈亏 = (今日收盘价 - 昨日收盘价) * 昨日持仓数量
//                double pnlFromYesterday = (closePrice - prevClosePrice) * prevQty;
//
//
//                // 今日新增买入部分的当日浮动盈亏 = (今日收盘价 - 今日买入价) * 今日买入数量
//                // 由于所有交易都发生在收盘价，因此 今日买入价 = 今日收盘价，当日浮盈=0
//                double pnlFromTodayBuy = 0;
//                // TODO   新增了 open_B -> close   =>   当日B 也会产生 浮动盈亏
//                // double pnlFromTodayBuy = (closePrice - today_open_price) * (qty - prevQty);
//
//
//                // 当日浮动盈亏总额
//                todayPnl = pnlFromYesterday + pnlFromTodayBuy;
//
//
//                // 当日浮动盈亏率 = 当日盈亏额 / 昨日持仓成本
//                // ⚠️ 注意：分母必须是昨日的成本，而不是今日总成本，否则会稀释掉当日盈亏
//                // todayPnlPct = (prevTotalCost > 0) ? (todayPnl * 100 / prevTotalCost) : 0;
//
//
//                // 当日浮动盈亏率 = 当日盈亏额 / 总成本
//                // ⚠️ 注意：分母必须是今日的总成本，今日新买入  ->  会等比例 稀释掉当日盈亏
//                // todayPnlPct = (totalCost > 0) ? (todayPnl / totalCost * 100) : 0;
//
//
//                // 当日浮动盈亏率 = (当日持仓市值 - 昨日持仓市值) / 昨日持仓市值 × 100%
//                // 当日浮动盈亏率 = 当日浮动盈亏 / 昨日持仓市值 × 100%
//                todayPnlPct = todayPnl / (prevMarketValue) * 100;


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
                              taskId, tradeDate, stockCode, todayPnlPct, todayPnl, totalCost, JSON.toJSONString(prevPos), JSON.toJSONString(todaySellAndNotClearMap.get(stockCode)));

                    // TODO   发现有 S后 剩余1股 bug   ->   已 fix（大盘仓位限制->等比减仓   bug）
                    todayPnlPct = Math.min(todayPnlPct, 9999.99);
                    todayPnlPct = Math.max(todayPnlPct, -9999.99);
                }


                // -----------------------------------------------------------------------------------------------------


                // 当日涨跌幅（%）
                // closeTodayReturnPct = (closePrice / prevClosePrice - 1) * 100;
                // closeTodayReturnPct = getChangePct(stockCode, tradeDate);


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
            positionRecordDO.setTradeDate(tradeDate);
            positionRecordDO.setStockId(positionInfo.stockId);
            positionRecordDO.setStockCode(stockCode);
            positionRecordDO.setStockName(positionInfo.stockName);


            // 主线板块
            Set<String> stock__blockCodeNameSet__inTopBlock = data.stock__inTopBlockCache.get(tradeDate, k -> Maps.newConcurrentMap()).get(stockCode);
            positionRecordDO.setTopBlockSet(JSON.toJSONString(stock__blockCodeNameSet__inTopBlock));


            positionRecordDO.setAvgCostPrice(of(avgCost));  // 成本价
            positionRecordDO.setClosePrice(of(closePrice)); // 实时价
            // 持仓数量
            positionRecordDO.setQuantity(qty);
            positionRecordDO.setAvlQuantity(avlQuantity);


            // 当前市值 = 持仓数量 x 当前收盘价
            positionRecordDO.setMarketValue(of(qty * closePrice)); // 实时市值

            // 仓位占比 = 持仓市值 / 总资金
            BigDecimal positionPct = positionRecordDO.getMarketValue().divide(of(x.get().capital), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            positionRecordDO.setPositionPct(of(Math.min(positionPct.doubleValue(), 9999.99)));


            // 当日盈亏额
            if (Double.isNaN(todayPnl)) {
                todayPnl = 0.0;
                log.error("todayPnl - err     >>>     taskId : {} , tradeDate : {} , stockCode : {}   ,   todayPnlPct : {} , todayPnl : {} ,totalCost : {} , prevPos : {} , todayTr : {}",
                          taskId, tradeDate, stockCode, todayPnlPct, todayPnl, totalCost, JSON.toJSONString(prevPos), JSON.toJSONString(todaySellAndNotClearMap.get(stockCode)));
            }
            positionRecordDO.setCapTodayPnl(of(todayPnl)); // 仅作为展示 -> 不参与任何计算
            // 当日盈亏率（%）
            positionRecordDO.setCapTodayPnlPct(of(todayPnlPct)); // 仅作为展示 -> 不参与任何计算

            // 累计盈亏额
            positionRecordDO.setCapTotalPnl(of(totalPnl)); // 仅作为展示 -> 不参与任何计算
            // 累计盈亏率（%） = 盈亏额 / 总成本  x 100%
            positionRecordDO.setCapTotalPnlPct(of(pnlPct)); // 仅作为展示 -> 不参与任何计算


            // 当日涨跌幅（%）
            positionRecordDO.setChangePct(of(changePct)); // 仅作为展示 -> 不参与任何计算

            // 首次买入价格-累计涨幅（%）
            positionRecordDO.setPriceTotalReturnPct(of(priceTotalReturnPct)); // 仅作为展示 -> 不参与任何计算
            // 首次买入价格-最大涨幅（%）
            positionRecordDO.setPriceMaxReturnPct(of(priceMaxReturnPct));
            // 首次买入价格-最大回撤（%）
            positionRecordDO.setPriceMaxDrawdownPct(of(priceMaxDrawdownPct));


            positionRecordDO.setBuyDate(positionInfo.buyDate);
            positionRecordDO.setHoldingDays(positionInfo.getHoldingDays(tradeDate, data.dateIndexMap));
            positionRecordDO.setBuyPrice(positionInfo.initBuyPrice);


            // ---------------------------------------------------------------------------------------------------------


            if (null == positionRecordDO.getPriceMaxDrawdownPct()) {
                log.error("todayPositionRecordList - getPriceMaxDrawdownPct err     >>>     taskId : {} , tradeDate : {} , stockCode : {} , positionRecordDO : {}",
                          taskId, tradeDate, stockCode, JSON.toJSONString(positionRecordDO));
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
     * @param tradeDate     当前交易日
     * @param todayClearMap 当日清仓列表             ->   清仓stockCode - 清仓（卖出记录）
     * @return
     */
    private List<BtPositionRecordDO> todayClearPositionRecordList(Long taskId,
                                                                  LocalDate tradeDate,
                                                                  Map<String, BtTradeRecordDO> todayClearMap) {


        List<BtPositionRecordDO> clearPositionRecordDOList = Lists.newArrayList();


        todayClearMap.forEach((stockCode, tr) -> {


            // 当日收盘价（实时价）
            double closePrice = getClosePrice(stockCode, tradeDate);


            // ---------------------------------------------------------------------------------------------------------
            //                                              计算当日浮动盈亏
            // ---------------------------------------------------------------------------------------------------------


            double todayPnl = 0;
            double todayPnlPct = 0;


            // 当日涨跌幅（%）
            double changePct = getChangePct(stockCode, tradeDate);


            double priceTotalReturnPct = 0;
            double priceMaxReturnPct = 0;
            double priceMaxDrawdownPct = 0;


            // 昨日持仓 数量、成本
            BtPositionRecordDO prevPos = x.get().prev__stockCode_positionDO_Map.get(stockCode);

            // 昨日必须 有持仓  ->  今日 才能S（T+1）
            if (prevPos == null) {
                return;
            }


            // 首次买入价格
            double initBuyPrice = prevPos.getBuyPrice().doubleValue();


            double prevAvgCostPrice = prevPos.getAvgCostPrice().doubleValue();
            double prevClosePrice = getPrevClosePrice(stockCode, tradeDate);
            int prevQty = prevPos.getQuantity();
            double prevMarketValue = prevPos.getMarketValue().doubleValue();
            double prevTotalCost = prevAvgCostPrice * prevQty;


            // ---------------------------------------------------------------------------------------------------------


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
                          taskId, tradeDate, stockCode, todayPnlPct, todayPnl, totalCost, JSON.toJSONString(prevPos), JSON.toJSONString(tr));

                // TODO   发现有 S后 剩余1股 bug   ->   已 fix（大盘仓位限制->等比减仓   bug）
                todayPnlPct = Math.min(todayPnlPct, 9999.99);
                todayPnlPct = Math.max(todayPnlPct, -9999.99);
            }


            // ---------------------------------------------------------------------------------------------------------


            // 当日涨跌幅（%）
//            closeTodayReturnPct = (closePrice / prevClosePrice - 1) * 100;
//            closeTodayReturnPct = getChangePct(stockCode, tradeDate);


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
            positionRecordDO.setTradeDate(tradeDate);
            positionRecordDO.setStockId(prevPos.getStockId());
            positionRecordDO.setStockCode(stockCode);
            positionRecordDO.setStockName(prevPos.getStockName());

            // 主线板块
            Set<String> stock__blockCodeNameSet__inTopBlock = data.stock__inTopBlockCache.get(tradeDate, k -> Maps.newConcurrentMap()).get(stockCode);
            positionRecordDO.setTopBlockSet(JSON.toJSONString(stock__blockCodeNameSet__inTopBlock));

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
            if (Double.isNaN(todayPnl)) {
                todayPnl = 0.0;
                log.error("todayPnl - err     >>>     taskId : {} , tradeDate : {} , stockCode : {}   ,   todayPnlPct : {} , todayPnl : {} ,totalCost : {} , prevPos : {}",
                          taskId, tradeDate, stockCode, todayPnlPct, todayPnl, totalCost, JSON.toJSONString(prevPos));
            }
            positionRecordDO.setCapTodayPnl(of(todayPnl));
            // 当日盈亏率（%）
            positionRecordDO.setCapTodayPnlPct(of(todayPnlPct));

            // 累计盈亏额
            positionRecordDO.setCapTotalPnl(of(totalPnl));
            // 累计盈亏率（%） = 盈亏额 / 总成本  x 100%
            positionRecordDO.setCapTotalPnlPct(of(pnlPct));


            // 当日涨跌幅（%）
            positionRecordDO.setChangePct(of(changePct));

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
                          taskId, tradeDate, stockCode, JSON.toJSONString(positionRecordDO));
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
     * 个股   指定日期 -> 涨跌幅（%）
     *
     * @param stockCode
     * @param tradeDate
     * @return
     */
    private double getChangePct(String stockCode, LocalDate tradeDate) {

        StockFun fun = data.getFun(stockCode);
        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
        KlineArrDTO klineArrDTO = fun.getKlineArrDTO();

        Integer idx = dateIndexMap.get(tradeDate);
        // TODO   当日停牌 -> 无法BS    （605255   2025-09-11）
        if (null == idx || idx <= 0) {
            log.error("getChangePct - err   [当日停牌 -> change_pct=0]     >>>     stockCode : {} , tradeDate : {}", stockCode, tradeDate);
            return 0.0;
        }

        return klineArrDTO.change_pct[idx];
    }


    /**
     * 个股   指定日期  ->  prev_收盘价（如果停牌 -> 则获取 前一个交易日的收盘价）
     *
     * @param stockCode
     * @param tradeDate
     * @return
     */
    private double getPrevClosePrice(String stockCode, LocalDate tradeDate) {
        LocalDate prev_date = tradeDateDecr(tradeDate);
        return getActClosePrice(stockCode, prev_date);

//        StockFun fun = data.getFun(stockCode);
//        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
//        KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
//
//        Integer idx = dateIndexMap.get(tradeDate);
//        if (idx == null || idx <= 0) {
//            return 0.0;
//        }
//
//
//        double prev_close = klineArrDTO.close[idx - 1];
//        return prev_close;
//
//
//        LocalDate prev_date = klineArrDTO.date[idx - 1];
//        return getClosePrice(stockCode, prev_date);
    }


    /**
     * 是否 开盘BS
     *
     * @param today_tradeDate
     * @return
     */

    private boolean is_openBS(LocalDate today_tradeDate) {
        BacktestOpenBSDTO bt_openBSDTO = btOpenBSDTO.get();
        return bt_openBSDTO != null && Objects.equals(bt_openBSDTO.next_date, today_tradeDate);
    }


    /**
     * 获取个股   指定日期 -> 实时价格（开盘BS->开盘价  /  收盘BS->收盘价）
     *
     * @param stockCode
     * @param tradeDate
     * @return
     */
    private double getClosePrice(String stockCode, LocalDate tradeDate) {
        if (is_openBS(tradeDate)) {
            double open = getOpenPrice(stockCode, tradeDate);
            log.info("getClosePrice - open     >>>     stockCode : {} , tradeDate : {} , close=open[{}]", stockCode, tradeDate, open);
            return open;
        }

        double close = getActClosePrice(stockCode, tradeDate);
        log.info("getClosePrice - close     >>>     stockCode : {} , tradeDate : {} , close=close[{}]", stockCode, tradeDate, close);
        return close;
    }


    /**
     * 个股   指定日期 -> 开盘价（如果停牌 -> 则获取 前一个交易日的开盘价）
     *
     * @param stockCode
     * @param tradeDate
     * @return
     */
    private double getOpenPrice(String stockCode, LocalDate tradeDate) {
        if (data.stock__dateOpenMap.get(stockCode) == null) {
            return 0.0;
        }


        Double openPrice = data.stock__dateOpenMap.get(stockCode).get(tradeDate);


        // 停牌（603039 -> 2023-04-03）
        int count = 0;
        while (openPrice == null && count++ < 500) {
            // 交易日 往前一位
            tradeDate = tradeDateDecr(tradeDate);
            openPrice = data.stock__dateOpenMap.get(stockCode).get(tradeDate);
        }


        return openPrice == null ? 0.0 : openPrice;
    }

    /**
     * 个股   指定日期 -> 收盘价（实时价/最新价）     （如果停牌 -> 则获取 前一个交易日的收盘价）
     *
     * @param stockCode
     * @param tradeDate
     * @return
     */
    private double getActClosePrice(String stockCode, LocalDate tradeDate) {
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
        x.set(new Stat());


        tradeRecord___idSet__cache.set(Sets.newHashSet());
        tradeRecordList__cache.set(Lists.newArrayList());


        log.info("--------------------------- data.stockDOList - before     >>>     size : {} , 线程 : {}",
                 ListUtil.size(data.stockDOList), Thread.currentThread().getName());


        // 仅明确 回测ETF   ->   可只加载 ETF数据（回测个股 -> 有大盘极限底 ETF抄底策略）
        Integer stockType = Objects.equals(btCompareDTO.get().getStockType(), StockTypeEnum.ETF.type) ? StockTypeEnum.ETF.type : null;


        // 全量行情
        data = initDataService.initData(startDate, endDate, stockType, false, 0);


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
        prev_dailyReturnDO__cache.remove();

        btCompareDTO.remove();
        btOpenBSDTO.remove();
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


        // --------------------------- refresh_statData__prev()


        // 1、清空
        x.remove();


        // 2、today -> prev
        x.get().prevCapital = last_dailyReturnDO.getCapital().doubleValue();
        x.get().prevAvlCapital = last_dailyReturnDO.getAvlCapital().doubleValue();
        x.get().prev__stockCode_positionDO_Map = stockCode_positionDO_Map;


        x.get().taskId = taskId;
//        x.get().tradeDate = tradeDate;


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

        // 当前 账户总资金   =   当前市值 + 当前可用资金    // 账户总资金  ->  仅与 今日市值/可用资金  相关❗❗❗
        double capital;   // 仅与 持仓个股 今日涨跌相关   =>   至[收盘时刻]   ->   个股 涨跌 已固定不变  ->  账户总资金 也不再变❗❗❗
        //
        // [收盘价BS]  =>  [收盘时刻]  ->  个股 涨跌[收盘价] 已固定不变  =>  S前  ->  账户总资金 已全程固定 不再变化
        //                                                         =>  S前  ->  持仓市值  =  S前_持仓市值（持仓个股 市值累加 -> [今日收盘价] x 持仓数量  累加）
        //                                                         =>  S前  ->  可用资金  =  prev_可用资金（昨日可用资金）
        //
        //
        //                                                      （S->B）阶段：
        //                                                        =>  变化的仅仅是  调仓换股（持仓市值 ⇋ 可用资金     互相 腾挪变换）
        //
        //                                                        =>  S后  ->  持仓市值  =  S前_持仓市值   -  S金额
        //                                                        =>  S后  ->  可用资金  =  prev_可用资金  +  S金额
        //
        //                                                        =>  B后  ->  持仓市值  =  S后_持仓市值  +  B金额
        //                                                        =>  B后  ->  可用资金  =  S后_可用资金  -  B金额


        // ------------------------------------------------------------


        // ----------------------------------------- 可变


        // 当前 持仓列表
        List<BtPositionRecordDO> positionRecordDOList;
        Set<String> positionStockCodeSet;
        Map<String, BtPositionRecordDO> stockCode_positionDO_Map = Maps.newHashMap();


        // 当前 持仓总市值   <=   仓位限制
        double marketValue;


        // 当前 可用资金   =   总资金 - 持仓总市值（✅ ->  随着个股涨跌 => 市值变化 -> 账户总资金 变化）
        // 当前 可用资金   =   昨日可用资金 + 卖出金额 - 买入金额（✅）    // 可用资金  ->  仅与 今日卖出/买入  相关❗❗❗
        double avlCapital;   // 以上计算方式 等价（若结果不一致  ->  BUG）

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

            this.sellCapital = calc_sellCapital();
            this.buyCapital = calc_buyCapital();


            // ------------------------------------------ 可变（持仓列表 -> 实时计算）


            // 当前 持仓列表
            this.positionRecordDOList = positionRecordDOList;
            this.positionStockCodeSet = calc_positionStockCodeSet();
            this.stockCode_positionDO_Map = calc_stockCode_positionDO_Map();


            this.marketValue = calc_marketValue();
            this.avlCapital = calc_avlCapital();
            this.actAvlCapital = calc_actAvlCapital();


            // ------------------------------------------


            // check
            checkStatData();
        }


        private void checkStatData() {

            // 总资金  =  总市值 + 可用资金
            double capital_2 = marketValue + avlCapital;
            if (!TdxFunCheck.equals(capital, capital_2)) {
                log.warn("check err - capital     >>>     [{}] [{}] , capital : {} , capital_2 : {}",
                         x.get().taskId, x.get().tradeDate, capital, capital_2);
            }

//            // 可用资金  =  prev_可用资金 + 卖出 - 买入
//            double avlCapital_2 = x.get().prevAvlCapital + sellCapital - buyCapital;
//            // 前置init阶段 -> 不校验 （今日收盘capital -> 还未计算）
//            if (tradeRecordDOList != null && !TdxFunCheck.equals(avlCapital, avlCapital_2, /*x.get().capital * 0.001*/ 1, 0.01)) {
//                log.warn("check err - avlCapital     >>>     [{}] [{}] , avlCapital : {} , avlCapital_2 : {}",
//                         x.get().taskId, x.get().tradeDate, avlCapital, avlCapital_2);
//            }
        }


        // ------------------------------------------------------------


        // -------------------------- 持仓


        private Set<String> calc_positionStockCodeSet() {
            // 持仓 code列表
            return positionRecordDOList.stream().map(BtPositionRecordDO::getStockCode).collect(Collectors.toSet());
        }


        private Map<String, BtPositionRecordDO> calc_stockCode_positionDO_Map() {
            return positionRecordDOList.stream().collect(Collectors.toMap(BtPositionRecordDO::getStockCode, e -> e));
        }

        private double calc_marketValue() {
            if (CollectionUtils.isEmpty(positionRecordDOList)) {
                return 0;
            }

            return positionRecordDOList.stream()
                                       .map(BtPositionRecordDO::getMarketValue)
                                       .reduce(BigDecimal.ZERO, BigDecimal::add)
                                       .doubleValue();
        }

        private double calc_avlCapital() {
            double avlCapital_1 = capital - marketValue;

            // 可用资金 = 昨日可用资金 + 今日卖出 - 今日买入
            double avlCapital_2 = x.get().prevAvlCapital + sellCapital - buyCapital; // 可用资金 仅与 今日卖出/买入 相关（可用资金 = 昨日可用资金 + 卖出 - 买入）


            // 前置init阶段 -> 不校验 （今日收盘capital -> 还未计算）
            if (tradeRecordDOList != null && !TdxFunCheck.equals(avlCapital_1, avlCapital_2, /*1000*/ 10, 0.01)) {
                log.warn("getAvlCapital err     >>>     [{}] [{}] , {} , {}", x.get().taskId, x.get().tradeDate, avlCapital_1, avlCapital_2);
            }


            if (tradeRecordDOList != null) {
                if (avlCapital_1 < -0.1 || avlCapital_2 < -0.1) {
                    log.error("getAvlCapital err[可用为负]     >>>     [{}] [{}] , {} , {}", x.get().taskId, x.get().tradeDate, avlCapital_1, avlCapital_2);
                }
            }


            return avlCapital_1;
        }

        private double calc_actAvlCapital() {
            return positionLimitAmount - marketValue;
        }


        // -------------------------- B/S


        private double calc_sellCapital() {
            if (CollectionUtils.isEmpty(tradeRecordDOList)) {
                return 0;
            }

            return tradeRecordDOList.stream()
                                    .filter(e -> e.getTradeType().equals(BtTradeTypeEnum.SELL.getTradeType()))
                                    .map(BtTradeRecordDO::getAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .doubleValue();
        }

        private double calc_buyCapital() {
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
        Set<String> positionStockCodeSet;
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


    private void copyBean(CalcStat stat, Stat x) {

//            x.prevCapital = stat.prevCapital;
//            x.prevAvlCapital = stat.prevAvlCapital;
//            x.prev__stockCode_positionDO_Map = stat.prev__stockCode_positionDO_Map;

//            x.taskId = stat.taskId;
//            x.tradeDate = stat.tradeDate;


        x.positionLimitRate = stat.positionLimitRate;
        x.positionLimitAmount = stat.positionLimitAmount;


        x.capital = stat.capital;


        x.positionRecordDOList = stat.positionRecordDOList;
        x.positionStockCodeSet = stat.positionStockCodeSet;
        x.stockCode_positionDO_Map = stat.stockCode_positionDO_Map;


//            x.clearPositionRecordDOList = stat.clearPositionRecordDOList;


        x.marketValue = stat.marketValue;
        x.avlCapital = stat.avlCapital;
        x.actAvlCapital = stat.actAvlCapital;

        x.sellCapital = stat.sellCapital;
        x.buyCapital = stat.buyCapital;
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
        return NumUtil.num2Decimal(val, 5);
    }


    /**
     * 辅助类：包装一条买入记录   及其 持有数量（剩余可抵销数量）
     **/
    @Data
    @AllArgsConstructor
    private static class BuyTradeRecord {
        // 买入记录（原始买单数据）
        final BtTradeRecordDO original;
        // b_tr__original  ->  持有数量（剩余可抵销数量）
        int posQty;
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
        // 平局天数
        public int drawDayCount;
        // 亏损天数
        public int lossDayCount;


        // -------------------------

        // 持仓天数
        public int holdPosDayCount;
        // 空仓天数
        public int clearPosDayCount;


        // 持仓平均仓位（%）
        public double avgPosWhenHoldPct;
        // 总平均仓位（%）
        public double avgPosPct;


        // 持仓日均持股数量
        public double avgPosWhenHoldCount;
        // 总日均持股数量
        public double avgPosCount;


        // -------------------------


        // 每日收益率 列表
        List<BigDecimal> dailyReturnList = Lists.newArrayList();
    }


    private void print__open_BS__close_BS(String openBS__closeBS) {

//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println("——————————————————————————————————————— " + openBS__closeBS + " ————————————————————————————————————————————————");
//        System.out.println("——————————————————————————————————————— " + openBS__closeBS + " ————————————————————————————————————————————————");
//        System.out.println("——————————————————————————————————————— " + openBS__closeBS + " ————————————————————————————————————————————————");
//        System.out.println("——————————————————————————————————————— " + openBS__closeBS + " ————————————————————————————————————————————————");
//        System.out.println("——————————————————————————————————————— " + openBS__closeBS + " ————————————————————————————————————————————————");
//        System.out.println("——————————————————————————————————————— " + openBS__closeBS + " ————————————————————————————————————————————————");
//        System.out.println("——————————————————————————————————————— " + openBS__closeBS + " ————————————————————————————————————————————————");
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
//        System.out.println();
    }


}