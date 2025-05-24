package com.bebopze.tdx.quant.strategy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.EastMoneyKlineAPI;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.bebopze.tdx.quant.indicator.StockFunLast;
import com.bebopze.tdx.quant.parser.writer.TdxBlockNewWriter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 快捷操作
 *
 * @author: bebopze
 * @date: 2025/5/17
 */
@Slf4j
public class QuickOption {


    /**
     * 一键清仓     -     全部持仓
     *
     * @param fun
     */
    public static void 一键清仓(StockFunLast fun) {

    }

    public static void 一键建仓(StockFunLast fun) {

    }


    public static void 一键卖出(String stockCode) {
        一键卖出(new StockFunLast(stockCode));
    }

    /**
     * 一键清仓     -     指定个股
     *
     * @param fun
     */
    public static void 一键卖出(StockFunLast fun) {

        String stockCode = fun.getStockCode();
        String stockName = fun.getStockName();

        BigDecimal buy5 = fun.getShszQuoteSnapshotResp().getFivequote().getBuy5();


        // ---------- check


        // 持仓个股
        CcStockInfo ccStockInfo = fun.getQueryCreditNewPosResp().getStocks().stream()
                .filter(e -> Objects.equals(e.getStkcode(), stockCode))
                .findAny().orElse(null);
        Assert.notNull(ccStockInfo, String.format("当前个股 [%s-%s] 无持仓", stockCode, stockName));

        // 可卖数量
        Integer stkavl = ccStockInfo.getStkavl();
        Assert.isTrue(stkavl > 0, String.format("当前个股 [%s-%s] 可用数量不足：[%s]     >>>     ccStockInfo : %s",
                                                stockCode, stockName, stkavl, JSON.toJSONString(ccStockInfo)));


        // ---------- sell
        SubmitTradeV2Req req = new SubmitTradeV2Req();
        req.setStockCode(stockCode);
        req.setStockName(stockName);
        req.setPrice(buy5);
        req.setAmount(stkavl);

        req.setTradeTypeEnum(TradeTypeEnum.SELL);
        req.setMarket(ccStockInfo.getMarket());


        Integer wtbh = EastMoneyTradeAPI.submitTradeV2(req);
        log.info("[一键卖出] suc     >>>     委托编号 : {}", wtbh);
    }


    // 一键买入
    public static void 一键买入() {


    }


    /**
     * 等比买入 -
     *
     * @param stockCodeList
     * @param N             等比买入   排名 前N只
     */
    public static void 等比买入(List<String> stockCodeList, int N) {


        // 权重规则   倒序
        List<String> sort__stockCodeList = scoreSort(stockCodeList, N);


        // write   ->   TDX（策略-等比买入）
        TdxBlockNewWriter.write("CL-DBMR", sort__stockCodeList);


        // 最小等份   ->   30
        N = Math.min(N, stockCodeList.size());
        N = Math.max(N, 30);


        // ---------- 我的持仓 -> 可用金额
        QueryCreditNewPosResp queryCreditNewPosResp = EastMoneyTradeAPI.queryCreditNewPosV2();


        // 融资-剩余额度
        BigDecimal acreditavl = queryCreditNewPosResp.getAcreditavl();
        // 担保-剩余额度
        BigDecimal marginavl = queryCreditNewPosResp.getMarginavl();

        // 剩余总额度  =  融资 + 担保
        BigDecimal totalAmount = acreditavl.add(marginavl);


        // 等比额度  =  总额度 / N份
        double avgAmount = totalAmount.doubleValue() / (N * 1.05);


        // ---------- 等比买入
        avgBuy(sort__stockCodeList, avgAmount);
    }


    /**
     * 等比买入
     *
     * @param sort__stockCodeList
     * @param avgAmount
     */
    private static void avgBuy(List<String> sort__stockCodeList, double avgAmount) {

        for (String stockCode : sort__stockCodeList) {
            StockFunLast fun = new StockFunLast(stockCode);


            String stockName = fun.getStockName();

            // 卖5
            BigDecimal sell5 = fun.getShszQuoteSnapshotResp().getFivequote().getSale5();


            // 融资-剩余额度
            BigDecimal curr_acreditavl = fun.getQueryCreditNewPosResp().getAcreditavl();


            // ---------- buy
            SubmitTradeV2Req req = new SubmitTradeV2Req();
            req.setStockCode(stockCode);
            req.setStockName(stockName);
            req.setMarket(StockMarketEnum.getEastMoneyMarketByStockCode(stockCode));
            // 买入价格
            req.setPrice(sell5);


            // 可买数量
            int buyCount = buyCount(avgAmount, sell5);
            // 买入数量
            req.setAmount(buyCount * 100);


            // 买入金额
            double curr_buyAmount = req.getPrice().doubleValue() * req.getAmount();


            // 融资-剩余额度 > 买入金额
            if (curr_acreditavl.doubleValue() > curr_buyAmount) {


                req.setTradeTypeEnum(TradeTypeEnum.RZ_BUY);

                Integer wtbh = EastMoneyTradeAPI.submitTradeV2(req);
                log.info("[一键买入] suc     >>>     委托编号 : {}", wtbh);


            } else {

                // 最大-可买数量  =  融资-剩余额度 / 买入价格
                int rz_max_buyCount = (int) (curr_acreditavl.doubleValue() / sell5.doubleValue() / 100);

                if (rz_max_buyCount > 0) {

                    // 融资买
                    req.setAmount(rz_max_buyCount * 100);


                    req.setTradeTypeEnum(TradeTypeEnum.RZ_BUY);

                    Integer wtbh = EastMoneyTradeAPI.submitTradeV2(req);
                    log.info("[一键买入] suc     >>>     委托编号 : {}", wtbh);


                    // 担保买
                    req.setAmount((buyCount - rz_max_buyCount) * 100);

                    req.setTradeTypeEnum(TradeTypeEnum.ZY_BUY);

                    Integer wtbh2 = EastMoneyTradeAPI.submitTradeV2(req);
                    log.info("[一键买入] suc     >>>     委托编号 : {}", wtbh2);


                } else {

                    // 担保买
                    req.setTradeTypeEnum(TradeTypeEnum.ZY_BUY);

                    Integer wtbh2 = EastMoneyTradeAPI.submitTradeV2(req);
                    log.info("[一键买入] suc     >>>     委托编号 : {}", wtbh2);
                }
            }


            SleepUtils.sleep(100);
        }
    }


    /**
     * 权重规则   排序
     *
     * @param stockCodeList
     * @param N
     * @return
     */
    private static List<String> scoreSort(List<String> stockCodeList, int N) {

        Map<String, BigDecimal> stockCode_amo_map = Maps.newHashMap();
        Map<String, double[]> stockCode_close_map = Maps.newHashMap();
        Map<String, String> stockCode_stockName_map = Maps.newHashMap();


        // Step 1: 获取数据
        for (String stockCode : stockCodeList) {

            // 实时行情
            SHSZQuoteSnapshotResp shszQuoteSnapshotResp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
            String stockName = shszQuoteSnapshotResp.getName();

            // 历史行情
            StockKlineHisResp stockKlineHisResp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);
            List<String> klines = stockKlineHisResp.getKlines();
            double[] close = ConvertStockKline.fieldValArr(ConvertStockKline.strList2DTOList(klines), "close");


            BigDecimal amount = shszQuoteSnapshotResp.getRealtimequote().getAmount();
            stockCode_amo_map.put(stockCode, amount);
            stockCode_close_map.put(stockCode, close);
            stockCode_stockName_map.put(stockCode, stockName);


            // 间隔50ms
            SleepUtils.sleep(50);
        }


        // ----------------- 规则排名

        // 金额 -> 涨幅榜（近10日） -> ...

        // TODO     RPS（50） -> 大均线多头（20） -> 60日新高（10） -> 涨幅榜（10） -> 成交额-近10日（10） -> ...


        // Step 2: 计算各项指标 & 打分
        List<StockScore> scoredStocks = Lists.newArrayList();

        // 用于归一化处理
        double maxAmount = 0;
        double maxRecentReturn = 0;
        double maxMidReturn = 0;


        // Step 2.1: 遍历所有股票，计算原始值
        for (String code : stockCodeList) {
            String stockName = stockCode_stockName_map.get(code);


            double[] closes = stockCode_close_map.get(code);
            if (closes == null || closes.length < 20) continue;

            // 近10日涨幅：(今日收盘价 / 10日前收盘价 - 1)
            double changePct_d10 = (closes[closes.length - 1] / closes[closes.length - 11]) - 1;

            // 中期涨幅：(今日收盘价 / 20日前收盘价 - 1)
            double midReturn = (closes[closes.length - 1] / closes[closes.length - 21]) - 1;

            BigDecimal amount = stockCode_amo_map.get(code);

            // 更新最大值用于归一化
            maxAmount = Math.max(maxAmount, amount.doubleValue());
            maxRecentReturn = Math.max(maxRecentReturn, Math.abs(changePct_d10));
            maxMidReturn = Math.max(maxMidReturn, Math.abs(midReturn));

            scoredStocks.add(new StockScore(code, stockName, amount, changePct_d10, midReturn, 0));
        }


        // Step 3: 归一化 & 加权打分
        for (StockScore s : scoredStocks) {
            double amountScore = s.amount.doubleValue() / maxAmount * 50;            // 权重50%
            double recentScore = s.changePct_d10 / maxRecentReturn * 30;             // 权重30%
            double midScore = s.midTermChangePct / maxMidReturn * 20;                // 权重20%

            s.score = amountScore + recentScore + midScore;
        }


        // Step 4: 按得分排序，取前N名
        List<StockScore> topNStocks = scoredStocks.stream()
                .sorted(Comparator.comparingDouble((StockScore s) -> -s.score))
                .limit(N)
                .collect(Collectors.toList());


        // 输出结果或进一步操作
        topNStocks.forEach(JSON::toJSONString);


        return topNStocks.stream().map(StockScore::getStockCode).collect(Collectors.toList());
    }


    private static int buyCount(double avgAmount, BigDecimal sell5) {

        double count = avgAmount / sell5.doubleValue() / 100;


        // 整数
        int buyCount = (int) count;

        // 小数
        double decimalDigit = ((count * 10) % 10);
        // 凑整  ->  多买入1份
        if (decimalDigit >= 6.5) {
            buyCount++;
        }

        return buyCount;
    }


    public static void main(String[] args) {

        double avgAmount = 10000;
        double sell5 = 25.23;


        // 可买数量
        double count = avgAmount / sell5 / 100;

        // 小数位的值
        double decimalDigit = ((count * 10) % 10);

        // 凑整  ->  多买入1份
        if (decimalDigit >= 6.5) {
            count++;
        }


        System.out.println();
    }


    // 等比卖出 - 减仓
    public static void 等比卖出(String stockCode) {


    }

    private static int amoCompare(SHSZQuoteSnapshotResp o1, SHSZQuoteSnapshotResp o2) {
        return o1.getRealtimequote().getAmount().compareTo(o2.getRealtimequote().getAmount());
    }


    @Data
    @AllArgsConstructor
    public static class StockScore {

        String stockCode;
        String stockName;
        BigDecimal amount;           // 成交额
        double changePct_d10;        // 近10日涨幅
        double midTermChangePct;     // 中期涨幅
        double score;                // 总分
    }
}
