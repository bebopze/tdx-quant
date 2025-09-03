package com.bebopze.tdx.quant.common.tdxfun;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

import static com.bebopze.tdx.quant.common.tdxfun.TdxExtFun.均线预萌出;
import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.*;


/**
 * 通达信“月多”量化公式 — 动态周/月聚合修正版                                     // 未来函数 bug   ->   修复版
 *
 * 关键修正点：
 * 1) 对于任意交易日 d，当日关联的「周K」= 本周周一(或本周首个交易日)至 d 的动态聚合；
 * 当周未结束前，周K 的 close/high/low 会每日更新。
 * 2) 对于任意交易日 d，当日关联的「月K」= 当月1日(或当月首个交易日)至 d 的动态聚合；
 * 当月未结束前，月K 的 close/high/low 会每日更新。
 * 3) 依上述动态聚合序列，逐日计算：周级 MACD、周级 SAR、月级 MACD 及其派生条件；
 * 月多 := MACD月多 && (SAR周多 || 均线预萌出)。
 *
 * 说明：为保证与既有 TdxFun 指标实现(如 MACD / TDX_SAR / BARSLASTCOUNT)的一致性，
 * 本实现在“逐日循环”中，每天都用“当前已完成周/月 + 当前进行中的动态周/月”序列，
 * 直接调用对应函数重新计算需要的【最新一根】周/月指标值。这样可以避免手写 EMA/SAR 的状态机差异。
 * 在一般日线样本规模下(数千根)，性能足以满足回测/选股需求。
 */
@Slf4j
public class MonthlyBullSignal2 {

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * 输入：按交易日先后排序的日K序列
     * 输出：与日K等长的布尔数组，标记每个交易日是否满足“月多”
     */
    public static boolean[] computeMonthlyBull(List<MonthlyBullSignal.KlineBar> dailyKlines) {
        final int nDays = dailyKlines.size();
        if (nDays == 0) return new boolean[0];


        // ---------- 1) 提取日线数组 ----------
        double[] dayClose = new double[nDays];
        double[] dayHigh = new double[nDays];
        double[] dayLow = new double[nDays];
        LocalDate[] dayDate = new LocalDate[nDays];
        double[] dayOpen = new double[nDays];

        for (int i = 0; i < nDays; i++) {
            MonthlyBullSignal.KlineBar b = dailyKlines.get(i);
            dayDate[i] = b.date;
            dayOpen[i] = b.open;
            dayClose[i] = b.close;
            dayHigh[i] = b.high;
            dayLow[i] = b.low;
        }


        // ---------- 2) 日线指标(一次计算，循环内复用) ----------
        // MACD(日) 及 10日HHV(MACD)
        double[][] macdDay = MACD(dayClose);
        double[] DIF_D = macdDay[0];
        double[] DEA_D = macdDay[1];
        double[] MACD_D = macdDay[2];
        double[] HHV_MACD_10 = HHV(MACD_D, 10);

        // 均线预萌出(日) 序列
        boolean[] preBreak = 均线预萌出(dayClose);


        // ---------- 3) 迭代构建“动态周/月序列”，逐日计算 ----------
        boolean[] monthlyBull = new boolean[nDays];

        final WeekFields ISO = WeekFields.ISO; // 周一为一周起始

        // 累积保存“到当天为止”的周/月K序列（包含当前进行中的动态周/月K作为末尾一根）
        List<MonthlyBullSignal.KlineBar> weeks = new ArrayList<>();
        List<MonthlyBullSignal.KlineBar> months = new ArrayList<>();

        // 追踪当前进行中的周/月“键”
        Integer curWeekYear = null; // weekBasedYear
        Integer curWeekNo = null;   // weekOfWeekBasedYear
        YearMonth curYm = null;

        for (int i = 0; i < nDays; i++) {
            LocalDate d = dayDate[i];
            int wYear = d.get(ISO.weekBasedYear());
            int wNo = d.get(ISO.weekOfWeekBasedYear());
            YearMonth ym = YearMonth.from(d);


            // ---- 周K 动态聚合 ----
            if (curWeekYear == null || !curWeekYear.equals(wYear) || !curWeekNo.equals(wNo)) {
                // 新的一周（第一天）：追加一根周K（以当日作为当前周的“最新日”）
                weeks.add(new MonthlyBullSignal.KlineBar(d, dayOpen[i], dayHigh[i], dayLow[i], dayClose[i]));
                curWeekYear = wYear;
                curWeekNo = wNo;
            } else {
                // 一周只有 一根K线（每天动态计算 替换本周old_val）

                // 同一周：更新最后一根的 high/low/close（open 不变）
                MonthlyBullSignal.KlineBar last = weeks.get(weeks.size() - 1);
                last.high = Math.max(last.high, dayHigh[i]);
                last.low = Math.min(last.low, dayLow[i]);
                last.close = dayClose[i];
                last.date = d; // 用当前交易日作为该动态周K的代表日期
            }


            // ---- 月K 动态聚合 ----
            if (curYm == null || !curYm.equals(ym)) {
                // 新的一月（第一天）：追加一根月K（以当日作为当前月的“最新日”）
                months.add(new MonthlyBullSignal.KlineBar(d, dayOpen[i], dayHigh[i], dayLow[i], dayClose[i]));
                curYm = ym;
            } else {
                // 一月只有 一根K线（每天动态计算 替换本月old_val）
                MonthlyBullSignal.KlineBar last = months.get(months.size() - 1);
                last.high = Math.max(last.high, dayHigh[i]);
                last.low = Math.min(last.low, dayLow[i]);
                last.close = dayClose[i];
                last.date = d; // 用当前交易日作为该动态月K的代表日期
            }


            // ----------------------------------------------------------------


            // ---- 取“当前”为止的周/月数组 ----
            double[] wClose = extractClose(weeks);
            double[] wHigh = extractHigh(weeks);
            double[] wLow = extractLow(weeks);

            double[] mClose = extractClose(months);

            // ---- 周级 MACD (以“动态周序列”的最后一根为当前) ----
            double[][] macdW = MACD(wClose);
            double macdW_last = macdW[2][macdW[2].length - 1];
            boolean macdWeekCross = macdW_last >= 0; // MACD周金叉

            // ---- 月级 MACD + 近似金叉判定 (以“动态月序列”的最后一根为当前) ----
            double[][] macdM = MACD(mClose);
            double[] DIF_M = macdM[0];
            double[] DEA_M = macdM[1];
            double[] MACD_M = macdM[2];
            int mLen = MACD_M.length;

            double absDIF_M = Math.abs(DIF_M[mLen - 1]);
            double absDEA_M = Math.abs(DEA_M[mLen - 1]);
            double ratio = (absDEA_M == 0 && absDIF_M == 0)
                    ? 0.0
                    : Math.min(absDEA_M, absDIF_M) / Math.max(absDEA_M, absDIF_M);

            double[] HHV_MACD_M_9 = HHV(MACD_M, 9);
            double HHV_MACD_M_9_last = HHV_MACD_M_9[mLen - 1];

            // REF(DIF_M,1)
            double[] REF_DIF_M_1 = REF(DIF_M, 1);
            boolean[] c1 = new boolean[mLen]; // DIF_M >= REF(DIF_M,1)
            boolean[] c2 = new boolean[mLen]; // DIF_M >  REF(DIF_M,1)
            for (int k = 0; k < mLen; k++) {
                boolean valid = !Double.isNaN(REF_DIF_M_1[k]);
                c1[k] = valid && (DIF_M[k] >= REF_DIF_M_1[k]);
                c2[k] = valid && (DIF_M[k] > REF_DIF_M_1[k]);
            }
            int[] blc1 = BARSLASTCOUNT(c1);
            int[] blc2 = BARSLASTCOUNT(c2);
            int blc1_last = blc1[mLen - 1];
            int blc2_last = blc2[mLen - 1];

            boolean MACD_M_nearCross = (blc1_last >= (int) (1.2 * 20) && ratio >= 0.9)
                    || (blc2_last >= 1 && ratio >= 0.95);

            boolean macdMonthCross = (MACD_M[mLen - 1] >= 0)
                    || (MACD_M[mLen - 1] == HHV_MACD_M_9_last && MACD_M_nearCross);

            // 组合：MACD月多 = 月金叉 && 周金叉 && 日上0轴
            boolean macdDayAbove0 = (DIF_D[i] >= 0 && DEA_D[i] >= 0)
                    || (MACD_D[i] >= 0 && MACD_D[i] == HHV_MACD_10[i]);
            boolean macdMonthBull = macdMonthCross && macdWeekCross && macdDayAbove0;

            // ---- 周级 SAR (以“动态周序列”的最后一根为当前) ----
            double[] sarW = TDX_SAR(wHigh, wLow);
            double sarW_last = sarW[sarW.length - 1];
            boolean sarWeekBull = dayClose[i] >= sarW_last;

            // ---- 最终：月多 ----
            monthlyBull[i] = macdMonthBull && (sarWeekBull || preBreak[i]);
        }

        return monthlyBull;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                              小工具：提取字段数组
    // -----------------------------------------------------------------------------------------------------------------


    private static double[] extractClose(List<MonthlyBullSignal.KlineBar> bars) {
        double[] arr = new double[bars.size()];
        for (int i = 0; i < bars.size(); i++) arr[i] = bars.get(i).close;
        return arr;
    }

    private static double[] extractHigh(List<MonthlyBullSignal.KlineBar> bars) {
        double[] arr = new double[bars.size()];
        for (int i = 0; i < bars.size(); i++) arr[i] = bars.get(i).high;
        return arr;
    }

    private static double[] extractLow(List<MonthlyBullSignal.KlineBar> bars) {
        double[] arr = new double[bars.size()];
        for (int i = 0; i < bars.size(); i++) arr[i] = bars.get(i).low;
        return arr;
    }


    // -----------------------------------------------------------------------------------------------------------------


//    /**
//     * K线数据结构
//     */
//    @Data
//    @AllArgsConstructor
//    public static class KlineBar {
//        public LocalDate date;
//        public double open, high, low, close;
//    }


}