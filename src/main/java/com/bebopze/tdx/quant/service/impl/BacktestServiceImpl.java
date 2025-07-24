package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;


/**
 * 回测
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
    public Long backtest(LocalDate startDate, LocalDate endDate) {
        return backTestStrategy.backtest(startDate, endDate);
    }


    @Override
    public void checkBacktest(Long taskId) {


        BacktestCache data = backTestStrategy.getData();


        // task
        BtTaskDO taskDO = btTaskService.getById(taskId);
        Assert.notNull(taskDO, String.format("task不存在：%s", taskId));


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
                log.error("execBacktestDaily     >>>     taskId : {} , tradeDate : {} , exMsg : {}", taskDO.getId(), tradeDate, e.getMessage(), e);
            }
        }


    }

    @Override
    public Map analysis(Long taskId) {


        BacktestCache data = backTestStrategy.getData();


        // task
        BtTaskDO taskDO = btTaskService.getById(taskId);
        Assert.notNull(taskDO, String.format("task不存在：%s", taskId));


        List<BtPositionRecordDO> positionRecordDOList = btPositionRecordService.listByTaskIdAndTradeDateRange(taskId, taskDO.getStartDate(), taskDO.getEndDate());


        Map<LocalDate, Map<String, Set<String>>> date___block_stockSet_Map = new TreeMap<>();
        // Map<String, Set<String>> block_stockSet_Map = new HashMap<>();


        for (BtPositionRecordDO positionRecordDO : positionRecordDOList) {

            LocalDate tradeDate = positionRecordDO.getTradeDate();


            // 按 板块 分类
            String stockCode = positionRecordDO.getStockCode();
            String stockName = positionRecordDO.getStockName();
            String stockCodeName = stockCode + "-" + stockName;


            // String gn = data.getGn(stockCode);
            // String yjhyLv1 = data.getYjhyLv1(stockCode);
            String pthyLv2 = data.getPthyLv2(stockCode);


            // 日期 - resultMap（板块 - 个股列表）
            Map<String, Set<String>> block_stockSet_Map = date___block_stockSet_Map.computeIfAbsent(tradeDate, k -> Maps.newHashMap());
            // 板块 - 个股列表
            block_stockSet_Map.computeIfAbsent(pthyLv2, k -> Sets.newHashSet()).add(stockCodeName);
        }


        return date___block_stockSet_Map;
    }


//    private void execCheckBacktestDaily2(LocalDate preTradeDate, LocalDate tradeDate, BtTaskDO taskDO) {
//        Long taskId = taskDO.getId();
//
//        // 1️⃣ 校验交易记录的 amount 与 price × quantity
//        List<BtTradeRecordDO> tradeList = btTradeRecordService.listByTaskIdAndTradeDate(taskId, tradeDate);
//
//        double buyAmt = 0, sellAmt = 0;
//        for (BtTradeRecordDO tr : tradeList) {
//            double expect = tr.getPrice().doubleValue() * tr.getQuantity();
//            double actual = tr.getAmount().doubleValue();
//            if (!equals(expect, actual)) {
//                throw new AssertionError(String.format(
//                        "[%s] trade_id=%d amount mismatch: price*qty=%.2f but amount=%.2f",
//                        tradeDate, tr.getId(), expect, actual
//                ));
//            }
//            if (tr.getTradeType() == 1) buyAmt += actual;
//            else sellAmt += actual;
//        }
//
//        // 2️⃣ 校验持仓快照字段逻辑
//        List<BtPositionRecordDO> positionList = btPositionRecordService.listByTaskIdAndTradeDate(taskId, tradeDate);
//
//        double computedMarketValue = 0;
//        for (BtPositionRecordDO pos : positionList) {
//            // quantity * close_price == market_value
//            double expectMV = pos.getQuantity() * pos.getClosePrice().doubleValue();
//            if (!equals(expectMV, pos.getMarketValue().doubleValue())) {
//                throw new AssertionError(String.format(
//                        "[%s] pos_id=%d market value mismatch: qty×close=%.2f but market_value=%.2f",
//                        tradeDate, pos.getId(), expectMV, pos.getMarketValue().doubleValue()
//                ));
//            }
//            // pnl calculation
//            double expectPnl = (pos.getClosePrice().doubleValue() - pos.getAvgCostPrice().doubleValue())
//                    * pos.getQuantity();
//            if (!equals(expectPnl, pos.getUnrealizedPnl().doubleValue())) {
//                throw new AssertionError(String.format(
//                        "[%s] pos_id=%d unrealized_pnl mismatch: expect=%.2f actual=%.2f",
//                        tradeDate, pos.getId(), expectPnl, pos.getUnrealizedPnl().doubleValue()
//                ));
//            }
//            // pnl ratio
//            double cost = pos.getAvgCostPrice().doubleValue() * pos.getQuantity();
//            double expectRatio = cost == 0 ? 0 : expectPnl / cost;
//            if (!equals(expectRatio, pos.getUnrealizedPnlRatio().doubleValue())) {
//                throw new AssertionError(String.format(
//                        "[%s] pos_id=%d pnl ratio mismatch: expect=%.4f actual=%.4f",
//                        tradeDate, pos.getId(), expectRatio, pos.getUnrealizedPnlRatio().doubleValue()
//                ));
//            }
//            computedMarketValue += pos.getMarketValue().doubleValue();
//        }
//
//        // 3️⃣ 校验每日收益表
//        BtDailyReturnDO daily = btDailyReturnService.getByTaskIdAndTradeDate(taskId, tradeDate);
//        if (daily == null) {
//            throw new AssertionError(tradeDate + " missing daily return record");
//        }
//
//        // 校验字段一致性
//        if (!equals(daily.getBuyCapital().doubleValue(), buyAmt)) {
//            throw new AssertionError(String.format(
//                    "[%s] buy_capital mismatch: record=%.2f sumTrades=%.2f",
//                    tradeDate, daily.getBuyCapital().doubleValue(), buyAmt
//            ));
//        }
//        if (!equals(daily.getSellCapital().doubleValue(), sellAmt)) {
//            throw new AssertionError(String.format(
//                    "[%s] sell_capital mismatch: record=%.2f sumTrades=%.2f",
//                    tradeDate, daily.getSellCapital().doubleValue(), sellAmt
//            ));
//        }
//        if (!equals(computedMarketValue, daily.getMarketValue().doubleValue())) {
//            throw new AssertionError(String.format(
//                    "[%s] market_value mismatch: expect=%.2f record=%.2f",
//                    tradeDate, computedMarketValue, daily.getMarketValue().doubleValue()
//            ));
//        }
//
//        // 校验 capital = market_value + avl_capital
//        double expectCapital = computedMarketValue + daily.getAvlCapital().doubleValue();
//        if (!equals(expectCapital, daily.getCapital().doubleValue())) {
//            throw new AssertionError(String.format(
//                    "[%s] capital mismatch: expect=%.2f record=%.2f",
//                    tradeDate, expectCapital, daily.getCapital().doubleValue()
//            ));
//        }
//
//        // 计算 profit_loss_amount = capital - prevCapital
//        BtDailyReturnDO prevDaily = btDailyReturnService.getByTaskIdAndTradeDate(taskId, preTradeDate);
//        double prevCapital = prevDaily != null
//                ? prevDaily.getCapital().doubleValue()
//                : taskDO.getInitialCapital().doubleValue();  // 第一日回测以 initial_capital 为前值
//
//        double expectPL = daily.getCapital().doubleValue() - prevCapital;
//        if (!equals(expectPL, daily.getProfitLossAmount().doubleValue())) {
//            throw new AssertionError(String.format(
//                    "[%s] profit_loss_amount mismatch: expect=%.2f record=%.2f",
//                    tradeDate, expectPL, daily.getProfitLossAmount().doubleValue()
//            ));
//        }
//
//        // daily_return = profit_loss_amount / prevCapital
//        double expectReturn = prevCapital == 0 ? 0 : expectPL / prevCapital;
//        if (!equals(expectReturn, daily.getDailyReturn().doubleValue())) {
//            throw new AssertionError(String.format(
//                    "[%s] daily_return mismatch: expect=%.6f record=%.6f",
//                    tradeDate, expectReturn, daily.getDailyReturn().doubleValue()
//            ));
//        }
//
//        // nav = capital / initial_capital
//        double expectNav = daily.getCapital().doubleValue() / taskDO.getInitialCapital().doubleValue();
//        if (!equals(expectNav, daily.getNav().doubleValue())) {
//            throw new AssertionError(String.format(
//                    "[%s] nav mismatch: expect=%.6f record=%.6f",
//                    tradeDate, expectNav, daily.getNav().doubleValue()
//            ));
//        }
//
//        log.info("[{}] check passed: buy=%.2f sell=%.2f mv=%.2f avl=%.2f cap=%.2f pnl=%.2f ret=%.4f nav=%.4f",
//                 tradeDate,
//                 daily.getBuyCapital(), daily.getSellCapital(),
//                 daily.getMarketValue(), daily.getAvlCapital(),
//                 daily.getCapital(), daily.getProfitLossAmount(),
//                 daily.getDailyReturn(), daily.getNav()
//        );
//    }


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


            double unrealizedPnl = positionRecordDO.getUnrealizedPnl().doubleValue();
            double unrealizedPnlRatio = positionRecordDO.getUnrealizedPnlRatio().doubleValue();

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


    @Override
    public void holdingStockRule(String stockCode) {

        // 买入    - 总金额

        // 当前/S  - 总金额


        // 差价 = 当前/S - B


        // 所有个股  差价累加


        strategyService.holdingStockRule(stockCode);
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
