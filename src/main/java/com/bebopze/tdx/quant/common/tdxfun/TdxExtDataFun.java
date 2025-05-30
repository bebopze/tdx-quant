package com.bebopze.tdx.quant.common.tdxfun;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.FastJson2Config;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.parser.tdxdata.LdayParser;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.bebopze.tdx.quant.common.tdxfun.TdxExtFun.changePct;
import static com.bebopze.tdx.quant.service.impl.ExtDataServiceImpl.fillNaN;


/**
 * 通达信 - 扩展数据                           Java实现
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@Slf4j
public class TdxExtDataFun {


    public static void test1() {

        double[] arr = {3.0, 2.0, 1.0};


        double[] doubles1 = fillNaN(arr, 0);
        double[] doubles2 = fillNaN(arr, 1);
        double[] doubles3 = fillNaN(arr, 2);
        double[] doubles4 = fillNaN(arr, 3);
        double[] doubles5 = fillNaN(arr, 10);
        // double[] doubles6 = fillNaN(arr, -1);


        System.out.println(JSON.toJSONString(doubles1));
        System.out.println(JSON.toJSONString(doubles2));
        System.out.println(JSON.toJSONString(doubles3));
        System.out.println(JSON.toJSONString(doubles4));
        System.out.println(JSON.toJSONString(doubles5));


        double[] fillNaN_arr = doubles5;
        if (Double.isNaN(fillNaN_arr[0])) {
            double v = fillNaN_arr[0];

            System.out.println("fillNaN     >>>     v : " + v);

            System.out.println(Objects.equals(v, Double.NaN));
        }
    }

    public static void main(String[] args) {
        FastJson2Config fastJson2Config = new FastJson2Config();


        test1();


        // 从本地DB   加载5000支个股的收盘价序列
        Map<String, double[]> stockCloseArrMap = loadAllStockKline();


        Map<String, double[]> RPS_N = computeRPS(stockCloseArrMap, 50);
        System.out.println(JSON.toJSONString(RPS_N));


        // 打印某只股票的最近几日 RPS
        String stockCode = "300059";

        double[] rps_val = RPS_N.get(stockCode);
        System.out.println("rps_val : " + JSON.toJSONString(rps_val));


        System.out.println("最近10日 " + stockCode + " RPS: " +
                                   Arrays.toString(Arrays.copyOfRange(RPS_N.get(stockCode), RPS_N.get(stockCode).length - 10, RPS_N.get(stockCode).length))
        );
    }


    /**
     * 计算 RPS   ->   save2DB
     */
    public static void calcRps() {

        // 从本地DB   加载5000支个股的收盘价序列
        Map<String, double[]> priceMap = loadAllStockKline();


        Map<String, double[]> RPS50 = computeRPS(priceMap, 50);
        Map<String, double[]> RPS120 = computeRPS(priceMap, 120);// 120 -> 100
        Map<String, double[]> RPS250 = computeRPS(priceMap, 250);// 250 -> 200


        // save -> DB
        IBaseStockService baseStockService = MybatisPlusUtil.getBaseStockService();
        Map<String, Long> codeIdMap = baseStockService.codeIdMap();


        List<BaseStockDO> baseStockDOList = Lists.newArrayList();
        RPS50.forEach((stockCode, v) -> {

            BaseStockDO baseStockDO = new BaseStockDO();
            baseStockDO.setId(codeIdMap.get(stockCode));
            // baseStockDO.setRps();

            baseStockDOList.add(baseStockDO);
        });
        baseStockService.updateBatchById(baseStockDOList, 500);


        // TODO   refresh cache
    }


    /**
     * 从本地DB   加载全部（5000+支）个股的 收盘价序列
     *
     * @return stock - close_arr
     */
    private static Map<String, double[]> loadAllStockKline() {
        Map<String, double[]> stockCloseArrMap = Maps.newHashMap();


        // 加载  最近500日   行情数据
        int DAY_LIMIT = 500;


        BaseStockMapper mapper = MybatisPlusUtil.getMapper(BaseStockMapper.class);


        List<BaseStockDO> baseStockDOList = mapper.listAllKline();
        baseStockDOList.forEach(e -> {

            String stockCode = e.getCode();
            double[] close_arr = ConvertStockKline.fieldValArr(e.getKLineHis(), "close");


            // 上市1年
            if (close_arr.length > 200) {
                stockCloseArrMap.put(stockCode, fillNaN(close_arr, DAY_LIMIT));


                double[] fillNaN_arr = stockCloseArrMap.get(stockCode);
                if (Double.isNaN(fillNaN_arr[0])) {
                    log.debug("fillNaN     >>>     stockCode : {}", stockCode);
                }
            }
        });


        return stockCloseArrMap;
    }


    private List<double[]> closr_arr__list;


    public void initData() {

        List<LdayParser.LdayDTO> ldayDTOS = LdayParser.parseByStockCode("");


        // From DB     ->     klines
        this.closr_arr__list = null;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  基础指标
    // -----------------------------------------------------------------------------------------------------------------


//    /**
//     * 计算  全市场个股的 N日 RPS   （N日涨幅 - 排名百分比）
//     *
//     *
//     * 计算  N日 RPS：
//     *
//     * 1、计算  全市场个股（假设共5000只） 的   N日涨幅
//     * 2、全部个股 N日涨幅   从小到大排序
//     * 3、个股N日RPS : 个股N日涨幅 在总排名 中的百分比（0–100）
//     *
//     *
//     * -
//     *
//     * @param priceMap 全市场收盘价，key=股票代码，value=该股按时间顺序的收盘价数组
//     * @param N        计算涨幅的周期
//     * @return key=股票代码，value=该股 N日涨幅 在全市场中的 百分位排名（0–100）
//     */
//    public static Map<String, Double> computeRPS(Map<String, double[]> priceMap, int N) {
//
//
//        // 1. 计算每只股票的 N 日涨幅（调用前面定义的 changePct）
//        Map<String, Double> pctMap = new HashMap<>();
//        for (Map.Entry<String, double[]> entry : priceMap.entrySet()) {
//
//            String symbol = entry.getKey();
//            double[] close = entry.getValue();
//            double[] pctArr = changePct(close, N);
//
//            // 取序列最后一期的涨幅 作为当前涨幅
//            double latestPct = pctArr[pctArr.length - 1];
//            pctMap.put(symbol, latestPct);
//        }
//
//
//        // 2. 将所有涨幅值排序，准备计算百分位
//        List<Double> allPcts = new ArrayList<>(pctMap.values());
//        Collections.sort(allPcts);
//
//        int total = allPcts.size();
//
//
//        // 3. 对每只股票，计算其涨幅在排序列表中的位置 rank（<= 当前值的数量），然后转换为百分位
//        Map<String, Double> rpsMap = new HashMap<>();
//        for (Map.Entry<String, Double> entry : pctMap.entrySet()) {
//
//            String symbol = entry.getKey();
//            double pct = entry.getValue();
//
//            // 找到第一个大于 pct 的索引位置 idx
//            int idx = Collections.binarySearch(allPcts, pct);
//            if (idx < 0) {
//                idx = -idx - 1;
//            } else {
//                // 如果有重复值，binarySearch 可能返回任意一个，需调整到最后一个相同值
//                while (idx + 1 < total && Objects.equals(allPcts.get(idx + 1), pct)) {
//                    idx++;
//                }
//                idx++;
//            }
//
//            // 百分位 = idx / total * 100
//            double percentile = idx * 100.0 / total;
//            rpsMap.put(symbol, percentile);
//        }
//
//
//        return rpsMap;
//    }


    /**
     * 计算  全市场个股   N日RPS   序列     （RPS = N日涨幅 -> 总排名百分比）
     *
     *
     * 计算  N日 RPS：
     *
     * 1、计算  全市场个股（假设共5000只） 的   N日涨幅
     * 2、全部个股 N日涨幅   从小到大排序
     * 3、个股N日RPS : 个股N日涨幅 在总排名 中的百分比（0–100）
     *
     *
     * -
     *
     * @param stockCloseArrMap 全市场收盘价，key=股票代码，value=按时间顺序的收盘价数组
     * @param N                计算涨幅的周期（天数）
     * @return key=股票代码，value=该股按时间序列的 N日 RPS（0–100）
     */
    public static Map<String, double[]> computeRPS(Map<String, double[]> stockCloseArrMap, int N) {

        // 1. 首先计算每只股票的 N 日涨幅序列
        Map<String, double[]> pctMap = new HashMap<>();
        int totalStocks = stockCloseArrMap.size();
        int seriesLength = -1;

        for (Map.Entry<String, double[]> entry : stockCloseArrMap.entrySet()) {

            double[] close = entry.getValue();
            if (seriesLength < 0) seriesLength = close.length;
            double[] pct = changePct(close, N);

            pctMap.put(entry.getKey(), pct);
        }


        // 2. 对每个交易日 t，从所有股票的 pctMap 中收集该日的 N日涨幅值，排序
        //    并计算百分位
        Map<String, double[]> rpsMap = new HashMap<>();
        // 初始化 result arrays
        for (String stockCode : stockCloseArrMap.keySet()) {
            rpsMap.put(stockCode, new double[seriesLength]);
        }

        // 对每个交易日
        for (int t = 0; t < seriesLength; t++) {

            // 收集当日所有股票的涨幅
            List<StockPct> list = new ArrayList<>(totalStocks);
            for (Map.Entry<String, double[]> entry : pctMap.entrySet()) {
                double[] pctSeq = entry.getValue();
                double pct = pctSeq[t];
                list.add(new StockPct(entry.getKey(), pct));
            }

            // 按 pct 升序排序（NaN 放在开头）
            list.sort(Comparator.comparingDouble(sp -> Double.isNaN(sp.pct) ? Double.NEGATIVE_INFINITY : sp.pct));


            // 3. 计算每个股票的百分位 = (排名位置+1) / total * 100
            // 如果存在相同值，我们采用“最后一个相同值的位置”来计算，以保证并列股票同分位
            Map<String, Integer> lastIndex = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                lastIndex.put(list.get(i).stockCode, i);
            }
            for (StockPct sp : list) {
                int idx = lastIndex.get(sp.stockCode);
                double percentile = (idx + 1) * 100.0 / totalStocks;
                rpsMap.get(sp.stockCode)[t] = percentile;
            }
        }


        return rpsMap;
    }

    // 辅助类：存储单只股票在某一日的涨幅
    @AllArgsConstructor
    private static class StockPct {
        String stockCode;
        double pct;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  高级指标
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  复杂指标
    // -----------------------------------------------------------------------------------------------------------------


}