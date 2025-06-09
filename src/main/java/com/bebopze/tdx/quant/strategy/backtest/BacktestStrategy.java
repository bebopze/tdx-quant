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
    int DAY_LIMIT = 2000;
    boolean init = false;

    BacktestCache data = new BacktestCache();


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


        // 数据初始化   ->   加载 全量行情数据
        initData();


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   初始化
        // -------------------------------------------------------------------------------------------------------------


        BtTaskDO taskDO = new BtTaskDO();
        // BS策略
        taskDO.setBuyStrategy("Buy-Strategy-1");
        taskDO.setSellStrategy("Sell-Strategy-1");
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
        LocalDate endDate = DateTimeUtil.min(taskDO.getEndDate(), data.dateList.get(data.dateList.size() - 1));


        // 初始资金
        BigDecimal[] prevCapital = {taskDO.getInitialCapital()};
        // 初始净值 1.0000
        BigDecimal[] prevNav = {BigDecimal.ONE};


        // ----------------  汇总 统计

        // 盈利天数
        int[] winCount = {0};

        // 收益率 - 峰值
        BigDecimal[] peakNav = {BigDecimal.ZERO};
        // 最大回撤
        BigDecimal[] maxDrawdown = {BigDecimal.ZERO};


        // 每日 收益率
        List<BigDecimal> dailyReturnList = Lists.newArrayList();


        while (tradeDate.isBefore(endDate)) {

            tradeDate = tradeDateIncr(tradeDate);


            try {
                backtest__daily(taskId, tradeDate, dailyReturnList, winCount, peakNav, maxDrawdown, prevNav, prevCapital);
            } catch (Exception e) {
                log.error("daily     >>>     exMsg : {}", e.getMessage(), e);
            }
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            总收益
        // -------------------------------------------------------------------------------------------------------------


        sumTotalReturn(taskId);


        // 4. 全期汇总：更新 bt_task
        int totalDays = (int) (ChronoUnit.DAYS.between(tradeDate, endDate) + 1);
        BigDecimal finalNav = prevNav[0];
        BigDecimal finalCapital = prevCapital[0];


        BigDecimal totalReturn = finalNav.subtract(BigDecimal.ONE);                     // 净值增幅
        BigDecimal totalReturnPct = totalReturn.multiply(BigDecimal.valueOf(100));      // %
        BigDecimal annualReturnPct = BigDecimal.valueOf(Math.pow(finalNav.doubleValue(), 252.0 / totalDays) - 1).multiply(BigDecimal.valueOf(100));


        // 夏普比率 = 平均日收益 / 日收益标准差 * sqrt(252)
        double mean = dailyReturnList.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
        double sd = Math.sqrt(dailyReturnList.stream()
                                             .mapToDouble(r -> Math.pow(r.doubleValue() - mean, 2)).sum()
                                      / dailyReturnList.size());
        BigDecimal sharpe = BigDecimal.valueOf(mean / sd * Math.sqrt(252));

        BigDecimal winRate = BigDecimal.valueOf((double) winCount[0] / totalDays * 100);
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
        taskDO.setMaxDrawdownPct(maxDrawdown[0]);
        taskDO.setWinRate(winRate);
        taskDO.setProfitFactor(profitFactor);


        btTaskService.updateById(taskDO);
    }

    private void backtest__daily(Long taskId, LocalDate tradeDate, List<BigDecimal> dailyReturnList,
                                 int[] winCount,
                                 BigDecimal[] peakNav,
                                 BigDecimal[] maxDrawdown, BigDecimal[] prevNav, BigDecimal[] prevCapital) {


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日持仓（S前）
        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------- 卖出策略（ 先S[淘汰]  =>  空余资金  ->  B[新上榜] ）


        // 获取 -> 持仓列表
        List<BtPositionRecordDO> positionRecordDOList__S = getDailyPositions(taskId, tradeDate);
        List<String> positionStockCodeList__S = positionRecordDOList__S.stream().map(BtPositionRecordDO::getStockCode).collect(Collectors.toList());


        // code - DO
        Map<String, BtPositionRecordDO> stockCode_positionDO_map = Maps.newHashMap();
        for (BtPositionRecordDO positionRecordDO : positionRecordDOList__S) {
            stockCode_positionDO_map.put(positionRecordDO.getStockCode(), positionRecordDO);
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略
        // -------------------------------------------------------------------------------------------------------------


        // 卖出策略
        List<String> sell__stockCodeList = backTestSellStrategy.rule(data, tradeDate, positionStockCodeList__S);
        log.info("卖出策略     >>>     size : {} , sell__stockCodeList : {}", sell__stockCodeList.size(), JSON.toJSONString(sell__stockCodeList));


        // 持仓个股   ->   匹配 淘汰
        positionStockCodeList__S.removeAll(sell__stockCodeList);


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）

        // 2.2 每日 淘汰策略（S策略 - 2）[排名]走弱 -> 末位淘汰 ->  stockCodeList（对昨日 持股 -> 末位淘汰[设置末尾淘汰 - 分数线/排名线 ]）


        // -------------------------------------------------------------------------------------------------------------
        //                                            S策略 -> 交易 record
        // -------------------------------------------------------------------------------------------------------------


        for (String stockCode : sell__stockCodeList) {

            BtTradeRecordDO tradeRecordDO = new BtTradeRecordDO();
            tradeRecordDO.setTradeType(BtTradeTypeEnum.SELL.getTradeType());
            tradeRecordDO.setStockId(data.stock__codeIdMap.get(stockCode));
            tradeRecordDO.setStockCode(stockCode);
            tradeRecordDO.setStockName(data.stock__codeNameMap.get(stockCode));
            tradeRecordDO.setTradeDate(tradeDate);
            tradeRecordDO.setPrice(NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate)));
            tradeRecordDO.setQuantity(stockCode_positionDO_map.get(stockCode).getQuantity());
            // 成交额 = 价格 x 数量
            tradeRecordDO.setAmount(NumUtil.double2Decimal(tradeRecordDO.getPrice().doubleValue() * tradeRecordDO.getQuantity()));
            tradeRecordDO.setFee(BigDecimal.ZERO);


            btTradeRecordService.save(tradeRecordDO);
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            B策略
        // -------------------------------------------------------------------------------------------------------------


        // 买入策略
        List<String> buy__stockCodeList = backTestBuyStrategy.rule(data, tradeDate);
        log.info("买入策略     >>>     size : {} , buy__stockCodeList : {}", buy__stockCodeList.size(), JSON.toJSONString(buy__stockCodeList));


        // 持仓个股   ->   匹配 淘汰
        positionStockCodeList__S.removeAll(sell__stockCodeList);


        // -------------------------------------------------------------------------------------------------------------
        //                                            B策略 -> 交易 record
        // -------------------------------------------------------------------------------------------------------------


        for (String stockCode : buy__stockCodeList) {

            BtTradeRecordDO tradeRecordDO = new BtTradeRecordDO();
            tradeRecordDO.setTradeType(BtTradeTypeEnum.BUY.getTradeType());
            tradeRecordDO.setStockId(data.stock__codeIdMap.get(stockCode));
            tradeRecordDO.setStockCode(stockCode);
            tradeRecordDO.setStockName(data.stock__codeNameMap.get(stockCode));
            tradeRecordDO.setTradeDate(tradeDate);
            tradeRecordDO.setPrice(NumUtil.double2Decimal(getClosePrice(stockCode, tradeDate)));
            tradeRecordDO.setQuantity(stockCode_positionDO_map.get(stockCode).getQuantity());
            // 成交额 = 价格 x 数量
            tradeRecordDO.setAmount(NumUtil.double2Decimal(tradeRecordDO.getPrice().doubleValue() * tradeRecordDO.getQuantity()));
            tradeRecordDO.setFee(BigDecimal.ZERO);


            btTradeRecordService.save(tradeRecordDO);
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日持仓
        // -------------------------------------------------------------------------------------------------------------


        // 获取 -> 持仓列表
        List<BtPositionRecordDO> positionRecordDOList = getDailyPositions(taskId, tradeDate);


        btPositionRecordService.saveBatch(positionRecordDOList);


        // -------------------------------------------------------------------------------------------------------------
        //                                            每日收益
        // -------------------------------------------------------------------------------------------------------------


        try {


            BtDailyReturnDO dailyReturnDO = calcDailyReturn(taskId, tradeDate, positionRecordDOList);
            dailyReturnList.add(dailyReturnDO.getDailyReturn());


            BigDecimal nav = dailyReturnDO.getNav();
            BigDecimal capital = dailyReturnDO.getCapital();


            log.debug("dailyReturnDO : {} , dailyReturnList : {}", JSON.toJSONString(dailyReturnDO), JSON.toJSONString(dailyReturnList));


            // 汇总统计 - 指标更新
            if (dailyReturnDO.getDailyReturn().compareTo(BigDecimal.ZERO) > 0) winCount[0]++;
            peakNav[0] = peakNav[0].max(nav);
            BigDecimal dd = peakNav[0].subtract(nav).divide(peakNav[0], 8, RoundingMode.HALF_UP);
            maxDrawdown[0] = maxDrawdown[0].max(dd);


            prevNav[0] = nav;
            prevCapital[0] = capital;


        } catch (Exception e) {

            log.error("peakNav : {} , prevCapital : {} , maxDrawdown : {} , exMsg : {}",
                      peakNav, prevCapital, maxDrawdown, e.getMessage(), e);
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
        Double closePrice = data.stock__dateCloseMap.get(stockCode).get(tradeDate);
        return closePrice == null ? 0.0 : closePrice;
    }


    private synchronized void initData() {

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
        data.stockDOList = data.stockDOList.stream().filter(e -> StringUtils.isNotBlank(e.getName()) && StringUtils.isNotBlank(e.getKlineHis())).collect(Collectors.toList());


        // -----------------------------------------------------------------------------


        // 行情起点
        LocalDate dateLine = LocalDate.of(2020, 1, 1);


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


        data.blockDOList.parallelStream().forEach(e -> {

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


            data.blockId_stockIdList_Map.computeIfAbsent(blockId, k -> Lists.newArrayList()).add(stockId);
            data.stockId_blockIdList_Map.computeIfAbsent(stockId, k -> Lists.newArrayList()).add(blockId);
        }
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
