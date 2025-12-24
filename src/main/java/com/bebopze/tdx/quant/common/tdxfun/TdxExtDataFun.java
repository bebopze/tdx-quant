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
    public static Map<String, double[]> computeRPS_2(Map<String, LocalDate[]> codeDateMap,
                                                     Map<String, double[]> codeCloseMap,
                                                     int N) {


        // 1. 为每个股票构建：Map<日期, N日涨幅>，并同时收集所有“有效涨幅”的日期
        Map<String, TreeMap<LocalDate, Double>> returnsMap = new HashMap<>();
        Set<LocalDate> allDates = new TreeSet<>();  // 按自然升序保存所有出现过的涨幅日期


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
            int size = list.size();
            if (size == 0) continue;

            list.sort(Comparator.comparingDouble(Map.Entry::getValue));


            // 当日 参与排序 有效个股数量  必须 > 100（否则，当日异常）     // lastDays -> 截取最后N个   =>   期间存在：停牌个股 -> 会导致 日期异常（可能停牌5个月  =>  截取到的 K线日期 -> 5个月之前的K线  而非当前最近N天）
            if (size < codeDateMap.size() / 10) {
                log.error("当日参与排序个股数量小于【{} = code_size / 10】，异常！ date=[{}] size=[{}]", codeDateMap.size() / 10, date, size);
            }
            /*if (size == 1) {
                String code = list.get(0).getKey();
                int idx = dateIndexMap.get(code).get(date);
                rpsResult.get(code)[idx] = 100.0;
            }*/
            else {
                for (int i = 0; i < size; i++) {
                    String code = list.get(i).getKey();
                    double rps = (i * 1.0 / (size - 1)) * 100.0;
                    int idx = dateIndexMap.get(code).get(date);
                    rpsResult.get(code)[idx] = rps;
                }
            }
        }

        return rpsResult;
    }


    public static Map<String, double[]> computeRPS(Map<String, LocalDate[]> codeDateMap,
                                                   Map<String, double[]> codeCloseMap,
                                                   int N) {

        return computeRPS(codeDateMap, codeCloseMap, N, 0);
    }


    /**
     * 计算全市场个股的 N 日 RPS 序列（仅计算最后 lastDays 天的数据）。
     *
     * @param codeDateMap  Map<股票代码, LocalDate[] 日期序列>，日期按升序排列
     * @param codeCloseMap Map<股票代码, double[] 收盘价序列>，与日期数组一一对应
     * @param N            N 日涨幅周期
     * @param lastDays     仅计算最后 lastDays 天的数据（<=0 时计算全部）
     * @return Map<股票代码, double [ ]>            double[] 与该股日期序列长度（lastDays）一致，若无 RPS 则为 NaN
     */
    public static Map<String, double[]> computeRPS(Map<String, LocalDate[]> codeDateMap,
                                                   Map<String, double[]> codeCloseMap,
                                                   int N,
                                                   Integer lastDays) {


        if (lastDays == null || lastDays <= 0) {
            lastDays = Integer.MAX_VALUE;
        }


        // 1. N 日涨幅
        Set<LocalDate> allDates = new TreeSet<>();
        Map<String, TreeMap<LocalDate, Double>> returnsMap = returnsMap(codeDateMap, codeCloseMap, N, lastDays, allDates);


        // 2. 预分配 RPS 数组
        Map<String, double[]> rpsResult = new HashMap<>();
        Map<String, Map<LocalDate, Integer>> rpsDateIndexMap = new HashMap<>();

        Integer finalLastDays = lastDays;
        codeDateMap.forEach((code, dates) -> {
            int len = dates.length;


            int startIdx = Math.max(0, len - finalLastDays);
            int resultLen = len - startIdx;  // 实际需要计算的天数


            double[] rpsArr = new double[resultLen];
            Arrays.fill(rpsArr, Double.NaN);
            rpsResult.put(code, rpsArr);


            Map<LocalDate, Integer> idxMap = new HashMap<>();
            for (int i = startIdx, j = 0; i < len; i++, j++) {
                idxMap.put(dates[i], j); // j 是 RPS 数组中的索引
            }
            rpsDateIndexMap.put(code, idxMap);
        });


        // 3. 按日期聚合所有股票涨幅
        Map<LocalDate, List<Map.Entry<String, Double>>> dateToList = new TreeMap<>();
        for (LocalDate dt : allDates) {
            dateToList.put(dt, new ArrayList<>());
        }

        returnsMap.forEach((code, date_pct_Map) -> {
            date_pct_Map.forEach((date, pct) -> {
                dateToList.get(date).add(new AbstractMap.SimpleEntry<>(code, pct));
            });
        });


        // 4. 计算 RPS
        dateToList.forEach((date, list) -> {

            int size = list.size();
            if (size == 0) {
                return;
            }


            list.sort(Comparator.comparingDouble(Map.Entry::getValue));


            // 当日 参与排序 有效个股数量  必须 > 100（否则，当日异常）     // lastDays -> 截取最后N个   =>   期间存在：停牌个股 -> 会导致 日期异常（可能停牌5个月  =>  截取到的 K线日期 -> 5个月之前的K线  而非当前最近N天）
            if (size < codeDateMap.size() / 10) {
                log.error("当日参与排序个股数量小于【{} = code_size / 10】，异常！ date=[{}] size=[{}]", codeDateMap.size() / 10, date, size);
            } else {
                for (int i = 0; i < size; i++) {
                    String code = list.get(i).getKey();
                    double rps = (i * 100.0) / (size - 1);
                    int idx = rpsDateIndexMap.get(code).get(date);
                    rpsResult.get(code)[idx] = rps;
                }
            }
        });


        return rpsResult;
    }

    private static Map<String, TreeMap<LocalDate, Double>> returnsMap(Map<String, LocalDate[]> codeDateMap,
                                                                      Map<String, double[]> codeCloseMap,
                                                                      int N,
                                                                      Integer lastDays,
                                                                      Set<LocalDate> allDates) {


        Map<String, TreeMap<LocalDate, Double>> returnsMap = new HashMap<>();


        boolean limit = lastDays != null && lastDays > 0;


        for (String code : codeDateMap.keySet()) {
            LocalDate[] dates = codeDateMap.get(code);
            double[] closes = codeCloseMap.get(code);


            int len = dates.length;
            if (len <= N) {
//                returnsMap.put(code, new TreeMap<>());
                continue;
            }


            // 关键：盘中 lastDays 模式下，大幅减少遍历
            int startIdx;
            if (limit) {
                // 从尾部往前 lastDays 天
                startIdx = Math.max(N, len - lastDays);
            } else {
                startIdx = N;
            }


            TreeMap<LocalDate, Double> dayReturns = new TreeMap<>();

            for (int i = startIdx; i < len; i++) {
                double prev = closes[i - N];
                if (prev != 0) {
                    // N日涨幅
                    double pct = (closes[i] / prev - 1.0) * 100.0;

                    LocalDate date = dates[i];
                    dayReturns.put(date, pct);

                    allDates.add(date);
                }
            }


            returnsMap.put(code, dayReturns);
        }


        return returnsMap;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  高级指标
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  复杂指标
    // -----------------------------------------------------------------------------------------------------------------


}