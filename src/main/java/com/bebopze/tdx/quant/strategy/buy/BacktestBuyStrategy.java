package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.convert.ConvertDate;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.strategy.QuickOption;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 回测 - B策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategy extends BuyStrategy {


    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;


    public void initData(BacktestStrategy backTestStrategy) {


        // BackTestStrategy strategy = strategyThreadLocal.get();


        this.dateIndexMap = backTestStrategy.getDateIndexMap();


        this.blockDOList = backTestStrategy.getBlockDOList();
        this.block__dateCloseMap = backTestStrategy.getBlock__dateCloseMap();


        this.stockDOList = backTestStrategy.getStockDOList();
        this.stock__dateCloseMap = backTestStrategy.getStock__dateCloseMap();
    }


    public List<String> rule(BacktestStrategy backTestStrategy, LocalDate tradeDate) {


        initData(backTestStrategy);


        // -------------------------------------------------------------------------------------------------------------
        //                                                      板块
        // -------------------------------------------------------------------------------------------------------------


        List<String> filter__blockCodeList = Lists.newArrayList();
        for (BaseBlockDO baseBlockDO : blockDOList) {
            String blockCode = baseBlockDO.getCode();


            // 1、in__板块-月多


            // 2、in__板块-60日新高

            // 3、in__板块-RPS三线红


            // 4、in__板块占比-TOP1


            // 5、xxx


            BlockFun blockFun = new BlockFun(blockCode, baseBlockDO);


            boolean[] 月多_arr = blockFun.月多();

            boolean[] _60日新高_arr = blockFun.N日新高(60);
            boolean[] RPS三线红_arr = blockFun.RPS三线红(80);

            boolean[] 大均线多头_arr = blockFun.大均线多头();

            boolean[] SSF多_arr = blockFun.SSF多();


            boolean 月多 = getByDate(月多_arr, tradeDate);
            boolean _60日新高 = getByDate(_60日新高_arr, tradeDate);
            boolean RPS三线红 = getByDate(RPS三线红_arr, tradeDate);
            boolean 大均线多头 = getByDate(大均线多头_arr, tradeDate);
            boolean SSF多 = getByDate(SSF多_arr, tradeDate);


            boolean flag = 月多 && (_60日新高 || RPS三线红 || 大均线多头) && SSF多;
            if (flag) {
                filter__blockCodeList.add(blockCode);
            }
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                                      个股
        // -------------------------------------------------------------------------------------------------------------


        List<String> filter__stockCodeList = Lists.newArrayList();
        for (BaseStockDO baseStockDO : stockDOList) {
            String stockCode = baseStockDO.getCode();


            // 1、in__60日新高

            // 2、in__月多


            // 3、in__RPS三线红

            // 4、in__大均线多头


            // 5、SSF多

            // 6、xxx


            StockFun fun = new StockFun(stockCode, baseStockDO);


            boolean[] 月多_arr = fun.月多();

            boolean[] _60日新高_arr = fun.N日新高(60);
            boolean[] RPS三线红_arr = fun.RPS三线红(80);

            boolean[] 大均线多头_arr = fun.大均线多头();

            boolean[] SSF多_arr = fun.SSF多();


            boolean 月多 = getByDate(月多_arr, tradeDate);
            boolean _60日新高 = getByDate(_60日新高_arr, tradeDate);
            boolean RPS三线红 = getByDate(RPS三线红_arr, tradeDate);
            boolean 大均线多头 = getByDate(大均线多头_arr, tradeDate);
            boolean SSF多 = getByDate(SSF多_arr, tradeDate);


            boolean flag = _60日新高 && 月多 && (RPS三线红 || 大均线多头) && SSF多;
            if (flag) {
                filter__stockCodeList.add(stockCode);
            }
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                                   板块 - 个股
        // -------------------------------------------------------------------------------------------------------------


        // List<BaseBlockDO> baseBlockDOList = baseBlockRelaStockService.listBlockByStockCodeList(filter__stockCodeList);

        List<BaseStockDO> baseStockDOList = baseBlockRelaStockService.listStockByBlockCodeList(filter__blockCodeList);
        List<String> filterBlock__stockCodeList = baseStockDOList.stream().map(BaseStockDO::getCode).collect(Collectors.toList());


        // 交集
        Collection<String> intersection__stockCodeList = CollectionUtils.intersection(filterBlock__stockCodeList, filter__stockCodeList);


        // 按照 规则打分 -> sort
        List<String> filterSort__stockCodeList = scoreSort(intersection__stockCodeList, 30);


        return filterSort__stockCodeList;
    }


    /**
     * 权重规则   排序
     *
     * @param stockCodeList
     * @param N
     * @return
     */
    public static List<String> scoreSort(Collection<String> stockCodeList, int N) {

        Map<String, BigDecimal> stockCode_amo_map = Maps.newHashMap();
        Map<String, double[]> stockCode_close_map = Maps.newHashMap();
        Map<String, String> stockCode_stockName_map = Maps.newHashMap();


//        // Step 1: 获取数据
//        for (String stockCode : stockCodeList) {
//
//            // 实时行情
//            SHSZQuoteSnapshotResp shszQuoteSnapshotResp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
//            String stockName = shszQuoteSnapshotResp.getName();
//
//            // 历史行情
//            StockKlineHisResp stockKlineHisResp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);
//            List<String> klines = stockKlineHisResp.getKlines();
//            double[] close = ConvertStockKline.fieldValArr(ConvertStockKline.strList2DTOList(klines), "close");
//
//
//            BigDecimal amount = shszQuoteSnapshotResp.getRealtimequote().getAmount();
//            stockCode_amo_map.put(stockCode, amount);
//            stockCode_close_map.put(stockCode, close);
//            stockCode_stockName_map.put(stockCode, stockName);
//
//
//            // 间隔50ms
//            SleepUtils.sleep(50);
//        }


        // ----------------- 规则排名

        // 金额 -> 涨幅榜（近10日） -> ...

        // TODO     RPS（50） -> 大均线多头（20） -> 60日新高（10） -> 涨幅榜（10） -> 成交额-近10日（10） -> ...


        // Step 2: 计算各项指标 & 打分
        List<QuickOption.StockScore> scoredStocks = Lists.newArrayList();

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

            scoredStocks.add(new QuickOption.StockScore(code, stockName, amount, changePct_d10, midReturn, 0));
        }


//        // Step 3: 归一化 & 加权打分
//        for (QuickOption.StockScore s : scoredStocks) {
//            double amountScore = s.amount.doubleValue() / maxAmount * 50;            // 权重50%
//            double recentScore = s.changePct_d10 / maxRecentReturn * 30;             // 权重30%
//            double midScore = s.midTermChangePct / maxMidReturn * 20;                // 权重20%
//
//            s.score = amountScore + recentScore + midScore;
//        }


        // Step 4: 按得分排序，取前N名
        List<QuickOption.StockScore> topNStocks = scoredStocks.stream()
                .sorted(Comparator.comparingDouble((QuickOption.StockScore s) -> -s.getScore()))
                .limit(N)
                .collect(Collectors.toList());


        // 输出结果或进一步操作
        topNStocks.forEach(JSON::toJSONString);


        return topNStocks.stream().map(QuickOption.StockScore::getStockCode).collect(Collectors.toList());
    }


    private boolean getByDate(boolean[] arr, LocalDate date) {

        boolean result = ConvertDate.getByDate(arr, date);


        int idx = dateIndexMap.get(DateTimeUtil.format_yyyy_MM_dd(date));
        boolean result2 = arr[idx];


        log.debug("getByDate     >>>     {}", result == result2);
        return result2;
    }


    /**
     * 个股   指定日期 -> 收盘价
     *
     * @param blockCode
     * @param tradeDate
     * @return
     */
    private double getBlockClosePrice(String blockCode, LocalDate tradeDate) {
        Double closePrice = stock__dateCloseMap.get(blockCode).get(DateTimeUtil.format_yyyy_MM_dd(tradeDate));
        return closePrice == null ? 0.0 : closePrice;
    }

    /**
     * 个股   指定日期 -> 收盘价
     *
     * @param stockCode
     * @param tradeDate
     * @return
     */
    private double getStockClosePrice(String stockCode, LocalDate tradeDate) {
        Double closePrice = stock__dateCloseMap.get(stockCode).get(DateTimeUtil.format_yyyy_MM_dd(tradeDate));
        return closePrice == null ? 0.0 : closePrice;
    }

}
