package com.bebopze.tdx.quant.common.tdxfun;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;


/**
 * 通达信 - 扩展数据                           Java实现
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@Slf4j
public class TdxExtDataFun {


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  基础指标
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 计算  全市场个股   N日RPS   序列     （RPS = N日涨幅 -> 总排名百分位）            calculateNDayRPS
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

    /**
     * 计算全市场个股的 N 日 RPS 序列。
     *
     * @param N            N 日涨幅周期
     * @param codeDateMap  Map<股票代码, String[] 日期序列>，日期按升序排列
     * @param codeCloseMap Map<股票代码, double[] 收盘价序列>，与日期数组一一对应
     * @return Map<股票代码, double [ ]>            double[] 与该股日期序列长度一致，若无 RPS 则为 NaN
     */
    public static Map<String, double[]> computeRPS(Map<String, LocalDate[]> codeDateMap,
                                                   Map<String, double[]> codeCloseMap,
                                                   int N) {


//        Map<String, TreeMap<String, Double>> stockPriceMap = Maps.newHashMap();


        // 1. 为每个股票构建：Map<日期, N日涨幅>，并同时收集所有“有效涨幅”的日期
        Map<String, TreeMap<LocalDate, Double>> returnsMap = new HashMap<>();
        Set<LocalDate> allDates = new TreeSet<>();  // 按自然升序保存所有出现过的涨幅日期


//        TreeMap<String, Double> datePctMap = new TreeMap<>();
//
//
//        stockPriceMap.forEach((code, dateCloseMap) -> {
//
//
//            int size = dateCloseMap.size();
//            double prev = Double.NaN;
//            if (size > N) {
//
//                for (Map.Entry<String, Double> entry : dateCloseMap.entrySet()) {
//                    String date = entry.getKey();
//                    Double close = entry.getValue();
//
//
//                    if (!Double.isNaN(prev)) {
//                        double pct = (close / prev - 1.0) * 100.0;
//                        datePctMap.put(date, pct);
//                        allDates.add(date);
//                    }
//                    prev = close;
//                    returnsMap.put(code, datePctMap);
//                }
//            }
//        });

        for (String code : codeDateMap.keySet()) {
            LocalDate[] dates = codeDateMap.get(code);
            double[] closes = codeCloseMap.get(code);
            TreeMap<LocalDate, Double> dayReturns = new TreeMap<>();

            if (dates.length > N) {
                for (int i = N; i < dates.length; i++) {
                    double prev = closes[i - N];
                    if (prev != 0) {
                        double pct = (closes[i] / prev - 1.0) * 100.0;
                        LocalDate dt = dates[i];
                        dayReturns.put(dt, pct);
                        allDates.add(dt);
                    }
                }
            }
            returnsMap.put(code, dayReturns);
        }

        // 2. 为每个股票预分配 RPS 结果数组，长度与其日期序列一致，填充 NaN
        Map<String, double[]> rpsResult = new HashMap<>();
        Map<String, Map<LocalDate, Integer>> dateIndexMap = new HashMap<>();

        for (String code : codeDateMap.keySet()) {
            LocalDate[] dates = codeDateMap.get(code);
            int len = dates.length;
            double[] rpsArr = new double[len];
            Arrays.fill(rpsArr, Double.NaN);
            rpsResult.put(code, rpsArr);

            // 构建 日期->索引 映射，便于后续快速定位
            Map<LocalDate, Integer> idxMap = new HashMap<>();
            for (int i = 0; i < len; i++) {
                idxMap.put(dates[i], i);
            }
            dateIndexMap.put(code, idxMap);
        }

        // 3. 构建“按日期聚合所有股票涨幅”的结构：Map<日期, List< (code, pct) >>
        Map<LocalDate, List<Map.Entry<String, Double>>> dateToList = new TreeMap<>();
        for (LocalDate date : allDates) {
            dateToList.put(date, new ArrayList<>());
        }
        for (String code : returnsMap.keySet()) {
            TreeMap<LocalDate, Double> codeReturns = returnsMap.get(code);
            for (Map.Entry<LocalDate, Double> e : codeReturns.entrySet()) {
                LocalDate date = e.getKey();
                double pct = e.getValue();
                dateToList.get(date).add(new AbstractMap.SimpleEntry<>(code, pct));
            }
        }

        // 4. 对每个日期，按涨幅升序排序，计算 RPS 并写入对应股票的结果数组
        for (LocalDate date : dateToList.keySet()) {
            List<Map.Entry<String, Double>> list = dateToList.get(date);
            int m = list.size();
            if (m == 0) continue;

            list.sort(Comparator.comparingDouble(Map.Entry::getValue));

            if (m == 1) {
                String code = list.get(0).getKey();
                int idx = dateIndexMap.get(code).get(date);
                rpsResult.get(code)[idx] = 100.0;
            } else {
                for (int i = 0; i < m; i++) {
                    String code = list.get(i).getKey();
                    double rps = (i * 1.0 / (m - 1)) * 100.0;
                    int idx = dateIndexMap.get(code).get(date);
                    rpsResult.get(code)[idx] = rps;
                }
            }
        }

        return rpsResult;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  高级指标
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  复杂指标
    // -----------------------------------------------------------------------------------------------------------------


}