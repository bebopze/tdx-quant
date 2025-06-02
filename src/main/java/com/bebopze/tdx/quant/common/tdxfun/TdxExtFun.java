package com.bebopze.tdx.quant.common.tdxfun;

import com.bebopze.tdx.quant.common.util.DateTimeUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.*;
import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.EMA;


/**
 * 通达信 - 扩展指标（自定义指标）                           Java实现
 *
 * @author: bebopze
 * @date: 2025/5/17
 */
public class TdxExtFun {


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  基础指标
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * SSF 指标                          ->   已验证 ✅
     *
     *
     * N = 3, M = 21
     * ------------------------------
     * X1 = |C - REF(C, 11)|
     * X2 = SUM(|C - REF(C,1)|, 11)
     * X3 = |X1 / X2|
     * X4 = X3*(2/N - 2/M) + 2/M
     * X5 = X4^2
     * SSF = EMA( DMA(C, X5), 2 )
     */
    public static double[] SSF(double[] close) {
        int len = close.length;
        int N = 3, M = 21;
        // X1: |C - REF(C,11)|
        double[] ref11 = REF(close, 11);
        double[] x1 = new double[len];
        for (int i = 0; i < len; i++) {
            x1[i] = Math.abs(close[i] - ref11[i]);
        }

        // X2: SUM(|C - REF(C,1)|, 11)
        double[] ref1 = REF(close, 1);
        double[] absDiff1 = new double[len];
        for (int i = 0; i < len; i++) {
            absDiff1[i] = Math.abs(close[i] - ref1[i]);
        }
        double[] x2 = SUM(absDiff1, 11);

        // X3: |X1 / X2|
        double[] x3 = new double[len];
        for (int i = 0; i < len; i++) {
            x3[i] = Math.abs(x2[i] != 0 ? x1[i] / x2[i] : 0.0);
        }

        // X4: X3*(2/N - 2/M) + 2/M
        double factor1 = 2.0 / N - 2.0 / M;
        double factor2 = 2.0 / M;
        double[] x4 = new double[len];
        for (int i = 0; i < len; i++) {
            x4[i] = x3[i] * factor1 + factor2;
        }

        // X5: X4^2
        double[] x5 = new double[len];
        for (int i = 0; i < len; i++) {
            x5[i] = x4[i] * x4[i];
        }

        // SSF = EMA(DMA(C, X5), 2)
        double[] dmaC = DMA(close, x5);         // 先动态移动平均，平滑因子序列为 X5
        double[] ssf = EMA(dmaC, 2);         // 再对结果做 2 周期指数平滑
        return ssf;
    }


    /**
     * N日涨幅：C/REF(C, N)  *100-100                          ->   已验证 ✅
     *
     *
     * -   用于计算 RPS   原始指标               ==>          EXTRS : C/REF(C,N) -1;（陶博士）
     *
     *
     * -
     *
     * @param close 收盘价序列
     * @param N     周期天数
     * @return 与原序列等长的数组，第 i 位为 (close[i] / close[i - N])  *100-100
     */
    public static double[] changePct(double[] close, int N) {
        int len = close.length;
        double[] ref = REF(close, N);
        double[] pct = new double[len];
        for (int i = 0; i < len; i++) {
            if (Double.isNaN(ref[i]) || ref[i] == 0) {
                pct[i] = Double.NaN;  // 无法计算或除以0时返回 NaN
            } else {
                pct[i] = (close[i] / ref[i] - 1) * 100.0;
                // pct[i] = close[i] / ref[i] * 100.0 - 100.0;
            }
        }
        return pct;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  高级指标
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * N日新高                                       - isNDaysHigh
     *
     * @param high 原始序列（如 最高价序列）
     * @param N    周期天数
     * @return 布尔数组，第 i 位为 true 时表示 high[i] 刚好等于过去 N 期（含当期）的最高值
     */
    public static boolean[] N日新高(double[] high, int N) {
        double[] hhv = HHV(high, N);

        int len = high.length;
        boolean[] signal = new boolean[len];

        for (int i = 0; i < len; i++) {
            // 当期值等于 N 期内最高值，且不是 NaN 时视为新高
            signal[i] = !Double.isNaN(hhv[i]) && high[i] == hhv[i];
        }

        return signal;
    }


    /**
     * 均线预萌出信号                         MAPreBreakout
     * <p>
     * 伪代码：
     * MA5   := MA(C, 5);
     * MA10  := MA(C,10);
     * MA20  := MA(C,20);
     * MA50  := IF(MA(C,50)=DRAWNULL,0,MA(C,50));
     * MA100 := IF(MA(C,100)=DRAWNULL,0,MA(C,100));
     * MA120 := IF(MA(C,120)=DRAWNULL,0,MA(C,120));
     * MA150 := IF(MA(C,150)=DRAWNULL,0,MA(C,150));
     * MA200 := IF(MA(C,200)=DRAWNULL,0,MA(C,200));
     *
     *
     * 预萌出1 :=
     * (C>=MA10 AND MA10>=MA20 AND MA20>=MA50 AND C>=MA100 AND C>=MA200)
     * AND
     * (MA10>=REF(MA10,1) AND MA20>=REF(MA20,1) AND MA50>=REF(MA50,1))
     * AND
     * (MA100>=REF(MA100,1) || MA200>=REF(MA200,1));
     *
     *
     * 预萌出2 :=
     * MA多(5) AND MA多(10) AND MA多(20) AND MA多(50) AND MA多(100) AND MA多(200)
     * AND
     * (MA50≥MA100 AND MA100≥MA200);
     *
     *
     * 均线预萌出 :=  预萌出1 || 预萌出2;
     */
    public static boolean[] 均线预萌出(double[] close) {
        int len = close.length;


        // 计算各条均线
        double[] MA5 = MA(close, 5);
        double[] MA10 = MA(close, 10);
        double[] MA20 = MA(close, 20);
        double[] MA50 = MA(close, 50);
        double[] MA100 = MA(close, 100);
        double[] MA120 = MA(close, 120);
        double[] MA150 = MA(close, 150);
        double[] MA200 = MA(close, 200);

        // 将不足周期时的 NaN 置为 0（对应 DRAWNULL）
        MA50 = Arrays.stream(MA50).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA100 = Arrays.stream(MA100).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA120 = Arrays.stream(MA120).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA150 = Arrays.stream(MA150).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA200 = Arrays.stream(MA200).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();

        // 准备上一期均线
        double[] MA5_1 = REF(MA5, 1);
        double[] MA10_1 = REF(MA10, 1);
        double[] MA20_1 = REF(MA20, 1);
        double[] MA50_1 = REF(MA50, 1);
        double[] MA100_1 = REF(MA100, 1);
        double[] MA200_1 = REF(MA200, 1);


        boolean[] result = new boolean[len];
        for (int i = 0; i < len; i++) {

            // 预萌出1 条件
            boolean cond1 =
                    close[i] >= MA10[i]
                            && MA10[i] >= MA20[i]
                            && MA20[i] >= MA50[i]
                            && close[i] >= MA100[i]
                            && close[i] >= MA200[i]
                            && MA10[i] >= MA10_1[i]
                            && MA20[i] >= MA20_1[i]
                            && MA50[i] >= MA50_1[i]
                            && (MA100[i] >= MA100_1[i] || MA200[i] >= MA200_1[i]);


            // 预萌出2 条件：MA多(N) 定义为 close>=MA(N) 且 MA(N)>=REF(MA(N),1)
            boolean ma5Up = close[i] >= MA5[i] && MA5[i] >= MA5_1[i];
            boolean ma10Up = close[i] >= MA10[i] && MA10[i] >= MA10_1[i];
            boolean ma20Up = close[i] >= MA20[i] && MA20[i] >= MA20_1[i];
            boolean ma50Up = close[i] >= MA50[i] && MA50[i] >= MA50_1[i];
            boolean ma100Up = close[i] >= MA100[i] && MA100[i] >= MA100_1[i];
            boolean ma200Up = close[i] >= MA200[i] && MA200[i] >= MA200_1[i];

            // 大均线多头排列：MA50 ≥ MA100 ≥ MA200
            boolean bigMaBull =
                    MA50[i] >= MA100[i]
                            && MA100[i] >= MA200[i];

            boolean cond2 = ma5Up && ma10Up && ma20Up && ma50Up && ma100Up && ma200Up && bigMaBull;


            result[i] = cond1 || cond2;
        }


        return result;
    }


    /**
     * 均线萌出（多头排列 且 各均线向上）       - MABreakout
     *
     * @param close 收盘价序列
     * @return 每个周期是否满足均线萌出
     */
    public static boolean[] 均线萌出(double[] close) {
        int len = close.length;


        // 计算各周期均线
        double[] MA10 = MA(close, 10);
        double[] MA20 = MA(close, 20);
        double[] MA50 = MA(close, 50);
        double[] MA100 = MA(close, 100);
        double[] MA200 = MA(close, 200);

        // 将 NaN（不足周期时产生）替换为 0
        MA50 = Arrays.stream(MA50).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA100 = Arrays.stream(MA100).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA200 = Arrays.stream(MA200).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();


        boolean[] result = new boolean[len];
        for (int i = 0; i < len; i++) {

            boolean bullOrder = close[i] >= MA10[i]
                    && MA10[i] >= MA20[i]
                    && MA20[i] >= MA50[i]
                    && MA50[i] >= MA100[i]
                    && MA50[i] >= MA200[i];

            boolean maUp = i > 0
                    && MA10[i] >= MA10[i - 1]
                    && MA20[i] >= MA20[i - 1]
                    && MA50[i] >= MA50[i - 1]
                    && MA100[i] >= MA100[i - 1]
                    && MA200[i] >= MA200[i - 1];

            result[i] = bullOrder && maUp;
        }


        return result;
    }


    /**
     * 计算“大均线多头”布尔序列                            - largeMABull
     *
     * 伪代码：
     *
     * MA50 := IF(MA(C, 50)=DRAWNULL, 0, MA(C, 50));
     * MA60 := IF(MA(C, 60)=DRAWNULL, 0, MA(C, 60));
     * MA100:= IF(MA(C,100)=DRAWNULL, 0, MA(C,100));
     * MA120:= IF(MA(C,120)=DRAWNULL, 0, MA(C,120));
     * MA200:= IF(MA(C,200)=DRAWNULL, 0, MA(C,200));
     * MA250:= IF(MA(C,250)=DRAWNULL, 0, MA(C,250));
     *
     * 大均线多头 :=
     * (C > MA50 && MA50 > MA100 && MA100 > MA200
     * && MA50 >= REF(MA50,1) && MA100 >= REF(MA100,1) && MA200 >= REF(MA200,1))
     * ||
     * (C > MA60 && MA60 > MA100 && MA100 > MA200
     * && MA60 >= REF(MA60,1) && MA100 >= REF(MA100,1) && MA200 >= REF(MA200,1))
     * ||
     * (C > MA50 && MA50 > MA120 && MA120 > MA250
     * && MA50 >= REF(MA50,1) && MA120 >= REF(MA120,1) && MA250 >= REF(MA250,1))
     * ||
     * (C > MA60 && MA60 > MA120 && MA120 > MA250
     * && MA60 >= REF(MA60,1) && MA120 >= REF(MA120,1) && MA250 >= REF(MA250,1));
     *
     * @param close 日线收盘价数组
     * @return 与 close 等长的布尔数组，true 表示当日满足“大均线多头”
     */
    public static boolean[] 大均线多头(double[] close) {
        int n = close.length;


        // 1. 计算原始各周期移动平均
        double[] MA50 = MA(close, 50);
        double[] MA60 = MA(close, 60);
        double[] MA100 = MA(close, 100);
        double[] MA120 = MA(close, 120);
        double[] MA200 = MA(close, 200);
        double[] MA250 = MA(close, 250);

        // 2. 将 NaN（周期不足）替换为 0
        MA50 = Arrays.stream(MA50).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA60 = Arrays.stream(MA60).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA100 = Arrays.stream(MA100).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA120 = Arrays.stream(MA120).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA200 = Arrays.stream(MA200).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA250 = Arrays.stream(MA250).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();

        // 3. 计算上一日同周期均线（REF）
        double[] MA50_1 = REF(MA50, 1);
        double[] MA60_1 = REF(MA60, 1);
        double[] MA100_1 = REF(MA100, 1);
        double[] MA120_1 = REF(MA120, 1);
        double[] MA200_1 = REF(MA200, 1);
        double[] MA250_1 = REF(MA250, 1);


        // 4. 遍历逐日判断
        boolean[] result = new boolean[n];
        for (int i = 1; i < n; i++) {

            boolean cond1 = close[i] > MA50[i]
                    && MA50[i] > MA100[i]
                    && MA100[i] > MA200[i]
                    && MA50[i] >= MA50_1[i]
                    && MA100[i] >= MA100_1[i]
                    && MA200[i] >= MA200_1[i];

            boolean cond2 = close[i] > MA60[i]
                    && MA60[i] > MA100[i]
                    && MA100[i] > MA200[i]
                    && MA60[i] >= MA60_1[i]
                    && MA100[i] >= MA100_1[i]
                    && MA200[i] >= MA200_1[i];

            boolean cond3 = close[i] > MA50[i]
                    && MA50[i] > MA120[i]
                    && MA120[i] > MA250[i]
                    && MA50[i] >= MA50_1[i]
                    && MA120[i] >= MA120_1[i]
                    && MA250[i] >= MA250_1[i];

            boolean cond4 = close[i] > MA60[i]
                    && MA60[i] > MA120[i]
                    && MA120[i] > MA250[i]
                    && MA60[i] >= MA60_1[i]
                    && MA120[i] >= MA120_1[i]
                    && MA250[i] >= MA250_1[i];


            result[i] = cond1 || cond2 || cond3 || cond4;
        }


        return result;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  复杂指标
    // -----------------------------------------------------------------------------------------------------------------


    // ----------------------------------- TODO     月多


    public static boolean[] 月多(String[] dateArr, double[] closeArr, double[] highArr, double[] lowArr) {
        List<MonthlyBullSignal.DailyBar> dailyBarList = new ArrayList<>();


        for (int i = 0; i < dateArr.length; i++) {
            MonthlyBullSignal.DailyBar dailyBar = new MonthlyBullSignal.DailyBar(DateTimeUtil.parseDate_yyyy_MM_dd(dateArr[i]),
                                                                                 0.00, highArr[i], lowArr[i], closeArr[i]);
            dailyBarList.add(dailyBar);
        }


        return MonthlyBullSignal.computeMonthlyBull(dailyBarList);
    }


//    /**
//     * 计算“月多”信号                             monthlyBull
//     *
//     * @param date  日线交易日期序列
//     * @param close 日线收盘价序列
//     * @param high  日线最高价序列
//     * @param low   日线最低价序列
//     * @return 布尔数组：true 表示月多
//     */
//    public static boolean[] 月多(String[] date, double[] close, double[] high, double[] low) {
//        int len = close.length;
//
//        // —— 1. 各周期 MACD ——
//        // 日线 MACD
//        double[][] macdDay = MACD(close);
//        double[] difDay = macdDay[0];
//        double[] deaDay = macdDay[1];
//        double[] macdD = macdDay[2];
//
//        // 周线 MACD（假设已有方法按周线重采样后调用）
//        double[] closeWeek = resampleToWeek(date, close);
//        double[][] macdWeekArr = MACD(closeWeek);
//        double[] macdW = macdWeekArr[2];
//
//        // 月线 MACD（假设已有方法按月线重采样后调用）
//        double[] closeMonth = resampleToMonth(date, close);
//        double[][] macdMonthArr = MACD(closeMonth);
//        double[] difM = macdMonthArr[0];
//        double[] deaM = macdMonthArr[1];
//        double[] macdM = macdMonthArr[2];
//
//
//        // —— 2. 计算 MACD 月度信号 ——
//        // 月度比率
//        boolean[] macdMonthBull = new boolean[closeMonth.length];
//        for (int i = 0; i < closeMonth.length; i++) {
//            double absDIF = Math.abs(difM[i]);
//            double absDEA = Math.abs(deaM[i]);
//            double ratio = Math.min(absDEA, absDIF) / Math.max(absDEA, absDIF);
//
//            // TODO   接近金叉
//            // boolean nearGolden =
//            //        (BARSLASTCOUNT(difM[i] >= REF(difM, 1)[i]) >= 1.2 * 20 && ratio >= 0.9)
//            //                 || (BARSLASTCOUNT(difM[i] > REF(difM, 1)[i]) >= 1 && ratio >= 0.95);
//
//            // 月度金叉
//            boolean golden = macdM[i] >= 0 || (macdM[i] == HHV(macdM, 9)[i] /*&& nearGolden*/);
//
//            macdMonthBull[i] = golden;
//        }
//
//        // —— 3. 计算 “SAR 周多” ——
//        double[] sarWeek = SAR(high, low, /* period=WEEK*/ 5);
//        boolean[] sarWeekBull = new boolean[len];
//        for (int i = 0; i < len; i++) {
//            sarWeekBull[i] = close[i] >= sarWeek[i];
//        }
//
//        // —— 4. 汇总“月多”：月度金叉 & (SAR 周多 || BARSSINCEN(均线萌出||预萌出, 2)==0) ——
//        boolean[] maOut = 均线萌出(close);
//        boolean[] maPre = 均线预萌出(close);
//        int[] barsSince = BARSSINCEN(or(maOut, maPre), 2);
//
//        boolean[] result = new boolean[len];
//        // 对齐月线与日线索引：假设 `mapMonthIndex(i)` 将日线索引映射到对应的月线索引
//        for (int i = 0; i < len; i++) {
//            int mi = mapMonthIndex(i);
//            boolean mBull = macdMonthBull[mi];
//            result[i] = mBull && (sarWeekBull[i] || barsSince[i] == 0);
//        }
//
//        return result;
//    }
//
//    private static double[] resampleToWeek(String[] date, double[] close) {
//        double[] weekClose = KlineAggregator.toWeekClose(date, close);
//        return weekClose;
//    }
//
//    private static double[] resampleToMonth(String[] date, double[] close) {
//        double[] monthClose = KlineAggregator.toMonthClose(date, close);
//        return monthClose;
//    }
//
//
//    // 布尔数组按位 OR
//    private static boolean[] or(boolean[] a, boolean[] b) {
//        int n = a.length;
//        boolean[] c = new boolean[n];
//        for (int i = 0; i < n; i++) c[i] = a[i] || b[i];
//        return c;
//    }

    // ------ 说明 ------
    // 1. resampleToWeek / resampleToMonth: 请用已有方法或库实现从日线到周/月线的重采样。
    // 2. HHV(arr, n): 计算数组 arr 最近 n 期的最高值序列。
    // 3. BARSLASTCOUNT 和 BARSSINCEN: 你已有工具方法直接调用。
    // 4. maBreakout / maPreBreakout: 前面定义的“均线萌出”和“均线预萌出”函数。
    // 5. mapMonthIndex: 日线索引到月线索引的映射，取决于数据结构，需自行实现。


}