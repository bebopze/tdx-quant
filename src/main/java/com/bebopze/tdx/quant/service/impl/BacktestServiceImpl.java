package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.ThreadPoolType;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestAnalysisDTO;
import com.bebopze.tdx.quant.common.domain.dto.backtest.MaxDrawdownPctDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.ParallelCalcUtil;
import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import com.bebopze.tdx.quant.dal.service.IBtDailyReturnService;
import com.bebopze.tdx.quant.dal.service.IBtPositionRecordService;
import com.bebopze.tdx.quant.dal.service.IBtTaskService;
import com.bebopze.tdx.quant.dal.service.IBtTradeRecordService;
import com.bebopze.tdx.quant.parser.check.TdxFunCheck;
import com.bebopze.tdx.quant.service.BacktestService;
import com.bebopze.tdx.quant.service.StrategyService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.bebopze.tdx.quant.strategy.buy.BuyStrategy__ConCombiner;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.guava.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * 策略 - 回测
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
@Slf4j
@Service
public class BacktestServiceImpl implements BacktestService {


    @Autowired
    private BacktestStrategy backTestStrategy;

    @Autowired
    private StrategyService strategyService;


    @Autowired
    private IBtTaskService btTaskService;

    @Autowired
    private IBtTradeRecordService btTradeRecordService;

    @Autowired
    private IBtPositionRecordService btPositionRecordService;

    @Autowired
    private IBtDailyReturnService btDailyReturnService;


    @Override
    public void execBacktest(LocalDate startDate, LocalDate endDate, boolean resume, Integer batchNo) {


        List<List<String>> buy_conCombinerList = BuyStrategy__ConCombiner.generateCombinations(2);
        // List<List<String>> sell_conCombinerList = SellStrategy__ConCombiner.generateCombinations();


        // Sell策略 ： 暂时固定
        // List<String> sellConList = Lists.newArrayList("月空_MA20空", "SSF空", "高位爆量上影大阴", "C_SSF_偏离率>25%");
        List<String> sellConList = Lists.newArrayList("个股S", "板块S", "主线S");


        // 主线策略
        // TopBlockStrategyEnum topBlockStrategyEnum = TopBlockStrategyEnum.LV2;


        // -------------------------------------------------------------------------------------------------------------


        Set<String> finishSet = Sets.newHashSet();


        // 中断 -> 恢复执行
        BtTaskDO batchNoEntity = filterFinishTaskList(startDate, endDate, resume, batchNo, finishSet);


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // 同一批次  ->  日期一致性
        LocalDate finalStartDate = batchNoEntity.getStartDate();
        LocalDate finalEndDate = batchNoEntity.getEndDate();

        Integer finalBatchNo = batchNoEntity.getBatchNo();


        // -------------------------------------------------------------------------------------------------------------


        // -----------------------------------------------------------------------------


        AtomicInteger current = new AtomicInteger();
        int total = buy_conCombinerList.size();


        ParallelCalcUtil.forEach(buy_conCombinerList,


                                 buyConList -> {
                                     long start = System.currentTimeMillis();


                                     Arrays.stream(TopBlockStrategyEnum.values())
                                           // 暂无 LV1 主线策略
                                           .filter(e -> !e.equals(TopBlockStrategyEnum.LV1))
                                           .filter(e -> !finishSet.contains(getKey(finalBatchNo, e.getDesc(), buyConList, sellConList)))
                                           .forEach(topBlockStrategyEnum -> backTestStrategy.backtest(finalBatchNo, topBlockStrategyEnum, buyConList, sellConList, finalStartDate, finalEndDate));


                                     progressLog(finalBatchNo, current.incrementAndGet(), total, start);
                                 },

                                 // ThreadPoolType.CPU_INTENSIVE);
                                 ThreadPoolType.IO_INTENSIVE);
    }


    @Override
    public void execBacktestUpdate(Integer batchNo, List<Long> taskIdList, LocalDate startDate, LocalDate endDate) {


        checkAndGetTaskIdList(batchNo, taskIdList, startDate, endDate);


        ParallelCalcUtil.forEach(taskIdList,


                                 taskId -> {
                                     // long start = System.currentTimeMillis();


                                     backTestStrategy.backtest_update(taskId, startDate, endDate);


                                     // progressLog(finalBatchNo, current.incrementAndGet(), total, start);
                                 },


                                 // ThreadPoolType.CPU_INTENSIVE);
                                 ThreadPoolType.IO_INTENSIVE);
    }


    @Override
    public Long backtest2(TopBlockStrategyEnum topBlockStrategyEnum,
                          List<String> buyConList,
                          LocalDate startDate,
                          LocalDate endDate,
                          boolean resume,
                          Integer batchNo) {


        // Sell策略 ： 暂时固定
        // List<String> sellConList = Lists.newArrayList("月空_MA20空", "SSF空", "高位爆量上影大阴", "C_SSF_偏离率>25%");
        List<String> sellConList = Lists.newArrayList("个股S", "板块S", "主线S");


        // -------------------------------------------------------------------------------------------------------------


        return backTestStrategy.backtest(0, topBlockStrategyEnum, buyConList, sellConList, startDate, endDate);
    }


    @Override
    public Long backtestTrade(TopBlockStrategyEnum topBlockStrategyEnum,
                              LocalDate startDate,
                              LocalDate endDate,
                              boolean resume,
                              Integer batchNo) {


        // B/S策略
        List<String> buyConList = Lists.newArrayList("N100日新高", "月多");
        List<String> sellConList = Lists.newArrayList("个股S", "板块S", "主线S");


        // -------------------------------------------------------------------------------------------------------------

        resume = false;
        batchNo = 0;

        // -----------------------------------------------------------------------------


        return backTestStrategy.backtest(batchNo, topBlockStrategyEnum, buyConList, sellConList, startDate, endDate);
    }


    /**
     * 中断 -> 恢复执行          =>          过滤 已[finish]   ->   继续执行 未完成（del -> 未完成）/未进行 buyConList
     *
     * @param startDate
     * @param endDate
     * @param resume
     * @param batchNo
     * @param finishSet
     * @return
     */
    private BtTaskDO filterFinishTaskList(LocalDate startDate,
                                          LocalDate endDate,
                                          boolean resume,
                                          Integer batchNo,
                                          Set<String> finishSet) {


        // 任务批次号 - last
        BtTaskDO lastBatchNoEntity = btTaskService.getLastBatchNoEntity();

        Integer lastBatchNo = lastBatchNoEntity.getBatchNo();
        batchNo = batchNo == null ? lastBatchNo : batchNo;


        Assert.isTrue(batchNo <= lastBatchNo, String.format(" [任务批次号=%s]非法，当前[最大任务批次号=%s]", batchNo, lastBatchNo));


        BtTaskDO batchNoEntity = btTaskService.getBatchNoEntityByBatchNo(batchNo);
        Assert.notNull(batchNoEntity, String.format(" [任务批次号=%s]不存在，当前[最大任务批次号=%s]", batchNo, lastBatchNo));


        // -------------------------------------------------------------------------------------------------------------


        // 中断恢复
        if (!resume) {

            // 重新开一局
            BtTaskDO new_batchNoEntity = new BtTaskDO();

            new_batchNoEntity.setBatchNo(lastBatchNo + 1);
            new_batchNoEntity.setStartDate(startDate);
            new_batchNoEntity.setEndDate(endDate);

            return new_batchNoEntity;
        }


        // -------------------------------------------------------------------------------------------------------------


        // finish list
        List<BtTaskDO> finishTaskList = btTaskService.listByBatchNoAndStatus(batchNo, 2);


        finishTaskList.forEach(e -> {
            String key = getKey(e.getBatchNo(), e.getTopBlockStrategy(), Arrays.asList(e.getBuyStrategy().split(",")), Arrays.asList(e.getSellStrategy().split(",")));
            finishSet.add(key);
        });


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------- 3-待更新至 最新交易日；


        // finishTask     =>     endDate  <  last_tradeDate       ->     接着更新至 最新一个 交易日
        LocalDate last_tradeDate = LocalDate.now();// 直接用 now   ->   最后一个 交易日  即可（后续会 自动校正 至真实 last_tradeDate）


        finishTaskList.forEach(e -> {

            if (e.getEndDate().isBefore(last_tradeDate)) {

                // end_date + 1   ~   last_tradeDate
                e.setStartDate2(e.getEndDate().plusDays(1));
                e.setEndDate2(last_tradeDate);

                // 3-待更新至 最新交易日；
                e.setStatus(3);
            }
        });


        // TODO   支持 刷新  指定时间段内的 回测数据

        // 1、del   ->   指定时间段   回测数据
        // 2、重置   ->   date2   +   status=3


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------- 1-进行中（DEL -> 新开任务）


        // del errTask
        int count = btTaskService.delErrTaskByBatchNo(batchNo);


        return batchNoEntity;
    }


    private String getKey(Integer batchNo,
                          String topBlockStrategyEnumDesc,
                          List<String> buyConList,
                          List<String> sellConList) {

        return batchNo + "|" + topBlockStrategyEnumDesc + "|" + buyConList + "|" + sellConList;
    }

    private void progressLog(Integer batchNo, int current, int total, long start) {
        String msg = "Completed " + current + "/" + total + " chunks     耗时：" + DateTimeUtil.formatNow2Hms(start);
        log.info("📊 [批次号={}] 进度: {}/{} {}% | {}", batchNo, current, total, NumUtil.of(current * 100.0 / total), msg);
    }


    private void checkAndGetTaskIdList(Integer batchNo, List<Long> taskIdList, LocalDate startDate, LocalDate endDate) {
        Assert.isTrue(!startDate.isAfter(LocalDate.now()), String.format("开始日期=[%s]不能大于今日", startDate));
        Assert.isTrue(!startDate.isAfter(endDate), String.format("开始日期=[%s]不能大于结束日期=[%s]", startDate, endDate));


        if (null != batchNo) {

            List<Long> batchNo_taskIdList = btTaskService.listIdByBatchNoAndStatus(batchNo, null);
            Assert.notEmpty(batchNo_taskIdList, String.format("[任务批次号=%s]不存在", batchNo));

            taskIdList.clear();
            taskIdList.addAll(batchNo_taskIdList);

        } else {
            Assert.notEmpty(taskIdList, "taskIdList不能为空");
        }
    }


    @Override
    public void checkBacktest(Long taskId) {

        // task
        BtTaskDO taskDO = btTaskService.getById(taskId);
        Assert.notNull(taskDO, String.format("task不存在：%s", taskId));


        // cache
        BacktestCache data = backTestStrategy.getInitDataService().initData(taskDO.getStartDate(), taskDO.getEndDate(), false);


        // date
        LocalDate tradeDate = taskDO.getStartDate().minusDays(1);
        LocalDate endDate = DateTimeUtil.min(taskDO.getEndDate(), data.endDate());


        // ---------------------------------------------------------


        while (tradeDate.isBefore(endDate)) {

            LocalDate preTradeDate = tradeDate;
            tradeDate = backTestStrategy.tradeDateIncr(tradeDate);


            try {
                // 每日 - 回测（B/S）  check
                execCheckBacktestDaily(preTradeDate, tradeDate, taskDO);
            } catch (Exception e) {
                log.error("execCheckBacktestDaily     >>>     taskId : {} , tradeDate : {} , exMsg : {}", taskDO.getId(), tradeDate, e.getMessage(), e);
            }
        }


    }

    @Override
    public List<BtTaskDO> listTask(Long taskId,
                                   List<Integer> batchNoList,
                                   LocalDateTime startCreateTime,
                                   LocalDateTime endCreateTime) {

        return btTaskService.listByTaskId(taskId, batchNoList, startCreateTime, endCreateTime);
    }

    @Override
    public BacktestAnalysisDTO analysis(Long taskId) {


        // task
        BtTaskDO taskDO = btTaskService.getById(taskId);
        Assert.notNull(taskDO, String.format("task不存在：%s", taskId));


        BacktestAnalysisDTO dto = new BacktestAnalysisDTO();

        dto.setTask(taskDO);
        dto.setTradeRecordList(btTradeRecordService.listByTaskId(taskId));

        List<BtPositionRecordDO> allPositionRecordDOList = btPositionRecordService.listByTaskId(taskId);
        dto.setPositionRecordList(allPositionRecordDOList.stream().filter(e -> e.getPositionType() == 1).collect(Collectors.toList()));
        dto.setClearPositionRecordList(allPositionRecordDOList.stream().filter(e -> e.getPositionType() == 2).collect(Collectors.toList()));

        dto.setDailyReturnList(btDailyReturnService.listByTaskId(taskId));
        dto.setDailyDrawdownPctList(dailyDrawdownPctList(dto.getDailyReturnList()));


        return dto;
    }


    @Override
    public int delErrTaskByBatchNo(Integer batchNo) {
        return btTaskService.delErrTaskByBatchNo(batchNo);
    }

    @Override
    public int deleteByTaskIds(List<Long> taskIdList) {
        return btTaskService.delErrTaskByTaskIds(taskIdList);
    }

    @Override
    public List<BtTradeRecordDO> stockTradeRecordList(Long taskId, String stockCode) {
        return btTradeRecordService.listByTaskIdAndStockCode(taskId, stockCode);
    }


    private void execCheckBacktestDaily(LocalDate preTradeDate, LocalDate tradeDate, BtTaskDO taskDO) {

        // 首日
        if (preTradeDate.isBefore(taskDO.getStartDate())) {
            return;
        }


        Long taskId = taskDO.getId();


        // --------------------------------------------


        // 交易记录
        List<BtTradeRecordDO> tradeRecordDOList = btTradeRecordService.listByTaskIdAndTradeDate(taskId, tradeDate);


        double buyCapital_check = 0.0;
        double sellCapital_check = 0.0;

        for (BtTradeRecordDO tradeRecordDO : tradeRecordDOList) {

            // 交易类型：1-买入；2-卖出；
            Integer tradeType = tradeRecordDO.getTradeType();


            double amount = tradeRecordDO.getAmount().doubleValue();

            double price = tradeRecordDO.getPrice().doubleValue();
            int quantity = tradeRecordDO.getQuantity();

            // double amount_check = price * quantity;
            int quantity_check = (int) (amount / price);


            Assert.isTrue(intEquals(quantity_check, quantity),
                          String.format("amount_check err     >>>     %s / %s = quantity_check : %s , amount : %s", amount, price, quantity_check, quantity));


            if (tradeType == 1) {
                buyCapital_check += amount;
            } else if (tradeType == 2) {
                sellCapital_check += amount;
            }
        }


        // 持仓记录
        List<BtPositionRecordDO> positionRecordDOList = btPositionRecordService.listByTaskIdAndTradeDate(taskId, tradeDate);


        double totalMarketValue_check = 0.0;
        for (BtPositionRecordDO positionRecordDO : positionRecordDOList) {

            double avgCostPrice = positionRecordDO.getAvgCostPrice().doubleValue();
            double closePrice = positionRecordDO.getClosePrice().doubleValue();

            int quantity = positionRecordDO.getQuantity();
            int avlQuantity = positionRecordDO.getAvlQuantity();
            double marketValue = positionRecordDO.getMarketValue().doubleValue();


            double capTotalPnl = positionRecordDO.getCapTotalPnl().doubleValue();
            double capTotalPnlPct = positionRecordDO.getCapTotalPnlPct().doubleValue();

            LocalDate buyDate = positionRecordDO.getBuyDate();
            int holdingDays = positionRecordDO.getHoldingDays();


            // ---------------------------------- 汇总
            totalMarketValue_check += marketValue;
        }


        // 每日收益
        BtDailyReturnDO dailyReturnDO = btDailyReturnService.getByTaskIdAndTradeDate(taskId, tradeDate);


        double marketValue = dailyReturnDO.getMarketValue().doubleValue();
        double capital = dailyReturnDO.getCapital().doubleValue();

        double avlCapital = dailyReturnDO.getAvlCapital().doubleValue();

        double buyCapital = dailyReturnDO.getBuyCapital().doubleValue();
        double sellCapital = dailyReturnDO.getSellCapital().doubleValue();


        double profitLossAmount = dailyReturnDO.getProfitLossAmount().doubleValue();
        double dailyReturn = dailyReturnDO.getDailyReturn().doubleValue();
        double nav = dailyReturnDO.getNav().doubleValue();


        // ---------------------------------- pre


        BtDailyReturnDO pre_dailyReturnDO = btDailyReturnService.getByTaskIdAndTradeDate(taskId, preTradeDate);


        double pre_marketValue = pre_dailyReturnDO.getMarketValue().doubleValue();
        double pre_capital = pre_dailyReturnDO.getCapital().doubleValue();

        double pre_avlCapital = pre_dailyReturnDO.getAvlCapital().doubleValue();

        double pre_buyCapital = pre_dailyReturnDO.getBuyCapital().doubleValue();
        double pre_sellCapital = pre_dailyReturnDO.getSellCapital().doubleValue();


        double pre_profitLossAmount = pre_dailyReturnDO.getProfitLossAmount().doubleValue();
        double pre_dailyReturn = pre_dailyReturnDO.getDailyReturn().doubleValue();
        double pre_nav = pre_dailyReturnDO.getNav().doubleValue();


        // ---------------------------------- 汇总


        // 今日可用  =  昨日可用 + 今日卖出 - 今日买入
        double avlCapital_check = pre_avlCapital + sellCapital_check - buyCapital_check;

        // 今日总资金  =  总市值 + 今日可用
        double capital_check = totalMarketValue_check + avlCapital_check;


        // 当日盈亏额 = 当日总资金 - 昨日总资金
        double profitLossAmount_check = capital_check - pre_capital;
        // 当日收益率 = 当日盈亏额 / 昨日总资金
        double dailyReturn_check = profitLossAmount_check / pre_capital;
        // 当日净值 = 今日总资金 / 本金
        double nav_check = capital_check / taskDO.getInitialCapital().doubleValue();


        // --------------------------------------------------------------------


        Assert.isTrue(amountEquals(buyCapital_check, buyCapital),
                      String.format("check DailyReturn err     >>>     buyCapital_check : %s , buyCapital : %s", buyCapital_check, buyCapital));

        Assert.isTrue(amountEquals(sellCapital_check, sellCapital),
                      String.format("check DailyReturn err     >>>     sellCapital_check : %s , sellCapital : %s", sellCapital_check, sellCapital));

        // --------------

        Assert.isTrue(amountEquals(totalMarketValue_check, marketValue),
                      String.format("check DailyReturn err     >>>     totalMarketValue : %s , marketValue : %s", totalMarketValue_check, marketValue));


        Assert.isTrue(amountEquals(avlCapital_check, avlCapital),
                      String.format("check DailyReturn err     >>>     avlCapital_check : %s , avlCapital : %s", avlCapital_check, avlCapital));


        Assert.isTrue(amountEquals(capital_check, capital),
                      String.format("check DailyReturn err     >>>     capital_check : %s , capital : %s", capital_check, capital));


        // --------------

        Assert.isTrue(amountEquals(profitLossAmount_check, profitLossAmount),
                      String.format("check DailyReturn err     >>>     profitLossAmount_check : %s , profitLossAmount : %s", profitLossAmount_check, profitLossAmount));

        Assert.isTrue(equals(dailyReturn_check, dailyReturn),
                      String.format("check DailyReturn err     >>>     dailyReturn_check : %s , dailyReturn : %s", dailyReturn_check, dailyReturn));

        Assert.isTrue(equals(nav_check, nav),
                      String.format("check DailyReturn err     >>>     nav_check : %s , nav : %s", nav_check, nav));
    }


    @Deprecated
    @Override
    public void holdingStockRule(String stockCode) {

        // 买入    - 总金额

        // 当前/S  - 总金额


        // 差价 = 当前/S - B


        // 所有个股  差价累加


        strategyService.holdingStockRule(stockCode);
    }


    // -----------------------------------------------------------------------------------------------------------------


    private List<MaxDrawdownPctDTO> dailyDrawdownPctList(List<BtDailyReturnDO> dailyReturnList) {


        double maxNav = 0;
        LocalDate maxDate = null;

        double maxDrawdownPct = 0;
        LocalDate minDate = null;
        double minNav = 0;


        List<MaxDrawdownPctDTO> dtoList = Lists.newArrayList();


        for (BtDailyReturnDO e : dailyReturnList) {


            LocalDate tradeDate = e.getTradeDate();
            double nav = e.getNav().doubleValue();


            // 最大净值
            if (maxNav < nav) {
                maxNav = nav;
                maxDate = tradeDate;
            }


            // 当日回撤（负数）
            double drawdownPct = (nav / maxNav - 1) * 100;


            // 最大回撤
            if (maxDrawdownPct > drawdownPct) {
                maxDrawdownPct = drawdownPct;
                minDate = tradeDate;
                minNav = nav;
            }


            // -----------------------------------------------------------------


            MaxDrawdownPctDTO dto = new MaxDrawdownPctDTO();

            dto.setTradeDate(tradeDate);
            dto.setDrawdownPct(NumUtil.double2Decimal(drawdownPct));


            dto.setMaxNav(NumUtil.double2Decimal(maxNav, 4));
            dto.setMaxNavDate(maxDate);


            dto.setMaxDrawdownPct(NumUtil.double2Decimal(maxDrawdownPct));
            dto.setMaxDrawdownDate(minDate);
            dto.setMaxDrawdownNav(NumUtil.double2Decimal(minNav, 4));


            dtoList.add(dto);
        }


        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    private static boolean equals(Number a, Number b) {
        // ±1% 误差
        return TdxFunCheck.equals(a, b, 0.02, 0.01);
    }

    private static boolean amountEquals(double a, double b) {
        return Math.abs(a - b) <= 1 || TdxFunCheck.equals(a, b, 500, 0.01);
    }

    private static boolean intEquals(double a, double b) {
        return Math.abs(a - b) <= 1 || equals(a, b);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        boolean equals = amountEquals(-1.4799999999813735, -1.5);
        System.out.println(equals);
    }


}
