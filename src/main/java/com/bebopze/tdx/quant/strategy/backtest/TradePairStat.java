package com.bebopze.tdx.quant.strategy.backtest;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import com.bebopze.tdx.quant.dal.service.IBtTradeRecordService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * 交易统计  ->  胜率/...
 *
 * @author: bebopze
 * @date: 2025/7/25
 */
@Slf4j
@Component
public class TradePairStat {


    @Autowired
    private IBtTradeRecordService tradeRecordService;


    /**
     * 交易胜率  =  盈利笔数 / 总笔数                          // B+S（1买 -> 1卖） =>  1笔
     *
     * @param allTradeRecords
     * @return
     */
    public TradeStatResult calcTradeWinPct(List<BtTradeRecordDO> allTradeRecords) {


        // List<BtTradeRecordDO> allTradeRecords = tradeRecordService.listByTaskId(taskId);


        // ---------------------------------------------------------------------------------


        // 配对统计   ->   按 stockCode 分组


        // 个股  -  交易记录列表
        Map<String, List<BtTradeRecordDO>> stock_recordList_Map = Maps.newHashMap();
        for (BtTradeRecordDO tr : allTradeRecords) {
            stock_recordList_Map.computeIfAbsent(tr.getStockCode(), k -> Lists.newArrayList()).add(tr);
        }


        // ---------------------------------------------------------------------------------


        // ---------- 个股统计（stockCode维度  ->  单一个股 交易记录）

        Map<String, StockStat> stats = Maps.newHashMap();


        // ---------- 汇总统计（task维度  ->  全量 交易记录）

        // 交易总笔数（BS对  ->  1买 + 1卖）
        int totalPairs = 0;
        // 盈利总笔数
        int totalWinCount = 0;


        // ---------------------------------------------------------------------------------


        for (Map.Entry<String, List<BtTradeRecordDO>> entry : stock_recordList_Map.entrySet()) {


            // --------------------------------------- 个股维度


            // 个股  -  交易记录列表
            String stockCode = entry.getKey();
            List<BtTradeRecordDO> tradeRecordDOList = entry.getValue();


            // ---------------------------------------


            // 同一 group 内记录   =>   已按 自然顺序（B->S 交易日期 时序） 排序   ->   可直接配对


            // 买入记录 队列
            Deque<BtTradeRecordDO> buyQueue = new ArrayDeque<>();


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


                        // 买入记录  ->  出列
                        BtTradeRecordDO buy = buyQueue.pollFirst();

                        // 盈利  =  sell_price  >  buy_price
                        boolean win = tr.getPrice().doubleValue() > buy.getPrice().doubleValue();


                        // ---------------- 个股统计（stockCode维度  ->  单一个股 交易记录）
                        calcStockStat(tr, win, stats);


                        // ---------------- 汇总统计（task维度  ->  全量 交易记录）

                        // 交易总笔数
                        totalPairs++;
                        // 盈利总笔数
                        if (win) {
                            totalWinCount++;
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


        // 汇总统计
        double totalRate = totalPairs > 0 ? NumUtil.of(totalWinCount * 100.0 / totalPairs) : 0.0;

        log.debug("{}", String.format("合计\t\t%d\t%d\t%.2f%%", totalPairs, totalWinCount, totalRate));


        // --------------------------------------------------------- result


        TradeStatResult result = new TradeStatResult();
        // task维度
        result.setTotalWinCount(totalWinCount);
        result.setTotalPairs(totalPairs);
        result.setWinPct(totalRate);
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
    private void calcStockStat(BtTradeRecordDO tr, boolean win, Map<String, StockStat> stats) {

        String code = tr.getStockCode();
        String name = tr.getStockName();


        // 个股统计
        StockStat stockStat = stats.computeIfAbsent(code, k -> new StockStat(code, name));


        // 交易总笔数
        stockStat.totalPairs++;

        // 盈利
        if (win) {
            // 盈利总笔数
            stockStat.winCount++;
            // 交易胜率（%）
            stockStat.winPct = stockStat.totalPairs > 0 ? (stockStat.winCount * 100.0 / stockStat.totalPairs) : 0.0;
        }
    }


    private void printStockStat(Map<String, StockStat> stats) {

        log.debug("股票代码\t股票名称\t总交易数\t胜利数\t胜率(%%)\n");
        for (StockStat stat : stats.values()) {

            double rate = stat.totalPairs > 0 ? (stat.winCount * 100.0 / stat.totalPairs) : 0.0;

            log.debug("{}", String.format("%s\t%s\t%d\t%d\t%.2f%%", stat.stockCode, stat.stockName, stat.totalPairs, stat.winCount, rate));
        }
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 辅助类：股票统计数据
    @Data
    class StockStat {
        String stockCode, stockName;

        // 交易总笔数（BS对  ->  1买 + 1卖）
        int totalPairs = 0;
        // 盈利总笔数
        int winCount = 0;

        // 交易胜率（%）
        double winPct;


        StockStat(String stockCode, String stockName) {
            this.stockCode = stockCode;
            this.stockName = stockName;
        }
    }


    // 辅助类：交易统计数据
    @Data
    class TradeStatResult {


        // ---------------- 汇总统计（task维度  ->  全量 交易记录）


        // 交易总笔数（BS对  ->  1买 + 1卖）
        int totalPairs = 0;
        // 盈利总笔数
        int totalWinCount = 0;


        // 交易胜率  =  盈利总笔数 / 交易总笔数
        double winPct = 0;


        // ---------------- 个股统计（stockCode维度  ->  单一个股 交易记录）

        List<StockStat> stockStatList;
    }


}



