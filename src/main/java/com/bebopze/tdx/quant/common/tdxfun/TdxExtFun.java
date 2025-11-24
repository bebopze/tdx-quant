package com.bebopze.tdx.quant.common.tdxfun;

import com.bebopze.tdx.quant.common.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.*;
import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.EMA;
import static com.bebopze.tdx.quant.common.util.BoolUtil.bool2Int;


/**
 * 通达信 - 扩展指标（自定义指标）                           Java实现
 *
 * @author: bebopze
 * @date: 2025/5/17
 */
@Slf4j
public class TdxExtFun {


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  周期转换 指标
    // -----------------------------------------------------------------------------------------------------------------


//    public static List<KlineAggregator.PeriodDTO> toWeek(String[] date, double[] value) {
//        return KlineAggregator.toWeekly(date, value);
//        // List<MonthlyBullSignal.KlineBar> weeklyBarList = MonthlyBullSignal.aggregateToWeekly(dailyKlines);
//    }
//
//    public static List<KlineAggregator.PeriodDTO> toMonth(String[] date, double[] value) {
//        return KlineAggregator.toMonthly(date, value);
//        // List<MonthlyBullSignal.KlineBar> monthlyBarList = MonthlyBullSignal.aggregateToMonthly(dailyKlines);
//    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  简单指标
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                                      MA
    // -----------------------------------------------------------------------------------------------------------------


    public static boolean[] 上MA(double[] close, int N) {
        int len = close.length;
        boolean[] result = new boolean[len];


        // MA20
        double[] MA20 = MA(close, N);


        for (int i = 0; i < len; i++) {
            result[i] = close[i] >= MA20[i];
        }


        return result;
    }

    public static boolean[] 下MA(double[] close, int N) {
        int len = close.length;
        boolean[] result = new boolean[len];


        // MA20
        double[] MA20 = MA(close, N);


        for (int i = 0; i < len; i++) {
            result[i] = close[i] < MA20[i];
        }


        return result;
    }


    public static boolean[] MA向上(double[] close, int N) {
        int len = close.length;
        boolean[] result = new boolean[len];


        // MA20
        double[] MA20 = MA(close, N);


        for (int i = 0; i < len; i++) {
            if (i == 0) {
                result[i] = false;
            } else {
                result[i] = MA20[i] >= MA20[i - 1];
            }
        }


        return result;
    }


    public static boolean[] MA向下(double[] close, int N) {
        int len = close.length;
        boolean[] result = new boolean[len];


        // MA20
        double[] MA20 = MA(close, N);


        for (int i = 0; i < len; i++) {
            if (i == 0) {
                result[i] = false;
            } else {
                result[i] = MA20[i] < MA20[i - 1];
            }
        }

        return result;
    }


    public static boolean[] MA多(double[] close, int N) {
        return con_and(上MA(close, N), MA向上(close, N));
    }


    public static boolean[] MA空(double[] close, int N) {
        return con_and(下MA(close, N), MA向下(close, N));
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                      SSF
    // -----------------------------------------------------------------------------------------------------------------


    public static boolean[] 上SSF(double[] close, double[] ssf) {
        int len = close.length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {
            result[i] = close[i] >= ssf[i];
        }


        return result;
    }

    public static boolean[] 下SSF(double[] close, double[] ssf) {
        int len = close.length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {
            result[i] = close[i] < ssf[i];
        }

        return result;
    }


    public static boolean[] SSF向上(double[] close, double[] ssf) {
        int len = close.length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {
            if (i == 0) {
                result[i] = false;
            } else {
                result[i] = ssf[i] >= ssf[i - 1];
            }
        }

        return result;
    }

    public static boolean[] SSF向下(double[] close, double[] ssf) {
        int len = close.length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {
            if (i == 0) {
                result[i] = false;
            } else {
                result[i] = ssf[i] < ssf[i - 1];
            }
        }

        return result;
    }


    public static boolean[] SSF多(double[] close, double[] ssf) {
        return con_and(上SSF(close, ssf), SSF向上(close, ssf));
    }


    public static boolean[] SSF空(double[] close, double[] ssf) {
        return con_and(下SSF(close, ssf), SSF向下(close, ssf));
    }


    public static double[] C_SSF_偏离率(double[] close, double[] ssf) {
        int n = close.length;
        double[] result = new double[n];


        for (int i = 0; i < n; i++) {
            result[i] = NumUtil.of((close[i] / ssf[i] - 1) * 100);
        }


        return result;
    }

    public static double[] C_MA_偏离率(double[] close, int N) {
        return H_MA_偏离率(close, close, N);
    }


    public static double[] H_MA_偏离率(double[] high, double[] close, int N) {
        int n = close.length;
        double[] result = new double[n];


        double[] MA = MA(close, N);


        for (int i = 0; i < n; i++) {
            result[i] = NumUtil.of((high[i] / MA[i] - 1) * 100);
        }


        return result;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 结果合并   -   AND
     *
     * @param arr_list
     * @return
     */
    public static boolean[] con_and(boolean[]... arr_list) {
        int len = arr_list[0].length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {
            boolean acc = true;
            for (boolean[] arr : arr_list) {
                acc = arr[i];
                if (!acc) break;
            }
            result[i] = acc;
        }


        return result;
    }

    /**
     * 结果合并   -   OR
     *
     * @param arr_list
     * @return
     */
    public static boolean[] con_or(boolean[]... arr_list) {
        int len = arr_list[0].length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {
            boolean acc = false;
            for (boolean[] arr : arr_list) {
                acc = arr[i];
                if (acc) break;
            }
            result[i] = acc;
        }


        return result;
    }


    /**
     * 结果合并   -   求和 -> 比较
     *
     * @param N        sum >= N
     * @param arr_list
     * @return
     */
    public static boolean[] con_sumCompare(int N, boolean[]... arr_list) {
        int len = arr_list[0].length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {

            int count = 0;
            boolean acc = false;

            for (boolean[] arr : arr_list) {
                count += bool2Int(arr[i]);
                if (count >= N) {
                    acc = true;
                    break;
                }
            }

            result[i] = acc;
        }

        return result;
    }


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
     * 中期涨幅N
     *
     * @param high
     * @param low
     * @param close
     * @param N
     * @return
     */
    public static double[] 中期涨幅N(double[] high, double[] low, double[] close, int N) {
        int len = close.length;


        // L_DAY :   BARSLAST(MA空)  +  NL_DAY
        int[] L_DAY = BARSLAST(MA空(close, N));


        for (int i = 0; i < L_DAY.length; i++) {
            // + NL_DAY （ 10~20 ）
            L_DAY[i] += 15;
        }
        // log.debug("中期涨幅N     >>>     L_DAY : {}", JSON.toJSONString(L_DAY));


        // _L    :   LLV(L,   L_DAY)
        double[] L = LLV(low, L_DAY);


        // 中期涨幅 :   IF(上MA || MA向上,           H / _L  *100 -100,     0)

        boolean[] 上MA = 上MA(close, N);
        boolean[] MA向上 = MA向上(close, N);


        double[] 中期涨幅 = new double[len];
        for (int i = 0; i < L.length; i++) {
            if (Double.isNaN(L[i]) || L[i] == 0) continue;
            中期涨幅[i] = (上MA[i] || MA向上[i]) ? (high[i] / L[i] - 1) * 100.00f : 0.0;
        }
        // log.debug("中期涨幅N     >>>     中期涨幅 : {}", 中期涨幅);


        return 中期涨幅;
    }


    /**
     * 高位 - 爆量/上影/大阴
     *
     * @param high
     * @param low
     * @param close
     * @param amo
     * @param is20CM
     * @param date
     * @return
     */
    public static boolean[] 高位爆量上影大阴(double[] high,
                                             double[] low,
                                             double[] close,
                                             double[] amo,
                                             boolean is20CM,

                                             LocalDate[] date) {


        int len = close.length;
        boolean[] result = new boolean[len];


        // { ------------------ 高位爆量 }
        //
        // AMO_MA5  :=  MA(AMO, 5);
        // AMO_MA10 :=  MA(AMO,10);
        // 上影大阴 :=  上影大阴.上影大阴;
        //
        //
        // 高位爆量_1 :=  AMO/AMO_MA5>=1.9   AND   AMO/AMO_MA10>=2.1;
        //
        // 高位爆量_2 :=  NOT(高位爆量_1)    AND   AMO / REF( HHV(AMO,10) ,1) >= 1.35     {AND     AMO_亿>=10};
        //
        //
        //
        // 高位爆量上影 :=  (倍  AND  (高位爆量_1  ||  高位爆量_2   ||   上影大阴)  )   ||   (中期涨幅>100 AND 上影大阴  AND  高位爆量_1)       NODRAW;


        // 高位
        double[] 中期涨幅 = 中期涨幅N(high, low, close, 20);

        // 爆量
        boolean[] 爆量 = 爆量(amo);

        // 上影大阴
        boolean[] 上影大阴 = 上影大阴(high, low, close, is20CM);


        for (int i = 0; i < len; i++) {

//            if (date[i].isAfter(LocalDate.of(2020, 7, 10))) {
//                log.debug("------- date : {} , 中期涨幅 : {}", date[i], 中期涨幅[i]);
//                int x = 0;
//            }


            // 高位
            // boolean 高位 = is20CM ? 中期涨幅[i] >= 115 : 中期涨幅[i] >= 85;
            double 中期涨幅_H5 = max(中期涨幅, i, 5);
            boolean 高位1 = is20CM ? 中期涨幅_H5 >= 115 : 中期涨幅_H5 >= 85;
            boolean 高位2 = is20CM ? 中期涨幅_H5 >= 95 : 中期涨幅_H5 >= 75;
            boolean 高位3 = is20CM ? 中期涨幅_H5 >= 90 : 中期涨幅_H5 >= 70;


            // 高位- 爆量/上影/大阴
            boolean b1 = 高位1 && (爆量[i] || 上影大阴[i]);     // 强势卖[强]   ->   高位 - 爆量（大涨）
            boolean b2 = 高位2 && 上影大阴[i];                 // 左侧卖[中]   ->   高位 - 长上影/大阴线
            boolean b3 = 高位3 && 爆量[i] && 上影大阴[i];       // 右侧卖[弱]   ->  "高位" - 爆量 + 上影大阴（主力  ->  提前[做盘失败]  清仓式 跑路）


            result[i] = b1 || b2 || b3;


//            if (高位 && i > 2000) {
//                log.debug("高位爆量上影大阴     >>>     idx : {} , date : {} , 高位 : {} , 中期涨幅 : {} , 爆量 : {} , 上影大阴 : {} , result : {}", i, date[i], 高位, 中期涨幅[i], 爆量[i], 上影大阴[i], result[i]);
//                int x = 1;
//            }
        }

        return result;
    }


    /**
     * 是否涨停
     *
     * @param close
     * @param changePctLimit
     * @return
     */
    public static boolean[] 涨停(double[] close, Integer changePctLimit) {
        int len = close.length;
        boolean[] result = new boolean[len];


        // double changeLimit = changePctLimit * 0.98 * 0.01;   // 涨跌停（%）：9.8% / 19.8% / 29.8%


        for (int i = 1; i < len; i++) {
            // result[i] = close[i] >= close[i - 1] * (1 + changeLimit);


            double zt_price = close[i - 1] * (1 + changePctLimit * 0.01);
            double aStock__zt_price = NumUtil.of(zt_price, 2, RoundingMode.HALF_UP);   // A股 涨停价格（规则：保留2位小数 -> 四舍五入）

            result[i] = close[i] >= aStock__zt_price;
        }

        return result;
    }

    /**
     * 是否跌停
     *
     * @param close
     * @param changePctLimit
     * @return
     */
    public static boolean[] 跌停(double[] close, Integer changePctLimit) {
        int len = close.length;
        boolean[] result = new boolean[len];


        double changeLimit = changePctLimit * 0.998 * 0.01;   // 涨跌停（%）：9.98% / 19.98% / 29.98%

        for (int i = 1; i < len; i++) {
            result[i] = close[i] <= close[i - 1] * (1 - changeLimit);
        }

        return result;
    }


    /**
     * 近N日   最大值
     *
     * @param arr 序列
     * @param idx 当日 - idx
     * @param N   近N日
     * @return
     */
    private static double max(double[] arr, int idx, int N) {
        int len = arr.length;
        double max = 0;

        for (int i = 0; i < N; i++) {

            int _idx = idx - i;
            if (_idx >= 0 && _idx < len) {

                double v = arr[_idx];
                max = Math.max(max, v);
            }
        }

        return max;
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
     * 历史新高
     *
     * @param high 原始序列（如 最高价序列）
     * @return 布尔数组，第 i 位为 true 时表示 high[i] 刚好等于过去 全期 的最高值
     */
    public static boolean[] 历史新高(double[] high) {
        double[] hhv = HHV2(high, high.length);

        int len = high.length;
        boolean[] signal = new boolean[len];

        // 上市100日（过滤 新股）
        for (int i = 100; i < len; i++) {
            // 当期值等于 N 期内最高值，且不是 NaN 时视为新高
            signal[i] = high[i] == hhv[i];
        }

        return signal;
    }


    /**
     * 均线预萌出信号                         MAPreBreakout
     *
     *
     *
     * 通达信公式：
     *
     *
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
        double[] MA200 = MA(close, 200);

        // 将不足周期时的 NaN 置为 0（对应 DRAWNULL）
        MA50 = Arrays.stream(MA50).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
        MA100 = Arrays.stream(MA100).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
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
     * 小均线多头       - SmallMABull
     *
     *
     * MA10 :=IF(MA(C, 10)=DRAWNULL, 0, MA(C, 10));
     * MA20 :=IF(MA(C, 20)=DRAWNULL, 0, MA(C, 20));
     * MA50 :=IF(MA(C, 50)=DRAWNULL, 0, MA(C, 50));
     * MA60 :=IF(MA(C, 60)=DRAWNULL, 0, MA(C, 60));
     * MA100:=IF(MA(C,100)=DRAWNULL, 0, MA(C,100));
     * MA200:=IF(MA(C,200)=DRAWNULL, 0, MA(C,200));
     *
     *
     *
     * 小均线多头 :   C>MA10 AND MA10>MA20 AND MA20>MA50     AND     MA10>MA100 AND MA10>MA200
     * AND       MA10>=REF(MA10,1)  AND  MA20>=REF(MA20,1)
     * AND   (   MA50>=REF(MA50,1)  ||  MA60>=REF(MA60,1)   )
     * AND   MA100>=REF(MA100,1)  AND  MA200>=REF(MA200,1)       COLORMAGENTA;
     *
     * @param close 收盘价序列
     * @return 每个周期是否满足小均线多头排列
     */
    public static boolean[] 小均线多头(double[] close) {
        int n = close.length;


        // 1. 计算原始各周期移动平均
        double[] MA10 = MA(close, 10);
        double[] MA20 = MA(close, 20);
        double[] MA50 = MA(close, 50);
        double[] MA60 = MA(close, 60);
        double[] MA100 = MA(close, 100);
        double[] MA200 = MA(close, 200);

        // 2. 将 NaN（周期不足）替换为 0
        MA10 = NaN_2_0(MA10);
        MA20 = NaN_2_0(MA20);
        MA50 = NaN_2_0(MA50);
        MA60 = NaN_2_0(MA60);
        MA100 = NaN_2_0(MA100);
        MA200 = NaN_2_0(MA200);

        // 3. 计算上一日同周期均线（REF）
        double[] MA10_1 = REF(MA10, 1);
        double[] MA20_1 = REF(MA20, 1);
        double[] MA50_1 = REF(MA50, 1);
        double[] MA60_1 = REF(MA60, 1);
        double[] MA100_1 = REF(MA100, 1);
        double[] MA200_1 = REF(MA200, 1);


        // 4. 遍历逐日判断
        boolean[] result = new boolean[n];


        for (int i = 1; i < n; i++) {
            result[i] = close[i] > MA10[i]
                    && MA10[i] > MA20[i]
                    && MA20[i] > MA50[i]
                    && MA10[i] >= MA100[i]
                    && MA10[i] >= MA200[i]
                    && MA10[i] >= MA10_1[i]
                    && MA20[i] >= MA20_1[i]
                    && (MA50[i] >= MA50_1[i] || MA60[i] >= MA60_1[i])
                    && MA100[i] >= MA100_1[i]
                    && MA200[i] >= MA200_1[i];
        }


        return result;
    }

    /**
     * 计算“大均线多头”布尔序列                            - bigMaBull
     *
     *
     *
     * 通达信公式：
     *
     *
     * MA50 := IF(MA(C, 50)=DRAWNULL, 0, MA(C, 50));
     * MA60 := IF(MA(C, 60)=DRAWNULL, 0, MA(C, 60));
     * MA100:= IF(MA(C,100)=DRAWNULL, 0, MA(C,100));
     * MA120:= IF(MA(C,120)=DRAWNULL, 0, MA(C,120));
     * MA200:= IF(MA(C,200)=DRAWNULL, 0, MA(C,200));
     * MA250:= IF(MA(C,250)=DRAWNULL, 0, MA(C,250));
     *
     *
     *
     * 大均线多头 :=   (C > MA50 && MA50 > MA100 && MA100 > MA200 && MA50 >= REF(MA50,1) && MA100 >= REF(MA100,1) && MA200 >= REF(MA200,1))
     * ||   (C > MA60 && MA60 > MA100 && MA100 > MA200 && MA60 >= REF(MA60,1) && MA100 >= REF(MA100,1) && MA200 >= REF(MA200,1))
     * ||   (C > MA50 && MA50 > MA120 && MA120 > MA250 && MA50 >= REF(MA50,1) && MA120 >= REF(MA120,1) && MA250 >= REF(MA250,1))
     * ||   (C > MA60 && MA60 > MA120 && MA120 > MA250 && MA60 >= REF(MA60,1) && MA120 >= REF(MA120,1) && MA250 >= REF(MA250,1));
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
        MA50 = NaN_2_0(MA50);
        MA60 = NaN_2_0(MA60);
        MA100 = NaN_2_0(MA100);
        MA120 = NaN_2_0(MA120);
        MA200 = NaN_2_0(MA200);
        MA250 = NaN_2_0(MA250);

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


    /**
     * 均线大多头
     *
     *
     *
     * 通达信公式：
     *
     *
     * MA5  :=MA(C, 5);
     * MA10 :=MA(C,10);
     * MA20 :=MA(C,20);
     * MA50 :=IF(MA(C, 50)=DRAWNULL, 0, MA(C, 50));
     * MA100:=IF(MA(C,100)=DRAWNULL, 0, MA(C,100));
     * MA120:=IF(MA(C,120)=DRAWNULL, 0, MA(C,120));
     * MA150:=IF(MA(C,150)=DRAWNULL, 0, MA(C,150));
     * MA200:=IF(MA(C,200)=DRAWNULL, 0, MA(C,200));
     * MA250:=IF(MA(C,250)=DRAWNULL, 0, MA(C,250));
     *
     *
     *
     * 均线大多头 : ( C>=MA10 AND MA10>=MA20 AND MA20>=MA50 AND MA50>=MA100 AND MA100>=MA200 )
     *
     * AND        ( MA10>=REF(MA10,1) AND MA20>=REF(MA20,1) AND MA50>=REF(MA50,1) AND MA100>=REF(MA100,1) AND MA200>=REF(MA200,1) )     COLORRED;
     *
     * @param close
     * @return
     */
    public static boolean[] 均线大多头(double[] close) {
        int n = close.length;


        // 1. 计算原始各周期移动平均
        double[] MA10 = MA(close, 10);
        double[] MA20 = MA(close, 20);
        double[] MA50 = MA(close, 50);
        double[] MA100 = MA(close, 100);
        double[] MA200 = MA(close, 200);

        // 2. 将 NaN（周期不足）替换为 0
        MA10 = NaN_2_0(MA10);
        MA20 = NaN_2_0(MA20);
        MA50 = NaN_2_0(MA50);
        MA100 = NaN_2_0(MA100);
        MA200 = NaN_2_0(MA200);

        // // 3. 计算上一日同周期均线（REF）
        // double[] MA10_1 = REF(MA10, 1);
        // double[] MA20_1 = REF(MA20, 1);
        // double[] MA50_1 = REF(MA50, 1);
        // double[] MA100_1 = REF(MA100, 1);
        // double[] MA200_1 = REF(MA200, 1);


        // 4. 遍历逐日判断
        boolean[] result = new boolean[n];


        for (int i = 0; i < n; i++) {

            // 均线大多头 : ( C>=MA10 AND MA10>=MA20 AND MA20>=MA50 AND MA50>=MA100 AND MA100>=MA200 )
            //
            //  AND        ( MA10>=REF(MA10,1) AND MA20>=REF(MA20,1) AND MA50>=REF(MA50,1) AND MA100>=REF(MA100,1) AND MA200>=REF(MA200,1) )

            result[i] = i > 0
                    && close[i] >= MA10[i] && MA10[i] >= MA20[i] && MA20[i] >= MA50[i] && MA50[i] >= MA100[i] && MA100[i] >= MA200[i]
                    && MA10[i] >= MA10[i - 1] && MA20[i] >= MA20[i - 1] && MA50[i] >= MA50[i - 1] && MA100[i] >= MA100[i - 1] && MA200[i] >= MA200[i - 1];
        }


        return result;
    }


    /**
     * 均线极多头
     *
     *
     *
     * 通达信公式：
     *
     *
     * { ----------------------------------------------------------------------------------------------------------------------------- 均线X/Y  ->  均线多头排列 }
     * X1   :    ABS(C>MA5) + ABS(MA5>MA10) + ABS(MA10>MA20) + ABS(MA20>MA50) + ABS(MA50>MA60) + ABS(MA60>MA100)       NODRAW;
     *
     * X2_1 :=   MA100>MA120 AND MA120>MA200 AND MA200>MA250       NODRAW;
     * X2_2 :=   MA100>MA150 AND MA150>MA200 AND MA200>MA250       NODRAW;
     * X2   :    X2_1 || X2_2                                      NODRAW;
     *
     *
     *
     * Y    :    MA5>=REF(MA5,1) AND MA10>=REF(MA10,1) AND MA20>=REF(MA20,1) AND MA50>=REF(MA50,1) AND MA60>=REF(MA60,1)
     *
     * AND MA100>=REF(MA100,1) AND MA120>=REF(MA120,1) AND MA150>=REF(MA150,1) AND MA200>=REF(MA200,1) AND MA250>=REF(MA250,1)       NODRAW;
     *
     *
     *
     * 均线极多头_2   :   X1>=6 AND X2   AND   Y       COLORRED     DOTLINE;
     *
     *
     *
     * { ----------------------------------------------------------------------------------------------------------------------------- 均线Z  ->  均线间隔 }
     * 均线极多头 :     EVERY(均线极多头_2,   2)       AND       ( MA5/MA10>1.01  AND   MA10/MA20>1.01 AND MA20/MA50>1.03 )       COLORYELLOW;
     *
     * @param close
     * @return
     */
    public static boolean[] 均线极多头(double[] close) {
        int n = close.length;


        // 1. 计算原始各周期移动平均
        double[] MA5 = MA(close, 5);
        double[] MA10 = MA(close, 10);
        double[] MA20 = MA(close, 20);
        double[] MA50 = MA(close, 50);
        double[] MA60 = MA(close, 60);
        double[] MA100 = MA(close, 100);
        double[] MA120 = MA(close, 120);
        double[] MA150 = MA(close, 150);
        double[] MA200 = MA(close, 200);
        double[] MA250 = MA(close, 250);


        // // 2. 将 NaN（周期不足）替换为 0
        // MA5 = NaN_2_0(MA5);
        // MA10 = NaN_2_0(MA10);
        // MA20 = NaN_2_0(MA20);
        // MA50 = NaN_2_0(MA50);
        // MA60 = NaN_2_0(MA60);
        // MA100 = NaN_2_0(MA100);
        // MA120 = NaN_2_0(MA120);
        // MA150 = NaN_2_0(MA150);
        // MA200 = NaN_2_0(MA200);
        // MA250 = NaN_2_0(MA250);


        boolean[] X2 = new boolean[n];
        boolean[] Y = new boolean[n];
        boolean[] JDT_2 = new boolean[n];


        // 4. 遍历逐日判断
        boolean[] result = new boolean[n];


        for (int i = 0; i < n; i++) {


            // X1   :    ABS(C>MA5) + ABS(MA5>MA10) + ABS(MA10>MA20) + ABS(MA20>MA50) + ABS(MA50>MA60) + ABS(MA60>MA100)       NODRAW;

            int X1 = 0;
            if (!Double.isNaN(MA5[i]) && close[i] > MA5[i]) X1++;
            if (!Double.isNaN(MA10[i]) && MA5[i] > MA10[i]) X1++;
            if (!Double.isNaN(MA20[i]) && MA10[i] > MA20[i]) X1++;
            if (!Double.isNaN(MA50[i]) && MA20[i] > MA50[i]) X1++;
            if (!Double.isNaN(MA60[i]) && MA50[i] > MA60[i]) X1++;
            if (!Double.isNaN(MA100[i]) && MA60[i] > MA100[i]) X1++;


            // X2_1 :=   MA100>MA120 AND MA120>MA200 AND MA200>MA250       NODRAW;
            // X2_2 :=   MA100>MA150 AND MA150>MA200 AND MA200>MA250       NODRAW;
            // X2   :    X2_1 || X2_2                                      NODRAW;

            boolean X2_1 = (!Double.isNaN(MA250[i]) && MA100[i] > MA120[i] && MA120[i] > MA200[i] && MA200[i] > MA250[i]);
            boolean X2_2 = (!Double.isNaN(MA250[i]) && MA100[i] > MA150[i] && MA150[i] > MA200[i] && MA200[i] > MA250[i]);
            X2[i] = X2_1 || X2_2;


            // Y    :    MA5>=REF(MA5,1) AND MA10>=REF(MA10,1) AND MA20>=REF(MA20,1) AND MA50>=REF(MA50,1) AND MA60>=REF(MA60,1)
            //
            //          AND MA100>=REF(MA100,1) AND MA120>=REF(MA120,1) AND MA150>=REF(MA150,1) AND MA200>=REF(MA200,1) AND MA250>=REF(MA250,1)       NODRAW;

            Y[i] = (i > 0
                    && MA5[i] >= MA5[i - 1]
                    && MA10[i] >= MA10[i - 1]
                    && MA20[i] >= MA20[i - 1]
                    && MA50[i] >= MA50[i - 1]
                    && MA60[i] >= MA60[i - 1]
                    && MA100[i] >= MA100[i - 1]
                    && MA120[i] >= MA120[i - 1]
                    && MA150[i] >= MA150[i - 1]
                    && MA200[i] >= MA200[i - 1]
                    && MA250[i] >= MA250[i - 1]);


            // 均线极多头_2   :   X1>=6 AND X2   AND   Y       COLORRED     DOTLINE;
            JDT_2[i] = X1 >= 6 && X2[i] && Y[i];


            // 均线极多头 :     EVERY(均线极多头_2,   2)       AND       ( MA5/MA10>1.01  AND   MA10/MA20>1.01 AND MA20/MA50>1.03 )       COLORYELLOW;
            result[i] = i > 0

                    && JDT_2[i] && JDT_2[i - 1]

                    && MA5[i] / MA10[i] > 1.01
                    && MA10[i] / MA20[i] > 1.01
                    && MA20[i] / MA50[i] > 1.03;
        }


        return result;
    }


    /**
     * 大均线空头
     *
     *
     *
     * 通达信公式：
     *
     *
     * MA50_MA100跌幅  :   MA_MA跌幅( 50,100)     NODRAW;
     * MA100_MA200跌幅 :   MA_MA跌幅(100,200)     NODRAW;
     *
     * CON_0          :   MA50_MA100跌幅>3 || MA100_MA200跌幅>3       NODRAW;
     *
     *
     * 大均线空头 :       (CON_0)   AND (
     *
     * (        C<MA50 AND MA50<MA100 AND MA100<MA200     AND     MA50<REF(MA50,1) AND MA100<REF(MA100,1) AND MA200<REF(MA200,1)   )
     * ||   (   C<MA60 AND MA60<MA100 AND MA100<MA200     AND     MA60<REF(MA60,1) AND MA100<REF(MA100,1) AND MA200<REF(MA200,1)   )
     *
     *
     * ||   (   C<MA50 AND MA50<MA120 AND MA120<MA250     AND     MA50<REF(MA50,1) AND MA120<REF(MA120,1) AND MA250<REF(MA250,1)   )
     * ||   (   C<MA60 AND MA60<MA120 AND MA120<MA250     AND     MA60<REF(MA60,1) AND MA120<REF(MA120,1) AND MA250<REF(MA250,1)   )       )COLORCYAN;
     *
     * @param close
     * @return
     */
    public static boolean[] 大均线空头(double[] close) {
        int n = close.length;


        // 1. 计算原始各周期移动平均
        double[] MA50 = MA(close, 50);
        double[] MA60 = MA(close, 60);
        double[] MA100 = MA(close, 100);
        double[] MA120 = MA(close, 120);
        double[] MA200 = MA(close, 200);
        double[] MA250 = MA(close, 250);

        // 2. 将 NaN（周期不足）替换为 0
        MA50 = NaN_2_0(MA50);
        MA60 = NaN_2_0(MA60);
        MA100 = NaN_2_0(MA100);
        MA120 = NaN_2_0(MA120);
        MA200 = NaN_2_0(MA200);
        MA250 = NaN_2_0(MA250);

        // // 3. 计算上一日同周期均线（REF）
        // double[] MA50_1 = REF(MA50, 1);
        // double[] MA60_1 = REF(MA60, 1);
        // double[] MA100_1 = REF(MA100, 1);
        // double[] MA120_1 = REF(MA120, 1);
        // double[] MA200_1 = REF(MA200, 1);
        // double[] MA250_1 = REF(MA250, 1);


        // 4. 遍历逐日判断
        boolean[] result = new boolean[n];


        for (int i = 0; i < n; i++) {


            //  MA50_MA100跌幅 :   MA_MA跌幅( 50,100)     NODRAW;
            // MA100_MA200跌幅 :   MA_MA跌幅(100,200)     NODRAW;
            //
            // CON_0           :   MA50_MA100跌幅>3 || MA100_MA200跌幅>3       NODRAW;

            boolean con_0 = MA100[i] / MA50[i] >= 1.03 && MA200[i] / MA100[i] >= 1.03;


            //
            //
            // 大均线空头 :       (CON_0)   AND (
            //
            //               (   C<MA50 AND MA50<MA100 AND MA100<MA200     AND     MA50<REF(MA50,1) AND MA100<REF(MA100,1) AND MA200<REF(MA200,1)   )
            //          ||   (   C<MA60 AND MA60<MA100 AND MA100<MA200     AND     MA60<REF(MA60,1) AND MA100<REF(MA100,1) AND MA200<REF(MA200,1)   )
            //
            //
            //          ||   (   C<MA50 AND MA50<MA120 AND MA120<MA250     AND     MA50<REF(MA50,1) AND MA120<REF(MA120,1) AND MA250<REF(MA250,1)   )
            //          ||   (   C<MA60 AND MA60<MA120 AND MA120<MA250     AND     MA60<REF(MA60,1) AND MA120<REF(MA120,1) AND MA250<REF(MA250,1)   )       )COLORCYAN;


            result[i] = i > 0 && con_0 &&

                    (
                            (close[i] < MA50[i] && MA50[i] < MA100[i] && MA100[i] < MA200[i] && MA50[i] < MA50[i - 1] && MA100[i] < MA100[i - 1] && MA200[i] < MA200[i - 1])
                                    || (close[i] < MA60[i] && MA60[i] < MA100[i] && MA100[i] < MA200[i] && MA60[i] < MA60[i - 1] && MA100[i] < MA100[i - 1] && MA200[i] < MA200[i - 1])

                                    || (close[i] < MA50[i] && MA50[i] < MA120[i] && MA120[i] < MA250[i] && MA50[i] < MA50[i - 1] && MA120[i] < MA120[i - 1] && MA250[i] < MA250[i - 1])
                                    || (close[i] < MA60[i] && MA60[i] < MA120[i] && MA120[i] < MA250[i] && MA60[i] < MA60[i - 1] && MA120[i] < MA120[i - 1] && MA250[i] < MA250[i - 1])
                    );
        }


        return result;
    }


    /**
     * 均线大空头
     *
     *
     *
     * 通达信公式：
     *
     * 均线大空头 :   ( C<MA10 AND MA10<MA20 AND MA20<MA50 AND MA50<MA100 AND MA100<MA200 )
     *
     * AND            ( MA10<REF(MA10,1) AND MA20<REF(MA20,1) AND MA50<REF(MA50,1) AND MA100<REF(MA100,1) AND MA200<REF(MA200,1) )       COLORCYAN;
     *
     * @param close
     * @return
     */
    public static boolean[] 均线大空头(double[] close) {
        int n = close.length;


        // 1. 计算原始各周期移动平均
        double[] MA10 = MA(close, 10);
        double[] MA20 = MA(close, 20);
        double[] MA50 = MA(close, 50);
        double[] MA100 = MA(close, 100);
        double[] MA200 = MA(close, 200);

        // 2. 将 NaN（周期不足）替换为 0
        MA10 = NaN_2_0(MA10);
        MA20 = NaN_2_0(MA20);
        MA50 = NaN_2_0(MA50);
        MA100 = NaN_2_0(MA100);
        MA200 = NaN_2_0(MA200);

        // // 3. 计算上一日同周期均线（REF）
        // double[] MA50_1 = REF(MA50, 1);
        // double[] MA60_1 = REF(MA60, 1);
        // double[] MA100_1 = REF(MA100, 1);
        // double[] MA120_1 = REF(MA120, 1);
        // double[] MA200_1 = REF(MA200, 1);
        // double[] MA250_1 = REF(MA250, 1);


        // 4. 遍历逐日判断
        boolean[] result = new boolean[n];


        for (int i = 0; i < n; i++) {


            // 均线大空头 :   ( C<MA10 AND MA10<MA20 AND MA20<MA50 AND MA50<MA100 AND MA100<MA200 )
            //
            //    AND       ( MA10<REF(MA10,1) AND MA20<REF(MA20,1) AND MA50<REF(MA50,1) AND MA100<REF(MA100,1) AND MA200<REF(MA200,1) )       COLORCYAN;


            result[i] = i > 0 &&
                    close[i] < MA10[i] && MA10[i] < MA20[i] && MA20[i] < MA50[i] && MA50[i] < MA100[i] && MA100[i] < MA200[i]
                    && MA10[i] < MA10[i - 1] && MA20[i] < MA20[i - 1] && MA50[i] < MA50[i - 1] && MA100[i] < MA100[i - 1] && MA200[i] < MA200[i - 1];
        }


        return result;
    }


    public static int[] klineType(double[] close) {
        int n = close.length;
        int[] result = new int[n];


        for (int i = 0; i < n; i++) {

        }


        return result;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  复杂指标
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 月多
     *
     * @param date
     * @param open
     * @param high
     * @param low
     * @param close
     * @return
     */
    public static boolean[] 月多(LocalDate[] date, double[] open, double[] high, double[] low, double[] close) {

        List<MonthlyBullSignal.KlineBar> dailyKlines = Lists.newArrayList();
        for (int i = 0; i < date.length; i++) {
            MonthlyBullSignal.KlineBar klineBar = new MonthlyBullSignal.KlineBar(date[i], open[i], high[i], low[i], close[i]);
            dailyKlines.add(klineBar);
        }


        // return MonthlyBullSignal.computeMonthlyBull(dailyKlines);
        return MonthlyBullSignal2.computeMonthlyBull(dailyKlines);
    }


    /**
     * XZZB指标
     *
     *
     * // X_1 :=  ( EMA(C, 4) + MA(C, 4*2) + MA(C, 4*4) )  /  3;
     * // X_2 :=  ( EMA(C, 6) + MA(C, 6*2) + MA(C, 6*4) )  /  3;
     * // X_3 :=  ( EMA(C, 9) + MA(C, 9*2) + MA(C, 9*4) )  /  3;
     * // X_4 :=  ( EMA(C,13) + MA(C,13*2) + MA(C,13*4) )  /  3;
     * //
     * // X_5 :=  LLV(L,27);
     * // X_6 :=  HHV(H,34);
     * // X_7 :=  EMA((C-X_5) / (X_6-X_5)  *4,   4)  *25;
     *
     * X_8 :=  HHV(H, 30);
     * X_9 :=  LLV(L, 30);
     *
     * X_10:=  HHV(H, 60);
     * X_11:=  LLV(L, 60);
     *
     * X_12:=  100 * EMA(C,50);
     * X_13:=  100 * C;
     *
     * X_14:=  X_8  - X_9;
     * X_15:=  X_10 - X_11;
     *
     * X_16:=  (C    - X_9 ) / X_14   *10000;
     * X_17:=  (X_8  - C   ) / X_14   *10000;
     * X_18:=  (C    - X_11) / X_15   *10000;
     * X_19:=  (X_10 - C   ) / X_15   *10000;
     *
     * X_20:=  EMA(X_16,   2);
     * X_21:=  EMA(X_17, 250);
     *
     *
     *
     * XZZB涨 :  X_20>=X_21 AND X_13>=X_12     COLORRED   NODRAW;
     * XZZB跌 :  NOT(XZZB涨)                   COLORGREEN NODRAW;
     *
     * @return
     */
    public static boolean[] XZZB(double[] high, double[] low, double[] close) {
        int n = close.length;
        boolean[] result = new boolean[n];


//        // X_1 :=  ( EMA(C, 4) + MA(C, 4*2) + MA(C, 4*4) )  /  3;
//        double[] ema_4 = EMA(close, 4);
//        double[] ma_4_2 = MA(close, 4 * 2);
//        double[] ma_4_4 = MA(close, 4 * 4);
//
//
//        // X_2 :=  ( EMA(C, 6) + MA(C, 6*2) + MA(C, 6*4) )  /  3;
//        double[] ema_6 = EMA(close, 6);
//        double[] ma_6_2 = MA(close, 6 * 2);
//        double[] ma_6_4 = MA(close, 6 * 4);
//
//
//        // X_3 :=  ( EMA(C, 9) + MA(C, 9*2) + MA(C, 9*4) )  /  3;
//        double[] ema_9 = EMA(close, 9);
//        double[] ma_9_2 = MA(close, 9 * 2);
//        double[] ma_9_4 = MA(close, 9 * 4);
//
//
//        // X_4 :=  ( EMA(C,13) + MA(C,13*2) + MA(C,13*4) )  /  3;
//        double[] ema_13 = EMA(close, 13);
//        double[] ma_13_2 = MA(close, 13 * 2);
//        double[] ma_13_4 = MA(close, 13 * 4);
//
//
//        // X_5 :=  LLV(L,27);
//        double[] X_5 = LLV(low, 27);
//
//
//        // X_6 :=  HHV(H,34);
//        double[] X_6 = HHV(high, 34);
//
//
//        // X_7 :=  EMA((C-X_5) / (X_6-X_5)  *4,   4)  *25;
//        double[] r_7 = new double[close.length];
//        for (int i = 0; i < close.length; i++) {
//            r_7[i] = close[i] - X_5[i] / (X_6[i] - X_5[i]) * 4;
//        }
//        double[] X_7 = EMA(r_7, 4);


        // X_8 :=  HHV(H, 30);
        double[] X_8 = HHV(high, 30);

        // X_9 :=  LLV(L, 30);
        double[] X_9 = LLV(low, 30);

        // X_10:=  HHV(H, 60);
        double[] X_10 = HHV(high, 60);

        // X_11:=  LLV(L, 60);
        double[] X_11 = LLV(low, 60);


        // X_12:=  100 * EMA(C,50);
        double[] X_12 = EMA(close, 50);


        // X_13:=  100 * C;
        double[] X_13 = close;


        // X_14:=  X_8  - X_9;
        // X_15:=  X_10 - X_11;
        //
        // X_16:=  (C    - X_9 ) / X_14   *10000;
        // X_17:=  (X_8  - C   ) / X_14   *10000;
        // X_18:=  (C    - X_11) / X_15   *10000;
        // X_19:=  (X_10 - C   ) / X_15   *10000;
        //


        double[] X_16 = new double[n];
        double[] X_17 = new double[n];
        double[] X_18 = new double[n];
        double[] X_19 = new double[n];


        for (int i = 0; i < n; i++) {

//            double x_1 = (ema_4[i] + ma_4_2[i] + ma_4_4[i]) / 3;
//            double x_2 = (ema_6[i] + ma_6_2[i] + ma_6_4[i]) / 3;
//            double x_3 = (ema_9[i] + ma_9_2[i] + ma_9_4[i]) / 3;
//            double x_4 = (ema_13[i] + ma_13_2[i] + ma_13_4[i]) / 3;
//            double x_5 = X_5[i];
//            double x_6 = X_6[i];
//            X_7[i] = X_7[i] * 25;
            double x_8 = X_8[i];
            double x_9 = X_9[i];
            double x_10 = X_10[i];
            double x_11 = X_11[i];


            X_12[i] = X_12[i] * 100;
            X_13[i] = X_13[i] * 100;


            double x_14 = x_8 - x_9;
            double x_15 = x_10 - x_11;


            X_16[i] = (close[i] - x_9) / x_14 * 10000;
            X_17[i] = (x_8 - close[i]) / x_14 * 10000;
            X_18[i] = (close[i] - x_11) / x_15 * 10000;
            X_19[i] = (x_10 - close[i]) / x_15 * 10000;
        }


        // X_20:=  EMA(X_16,   2);
        double[] X_20 = EMA(X_16, 2);

        // X_21:=  EMA(X_17, 250);
        double[] X_21 = EMA(X_17, 250);


        // XZZB涨 :  X_20>=X_21 AND X_13>=X_12     COLORRED   NODRAW;
        // XZZB跌 :  NOT(XZZB涨)                   COLORGREEN NODRAW;


        for (int i = 0; i < n; i++) {
            result[i] = X_20[i] >= X_21[i] && X_13[i] >= X_12[i];
        }


        return result;
    }


    /**
     * BS区间
     *
     * @param close
     * @return
     */
    public static boolean[] BSQJ(double[] close) {
        int n = close.length;
        boolean[] result = new boolean[n];


        // 买线 := EMA(C,2);
        double[] 买线 = EMA(close, 2);


        // 卖线 := EMA(SLOPE(C,21) *20 +C, 42);
        double[] slope = SLOPE(close, 21);
        for (int i = 0; i < n; i++) {
            slope[i] = slope[i] * 20 + close[i];
        }
        double[] 卖线 = EMA(slope, 42);


        // 指导:=EMA((EMA(CLOSE,4)+EMA(CLOSE,6)+EMA(CLOSE,12)+EMA(CLOSE,24))/4,2);
        // 界:=MA(CLOSE,27);


        // B买:IF(CROSS(指导,界) OR CROSS(买线,卖线),C,DRAWNULL),COLORMAGENTA,NODRAW;
        // 持仓（含B）:IF(买线>=卖线,C,DRAWNULL),COLORRED,NODRAW;
        // S卖:IF(CROSS(界,指导) OR CROSS(卖线,买线),C,DRAWNULL),COLORLIGRAY,NODRAW;
        // 空仓（含S）:IF(买线<卖线,C,DRAWNULL),COLORGREEN,NODRAW;


        // 持仓（含B）
        for (int i = 0; i < n; i++) {
            result[i] = 买线[i] >= 卖线[i];
        }


        return result;
    }


    /**
     * RPS三线和 - sum value
     *
     * @param rps10
     * @param rps20
     * @param rps50
     * @param rps120
     * @param rps250
     * @return
     */
    public static double[] RPS三线和(double[] rps10,
                                     double[] rps20,
                                     double[] rps50,
                                     double[] rps120,
                                     double[] rps250) {

        int n = rps50.length;

        double[] result = new double[n];
        for (int i = 0; i < n; i++) {

            double v1 = rps10[i] + rps20[i] + rps50[i];
            double v2 = rps50[i] + NumUtil.NaN_0(rps120[i]) + NumUtil.NaN_0(rps250[i]);

            result[i] = Math.max(v1, v2);
        }

        return result;
    }

    /**
     * RPS五线和 - sum value
     *
     * @param rps10
     * @param rps20
     * @param rps50
     * @param rps120
     * @param rps250
     * @return
     */
    public static double[] RPS五线和(double[] rps10,
                                     double[] rps20,
                                     double[] rps50,
                                     double[] rps120,
                                     double[] rps250) {

        int n = rps50.length;

        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = rps10[i] + rps20[i] + rps50[i] + NumUtil.NaN_0(rps120[i]) + NumUtil.NaN_0(rps250[i]);
        }

        return result;
    }

    /**
     * RPS三线和 - sum value >= RPS
     *
     * @param rps10
     * @param rps20
     * @param rps50
     * @param rps120
     * @param rps250
     * @param RPS
     * @return
     */
    public static boolean[] RPS三线和2(double[] rps10,
                                       double[] rps20,
                                       double[] rps50,
                                       double[] rps120,
                                       double[] rps250,
                                       double RPS) {

        int n = rps50.length;

        boolean[] result = new boolean[n];
        for (int i = 0; i < n; i++) {

            double v1 = rps10[i] + rps20[i] + rps50[i];
            double v2 = rps50[i] + NumUtil.NaN_0(rps120[i]) + NumUtil.NaN_0(rps250[i]);

            result[i] = Math.max(v1, v2) >= RPS;
        }

        return result;
    }


    /**
     * RPS一线红(95) || RPS双线红(90) || RPS三线红(85);
     *
     * @param rps50
     * @param rps120
     * @param rps250
     * @param RPS1   RPS一线红
     * @param RPS2   RPS双线红
     * @param RPS3   RPS三线红
     * @return
     */
    public static boolean[] RPS红(double[] rps50,
                                  double[] rps120,
                                  double[] rps250,
                                  double RPS1,
                                  double RPS2,
                                  double RPS3) {

        // RPS一线红(95) || RPS双线红(90) || RPS三线红(85);


        int n = rps50.length;

        boolean[] result = new boolean[n];
        for (int i = 0; i < n; i++) {


            double rps_50 = rps50[i];
            double rps_120 = rps120[i];
            double rps_250 = rps250[i];


            boolean RPS一线红 = rps_50 >= RPS1 || rps_120 >= RPS1 || rps_250 >= RPS1;
            boolean RPS双线红 = bool2Int(rps_50 >= RPS2) + bool2Int(rps_120 >= RPS2) + bool2Int(rps_250 >= RPS2) >= 2;
            boolean RPS三线红 = rps_50 >= RPS3 && rps_120 >= RPS3 && rps_250 >= RPS3;


            result[i] = RPS一线红 || RPS双线红 || RPS三线红;
        }

        return result;
    }


    public static boolean[] RPS一线红(double[] rps50, double[] rps120, double[] rps250, double RPS) {
        int n = rps50.length;

        boolean[] result = new boolean[n];
        for (int i = 0; i < n; i++) {

            result[i] = rps50[i] >= RPS
                    || rps120[i] >= RPS
                    || rps250[i] >= RPS;
        }

        return result;
    }

    public static boolean[] RPS双线红(double[] rps50, double[] rps120, double[] rps250, double RPS) {
        int n = rps50.length;

        boolean[] result = new boolean[n];
        for (int i = 0; i < n; i++) {

            boolean c1 = rps50[i] >= RPS;
            boolean c2 = rps120[i] >= RPS;
            boolean c3 = rps250[i] >= RPS;

            result[i] = bool2Int(c1) + bool2Int(c2) + bool2Int(c3) >= 2;
        }

        return result;
    }


    /**
     * RPS三线红
     *
     * @param rps50
     * @param rps120
     * @param rps250
     * @param RPS
     * @return
     */
    public static boolean[] RPS三线红(double[] rps50, double[] rps120, double[] rps250, double RPS) {
        int n = rps50.length;

        boolean[] result = new boolean[n];
        for (int i = 0; i < n; i++) {

            result[i] = rps50[i] >= RPS
                    && rps120[i] >= RPS
                    && rps250[i] >= RPS;
        }

        return result;
    }


    /**
     * 上影大阴                                     - upperShadowBigBear
     *
     *
     * - TODO       高位 - 爆量/上影/大阴
     *
     * @param high   最高价序列
     * @param low    最低价序列
     * @param close  收盘价序列
     * @param is20CM 涨跌幅限制 20%/30%     true/false
     * @return 布尔序列，true 表示当天为上影大阴
     */
    public static boolean[] 上影大阴(double[] high, double[] low, double[] close, boolean is20CM) {

        int n = close.length;
        if (high.length != n || low.length != n) {
            throw new IllegalArgumentException("非法数据：数组长度不一致");
        }


        boolean[] result = new boolean[n];


        // 计算 涨幅、振幅、上影线比例
        double[] pctChange = new double[n];
        double[] amplitude = new double[n];
        double[] upperShadowRatio = new double[n];

        // Arrays.fill(pctChange, Double.NaN);
        // Arrays.fill(amplitude, Double.NaN);
        // Arrays.fill(upperShadowRatio, Double.NaN);


        for (int i = 0; i < n; i++) {
            // 涨幅： (C/REF(C,1) -1) *100
            if (i > 0 && close[i - 1] != 0) {
                pctChange[i] = (close[i] / close[i - 1] - 1.0) * 100.0;
            }
            // 振幅： (H/L -1) *100
            if (low[i] != 0) {
                amplitude[i] = (high[i] / low[i] - 1.0) * 100.0;
            }
            // 上影线比例： (C - L)/(H - L)
            double range = high[i] - low[i];
            if (range != 0) {
                upperShadowRatio[i] = (close[i] - low[i]) / range;
            }
        }


        // 计算上影大阴： 上影线比例 < 0.4   并且 (跌幅 或 振幅 超限)          ==>          收盘价 位于振幅 下1/3

        // 上影线_比例 < 0.4     AND     (  IF(_20CM, 涨幅<-9, 涨幅<-4.5)   ||   IF(_20CM, 振幅>10, 振幅>5)  )


        for (int i = 0; i < n; i++) {
            boolean condShadow = /*!Double.isNaN(upperShadowRatio[i]) &&*/ upperShadowRatio[i] < 0.4;
            boolean condFall;
            boolean condAmp;

            if (is20CM) {
                condFall = pctChange[i] < -9.0;
                condAmp = amplitude[i] > 10.0;
            } else {
                condFall = pctChange[i] < -4.5;
                condAmp = amplitude[i] > 5.0;
            }

            result[i] = condShadow && (condFall || condAmp);
        }


        return result;
    }


    public static boolean[] 爆量(double[] amo) {
        int n = amo.length;
        boolean[] result = new boolean[n];


        // { ------------------ 高位爆量 }
        //
        // AMO_MA5  :=  MA(AMO, 5);
        // AMO_MA10 :=  MA(AMO,10);
        // 上影大阴 :=  上影大阴.上影大阴;
        //
        //
        // 高位爆量_1 :=  AMO/AMO_MA5>=1.9   AND   AMO/AMO_MA10>=2.1;
        //
        // 高位爆量_2 :=  NOT(高位爆量_1)    AND   AMO / REF( HHV(AMO,10) ,1) >= 1.35     {AND     AMO_亿>=10};
        //
        //
        //
        // 高位爆量上影 :=  (倍  AND  (高位爆量_1  ||  高位爆量_2   ||   上影大阴)  )   ||   (中期涨幅>100 AND 上影大阴  AND  高位爆量_1)       NODRAW;


        // AMO_MA5  :=  MA(AMO, 5);
        // AMO_MA10 :=  MA(AMO,10);
        double[] AMO_MA5 = MA(amo, 5);
        double[] AMO_MA10 = MA(amo, 10);


        double[] AMO_H10 = HHV(amo, 10);
        double[] REF1_AMO_H10 = REF(AMO_H10, 1);


        for (int i = 0; i < n; i++) {

            // 高位爆量_1 :=  AMO/AMO_MA5>=1.9   AND   AMO/AMO_MA10>=2.1;
            boolean 高位爆量_1 = amo[i] / AMO_MA5[i] >= 1.9 && amo[i] / AMO_MA10[i] >= 2.1;

            // 高位爆量_2 :=  NOT(高位爆量_1)    AND   AMO / REF( HHV(AMO,10) ,1) >= 1.35     {AND     AMO_亿>=10};
            boolean 高位爆量_2 = !高位爆量_1 && amo[i] / REF1_AMO_H10[i] >= 1.35;


            result[i] = 高位爆量_1 || 高位爆量_2;
        }


        return result;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  趋势指标
    // -----------------------------------------------------------------------------------------------------------------


    public static void 趋势股() {

        // 1、60日新高

        // 2、大均线多头 / 均线预萌出

        // 3、小均线多头              ->      MA20


        // 4、强趋势   ->   MA10支撑
        // 4、趋势股   ->   MA20支撑
        // 5、慢趋势   ->   MA50支撑


        // C_MA_偏离率();


    }


    /**
     * 趋势支撑线
     *
     *
     *
     * 小均线多头 :=  小均线多头.小均线多头;
     * 均线大多头 :=  均线大多头.均线大多头;
     *
     *
     *
     * { -------------------------------------------------------------------------- }
     * 支撑线 :
     *
     * IF(MA多(  5) AND COUNT(MA多(  5), 10)>= 9   AND   小均线多头,      5,
     * IF(MA多( 10) AND COUNT(MA多( 10), 15)>=14   AND   均线大多头,     10,
     * IF(MA多( 20) AND COUNT(MA多( 20), 20)>=19,     20,
     * IF(MA多( 50) AND COUNT(MA多( 50), 50)>=47,     50,
     * IF(MA多( 60) AND COUNT(MA多( 60), 50)>=47,     60,
     * IF(MA多(100) AND COUNT(MA多(100), 70)>=67,    100,
     * IF(MA多(120) AND COUNT(MA多(120), 70)>=67,    120,
     * IF(MA多(150) AND COUNT(MA多(150), 75)>=72,    150,
     * IF(MA多(200) AND COUNT(MA多(200), 90)>=85,    200,
     * IF(MA多(250) AND COUNT(MA多(250), 90)>=85,    250,
     *
     * 0))))))))))       COLORWHITE DOTLINE;
     *
     * @param close
     * @return
     */
    public static int[] 短期趋势支撑线(double[] close) {
        int n = close.length;
        int[] MA = new int[n];


        // 小均线多头 :=  小均线多头.小均线多头;
        // 均线大多头 :=  均线大多头.均线大多头;
        boolean[] 小均线多头 = 小均线多头(close);
        boolean[] 均线大多头 = 均线大多头(close);


        // { -------------------------------------------------------------------------- }
        // 支撑线 :
        //
        //   IF(MA多(  5) AND COUNT(MA多(  5), 10)>= 9   AND   小均线多头,      5,
        //   IF(MA多( 10) AND COUNT(MA多( 10), 15)>=14   AND   均线大多头,     10,
        //   IF(MA多( 20) AND COUNT(MA多( 20), 20)>=19,     20,
        //   IF(MA多( 50) AND COUNT(MA多( 50), 50)>=47,     50,
        //   IF(MA多( 60) AND COUNT(MA多( 60), 50)>=47,     60,
        //   IF(MA多(100) AND COUNT(MA多(100), 70)>=67,    100,
        //   IF(MA多(120) AND COUNT(MA多(120), 70)>=67,    120,
        //   IF(MA多(150) AND COUNT(MA多(150), 75)>=72,    150,
        //   IF(MA多(200) AND COUNT(MA多(200), 90)>=85,    200,
        //   IF(MA多(250) AND COUNT(MA多(250), 90)>=85,    250,
        //
        //   0))))))))))       COLORWHITE DOTLINE;


        boolean[] MA5_多 = MA多(close, 5);
        boolean[] MA10_多 = MA多(close, 10);
        boolean[] MA20_多 = MA多(close, 20);
        boolean[] MA50_多 = MA多(close, 50);
        boolean[] MA60_多 = MA多(close, 60);
        boolean[] MA100_多 = MA多(close, 100);
        boolean[] MA120_多 = MA多(close, 120);
        boolean[] MA150_多 = MA多(close, 150);
        boolean[] MA200_多 = MA多(close, 200);
        boolean[] MA250_多 = MA多(close, 250);


        int[] count_MA5_多 = COUNT(MA5_多, 10);
        int[] count_MA10_多 = COUNT(MA10_多, 15);
        int[] count_MA20_多 = COUNT(MA20_多, 20);
        int[] count_MA50_多 = COUNT(MA50_多, 50);
        int[] count_MA60_多 = COUNT(MA60_多, 50);
        int[] count_MA100_多 = COUNT(MA100_多, 70);
        int[] count_MA120_多 = COUNT(MA120_多, 70);
        int[] count_MA150_多 = COUNT(MA150_多, 75);
        int[] count_MA200_多 = COUNT(MA200_多, 90);
        int[] count_MA250_多 = COUNT(MA250_多, 90);


        for (int i = 0; i < n; i++) {
            if (MA5_多[i] && count_MA5_多[i] >= 9 && 小均线多头[i]) {
                MA[i] = 5;
            } else if (MA10_多[i] && count_MA10_多[i] >= 14 && 均线大多头[i]) {
                MA[i] = 10;
            } else if (MA20_多[i] && count_MA20_多[i] >= 19) {
                MA[i] = 20;
            } else if (MA50_多[i] && count_MA50_多[i] >= 47) {
                MA[i] = 50;
            } else if (MA60_多[i] && count_MA60_多[i] >= 47) {
                MA[i] = 60;
            } else if (MA100_多[i] && count_MA100_多[i] >= 67) {
                MA[i] = 100;
            } else if (MA120_多[i] && count_MA120_多[i] >= 67) {
                MA[i] = 120;
            } else if (MA150_多[i] && count_MA150_多[i] >= 72) {
                MA[i] = 150;
            } else if (MA200_多[i] && count_MA200_多[i] >= 85) {
                MA[i] = 200;
            } else if (MA250_多[i] && count_MA250_多[i] >= 85) {
                MA[i] = 250;
            } else {
                // default
                MA[i] = 200;
            }
        }


        return MA;
    }

    /**
     * 中期趋势支撑线
     *
     * @param close
     * @param 短期趋势支撑线
     * @return
     */
    public static int[] 中期趋势支撑线(double[] close, int[] 短期趋势支撑线) {
        int[] result = new int[close.length];


        boolean[] 下MA20 = 下MA(close, 20);
        int[] BARSLAST__下MA20 = TdxFun.BARSLAST(下MA20);


        for (int i = 0; i < close.length; i++) {
            // 支撑线 区间起始日期   ->   距今天数
            int last__下MA20__days = BARSLAST__下MA20[i];

            // 支撑线 区间起始日期   ->   idx
            int lastIdx = Math.max(i - last__下MA20__days, 0);


            result[i] = 短期趋势支撑线[lastIdx];
        }


        return result;
    }

    public static int[] 趋势支撑线_2(double[] close, double[] ssf) {
        int n = close.length;
        int[] MA = new int[n];


        // -------------------------------------------------- 当日

        double[] C_MA50_偏离率 = C_MA_偏离率(close, 50);
        double[] C_MA20_偏离率 = C_MA_偏离率(close, 20);
        double[] C_MA10_偏离率 = C_MA_偏离率(close, 10);

//        double[] C_MA5_偏离率 = C_MA_偏离率(close, 5);
//        double[] C_SSF_偏离率 = C_SSF_偏离率(close, ssf);


        // -------------------------------------------------- 近日
        int N = 10;

        double[] C_MA50_偏离率_H = HHV(C_MA50_偏离率, 10);
        double[] C_MA20_偏离率_H = HHV(C_MA20_偏离率, 10);
        double[] C_MA10_偏离率_H = HHV(C_MA10_偏离率, 10);

//        double[] C_MA5_偏离率_H = HHV(C_MA5_偏离率, 10);
//        double[] C_SSF_偏离率_H = HHV(C_SSF_偏离率, 10);


        // ----------------------------------------------------------------------------------------------------


        for (int i = 0; i < n; i++) {

            double MA50_偏离率 = C_MA50_偏离率[i];
            double MA20_偏离率 = C_MA20_偏离率[i];
            double MA10_偏离率 = C_MA10_偏离率[i];

//            double MA5_偏离率 = C_MA5_偏离率[i];
//            double SSF_偏离率 = C_SSF_偏离率[i];


            double MA50_偏离率_H = C_MA50_偏离率_H[i];
            double MA20_偏离率_H = C_MA20_偏离率_H[i];
            double MA10_偏离率_H = C_MA10_偏离率_H[i];

//            double MA5_偏离率_H = C_MA5_偏离率_H[i];
//            double SSF_偏离率_H = C_SSF_偏离率_H[i];


            // 10CM


            // --------------------------------------------------


            // 默认值：MA20
            MA[i] = 20;


            // -------------------------------------------------- 当日


            if (MA10_偏离率 > 20) {
                MA[i] = 5;
                continue;
            }

            if (MA20_偏离率 > 25) {
                MA[i] = 10;
                continue;
            }

            if (MA50_偏离率 > 30) {
                MA[i] = 20;
                continue;
            }


            // -------------------------------------------------- 近日


            if (MA10_偏离率_H > 25) {
                MA[i] = 5;
                continue;
            }

            if (MA20_偏离率_H > 30) {
                MA[i] = 10;
                continue;
            }

            if (MA50_偏离率_H > 35) {
                MA[i] = 20;
                continue;
            }
        }


        return MA;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 将 NaN（周期不足）替换为 0
     *
     * @param arr
     * @return
     */
    private static double[] NaN_2_0(double[] arr) {
        return Arrays.stream(arr).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
    }


}