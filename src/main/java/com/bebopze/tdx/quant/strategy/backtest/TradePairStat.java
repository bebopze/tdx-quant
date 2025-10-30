package com.bebopze.tdx.quant.strategy.backtest;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.analysis.TopPoolDailyReturnDTO;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.util.NumUtil.of;


/**
 * 交易统计  ->  胜率/盈亏比
 *
 * @author: bebopze
 * @date: 2025/7/25
 */
@Slf4j
public class TradePairStat {


    /**
     * 胜率 / 盈亏比（笔级）= 盈利笔数 / 总笔数                          // B+S（1买 -> 1卖） =>  1笔
     *
     * @param allTradeRecords 交易记录列表
     * @return
     */
    public static TradeStatResult calcTradeWinPct(List<BtTradeRecordDO> allTradeRecords) {
        if (CollectionUtils.isEmpty(allTradeRecords)) {
            return new TradeStatResult();
        }


        // -------------------------------------------------------------------------------------------------------------


        // B/S配对统计   ->   按 stockCode 分组
        Map<String, List<BtTradeRecordDO>> stock_recordList_Map = allTradeRecords.stream().collect(Collectors.groupingBy(BtTradeRecordDO::getStockCode));


        // -------------------------------------------------------------------------------------------------------------


        // ---------- 个股统计（stockCode维度  ->  单一个股 交易记录）

        Map<String, StockStat> stats = Maps.newHashMap();


        // ---------- 汇总统计（task维度  ->  全量 交易记录）


        // 交易总笔数（BS对  ->  1买 + 1卖）
        int totalTrades = 0;
        // 交易总金额
        double totalAmount = 0;


        // 盈利总笔数
        int winTrades = 0;
        // 亏损总笔数
        int lossTrades = 0;


        // 总盈利率
        double winTotalRate = 0;
        // 总亏损率
        double lossTotalRate = 0;


        // 总盈利金额
        double winTotalAmount = 0;
        // 总亏损金额
        double lossTotalAmount = 0;


        List<Double> winRateList = Lists.newArrayList();
        List<Double> lossRateList = Lists.newArrayList();

        // -------------------------------------------------------------------------------------------------------------


        for (Map.Entry<String, List<BtTradeRecordDO>> entry : stock_recordList_Map.entrySet()) {


            // --------------------------------------- 个股维度


            // 个股  -  交易记录列表
            String stockCode = entry.getKey();
            List<BtTradeRecordDO> tradeRecordDOList = entry.getValue();


            // ---------------------------------------


            // 同一 group 内记录   =>   已按 自然顺序（B->S 交易日期 时序） 排序   ->   可直接配对


            // 买入记录 队列
            Deque<BtTradeRecordDO> buyQueue = new ArrayDeque<>();


            // 对当前个股的 B/S交易记录列表 进行 逐笔遍历
            for (BtTradeRecordDO tr : tradeRecordDOList) {


                // BUY
                if (tr.getTradeType() == 1) {
                    // 买入  ->  加入队列
                    buyQueue.addLast(tr);
                }


                // SELL
                else if (tr.getTradeType() == 2) {

                    // 卖出  ->  尝试配对（有S -> 必有B）
                    if (!buyQueue.isEmpty()) {


                        // 单笔买入记录  ->  出列
                        BtTradeRecordDO buy = buyQueue.pollFirst(); // 1笔卖出（个股S信号 都是一次性清仓） ->  N笔买入（连续触发B信号 -> 中途加仓）


                        // 单笔盈亏率  =  sell_price / buy_price - 1
                        double winLossRate = tr.getPrice().doubleValue() / buy.getPrice().doubleValue() - 1.0;

                        double winLossAmount = tr.getAmount().doubleValue() - buy.getPrice().doubleValue() * tr.getQuantity();


                        // 单笔盈利（单笔盈亏率 > 0 ）
                        boolean win = winLossRate > 0;
                        // 单笔亏损（单笔盈亏率 < 0 ）
                        boolean loss = winLossRate < 0;


                        // ---------------- 个股统计（stockCode维度  ->  单一个股 交易记录）
                        calcStockStat(tr, win, stats);


                        // ---------------- 汇总统计（task维度  ->  全量 交易记录）

                        // 交易总笔数
                        totalTrades++;
                        // 交易总金额
                        totalAmount += tr.getAmount().doubleValue();

                        // 盈利总笔数、总盈利率
                        if (win) {
                            winTrades++;
                            winTotalRate += winLossRate;
                            winTotalAmount += winLossAmount;
                            winRateList.add(winLossRate);
                        }
                        // 亏损总笔数、总亏损率
                        if (loss) {
                            lossTrades++;
                            lossTotalRate += winLossRate;
                            lossTotalAmount += winLossAmount;
                            lossRateList.add(winLossRate);
                        }

                    } else {

                        log.error("BS配对 - 异常     >>>     stockCode : {} , buy : {} , sell : {}",
                                  stockCode, JSON.toJSONString(buyQueue), JSON.toJSONString(tr));
                    }
                }
            }
        }


        // 输出结果
        printStockStat(stats);


        // -------------------------------------------- 汇总统计


        // 交易胜率（%） =  盈利总笔数 / 交易总笔数
        double winTradesPct = totalTrades > 0 ? winTrades * 100.0 / totalTrades : 0.0;
        // 交易败率（%） =  亏损总笔数 / 交易总笔数
        double lossTradesPct = totalTrades > 0 ? lossTrades * 100.0 / totalTrades : 0.0;


        // 笔均盈利率（%） =  总盈利率 / 盈利总笔数
        double avgWinTradesPct = winTrades > 0 ? winTotalRate * 100.0 / winTrades : 0.0;
        // 笔均亏损率（%） =  总亏损率 / 亏损总笔数
        double avgLossTradesPct = winTrades > 0 ? Math.abs(lossTotalRate * 100.0 / lossTrades) : 0.0;


//        double avgWinTradesPct_2 = winRateList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100.0;
//        double avgLossTradesPct_2 = lossRateList.stream().mapToDouble(Double::doubleValue).map(Math::abs).average().orElse(0.0) * 100.0;


        // 笔均盈利金额
        double avgWinTradeAmount = winTotalAmount / winTrades;
        // 笔均亏损金额
        double avgLossTradeAmount = Math.abs(lossTotalAmount / lossTrades);

        // 笔均盈亏金额
        double avgWinLossTradeAmount = avgWinTradeAmount - avgLossTradeAmount;

//        double winTotalTradeAmount = avgWinLossTradeAmount * totalTrades;
//        double lossTotalTradeAmount = avgWinLossTradeAmount * (winTrades + lossTrades);


        // 笔均盈亏比 = 笔均盈利率 / 笔均亏损率
        double winLossRatio = avgLossTradesPct == 0 ? Double.POSITIVE_INFINITY : avgWinTradesPct / avgLossTradesPct;


        // --------------------------------------------------------- result


        TradeStatResult result = new TradeStatResult();
        // task维度
        result.setTotal(totalTrades);
        result.setTotalTradeAmount(totalAmount);

        result.setWinTotal(winTrades);
        result.setWinPct(of(winTradesPct));

        result.setLossTotal(lossTrades);
        result.setLossPct(of(lossTradesPct));

        result.setAvgWinPct(of(avgWinTradesPct));
        result.setAvgLossPct(of(avgLossTradesPct));
        result.setProfitFactor(of(winLossRatio));


        // 个股维度
        result.setStockStatList(Lists.newArrayList(stats.values()));


        return result;
    }


    /**
     * 个股统计（stockCode维度  ->  单一个股 交易记录）
     *
     * @param tr
     * @param win
     * @param stats
     */
    private static void calcStockStat(BtTradeRecordDO tr, boolean win, Map<String, StockStat> stats) {

        String code = tr.getStockCode();
        String name = tr.getStockName();
        double amount = tr.getAmount().doubleValue();


        // 个股统计
        StockStat stockStat = stats.computeIfAbsent(code, k -> new StockStat(code, name));


        // 交易总笔数
        stockStat.totalPairs++;

        // 交易总金额
        stockStat.totalAmount += amount;


        // 盈利
        if (win) {
            // 盈利总笔数
            stockStat.winCount++;
            // 交易胜率（%）
            stockStat.winPct = stockStat.totalPairs > 0 ? (stockStat.winCount * 100.0 / stockStat.totalPairs) : 0.0;
        }
    }


    private static void printStockStat(Map<String, StockStat> stats) {

        log.debug("股票代码\t股票名称\t总交易数\t胜利数\t胜率(%%)\n");
        for (StockStat stat : stats.values()) {

            double rate = stat.totalPairs > 0 ? (stat.winCount * 100.0 / stat.totalPairs) : 0.0;

            log.debug("{}", String.format("%s\t%s\t%d\t%d\t%.2f%%", stat.stockCode, stat.stockName, stat.totalPairs, stat.winCount, rate));
        }
    }


    /**
     * 胜率 / 盈亏比（日级）= 盈利天数 / 总天数
     *
     * @param dailyReturnPctList 日收益率列表（%）
     * @return
     */
    public static TradeStatResult calcDayWinPct(List<Double> dailyReturnPctList) {


        // 交易总天数
        int totalTrades = dailyReturnPctList.size();


        // 盈利总天数
        int winDays = (int) dailyReturnPctList.stream().filter(r -> r > 0).count();
        // 亏损总天数
        int lossDays = (int) dailyReturnPctList.stream().filter(r -> r < 0).count();


        // 交易胜率（%） =  盈利总天数 / 交易总天数
        double winDaysPct = totalTrades > 0 ? winDays * 100.0 / totalTrades : 0.0;
        // 交易败率（%） =  亏损总天数 / 交易总天数
        double lossDaysPct = totalTrades > 0 ? lossDays * 100.0 / totalTrades : 0.0;


        // 日均盈利率（%） =  所有盈利日的当日收益率平均值
        double avgWinDaysPct = dailyReturnPctList.stream().filter(r -> r > 0).mapToDouble(Double::doubleValue).average().orElse(0.0);
        // 日均亏损率（%） =  所有亏损日的当日收益率平均值
        double avgLossDaysPct = dailyReturnPctList.stream().filter(r -> r < 0).mapToDouble(Double::doubleValue).map(Math::abs).average().orElse(0.0);

        // 日均盈亏比 = 日均盈利率 / 日均亏损率
        double winLossDaysRatio = avgLossDaysPct == 0 ? Double.POSITIVE_INFINITY : avgWinDaysPct / avgLossDaysPct;


        // --------------------------------------------------------- result


        TradeStatResult result = new TradeStatResult();

        result.setTotal(totalTrades);
        result.setTotalTradeAmount(0.0);

        result.setWinTotal(winDays);
        result.setWinPct(of(winDaysPct));

        result.setLossTotal(lossDays);
        result.setLossPct(of(lossDaysPct));

        result.setAvgWinPct(of(avgWinDaysPct));
        result.setAvgLossPct(of(avgLossDaysPct));
        result.setProfitFactor(of(winLossDaysRatio));


        return result;
    }

    public static MaxDrawdownDTO calcMaxDrawdown(Map<LocalDate, Double> date_dailyReturn_Map) {

        // ---------------------------------------- 汇总指标


        double nav = 1.0;            // 初始净值
        double capital = 100_0000;   // 初始资金

        double maxNav = 1.0;         // 净值max
        double minNav = 1.0;         // 净值min
        double peakNav = 1.0;        // 净值波峰
        double troughNav = 1.0;      // 净值波谷

        LocalDate peakDate = null;   // 净值波峰 日期
        LocalDate troughDate = null; // 净值波谷 日期
        LocalDate maxNavDate = null; // 净值max 日期
        LocalDate minNavDate = null; // 净值min 日期

        double maxDrawdown = 0.0;    // 最大回撤


        List<TopPoolDailyReturnDTO> dailyReturnDTOList = Lists.newArrayList();
        Set<String> preCodeSet = Sets.newHashSet();


        // -------------------------------------------------------------------------------------------------------------


        // 逐日遍历 -> 计算


        for (Map.Entry<LocalDate, Double> entry : date_dailyReturn_Map.entrySet()) {
            LocalDate date = entry.getKey();
            Double daily_return = entry.getValue();


            // Set<String> todayCodeSet = date_positionCodeMap.getOrDefault(date, Sets.newHashSet());


            // ------------------------------------------ 每日 收益/净值 -------------------------------------------------


            double rate = 1 + daily_return;

            nav *= rate;
            capital *= rate;


            TopPoolDailyReturnDTO dr = new TopPoolDailyReturnDTO();
            dr.setDate(date);
            dr.setDaily_return(of(daily_return));
            dr.setNav(of(nav));
            dr.setCapital(of(capital));


            // ------------------------------------------ 每日 调仓换股 比例 ----------------------------------------------


            // posReplaceRatio(dr, preCodeSet, todayCodeSet);


            dailyReturnDTOList.add(dr);


            // -------------------------
            // preCodeSet = todayCodeSet;


            // ------------------------------------------ 波峰/波谷/最大回撤 ----------------------------------------------


            // init
            maxNavDate = maxNavDate == null ? date : maxNavDate;
            minNavDate = minNavDate == null ? date : minNavDate;


            // 当日创 最大净值   ->   新 波峰
            if (nav > maxNav) {
                // 波峰
                maxNav = nav;      // 更新历史最高净值
                maxNavDate = date;
            }
            if (nav < minNav) {
                minNav = nav;      // 更新历史最低净值
                minNavDate = date;
            }


            // 当日回撤  = （净值 - 波峰）/ 波峰
            double drawdown = (maxNav - nav) / maxNav;

            // 当日创 最大回撤   ->   新 波谷
            if (drawdown > maxDrawdown) {
                // 波谷
                maxDrawdown = drawdown;
                troughNav = nav;
                troughDate = date;

                // 波谷 -> 波峰
                peakNav = maxNav;
                peakDate = maxNavDate;
            }
        }


        // -------------------------------------------------------------------------------------------------------------


        MaxDrawdownDTO result = new MaxDrawdownDTO();

        result.setMaxDrawdownPct(of(maxDrawdown * -100));
        result.setPeakNav(of(peakNav));
        result.setPeakDate(peakDate);
        result.setTroughNav(of(troughNav));
        result.setTroughDate(troughDate);


        result.setMaxNav(of(maxNav));
        result.setMaxNavDate(maxNavDate);
        result.setMinNav(of(minNav));
        result.setMinNavDate(minNavDate);


        return result;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    public static class MaxDrawdownDTO {
        /**
         * 最大回撤（%）
         */
        public double maxDrawdownPct;
        public double peakNav;            // 净值波峰
        public LocalDate peakDate;        // 净值波峰 日期
        public double troughNav;          // 净值波谷
        public LocalDate troughDate;      // 净值波谷 日期

        public double maxNav;             // max净值
        public LocalDate maxNavDate;      // max净值 日期
        public double minNav;             // min净值
        public LocalDate minNavDate;      // min净值 日期
    }

    // 辅助类：股票统计数据
    @Data
    static class StockStat {

        String stockCode;
        String stockName;


        // 交易总笔数（BS对  ->  1买 + 1卖）
        int totalPairs = 0;
        // 盈利总笔数
        int winCount = 0;


        // 交易胜率（%）
        double winPct;


        // 交易总金额
        double totalAmount = 0;


        StockStat(String stockCode, String stockName) {
            this.stockCode = stockCode;
            this.stockName = stockName;
        }
    }


    // 辅助类：交易统计数据
    @Data
    public static class TradeStatResult {


        // ---------------- 汇总统计（task维度  ->  全量 交易记录）


        // 交易总金额
        public double totalTradeAmount = 0;

        // 交易总笔数（BS对  ->  1买 + 1卖）
        public int total = 0;


        // 盈利总笔数
        public int winTotal = 0;
        // 亏损总笔数
        public int lossTotal = 0;


        // 交易胜率（%） =  盈利总笔数 / 交易总笔数
        public double winPct = 0;
        // 交易败率（%） =  亏损总笔数 / 交易总笔数
        public double lossPct = 0;


        // 笔均盈利率（%）
        public double avgWinPct = 0;
        // 笔均亏损率（%）
        public double avgLossPct = 0;
        // 笔均盈亏比 = 笔均盈利率 / 笔均亏损率
        public double profitFactor = 0;


        // ---------------- 个股统计（stockCode维度  ->  单一个股 交易记录）

        List<StockStat> stockStatList;
    }


}