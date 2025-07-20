package com.bebopze.tdx.quant.common.tdxfun;

import com.bebopze.tdx.quant.common.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

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


    public static List<KlineAggregator.PeriodDTO> toWeek(String[] date, double[] value) {
        return KlineAggregator.toWeekly(date, value);
        // List<MonthlyBullSignal.KlineBar> weeklyBarList = MonthlyBullSignal.aggregateToWeekly(dailyKlines);
    }

    public static List<KlineAggregator.PeriodDTO> toMonth(String[] date, double[] value) {
        return KlineAggregator.toMonthly(date, value);
        // List<MonthlyBullSignal.KlineBar> monthlyBarList = MonthlyBullSignal.aggregateToMonthly(dailyKlines);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  简单指标
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                                      MA
    // -----------------------------------------------------------------------------------------------------------------


    public static boolean[] 上MA(double[] close, int N) {
        int len = close.length;
        boolean[] arr = new boolean[len];


        // MA20
        double[] MA20_arr = MA(close, N);


        for (int i = 0; i < len; i++) {
            double MA20 = MA20_arr[i];
            double C = close[i];

            arr[i] = C >= MA20;
        }

        return arr;
    }

    public static boolean[] 下MA(double[] close, int N) {
        int len = close.length;
        boolean[] arr = new boolean[len];


        // MA20
        double[] MA20_arr = MA(close, N);


        for (int i = 0; i < len; i++) {
            double MA20 = MA20_arr[i];
            double C = close[i];

            arr[i] = C < MA20;
        }

        return arr;
    }


    public static boolean[] MA向上(double[] close, int N) {
        int len = close.length;
        boolean[] arr = new boolean[len];


        // MA20
        double[] MA20_arr = MA(close, N);


        for (int i = 0; i < len; i++) {

            if (i == 0) {
                arr[i] = false;

            } else {
                double MA20 = MA20_arr[i];
                double MA20_pre = MA20_arr[i - 1];

                arr[i] = MA20 >= MA20_pre;
            }
        }

        return arr;
    }


    public static boolean[] MA向下(double[] close, int N) {
        int len = close.length;
        boolean[] arr = new boolean[len];


        // MA20
        double[] MA20_arr = MA(close, N);


        for (int i = 0; i < len; i++) {


            if (i == 0) {
                arr[i] = false;

            } else {
                double MA20 = MA20_arr[i];
                double MA20_pre = MA20_arr[i - 1];

                arr[i] = MA20 < MA20_pre;
            }
        }

        return arr;
    }


    public static boolean[] MA多(double[] close, int N) {
        return con_merge(上MA(close, N), MA向上(close, N));
    }


    public static boolean[] MA空(double[] close, int N) {
        return con_merge(下MA(close, N), MA向下(close, N));
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                      SSF
    // -----------------------------------------------------------------------------------------------------------------


    public static boolean[] 上SSF(double[] close, double[] ssf) {
        int len = close.length;
        boolean[] arr = new boolean[len];


        try {
            for (int i = 0; i < len; i++) {
                double SSF = ssf[i];
                double C = close[i];

                arr[i] = C >= SSF;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return arr;
    }

    public static boolean[] 下SSF(double[] close, double[] ssf) {
        int len = close.length;
        boolean[] arr = new boolean[len];


        for (int i = 0; i < len; i++) {
            double SSF = ssf[i];
            double C = close[i];

            arr[i] = C < SSF;
        }

        return arr;
    }


    public static boolean[] SSF向上(double[] close, double[] ssf) {
        int len = close.length;
        boolean[] arr = new boolean[len];


        for (int i = 0; i < len; i++) {

            if (i == 0) {
                arr[i] = false;

            } else {
                double SSF = ssf[i];
                double SSF_pre = ssf[i - 1];

                arr[i] = SSF >= SSF_pre;
            }
        }

        return arr;
    }

    public static boolean[] SSF向下(double[] close, double[] ssf) {
        int len = close.length;
        boolean[] arr = new boolean[len];


        for (int i = 0; i < len; i++) {

            if (i == 0) {
                arr[i] = false;

            } else {
                double SSF = ssf[i];
                double SSF_pre = ssf[i - 1];

                arr[i] = SSF < SSF_pre;
            }
        }

        return arr;
    }


    public static boolean[] SSF多(double[] close, double[] ssf) {
        return con_merge(上SSF(close, ssf), SSF向上(close, ssf));
    }


    public static boolean[] SSF空(double[] close, double[] ssf) {
        return con_merge(下SSF(close, ssf), SSF向下(close, ssf));
    }


    public static double[] C_SSF_偏离率(double[] close, double[] ssf) {
        int n = close.length;

        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = NumUtil.of((close[i] / ssf[i] - 1) * 100);
        }

        return result;
    }


    /**
     * 结果合并   -   AND
     *
     * @param arr_list
     * @return
     */
    public static boolean[] con_merge(boolean[]... arr_list) {

        int len = arr_list[0].length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {
            boolean acc = true;
            for (boolean[] arr : arr_list) {
                acc &= arr[i];
                if (!acc) break;
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
     * 计算“大均线多头”布尔序列                            - bigMaBull
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


        return MonthlyBullSignal.computeMonthlyBull(dailyKlines);
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
    public static boolean[] RPS红(double[] rps50, double[] rps120, double[] rps250, int RPS1, int RPS2, int RPS3) {
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


    public static boolean[] RPS一线红(double[] rps50, double[] rps120, double[] rps250, int RPS) {
        int n = rps50.length;

        boolean[] result = new boolean[n];
        for (int i = 0; i < n; i++) {

            result[i] = rps50[i] >= RPS
                    || rps120[i] >= RPS
                    || rps250[i] >= RPS;
        }

        return result;
    }

    public static boolean[] RPS双线红(double[] rps50, double[] rps120, double[] rps250, int RPS) {
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
    public static boolean[] RPS三线红(double[] rps50, double[] rps120, double[] rps250, int RPS) {
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


}
