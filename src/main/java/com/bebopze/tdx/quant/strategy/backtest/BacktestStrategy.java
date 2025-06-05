package com.bebopze.tdx.quant.strategy.backtest;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.INDEX_BLOCK;
import static com.bebopze.tdx.quant.service.impl.ExtDataServiceImpl.fillNaN;


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


    public static final ThreadLocal<BacktestStrategy> strategyThreadLocal = new ThreadLocal<>();


    // 加载  最近N日   行情数据
    int DAY_LIMIT = 2000;


    /**
     * 交易日 - 基准
     */
    Map<String, Integer> dateIndexMap = Maps.newHashMap();


    List<BaseStockDO> stockDOList;
    Map<String, Map<String, Double>> stock__dateCloseMap = Maps.newHashMap();
    Map<String, Long> stock__codeIdMap = Maps.newHashMap();
    Map<String, String> stock__codeNameMap = Maps.newHashMap();


    List<BaseBlockDO> blockDOList;
    Map<String, Map<String, Double>> block__dateCloseMap = Maps.newHashMap();
    // Map<String, Long> block__codeIdMap = Maps.newHashMap();
    // Map<String, String> block__codeNameMap = Maps.newHashMap();


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


    public void backtest() {


        // -------------------------------------------------------------------------------------------------------------
        //                              回测-task   pre   ==>   板块、个股   行情数据 初始化
        // -------------------------------------------------------------------------------------------------------------


        // 加载   全量行情数据

        initData();


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   初始化
        // -------------------------------------------------------------------------------------------------------------


        BtTaskDO taskDO = new BtTaskDO();
        // BS策略
        taskDO.setBuyStrategy("");
        taskDO.setSellStrategy("");
        // 回测 - 时间段
        taskDO.setStartDate(LocalDate.of(2025, 1, 1));
        taskDO.setEndDate(LocalDate.now());
        // 初始本金
        taskDO.setInitialCapital(new BigDecimal("1000000"));

        btTaskService.save(taskDO);


        Long taskId = taskDO.getId();


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   按日 循环执行
        // -------------------------------------------------------------------------------------------------------------


        LocalDate tradeDate = taskDO.getStartDate().minusDays(1);
        LocalDate endDate = taskDO.getEndDate();


        // 初始资金
        BigDecimal prevCapital = taskDO.getInitialCapital();
        // 初始净值 1.0000
        BigDecimal prevNav = BigDecimal.ONE;


        // ----------------  汇总 统计

        // 盈利天数
        int winCount = 0;

        // 收益率 - 峰值
        BigDecimal peakNav = BigDecimal.ZERO;
        // 最大回撤
        BigDecimal maxDrawdown = BigDecimal.ZERO;


        // 每日 收益率
        List<BigDecimal> dailyReturnList = Lists.newArrayList();


        while (!tradeDate.isAfter(endDate)) {
            tradeDate = tradeDate.plusDays(1);


            // ---------------------------------------------------------------------------------------------------------
            //                                            每日持仓（S前）
            // ---------------------------------------------------------------------------------------------------------


            // -------------------------------------------------- 卖出策略（ 先S[淘汰]  =>  空余资金  ->  B[新上榜] ）


            // 获取 -> 持仓列表
            List<BtPositionRecordDO> positionRecordDOList__S = getDailyPositions(taskId, tradeDate);
            List<String> positionStockCodeList__S = positionRecordDOList__S.stream().map(BtPositionRecordDO::getStockCode).collect(Collectors.toList());


            // code - DO
            Map<String, BtPositionRecordDO> stockCode_positionDO_map = Maps.newHashMap();
            for (BtPositionRecordDO positionRecordDO : positionRecordDOList__S) {
                stockCode_positionDO_map.put(positionRecordDO.getStockCode(), positionRecordDO);
            }


            // ---------------------------------------------------------------------------------------------------------
            //                                            S策略
            // ---------------------------------------------------------------------------------------------------------


            // 卖出策略
            List<String> sell__stockCodeList = backTestSellStrategy.rule(this, tradeDate, positionStockCodeList__S);


            // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）

            // 2.2 每日 淘汰策略（S策略 - 2）[排名]走弱 -> 末位淘汰 ->  stockCodeList（对昨日 持股 -> 末位淘汰[设置末尾淘汰 - 分数线/排名线 ]）


            // ---------------------------------------------------------------------------------------------------------
            //                                            S策略 -> 交易 record
            // ---------------------------------------------------------------------------------------------------------


            for (String stockCode : sell__stockCodeList) {

                BtTradeRecordDO tradeRecordDO = new BtTradeRecordDO();
                tradeRecordDO.setTradeType(BtTradeTypeEnum.SELL.getTradeType());
                tradeRecordDO.setStockId(stock__codeIdMap.get(stockCode));
                tradeRecordDO.setStockCode(stockCode);
                tradeRecordDO.setStockName(stock__codeNameMap.get(stockCode));
                tradeRecordDO.setTradeDate(tradeDate);
                tradeRecordDO.setPrice(NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate)));
                tradeRecordDO.setQuantity(stockCode_positionDO_map.get(stockCode).getQuantity());
                // 成交额 = 价格 x 数量
                tradeRecordDO.setAmount(NumUtil.double2Decimal(tradeRecordDO.getPrice().doubleValue() * tradeRecordDO.getQuantity()));
                tradeRecordDO.setFee(BigDecimal.ZERO);


                btTradeRecordService.save(tradeRecordDO);
            }


            // ---------------------------------------------------------------------------------------------------------
            //                                            B策略
            // ---------------------------------------------------------------------------------------------------------


            // 买入策略
            List<String> buy__stockCodeList = backTestBuyStrategy.rule(this, tradeDate);


            // ---------------------------------------------------------------------------------------------------------
            //                                            B策略 -> 交易 record
            // ---------------------------------------------------------------------------------------------------------


            for (String stockCode : buy__stockCodeList) {

                BtTradeRecordDO tradeRecordDO = new BtTradeRecordDO();
                tradeRecordDO.setTradeType(BtTradeTypeEnum.BUY.getTradeType());
                tradeRecordDO.setStockId(stock__codeIdMap.get(stockCode));
                tradeRecordDO.setStockCode(stockCode);
                tradeRecordDO.setStockName(stock__codeNameMap.get(stockCode));
                tradeRecordDO.setTradeDate(tradeDate);
                tradeRecordDO.setPrice(NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate)));
                tradeRecordDO.setQuantity(stockCode_positionDO_map.get(stockCode).getQuantity());
                // 成交额 = 价格 x 数量
                tradeRecordDO.setAmount(NumUtil.double2Decimal(tradeRecordDO.getPrice().doubleValue() * tradeRecordDO.getQuantity()));
                tradeRecordDO.setFee(BigDecimal.ZERO);


                btTradeRecordService.save(tradeRecordDO);
            }


            // ---------------------------------------------------------------------------------------------------------
            //                                            每日持仓
            // ---------------------------------------------------------------------------------------------------------


            // 获取 -> 持仓列表
            List<BtPositionRecordDO> positionRecordDOList = getDailyPositions(taskId, tradeDate);


            btPositionRecordService.saveBatch(positionRecordDOList);


            // ---------------------------------------------------------------------------------------------------------
            //                                            每日收益
            // ---------------------------------------------------------------------------------------------------------


            BtDailyReturnDO dailyReturnDO = calcDailyReturn(taskId, tradeDate, positionRecordDOList);
            dailyReturnList.add(dailyReturnDO.getDailyReturn());


            BigDecimal nav = dailyReturnDO.getNav();
            BigDecimal capital = dailyReturnDO.getCapital();


            // 汇总统计 - 指标更新
            if (dailyReturnDO.getDailyReturn().compareTo(BigDecimal.ZERO) > 0) winCount++;
            peakNav = peakNav.max(nav);
            BigDecimal dd = peakNav.subtract(nav).divide(peakNav, 8, RoundingMode.HALF_UP);
            maxDrawdown = maxDrawdown.max(dd);


            prevNav = nav;
            prevCapital = capital;


            // -------------------------------------------------- 账户金额


            // 3、每日 - S金额计算


            // 4、每日 - B金额计算


            // 5、每日 - BS汇总


        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            总收益
        // -------------------------------------------------------------------------------------------------------------


        sumTotalReturn(taskId);


        // 4. 全期汇总：更新 bt_task
        int totalDays = (int) (ChronoUnit.DAYS.between(tradeDate, endDate) + 1);
        BigDecimal finalNav = prevNav;
        BigDecimal finalCapital = prevCapital;


        BigDecimal totalReturn = finalNav.subtract(BigDecimal.ONE);                     // 净值增幅
        BigDecimal totalReturnPct = totalReturn.multiply(BigDecimal.valueOf(100));      // %
        BigDecimal annualReturnPct = BigDecimal.valueOf(Math.pow(finalNav.doubleValue(), 252.0 / totalDays) - 1).multiply(BigDecimal.valueOf(100));


        // 夏普比率 = 平均日收益 / 日收益标准差 * sqrt(252)
        double mean = dailyReturnList.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double sd = Math.sqrt(dailyReturnList.stream()
                                      .mapToDouble(r -> Math.pow(r.doubleValue() - mean, 2)).sum()
                                      / dailyReturnList.size());
        BigDecimal sharpe = BigDecimal.valueOf(mean / sd * Math.sqrt(252));

        BigDecimal winRate = BigDecimal.valueOf((double) winCount / totalDays * 100);
        // 盈亏比 = 所有盈利日平均收益 / 所有亏损日平均亏损
        double avgWin = dailyReturnList.stream().filter(r -> r.doubleValue() > 0)
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double avgLoss = dailyReturnList.stream().filter(r -> r.doubleValue() < 0)
                .mapToDouble(BigDecimal::doubleValue).map(Math::abs).average().orElse(0);
        BigDecimal profitFactor = avgLoss == 0
                ? BigDecimal.valueOf(Double.POSITIVE_INFINITY)
                : BigDecimal.valueOf(avgWin / avgLoss);


        // BtTaskDO taskDO = new BtTaskDO();

        taskDO.setId(taskId);
        taskDO.setInitialCapital(new BigDecimal(1000000));
        taskDO.setFinalCapital(finalCapital);
        taskDO.setTotalDay(totalDays);
        taskDO.setTotalReturnPct(totalReturnPct);
        taskDO.setAnnualReturnPct(annualReturnPct);
        taskDO.setSharpeRatio(sharpe);
        taskDO.setMaxDrawdownPct(maxDrawdown);
        taskDO.setWinRate(winRate);
        taskDO.setProfitFactor(profitFactor);


        btTaskService.updateById(taskDO);


        // finally {
        strategyThreadLocal.remove();
        // }
    }


    /**
     * 计算  ->  每日收益率
     *
     * @param taskId
     * @param tradeDate
     * @param positionRecordDOList
     */
    private BtDailyReturnDO calcDailyReturn(Long taskId,
                                            LocalDate tradeDate,
                                            List<BtPositionRecordDO> positionRecordDOList) {

        // 当日 总市值
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        for (BtPositionRecordDO positionRecordDO : positionRecordDOList) {
            // 个股市值
            BigDecimal stockMarketValue = positionRecordDO.getMarketValue();
            totalMarketValue = totalMarketValue.add(stockMarketValue);
        }


        // 本金 -> bt_task
        BigDecimal initialCapital = new BigDecimal(1000000);
        // 初始净值 1.0000
        BigDecimal initialNav = BigDecimal.ONE;


        // 收益率 = 当日市值 / 本金
        BigDecimal nav = totalMarketValue.divide(initialCapital, 8, RoundingMode.HALF_UP);
        // 净值 = 1 + 收益率
        BigDecimal dailyReturn = initialNav.add(nav).setScale(8, RoundingMode.HALF_UP);
        // 资金 = 本金 x 收益率
        BigDecimal capital = initialCapital.multiply(nav).setScale(2, RoundingMode.HALF_UP);


        BtDailyReturnDO dailyReturnDO = new BtDailyReturnDO();

        dailyReturnDO.setTaskId(taskId);
        dailyReturnDO.setTradeDate(tradeDate);
        dailyReturnDO.setDailyReturn(dailyReturn);
        dailyReturnDO.setNav(nav);
        dailyReturnDO.setCapital(capital);
        dailyReturnDO.setBenchmarkReturn(null);

        btDailyReturnService.save(dailyReturnDO);


        return dailyReturnDO;
    }


    /**
     * 汇总计算 -> 总收益
     *
     * @param taskId
     */
    private void sumTotalReturn(Long taskId) {


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
        Map<Long, Integer> quantityMap = Maps.newHashMap();
        Map<Long, Integer> avlQuantityMap = Maps.newHashMap();
        Map<Long, Double> costBases = Maps.newHashMap();

        Map<Long, PositionInfo> idInfoMap = Maps.newHashMap();


        for (BtTradeRecordDO tradeRecordDO : doList) {


            Long stockId = tradeRecordDO.getId();
            String stockCode = tradeRecordDO.getStockCode();
            String stockName = tradeRecordDO.getStockName();
            // BUY 或 SELL
            Integer tradeType = tradeRecordDO.getTradeType();
            Integer quantity = tradeRecordDO.getQuantity();
            BigDecimal price = tradeRecordDO.getPrice();

            // 实际 交易日期
            LocalDate tradeDate = tradeRecordDO.getTradeDate();


            // 买入累加 -> 正数量&成本，卖出累减 -> 正数量&成本
            int sign = Objects.equals(BtTradeTypeEnum.BUY.getTradeType(), tradeType) ? +1 : -1;
            quantityMap.merge(stockId, sign * quantity, Integer::sum);
            costBases.merge(stockId, sign * price.doubleValue(), Double::sum);


            // T+1（🐶💩共产主义特色）
            if (!(sign == 1 && tradeDate.isEqual(endTradeDate))) {
                // 今日可用（排除 -> 当日+BUY）
                avlQuantityMap.merge(stockId, sign * quantity, Integer::sum);
            }


            PositionInfo positionInfo = idInfoMap.get(stockId);
            if (positionInfo == null) {

                positionInfo = new PositionInfo(stockId, stockCode, stockName, tradeDate);
                idInfoMap.put(stockId, positionInfo);

            } else {
                LocalDate buyDate = tradeDate.isBefore(positionInfo.buyDate) ? tradeDate : positionInfo.getBuyDate();
                positionInfo.setBuyDate(buyDate);
            }
        }


        // 2. 构造持仓对象列表
        List<BtPositionRecordDO> positionRecordDOList = Lists.newArrayList();

        for (Map.Entry<Long, Integer> e : quantityMap.entrySet()) {
            Long stockId = e.getKey();
            int qty = e.getValue();
            if (qty <= 0) continue;  // 当日未持仓或已经全部卖出


            Integer avlQuantity = avlQuantityMap.get(stockId);
            PositionInfo positionInfo = idInfoMap.get(stockId);


            double totalCost = costBases.getOrDefault(stockId, 0.0);
            double avgCost = totalCost / qty;

            // 查询当日收盘价
            double closePrice = getClosePrice(positionInfo.stockCode, endTradeDate);

            double pnl = (closePrice - avgCost) * qty;


            BtPositionRecordDO positionRecordDO = new BtPositionRecordDO();
            positionRecordDO.setTaskId(taskId);
            positionRecordDO.setTradeDate(endTradeDate);
            positionRecordDO.setStockId(stockId);
            positionRecordDO.setStockCode(positionInfo.stockCode);
            positionRecordDO.setStockName(positionInfo.stockName);
            positionRecordDO.setAvgCostPrice(BigDecimal.valueOf(avgCost));
            positionRecordDO.setClosePrice(BigDecimal.valueOf(closePrice));
            positionRecordDO.setQuantity(qty);
            positionRecordDO.setAvlQuantity(avlQuantity);
            positionRecordDO.setMarketValue(BigDecimal.valueOf(qty * closePrice));
            positionRecordDO.setUnrealizedPnl(BigDecimal.valueOf(pnl));
            positionRecordDO.setUnrealizedPnlRatio(BigDecimal.valueOf(pnl / (avgCost * qty)));
            positionRecordDO.setBuyDate(positionInfo.buyDate);
            positionRecordDO.setHoldingDays(positionInfo.getHoldingDays());


            positionRecordDOList.add(positionRecordDO);


            // -----------------------------------------------------


        }


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
        Double closePrice = stock__dateCloseMap.get(stockCode).get(DateTimeUtil.format_yyyy_MM_dd(tradeDate));
        return closePrice == null ? 0.0 : closePrice;
    }


    private void initData() {

        // 加载   全量行情数据 - 个股
        Map<String, double[]> stock__codeCloseMap = loadAllStockKline();


        // 加载   全量行情数据 - 板块
        Map<String, double[]> block__codeCloseMap = loadAllBlockKline();


        // ...

        strategyThreadLocal.set(this);
    }

    /**
     * 从本地DB   加载   全部板块的 收盘价序列
     *
     * @return stock - close_arr
     */
    private Map<String, double[]> loadAllBlockKline() {
        Map<String, double[]> codeCloseMap = Maps.newHashMap();


        blockDOList = baseBlockService.listAllKline();


        // TODO   停牌 - 日期-行情 问题（待验证   ->   暂忽略【影响基本为 0】）


        blockDOList.parallelStream().forEach(e -> {

            String blockCode = e.getCode();
            List<KlineDTO> klineDTOList = ConvertStockKline.str2DTOList(e.getKlineHis());


            double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");
            String[] date_arr = ConvertStockKline.strFieldValArr(klineDTOList, "date");


            // 基准板块（代替 -> 大盘指数）   =>     交易日 基准
            if (Objects.equals(blockCode, INDEX_BLOCK)) {
                for (int i = 0; i < date_arr.length; i++) {
                    dateIndexMap.put(date_arr[i], i);
                }
            }


            Map<String, Double> dateCloseMap = Maps.newHashMap();
            for (int i = 0; i < date_arr.length; i++) {
                dateCloseMap.put(date_arr[i], close_arr[i]);
            }
            block__dateCloseMap.put(blockCode, dateCloseMap);


            // 上市1年
            if (close_arr.length > 200) {
                codeCloseMap.put(blockCode, fillNaN(close_arr, DAY_LIMIT));
            }
        });


        return codeCloseMap;
    }

    /**
     * 从本地DB   加载   全部（5000+支）个股的 收盘价序列
     *
     * @return stock - close_arr
     */
    private Map<String, double[]> loadAllStockKline() {
        Map<String, double[]> stockCloseArrMap = Maps.newHashMap();


        // List<BaseStockDO> baseStockDOList = baseStockService.listAllKline();
        stockDOList = baseStockService.listAllKline();


        // TODO   停牌 - 日期-行情 问题（待验证   ->   暂忽略【影响基本为 0】）


        stockDOList.parallelStream().forEach(e -> {

            String stockCode = e.getCode();
            List<KlineDTO> klineDTOList = e.getKLineHis();


            double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");
            String[] date_arr = ConvertStockKline.strFieldValArr(klineDTOList, "date");


            // --------------------------------------------------------


            Map<String, Double> dateCloseMap = Maps.newHashMap();
            for (int i = 0; i < date_arr.length; i++) {
                dateCloseMap.put(date_arr[i], close_arr[i]);
            }
            stock__dateCloseMap.put(stockCode, dateCloseMap);


            stock__codeIdMap.put(stockCode, e.getId());
            stock__codeNameMap.put(stockCode, e.getName());


            // 上市1年
            if (close_arr.length > 200) {
                stockCloseArrMap.put(stockCode, fillNaN(close_arr, DAY_LIMIT));
            }
        });


        return stockCloseArrMap;
    }


    @Data
    @AllArgsConstructor
    public static class PositionInfo {
        private Long stockId;
        private String stockCode;
        private String stockName;
        private LocalDate buyDate;
        // private Integer holdingDays;

        public Integer getHoldingDays() {
            // 自然日（简单计算）
            long daysBetween = ChronoUnit.DAYS.between(LocalDate.now(), buyDate);
            return (int) daysBetween;
        }
    }


}
