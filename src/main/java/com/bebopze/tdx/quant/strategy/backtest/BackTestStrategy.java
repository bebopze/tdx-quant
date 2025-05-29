package com.bebopze.tdx.quant.strategy.backtest;

import com.bebopze.tdx.quant.common.constant.BtTradeTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.BuyStockStrategyResultDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.strategy.buy.BuyStockStrategy;
import com.bebopze.tdx.quant.strategy.sell.DownMASellStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bebopze.tdx.quant.service.impl.ExtDataServiceImpl.fillNaN;


/**
 * BS策略 - 回测
 *
 * @author: bebopze
 * @date: 2025/5/27
 */
@Slf4j
@Component
public class BackTestStrategy {


    // 加载  最近N日   行情数据
    int DAY_LIMIT = 2000;

    List<BaseStockDO> baseStockDOList;


    Map<String, Double> dateCloseMap = Maps.newHashMap();
    Map<String, Map<String, Double>> stock__dateCloseMap = Maps.newHashMap();


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
    private DownMASellStrategy sellStockStrategy;


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        LocalDate now = LocalDate.now();
        String str = DateTimeUtil.format_yyyy_MM_dd(now);

        System.out.println(str);
        // testStrategy_01();
    }


    // -----------------------------------------------------------------------------------------------------------------


    public void testStrategy_01() {


        Map<String, double[]> codeCloseMap = loadAllStockKline();


        // 0、起始日期 - 起始金额


        // -------------------------------------------------- B
        // 1.1、当日 B策略 -> stockCodeList
        // 1.2、昨日 B策略 -> stockCodeList


        // 买入策略
        BuyStockStrategyResultDTO resultDTO = buyStockStrategy.buyStockRule();


        List<String> stockCodeList = resultDTO.getStockCodeList();


        // -------------------------------------------------- S


        // B策略 - S策略     =>     彼此不关联 -> 解耦


        Long taskId = 1L;
        LocalDate tradeDate = LocalDate.now();


        // 获取 -> 持仓列表
        List<BtPositionRecordDO> doList = getDailyPositions(taskId, tradeDate);


        // 卖出策略
        sellStockStrategy.holdingStockRule(null);


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）

        // 2.2 每日 淘汰策略（S策略 - 2）[排名]走弱 -> 末位淘汰 ->  stockCodeList（对昨日 持股 -> 末位淘汰[设置末尾淘汰 - 分数线/排名线 ]）


        // -------------------------------------------------- 账户金额


        // 3、每日 - S金额计算


        // 4、每日 - B金额计算


        // 5、每日 - BS汇总


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
        }


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


    /**
     * 从本地DB   加载全部（5000+支）个股的 收盘价序列
     *
     * @return stock - close_arr
     */
    private Map<String, double[]> loadAllStockKline() {
        Map<String, double[]> stockCloseArrMap = Maps.newHashMap();


        // List<BaseStockDO> baseStockDOList = baseStockService.listAllKline();
        baseStockDOList = baseStockService.listAllKline();


        // TODO   停牌 - 日期-行情 问题（待验证   ->   暂忽略【影响基本为 0】）


        baseStockDOList.forEach(e -> {

            String stockCode = e.getCode();
            List<KlineDTO> klineDTOList = e.getKLineHis();


            double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");
            String[] date_arr = ConvertStockKline.strFieldValArr(klineDTOList, "date");


            for (int i = 0; i < date_arr.length; i++) {
                dateCloseMap.put(date_arr[i], close_arr[i]);
            }
            stock__dateCloseMap.put(stockCode, dateCloseMap);


            // 上市1年
            if (close_arr.length > 200) {
                stockCloseArrMap.put(stockCode, fillNaN(close_arr, DAY_LIMIT));


//                double[] fillNaN_arr = stockCloseArrMap.get(stockCode);
//                if (Double.isNaN(fillNaN_arr[0])) {
//                    log.debug("fillNaN     >>>     stockCode : {}", stockCode);
//                }
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
