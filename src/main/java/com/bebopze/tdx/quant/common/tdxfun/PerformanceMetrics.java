package com.bebopze.tdx.quant.common.tdxfun;

import java.util.Arrays;
import java.util.List;


/**
 * 卡玛/夏普/sortino/年化/最大回撤
 *
 * @author: bebopze
 * @date: 2025/10/23
 */
public class PerformanceMetrics {


    /**
     * 1年 交易日天数
     */
    private static final int DEFAULT_TRADING_DAYS = 252;


    /**
     * 计算 Sharpe, Sortino, Calmar（默认以 tradingDays=252 计算）
     *
     * @param dailyReturnRateList 日度收益（小数，如 0.02 表示 2%）
     * @param rfAnnual            年化无风险利率（小数），例如 0.02
     */
    public static Result computeAll(List<Double> dailyReturnRateList, double rfAnnual) {
        return computeAll(dailyReturnRateList, rfAnnual, DEFAULT_TRADING_DAYS);
    }


    public static Result computeAll(List<Double> dailyReturnRateList, double rfAnnual, int tradingDays) {
        Result res = new Result();
        if (dailyReturnRateList == null || dailyReturnRateList.isEmpty()) return res;


        int N = dailyReturnRateList.size();
        // 日均无风险利率
        double rfDaily = Math.pow(1.0 + rfAnnual, 1.0 / tradingDays) - 1.0;


        // 1) 日度均值与标准差（用于 Sharpe 的近似）
        // 日均收益
        double meanDaily = dailyReturnRateList.stream().mapToDouble(e -> e).summaryStatistics().getAverage();

        // 日收益标准差  =  sqrt( sum(diff ^2) / size )
        double sdDaily = Math.sqrt(dailyReturnRateList.stream()
                                                      .mapToDouble(r -> Math.pow(r - meanDaily, 2))
                                                      .sum() / N);   // 总体标准差 -> N       若需 样本标准差 stddev 用 (N-1)


        // 日均超额收益 = 日均收益 - 日均无风险利率
        double meanExcessDaily = meanDaily - rfDaily;
        // 算术 年化超额收益
        double annExcess = meanExcessDaily * tradingDays;
        // 年化标准差
        double annVol = sdDaily * Math.sqrt(tradingDays);
        // 夏普比例 = 年化超额收益 / 年化标准差
        res.sharpe = annVol == 0.0 ? Double.NaN : annExcess / annVol;


        // Sortino: 计算下行偏差（以 rfDaily 或 0 为目标）
        double target = rfDaily; // 可改为 0.0
        double sumDownSq = 0.0;
        int downCount = 0;
        for (double r : dailyReturnRateList) {
            double diff = r - target;
            if (diff < 0) {
                sumDownSq += diff * diff;
                downCount++;
            }
        }
        double downsideDaily = 0.0;
        if (downCount > 0) {
            downsideDaily = Math.sqrt(sumDownSq / N); // 使用 N 作为分母（与上面 sdDaily 一致）
        } else {
            downsideDaily = 0.0;
        }
        double downsideAnn = downsideDaily * Math.sqrt(tradingDays);

        // 年化收益（这里用几何年化更适合 Calmar，但用于 Sortino/Sharpe 的分子也可以用几何或算术）
        double geomAnnReturn = geometricAnnualReturn(dailyReturnRateList, tradingDays);
        double annReturnForRatios = geomAnnReturn; // 可换为 meanDaily*tradingDays

        // Sortino 用年化超额收益（用 geom 或 算术 均可，但需写明）
        double annExcessForSortino = annReturnForRatios - rfAnnual;
        res.sortino = (downsideAnn == 0.0) ? Double.NaN : annExcessForSortino / downsideAnn;


        // Calmar: 年化收益 / maxDrawdown
        double maxDd = maxDrawdown(dailyReturnRateList);
        res.maxDrawdown = maxDd; // 例如 0.4 表示 40%
        res.annualizedReturn = annReturnForRatios;
        res.calmar = (maxDd == 0.0) ? Double.NaN : annReturnForRatios / maxDd;


        return res;
    }


    // 计算基于净值序列的几何年化收益，若发生严重损失使得 final <= 0，则返回 -1.0
    private static double geometricAnnualReturn(List<Double> dailyReturns, int tradingDays) {
        int N = dailyReturns.size();
        double logSum = 0.0;
        for (double r : dailyReturns) {
            double mult = 1.0 + r;
            if (mult <= 0.0) {
                return -1.0; // 发生完全亏损或负净值，返回特殊值（直接亏损-100%了）
            }
            logSum += Math.log(mult);
        }
        double totalReturn = Math.exp(logSum); // V_T / V_0
        double ann = Math.pow(totalReturn, (double) tradingDays / N) - 1.0;
        return ann;
    }


    // 计算最大回撤（返回正值，例如 0.4 表示 40% 回撤）
    private static double maxDrawdown(List<Double> dailyReturns) {
        double peak = 1.0;
        double nav = 1.0;
        double maxDd = 0.0;

        for (double r : dailyReturns) {
            nav *= (1.0 + r);

            if (nav > peak) {
                peak = nav;
            }

            double dd = (peak - nav) / peak; // 例如 0.2 表示 20% 回撤
            if (dd > maxDd) {
                maxDd = dd;
            }
        }
        return maxDd;
    }


    public static class Result {
        // 夏普
        public double sharpe;
        // sortino
        public double sortino;
        // 卡玛
        public double calmar;
        // 年化（小数）
        public double annualizedReturn;
        // 最大回撤（小数）
        public double maxDrawdown;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 简单示例运行
    public static void main(String[] args) {

        List<Double> daily = Arrays.asList(0.02, -0.01, 0.015, -0.03, 0.0, 0.01);
        Result r = computeAll(daily, 0.02);

        System.out.println("Sharpe=" + r.sharpe);
        System.out.println("Sortino=" + r.sortino);
        System.out.println("Calmar=" + r.calmar);
        System.out.println("AnnReturn=" + r.annualizedReturn + ", MaxDD=" + r.maxDrawdown);
    }


}