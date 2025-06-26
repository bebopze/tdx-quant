package com.bebopze.tdx.quant.strategy.backtest;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.constant.BtTradeTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategy;
import com.bebopze.tdx.quant.strategy.buy.BuyStockStrategy;
import com.bebopze.tdx.quant.strategy.sell.BacktestSellStrategy;
import com.bebopze.tdx.quant.strategy.sell.DownMASellStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.INDEX_BLOCK;


/**
 * BS策略 - 回测
 *
 * @author: bebopze
 * @date: 2025/5/27
 */
@Data
@Slf4j
@Component
public class BacktestStrategy {


    // 加载  最近N日   行情数据
    // int DAY_LIMIT = 2000;

    boolean init = false;

    BacktestCache data = new BacktestCache();

    // 统计
    Stat x = new Stat();
    // private static ThreadLocal<Stat> x = new ThreadLocal<>();


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    @Autowired
    private IBtTaskService btTaskService;

    @Autowired
    private IBtTradeRecordService btTradeRecordService;

    @Autowired
    private IBtPositionRecordService btPositionRecordService;

    @Autowired
    private IBtDailyReturnService btDailyReturnService;


    @Autowired
    private BuyStockStrategy buyStockStrategy;

    @Autowired
    private BacktestBuyStrategy backTestBuyStrategy;

    @Autowired
    private BacktestSellStrategy backTestSellStrategy;

    @Autowired
    private DownMASellStrategy sellStockStrategy;


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        // testStrategy_01();
    }


    // -----------------------------------------------------------------------------------------------------------------


    public synchronized void backtest() {


        // -------------------------------------------------------------------------------------------------------------
        //                              回测-task   pre   ==>   板块、个股   行情数据 初始化
        // -------------------------------------------------------------------------------------------------------------


        // 数据初始化   ->   加载 全量行情数据
        initData();


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   创建
        // -------------------------------------------------------------------------------------------------------------


        BtTaskDO taskDO = createBacktestTask();


        Long taskId = taskDO.getId();


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   按日 循环执行
        // -------------------------------------------------------------------------------------------------------------


        LocalDate tradeDate = taskDO.getStartDate().minusDays(1);
        LocalDate endDate = DateTimeUtil.min(taskDO.getEndDate(), data.dateList.get(data.dateList.size() - 1));


        // 初始资金
        x.prevCapital = taskDO.getInitialCapital();
        // 初始净值 1.0000
        // x.prevNav = BigDecimal.ONE; // taskDO.getInitNav;

        // 可用金额
        x.prevAvlCapital = taskDO.getInitialCapital();


        // ----------------  汇总 统计

        // 盈利天数
        // int winCount = 0;
        // 收益率 - 峰值
        // x.peakNav = of(0.000001);
        // 最大回撤
        // BigDecimal maxDrawdown = BigDecimal.ZERO;


        // 每日 收益率
        List<BigDecimal> dailyReturnList = Lists.newArrayList();


        while (tradeDate.isBefore(endDate)) {

            tradeDate = tradeDateIncr(tradeDate);


            try {
                // 每日 - 回测（B/S）
                execBacktestDaily(taskId, tradeDate, dailyReturnList, taskDO);
            } catch (Exception e) {
                log.error("execBacktestDaily     >>>     taskId : {} , tradeDate : {} , exMsg : {}", taskId, tradeDate, e.getMessage(), e);
            }
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            总收益
        // -------------------------------------------------------------------------------------------------------------


        sumTotalReturn(taskDO, dailyReturnList);
    }


    private BtTaskDO createBacktestTask() {

        BtTaskDO taskDO = new BtTaskDO();
        // BS策略
        taskDO.setBuyStrategy("Buy-Strategy-1");
        taskDO.setSellStrategy("Sell-Strategy-1");
        // 回测 - 时间段
        taskDO.setStartDate(LocalDate.of(2025, 1, 1));
        taskDO.setEndDate(LocalDate.now());
        // 初始本金
        taskDO.setInitialCapital(new BigDecimal(1000_000));

        btTaskService.save(taskDO);


        return taskDO;
    }


    private void execBacktestDaily(Long taskId, LocalDate tradeDate, List<BigDecimal> dailyReturnList,
                                   BtTaskDO taskDO
                                   /*int[] winCount,
                                   BigDecimal[] peakNav,
                                   BigDecimal[] maxDrawdown,
                                   BigDecimal[] prevNav, BigDecimal[] prevCapital, BigDecimal[] prevAvlCapital*/) {


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日持仓（S前）
        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------- 卖出策略（ 先S[淘汰]  =>  空余资金  ->  B[新上榜] ）


        // 获取 -> 持仓列表
        List<BtPositionRecordDO> positionRecordDOList__S = getDailyPositions(taskId, tradeDate);
        List<String> positionStockCodeList__S = positionRecordDOList__S.stream().map(BtPositionRecordDO::getStockCode).collect(Collectors.toList());


        // code - DO
        Map<String, BtPositionRecordDO> stockCode_positionDO_Map = Maps.newHashMap();
        for (BtPositionRecordDO positionRecordDO : positionRecordDOList__S) {
            stockCode_positionDO_Map.put(positionRecordDO.getStockCode(), positionRecordDO);
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略
        // -------------------------------------------------------------------------------------------------------------


        // 卖出策略
        List<String> sell__stockCodeList = backTestSellStrategy.rule(data, tradeDate, positionStockCodeList__S);
        log.info("卖出策略     >>>     {} , size : {} , sell__stockCodeList : {}", tradeDate, sell__stockCodeList.size(), JSON.toJSONString(sell__stockCodeList));


        // 持仓个股   ->   匹配 淘汰


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）

        // 2.2 每日 淘汰策略（S策略 - 2）[排名]走弱 -> 末位淘汰 ->  stockCodeList（对昨日 持股 -> 末位淘汰[设置末尾淘汰 - 分数线/排名线 ]）


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略 -> 交易 record
        // -------------------------------------------------------------------------------------------------------------


        // 卖出金额
        BigDecimal sellCapital = BigDecimal.ZERO;


        for (String stockCode : sell__stockCodeList) {

            BtTradeRecordDO tradeRecordDO = new BtTradeRecordDO();
            tradeRecordDO.setTaskId(taskId);
            tradeRecordDO.setTradeType(BtTradeTypeEnum.SELL.getTradeType());
            tradeRecordDO.setStockId(data.stock__codeIdMap.get(stockCode));
            tradeRecordDO.setStockCode(stockCode);
            tradeRecordDO.setStockName(data.stock__codeNameMap.get(stockCode));
            tradeRecordDO.setTradeDate(tradeDate);
            tradeRecordDO.setPrice(NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate)));
            tradeRecordDO.setQuantity(stockCode_positionDO_Map.get(stockCode).getQuantity());
            // 成交额 = 价格 x 数量
            BigDecimal amount = NumUtil.double2Decimal(tradeRecordDO.getPrice().doubleValue() * tradeRecordDO.getQuantity());
            tradeRecordDO.setAmount(amount);
            tradeRecordDO.setFee(BigDecimal.ZERO);


            btTradeRecordService.save(tradeRecordDO);


            sellCapital = sellCapital.add(amount);
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            B策略
        // -------------------------------------------------------------------------------------------------------------


        // 买入策略
        List<String> buy__stockCodeList = backTestBuyStrategy.rule(data, tradeDate);
        log.info("买入策略     >>>     {} , size : {} , buy__stockCodeList : {}", tradeDate, buy__stockCodeList.size(), JSON.toJSONString(buy__stockCodeList));


        // -------------------------------------------------------------------------------------------------------------
        //                                            B策略 -> 交易 record
        // -------------------------------------------------------------------------------------------------------------

        // 可用金额  =  昨日 可用金额  +  今日 卖出金额     //  -  今日 买入金额
        BigDecimal avlCapital = x.prevAvlCapital.add(sellCapital);

        // 买入金额
        BigDecimal buyCapital = BigDecimal.ZERO;


        log.debug("B策略 -> 交易 record - start     >>>     date : {} , prevAvlCapital : {} , sellCapital : {} , avlCapital : {} , prevCapital : {}",
                  tradeDate, x.prevAvlCapital, sellCapital, avlCapital, x.prevCapital);


        int size_B = buy__stockCodeList.size();
        if (size_B > 0) {


            // 等比买入
            BigDecimal avg_amount = avlCapital.divide(of(size_B), 2, RoundingMode.HALF_UP);
            // 单一个股   单次最大买入  剩余资金 x 10%
            avg_amount = avg_amount.min(avlCapital.multiply(of(0.10)));       // 可用资金 * 10%


            // 单一个股   最大仓位限制：10%
            double amount_limit = x.prevCapital.multiply(of(0.10)).doubleValue();      // 总资金 * 10%


            for (String stockCode : buy__stockCodeList) {


                // 个股市值（如果 已持有）
                double marketValue = Optional.ofNullable(stockCode_positionDO_Map.get(stockCode)).map(e -> e.getMarketValue().doubleValue()).orElse(0.0);


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

                // 收盘价
                BigDecimal close = NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate));
                tradeRecordDO.setPrice(close);

                // 买入数量   =   可买仓位 / 收盘价                                  （忽略 🐶💩共产主义特色   ->   100股 bug）
                double qty = amount / close.doubleValue();
                tradeRecordDO.setQuantity((int) qty);

                // 成交额 = 价格 x 数量
                tradeRecordDO.setAmount(of(amount));

                tradeRecordDO.setFee(BigDecimal.ZERO);


                btTradeRecordService.save(tradeRecordDO);


                // -----------


                buyCapital = buyCapital.add(tradeRecordDO.getAmount());
            }


            // 剩余 可用资金  =  可用资金 - 买入总金额
            avlCapital = avlCapital.subtract(buyCapital);


            x.prevAvlCapital = avlCapital;


            log.debug("B策略 -> 交易 record - end     >>>     date : {} , prevAvlCapital : {} , sellCapital : {} , avlCapital : {} , prevCapital : {} , buyCapital : {}",
                      tradeDate, x.prevAvlCapital, sellCapital, avlCapital, x.prevCapital, buyCapital);
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日持仓 -> record
        // -------------------------------------------------------------------------------------------------------------


        // 获取 -> 持仓列表
        List<BtPositionRecordDO> positionRecordDOList = getDailyPositions(taskId, tradeDate);


        btPositionRecordService.saveBatch(positionRecordDOList);


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日收益
        // -------------------------------------------------------------------------------------------------------------


        try {
            BtDailyReturnDO dailyReturnDO = calcDailyReturn(taskId, taskDO.getInitialCapital(), x.prevCapital, avlCapital, buyCapital, sellCapital, tradeDate, positionRecordDOList);
            dailyReturnList.add(dailyReturnDO.getDailyReturn());


            BigDecimal nav = dailyReturnDO.getNav();
            BigDecimal capital = dailyReturnDO.getCapital();


            log.debug("dailyReturnDO : {} , dailyReturnList : {}", JSON.toJSONString(dailyReturnDO), JSON.toJSONString(dailyReturnList));


            // 汇总统计 - 指标更新
            if (dailyReturnDO.getDailyReturn().compareTo(BigDecimal.ZERO) > 0) x.winCount++;
            // 波峰净值
            x.peakNav = x.peakNav.max(nav);
            // 回撤 =（波峰净值 - 当日净值） / 波峰净值
            BigDecimal dd = x.peakNav.subtract(nav).divide(x.peakNav, 8, RoundingMode.HALF_UP);
            // 最大回撤
            x.maxDrawdown = x.maxDrawdown.max(dd);


            x.prevNav = nav;
            x.prevCapital = capital;


        } catch (Exception e) {

            log.error("peakNav : {} , prevCapital : {} , maxDrawdown : {} , exMsg : {}",
                      x.peakNav, x.prevCapital, x.maxDrawdown, e.getMessage(), e);
        }


        // -------------------------------------------------- 账户金额


        // 3、每日 - S金额计算


        // 4、每日 - B金额计算


        // 5、每日 - BS汇总
    }


    private LocalDate tradeDateIncr(LocalDate tradeDate) {
        Integer idx = data.dateIndexMap.get(tradeDate);

        // 非交易日
        while (idx == null) {
            // 下一自然日   ->   直至 交易日
            tradeDate = tradeDate.plusDays(1);
            idx = data.dateIndexMap.get(tradeDate);


            if (!DateTimeUtil.between(tradeDate, data.dateList.get(0), data.dateList.get(data.dateList.size() - 1))) {
                throw new BizException(String.format("[日期：%s]非法，超出有效交易日范围", tradeDate));
            }
        }


        return data.dateList.get(idx + 1);
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
    private BtDailyReturnDO calcDailyReturn(Long taskId,
                                            BigDecimal initialCapital,
                                            BigDecimal prevCapital,
                                            BigDecimal avlCapital, BigDecimal buyCapital, BigDecimal sellCapital,
                                            LocalDate tradeDate,
                                            List<BtPositionRecordDO> positionRecordDOList) {


        // 当日 持仓市值   =   个股市值   汇总
        BigDecimal marketValue = positionRecordDOList.stream()
                                                     .map(BtPositionRecordDO::getMarketValue)
                                                     .reduce(BigDecimal.ZERO, BigDecimal::add);


        // 总资金  =  持仓市值 + 可用资金
        BigDecimal capital = marketValue.add(avlCapital);


        // 净值 = 总资金 / 本金
        BigDecimal nav = capital.divide(initialCapital, 8, RoundingMode.HALF_UP);

        // 当日收益率 = 当日市值 / 昨日市值 - 1
        BigDecimal dailyReturn = capital.divide(prevCapital, 6, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
        log.debug("calcDailyReturn     >>>     date : {} , marketValue : {} , avlCapital : {} , capital : {} , prevCapital : {} , dailyReturn : {} , nav : {}",
                  tradeDate, marketValue, avlCapital, capital, prevCapital, dailyReturn, nav);


        BtDailyReturnDO dailyReturnDO = new BtDailyReturnDO();
        dailyReturnDO.setTaskId(taskId);
        // 日期
        dailyReturnDO.setTradeDate(tradeDate);
        // 当日收益率
        dailyReturnDO.setDailyReturn(dailyReturn);
        // 净值
        dailyReturnDO.setNav(nav);
        // 总资金
        dailyReturnDO.setCapital(capital);
        // 持仓市值
        dailyReturnDO.setMarketValue(marketValue);
        // 可用资金
        dailyReturnDO.setAvlCapital(avlCapital);
        // 买入金额
        dailyReturnDO.setBuyCapital(buyCapital);
        // 卖出金额
        dailyReturnDO.setSellCapital(sellCapital);

        // 基准收益（沪深300）
        dailyReturnDO.setBenchmarkReturn(null);

        btDailyReturnService.save(dailyReturnDO);


        // -----
        // prevCapital[0] = currCapital;


        return dailyReturnDO;
    }


    /**
     * 汇总计算 -> 总收益
     *
     * @param taskDO
     * @param dailyReturnList
     */
    private void sumTotalReturn(BtTaskDO taskDO,
                                /*BigDecimal[] prevNav,
                                BigDecimal[] prevCapital,
                                int[] winCount,
                                BigDecimal[] maxDrawdown,*/
                                List<BigDecimal> dailyReturnList) {


        // 4. 全期汇总：更新 bt_task
        int totalDays = (int) (ChronoUnit.DAYS.between(taskDO.getStartDate(), taskDO.getEndDate()) + 1);
        BigDecimal finalNav = x.prevNav;
        BigDecimal finalCapital = x.prevCapital;


        BigDecimal totalReturn = finalNav.subtract(BigDecimal.ONE);                     // 净值增幅
        BigDecimal totalReturnPct = totalReturn.multiply(of(100));      // %
        BigDecimal annualReturnPct = of(Math.pow(finalNav.doubleValue(), 252.0 / totalDays) - 1).multiply(of(100));


        // 夏普比率 = 平均日收益 / 日收益标准差 * sqrt(252)
        double mean = dailyReturnList.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double sd = Math.sqrt(dailyReturnList.stream()
                                             .mapToDouble(r -> Math.pow(r.doubleValue() - mean, 2)).sum()
                                      / dailyReturnList.size());
        BigDecimal sharpe = of(mean / sd * Math.sqrt(252));

        BigDecimal winRate = of((double) x.winCount / totalDays * 100);
        // 盈亏比 = 所有盈利日平均收益 / 所有亏损日平均亏损
        double avgWin = dailyReturnList.stream().filter(r -> r.doubleValue() > 0)
                                       .mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double avgLoss = dailyReturnList.stream().filter(r -> r.doubleValue() < 0)
                                        .mapToDouble(BigDecimal::doubleValue).map(Math::abs).average().orElse(0);
        BigDecimal profitFactor = avgLoss == 0
                ? of(Double.POSITIVE_INFINITY)
                : of(avgWin / avgLoss);


        // BtTaskDO taskDO = new BtTaskDO();

        // taskDO.setId(taskId);
        // taskDO.setInitialCapital(new BigDecimal(1000000));
        taskDO.setFinalCapital(finalCapital);
        taskDO.setTotalDay(totalDays);
        taskDO.setTotalReturnPct(totalReturnPct);
        taskDO.setAnnualReturnPct(annualReturnPct);
        taskDO.setSharpeRatio(sharpe);
        taskDO.setMaxDrawdownPct(x.maxDrawdown);
        taskDO.setWinRate(winRate);
        taskDO.setProfitFactor(profitFactor);


        btTaskService.updateById(taskDO);
    }


    /**
     * 获取 某回测任务 在指定日期的   持仓详情
     *
     * @param taskId
     * @param endTradeDate 统计 截止日期
     * @return
     */
    private List<BtPositionRecordDO> getDailyPositions(Long taskId, LocalDate endTradeDate) {

        List<BtTradeRecordDO> doList = btTradeRecordService.listByTaskIdAndTradeDate(taskId, endTradeDate);


        // 1. 汇总买卖
        Map<Long, Integer> quantityMap = Maps.newHashMap();     // 个股持仓 -   总数量
        Map<Long, Integer> avlQuantityMap = Maps.newHashMap();  // 个股持仓 - 可用数量（T+1）
        Map<Long, Double> amountMap = Maps.newHashMap();        // 个股持仓 -   总成本（买入价格 x 买入数量   ->   累加）

        Map<Long, PositionInfo> idInfoMap = Maps.newHashMap();  //


        for (BtTradeRecordDO tradeRecordDO : doList) {


            Long stockId = tradeRecordDO.getStockId();
            String stockCode = tradeRecordDO.getStockCode();
            String stockName = tradeRecordDO.getStockName();
            // BUY 或 SELL
            Integer tradeType = tradeRecordDO.getTradeType();
            Integer quantity = tradeRecordDO.getQuantity();
            BigDecimal amount = tradeRecordDO.getAmount();

            // 实际 交易日期
            LocalDate tradeDate = tradeRecordDO.getTradeDate();


            // 买入累加 / 卖出累减   ->   总数量、总成本
            int sign = Objects.equals(BtTradeTypeEnum.BUY.getTradeType(), tradeType) ? +1 : -1;
            // 个股持仓 - 总数量
            quantityMap.merge(stockId, sign * quantity, Integer::sum);
            // 个股持仓 - 总成本
            amountMap.merge(stockId, sign * amount.doubleValue(), Double::sum);


            // T+1（🐶💩共产主义特色）
            if (!(sign == 1 && tradeDate.isEqual(endTradeDate))) {
                // 今日可用（排除 -> 当日 BUY）
                avlQuantityMap.merge(stockId, sign * quantity, Integer::sum);
            } else {
                avlQuantityMap.merge(stockId, 0, Integer::sum);
            }


            PositionInfo positionInfo = idInfoMap.get(stockId);
            if (positionInfo == null) {

                positionInfo = new PositionInfo(stockId, stockCode, stockName, tradeDate);
                idInfoMap.put(stockId, positionInfo);

            } else {
                // 更新 -> 首次买入日期（用于计算 持仓天数）
                LocalDate buyDate = tradeDate.isBefore(positionInfo.buyDate) ? tradeDate : positionInfo.buyDate;
                positionInfo.setBuyDate(buyDate);
            }
        }


        // 2. 构造持仓对象列表
        List<BtPositionRecordDO> positionRecordDOList = Lists.newArrayList();


        quantityMap.forEach((stockId, qty) -> {
            if (qty <= 0) {
                return;  // 当日未持仓 或 已全部卖出
            }


            Integer avlQuantity = avlQuantityMap.getOrDefault(stockId, 0);
            PositionInfo positionInfo = idInfoMap.get(stockId);


            // 总成本
            double totalCost = amountMap.getOrDefault(stockId, 0.0);
            // 平均成本 = 总成本 / 持仓数量
            double avgCost = totalCost / qty;

            // 当日收盘价
            double closePrice = getClosePrice(positionInfo.stockCode, endTradeDate);

            // 浮动盈亏 = （当日收盘价 - 平均成本）x 持仓数量
            double pnl = (closePrice - avgCost) * qty;


            BtPositionRecordDO positionRecordDO = new BtPositionRecordDO();
            positionRecordDO.setTaskId(taskId);
            positionRecordDO.setTradeDate(endTradeDate);
            positionRecordDO.setStockId(stockId);
            positionRecordDO.setStockCode(positionInfo.stockCode);
            positionRecordDO.setStockName(positionInfo.stockName);
            positionRecordDO.setAvgCostPrice(of(avgCost));
            positionRecordDO.setClosePrice(of(closePrice));
            positionRecordDO.setQuantity(qty);
            positionRecordDO.setAvlQuantity(avlQuantity);
            // 当前市值 = 持仓数量 x 当前收盘价
            positionRecordDO.setMarketValue(of(qty * closePrice));
            // 盈亏额
            positionRecordDO.setUnrealizedPnl(of(pnl));
            // 盈亏率 = 盈亏额 / 总成本
            positionRecordDO.setUnrealizedPnlRatio(of(pnl / totalCost));
            positionRecordDO.setBuyDate(positionInfo.buyDate);
            positionRecordDO.setHoldingDays(positionInfo.getHoldingDays(endTradeDate, data.dateIndexMap));


            positionRecordDOList.add(positionRecordDO);
        });


        // save2DB
        // btPositionRecordService.saveBatch(positionRecordDOList);


        return positionRecordDOList;
    }


    /**
     * 个股   指定日期 -> 收盘价
     *
     * @param stockCode
     * @param tradeDate
     * @return
     */
    private double getClosePrice(String stockCode, LocalDate tradeDate) {
        Double closePrice = data.stock__dateCloseMap.get(stockCode).get(tradeDate);
        return closePrice == null ? 0.0 : closePrice;
    }


    private synchronized void initData() {
        // 重新初始化   统计数据
        x = new Stat();


        if (init) {
            return;
        }


        // 加载   全量行情数据 - 个股
        loadAllStockKline();


        // 加载   全量行情数据 - 板块
        loadAllBlockKline();


        // 板块-个股  /  个股-板块
        loadAllBlockRelaStock();


        init = true;
    }


    /**
     * 从本地DB   加载   全部个股（5000+）
     *
     * @return
     */
    private void loadAllStockKline() {


        // DB 数据加载
        data.stockDOList = baseStockService.listAllKline();
        // 空数据 过滤
        data.stockDOList = data.stockDOList.stream().filter(e -> StringUtils.isNotBlank(e.getName()) && StringUtils.isNotBlank(e.getKlineHis())
                // TODO   基金北向
                && e.getAmount().doubleValue() > 10 * 100000000).collect(Collectors.toList());


        // -----------------------------------------------------------------------------


        // 行情起点
        LocalDate dateLine = LocalDate.of(2023, 1, 1);


        // kline_his   ->   dateLine 截取   （ 内存爆炸 ）
        data.stockDOList.parallelStream().forEach(e -> {

            List<KlineDTO> klineDTOList = e.getKlineDTOList();
            klineDTOList = klineDTOList.parallelStream().filter(d -> d.getDate().isAfter(dateLine)).sorted(Comparator.comparing(KlineDTO::getDate)).collect(Collectors.toList());


            e.setKlineHis(ConvertStockKline.dtoList2JsonStr(klineDTOList));
        });


        // -----------------------------------------------------------------------------


        data.stockDOList.forEach(e -> {


            String stockCode = e.getCode();
            List<KlineDTO> klineDTOList = e.getKlineDTOList();


            LocalDate[] date_arr = ConvertStockKline.dateFieldValArr(klineDTOList, "date");
            double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");


            // --------------------------------------------------------


            data.codeStockMap.put(stockCode, e);
            data.stock__idCodeMap.put(e.getId(), stockCode);
            data.stock__codeIdMap.put(stockCode, e.getId());
            data.stock__codeNameMap.put(stockCode, StringUtils.defaultString(e.getName()));


            Map<LocalDate, Double> dateCloseMap = Maps.newHashMap();
            for (int i = 0; i < date_arr.length; i++) {
                dateCloseMap.put(date_arr[i], close_arr[i]);
            }
            data.stock__dateCloseMap.put(stockCode, dateCloseMap);
        });
    }


    /**
     * 从本地DB   加载   全部板块（380+）
     *
     * @return
     */
    private void loadAllBlockKline() {


        data.blockDOList = baseBlockService.listAllKline();


        // -------


        data.blockDOList.forEach(e -> {

            String blockCode = e.getCode();
            List<KlineDTO> klineDTOList = ConvertStockKline.str2DTOList(e.getKlineHis());


            LocalDate[] date_arr = ConvertStockKline.dateFieldValArr(klineDTOList, "date");
            double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");


            // -----------------------------------------------------------


            // 交易日 基准     ->     基准板块（代替 -> 大盘指数）
            if (Objects.equals(blockCode, INDEX_BLOCK)) {
                for (int i = 0; i < date_arr.length; i++) {
                    data.dateIndexMap.put(date_arr[i], i);
                    data.dateList.add(date_arr[i]);
                }
            }


            // -----------------------------------------------------------


            data.codeBlockMap.put(blockCode, e);
            data.block__idCodeMap.put(e.getId(), blockCode);
            data.block__codeIdMap.put(blockCode, e.getId());
            data.block__codeNameMap.put(blockCode, e.getName());


            Map<LocalDate, Double> dateCloseMap = Maps.newHashMap();
            for (int i = 0; i < date_arr.length; i++) {
                dateCloseMap.put(date_arr[i], close_arr[i]);
            }
            data.block__dateCloseMap.put(blockCode, dateCloseMap);
        });
    }


    /**
     * 从本地DB   加载全部   板块-个股
     */
    private void loadAllBlockRelaStock() {

        List<BaseBlockRelaStockDO> relaList = baseBlockRelaStockService.listAll();

        for (BaseBlockRelaStockDO rela : relaList) {

            Long blockId = rela.getBlockId();
            Long stockId = rela.getStockId();
            String blockCode = data.block__idCodeMap.get(blockId);
            String stockCode = data.stock__idCodeMap.get(stockId);


            // data.blockId_stockIdList_Map.computeIfAbsent(blockId, k -> Lists.newArrayList()).add(stockId);
            // data.stockId_blockIdList_Map.computeIfAbsent(stockId, k -> Lists.newArrayList()).add(blockId);


            data.blockCode_stockCodeList_Map.computeIfAbsent(blockCode, k -> Lists.newArrayList()).add(stockCode);
            data.stockCode_blockCodeList_Map.computeIfAbsent(stockCode, k -> Lists.newArrayList()).add(blockCode);
        }
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 统计
     */
    @Data
    public static class Stat {

        // 初始资金
        BigDecimal prevCapital;

        // 初始净值 1.0000
        BigDecimal prevNav = BigDecimal.ONE;

        // 可用金额
        BigDecimal prevAvlCapital;


        // -----------------------------------------


        // 收益率 - 峰值
        BigDecimal peakNav = of(0.000001);
        // 最大回撤
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        // 盈利天数
        int winCount = 0;
    }


    @Data
    @AllArgsConstructor
    public static class PositionInfo {
        private Long stockId;
        private String stockCode;
        private String stockName;
        private LocalDate buyDate;
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
        Assert.isTrue(!start.isAfter(end), "start不能大于end");


        Integer idx1 = dateIndexMap.get(start);
        Integer idx2 = dateIndexMap.get(end);

        Assert.notNull(idx1, "start非交易日");
        Assert.notNull(idx2, "end非交易日");

        return idx2 - idx1;
    }


//    private BigDecimal of(double val) {
//        return BigDecimal.valueOf(val);
//    }

    private static BigDecimal of(Number val) {
        return BigDecimal.valueOf(val.doubleValue()).setScale(4, RoundingMode.HALF_UP);
    }

}
