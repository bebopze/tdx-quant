package com.bebopze.tdx.quant.common.tdxfun;

import java.util.Arrays;
import java.util.stream.IntStream;


/**
 * 通达信/同花顺  函数库     -     Java实现
 * -
 * -
 * - MyTT.py     Java版                               https://github.com/mpquant/MyTT/blob/main/MyTT.py
 * -
 * - 转换自  原作者 mpquant  的  Python实现
 *
 * @author: bebopze
 * @date: 2025/5/13
 */
public class TdxFun {


    public static void main(String[] args) {

        // 测试案例：
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
        System.out.println("MA(data, 3) = " + Arrays.toString(MA(data, 3)));
        System.out.println("EMA(data, 3) = " + Arrays.toString(EMA(data, 3)));


        double[] a = {5, 4, 6, 7, 3, 8};
        boolean[] cond = {false, true, false, true, false, true};
        System.out.println("FORCAST(a,3): " + Arrays.toString(FORCAST(a, 3)));
        System.out.println("BARSLAST(cond): " + Arrays.toString(BARSLAST(cond)));
        System.out.println("CROSS MA(3) vs MA(5): " + Arrays.toString(CROSS(MA(a, 3), MA(a, 5))));
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    public static double[] REF(double[] S,
                               int N) {
        double[] r = new double[S.length];
        Arrays.fill(r, Double.NaN);
        for (int i = N; i < S.length; i++) r[i] = S[i - N];
        return r;
    }

    public static double[] DIFF(double[] S,
                                int N) {
        double[] r = new double[S.length];
        Arrays.fill(r, Double.NaN);
        for (int i = N; i < S.length; i++) r[i] = S[i] - S[i - N];
        return r;
    }

    public static double[] STD(double[] S,
                               int N) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) {
            if (i + 1 >= N) {
                double sum = 0;
                for (int j = i + 1 - N; j <= i; j++)
                    sum += Math.pow(S[j] - Arrays.stream(S, i + 1 - N, i + 1).average().orElse(Double.NaN), 2);
                r[i] = Math.sqrt(sum / N);
            } else r[i] = Double.NaN;
        }
        return r;
    }

    public static double[] SUM(double[] S,
                               int N) {
        double[] r = new double[S.length];
        if (N <= 0) {
            double cum = 0;
            for (int i = 0; i < S.length; i++) {
                cum += S[i];
                r[i] = cum;
            }
        } else {
            for (int i = 0; i < S.length; i++) {
                if (i + 1 >= N) {
                    double sum = 0;
                    for (int j = i + 1 - N; j <= i; j++) sum += S[j];
                    r[i] = sum;
                } else r[i] = Double.NaN;
            }
        }
        return r;
    }

    public static double[] CONST(double[] S) {
        double[] r = new double[S.length];
        double v = S[S.length - 1];
        Arrays.fill(r, v);
        return r;
    }

    public static double[] HHV(double[] S,
                               int N) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) {
            if (i + 1 >= N) {
                double max = Double.NEGATIVE_INFINITY;
                for (int j = i + 1 - N; j <= i; j++) max = Math.max(max, S[j]);
                r[i] = max;
            } else r[i] = Double.NaN;
        }
        return r;
    }

    public static double[] LLV(double[] S,
                               int N) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) {
            if (i + 1 >= N) {
                double min = Double.POSITIVE_INFINITY;
                for (int j = i + 1 - N; j <= i; j++) min = Math.min(min, S[j]);
                r[i] = min;
            } else r[i] = Double.NaN;
        }
        return r;
    }

    public static int[] HHVBARS(double[] S,
                                int N) {
        int[] r = new int[S.length];
        for (int i = N - 1; i < S.length; i++) {
            double max = Double.NEGATIVE_INFINITY;
            int idx = 0;
            for (int j = i + 1 - N; j <= i; j++)
                if (S[j] > max) {
                    max = S[j];
                    idx = i - j;
                }
            r[i] = idx;
        }
        return r;
    }

    public static int[] LLVBARS(double[] S,
                                int N) {
        int[] r = new int[S.length];
        for (int i = N - 1; i < S.length; i++) {
            double min = Double.POSITIVE_INFINITY;
            int idx = 0;
            for (int j = i + 1 - N; j <= i; j++)
                if (S[j] < min) {
                    min = S[j];
                    idx = i - j;
                }
            r[i] = idx;
        }
        return r;
    }


    public static double[] MA(double[] S,
                              int N) {
        double[] r = new double[S.length];
        double sum = 0;
        for (int i = 0; i < S.length; i++) {
            sum += S[i];
            if (i >= N) sum -= S[i - N];
            if (i + 1 >= N) r[i] = sum / N;
            else r[i] = Double.NaN;
        }
        return r;
    }

    public static double[] EMA(double[] S,
                               int N) {
        double[] r = new double[S.length];
        double alpha = 2.0 / (N + 1);
        r[0] = S[0];
        for (int i = 1; i < S.length; i++) r[i] = alpha * S[i] + (1 - alpha) * r[i - 1];
        return r;
    }

    public static double[] SMA(double[] S,
                               int N,
                               double M) {
        double[] r = new double[S.length];
        double alpha = M / N;
        r[0] = S[0];
        for (int i = 1; i < S.length; i++) r[i] = alpha * S[i] + (1 - alpha) * r[i - 1];
        return r;
    }

    public static double[] WMA(double[] S,
                               int N) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) {
            if (i + 1 >= N) {
                double num = 0, den = N * (N + 1) / 2.0;
                for (int j = 0; j < N; j++) num += (j + 1) * S[i - j];
                r[i] = num / den;
            } else r[i] = Double.NaN;
        }
        return r;
    }

    public static double[] DMA(double[] S,
                               double A) {
        double[] r = new double[S.length];
        r[0] = S[0];
        for (int i = 1; i < S.length; i++) r[i] = A * S[i] + (1 - A) * r[i - 1];
        return r;
    }


    /**
     * 动态移动平均：A 支持序列化平滑因子
     *
     * @param S 原始序列
     * @param A 每一周期的平滑系数数组，长度与 S 相同
     * @return 计算后的 DMA 序列
     */
    public static double[] DMA(double[] S,
                               double[] A) {
        int n = S.length;
        double[] Y = new double[n];
        if (n == 0) return Y;
        Y[0] = S[0];
        for (int i = 1; i < n; i++) {
            double alpha = A[i];
            Y[i] = alpha * S[i] + (1 - alpha) * Y[i - 1];
        }
        return Y;
    }


    public static double[] AVEDEV(double[] S,
                                  int N) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) {
            if (i + 1 >= N) {
                double mean = 0;
                for (int j = i + 1 - N; j <= i; j++) mean += S[j];
                mean /= N;
                double sumAbs = 0;
                for (int j = i + 1 - N; j <= i; j++) sumAbs += Math.abs(S[j] - mean);
                r[i] = sumAbs / N;
            } else r[i] = Double.NaN;
        }
        return r;
    }

    public static double[] SLOPE(double[] S,
                                 int N) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) {
            if (i + 1 >= N) {
                double xBar = 0, yBar = 0;
                for (int j = i + 1 - N; j <= i; j++) {
                    xBar += (j - (i + 1 - N));
                    yBar += S[j];
                }
                xBar /= N;
                yBar /= N;
                double num = 0, den = 0;
                for (int j = 0; j < N; j++) {
                    num += (j - xBar) * (S[i + 1 - N + j] - yBar);
                    den += (j - xBar) * (j - xBar);
                }
                r[i] = num / den;
            } else r[i] = Double.NaN;
        }
        return r;
    }


    // FORCAST: 线性回归预测 N 周期后的值
    public static double[] FORCAST(double[] S,
                                   int N) {
        double[] r = new double[S.length];
        Arrays.fill(r, Double.NaN);
        for (int i = N - 1; i < S.length; i++) {
            // 计算回归系数
            double sumX = 0, sumY = 0;
            for (int j = 0; j < N; j++) {
                sumX += j;
                sumY += S[i - N + 1 + j];
            }
            double xBar = sumX / N;
            double yBar = sumY / N;
            double num = 0, den = 0;
            for (int j = 0; j < N; j++) {
                num += (j - xBar) * (S[i - N + 1 + j] - yBar);
                den += (j - xBar) * (j - xBar);
            }
            double slope = num / den;
            r[i] = slope * (N - 1) + yBar;
        }
        return r;
    }

    // LAST: 前 A 到 B 周期内一直满足条件
    public static boolean[] LAST(boolean[] S,
                                 int A,
                                 int B) {
        int len = S.length;
        boolean[] r = new boolean[len];
        for (int i = A; i < len; i++) {
            boolean ok = true;
            for (int j = i - A + 1 + B; j <= i; j++) {
                if (!S[j]) {
                    ok = false;
                    break;
                }
            }
            r[i] = ok;
        }
        return r;
    }

    // COUNT: 最近 N 天 True 的天数
    public static int[] COUNT(boolean[] S,
                              int N) {
        int len = S.length;
        int[] r = new int[len];
        int sum = 0;
        for (int i = 0; i < len; i++) {
            sum += S[i] ? 1 : 0;
            if (i >= N) sum -= S[i - N] ? 1 : 0;
            r[i] = i + 1 >= N ? sum : sum;
        }
        return r;
    }

    // EVERY: 最近 N 天是否全为 True
    public static boolean[] EVERY(boolean[] S,
                                  int N) {
        int len = S.length;
        boolean[] r = new boolean[len];
        int[] cnt = COUNT(S, N);
        for (int i = 0; i < len; i++) r[i] = cnt[i] == N;
        return r;
    }

    // EXIST: 最近 N 天是否存在 True
    public static boolean[] EXIST(boolean[] S,
                                  int N) {
        int len = S.length;
        boolean[] r = new boolean[len];
        int[] cnt = COUNT(S, N);
        for (int i = 0; i < len; i++) r[i] = cnt[i] > 0;
        return r;
    }

    // FILTER: 满足条件后 N 周期内屏蔽
    public static boolean[] FILTER(boolean[] S,
                                   int N) {
        int len = S.length;
        boolean[] r = Arrays.copyOf(S, len);
        for (int i = 0; i < len; i++) {
            if (S[i]) {
                for (int j = 1; j <= N && i + j < len; j++) r[i + j] = false;
            }
        }
        return r;
    }

    // BARSLAST: 上次 True 到当前的周期数
    public static int[] BARSLAST(boolean[] S) {
        int len = S.length;
        int[] r = new int[len];
        int count = 0;
        for (int i = 0; i < len; i++) {
            if (S[i]) count = 0;
            else count++;
            r[i] = count;
        }
        return r;
    }

    // BARSLASTCOUNT: 连续 True 的周期数
    public static int[] BARSLASTCOUNT(boolean[] S) {
        int len = S.length;
        int[] r = new int[len];
        int count = 0;
        for (int i = 0; i < len; i++) {
            if (S[i]) count++;
            else count = 0;
            r[i] = count;
        }
        return r;
    }

    // BARSSINCEN: N 周期内第一次 True 到现在的周期数
    public static int[] BARSSINCEN(boolean[] S,
                                   int N) {
        int len = S.length;
        int[] r = new int[len];
        for (int i = N - 1; i < len; i++) {
            int idx = -1;
            for (int j = i - N + 1; j <= i; j++)
                if (S[j]) {
                    idx = j;
                    break;
                }
            r[i] = (idx == -1 ? 0 : i - idx);
        }
        return r;
    }

    // CROSS: 向上金叉
    public static boolean[] CROSS(double[] S1,
                                  double[] S2) {
        int len = S1.length;
        boolean[] r = new boolean[len];
        for (int i = 1; i < len; i++) r[i] = S1[i] > S2[i] && S1[i - 1] <= S2[i - 1];
        return r;
    }

    // LONGCROSS: N 周期内持续低于后向上交叉
    public static boolean[] LONGCROSS(double[] S1,
                                      double[] S2,
                                      int N) {
        boolean[] lt = LAST(new boolean[0], 0, 0); // placeholder
        // 实际可调用 LAST(S1<S2, N, 1)
        boolean[] cond = new boolean[S1.length];
        for (int i = 0; i < S1.length; i++) cond[i] = S1[i] < S2[i];
        boolean[] last = LAST(cond, N, 1);
        boolean[] cross = CROSS(S1, S2);
        boolean[] r = new boolean[S1.length];
        for (int i = 0; i < r.length; i++) r[i] = last[i] && cross[i];
        return r;
    }

    // VALUEWHEN: 条件成立时取 X，否则取上次成立的 X
    public static double[] VALUEWHEN(boolean[] S,
                                     double[] X) {
        double[] r = new double[X.length];
        double last = Double.NaN;
        for (int i = 0; i < S.length; i++) {
            if (S[i]) last = X[i];
            r[i] = last;
        }
        return r;
    }

    // BETWEEN: S 在 A 和 B 之间
    public static boolean[] BETWEEN(double[] S,
                                    double[] A,
                                    double[] B) {
        int len = S.length;
        boolean[] r = new boolean[len];
        for (int i = 0; i < len; i++) r[i] = (A[i] < S[i] && S[i] < B[i]) || (A[i] > S[i] && S[i] > B[i]);
        return r;
    }

    // TOPRANGE: 当前值是前所有值中第几高
    public static int[] TOPRANGE(double[] S) {
        int len = S.length;
        int[] r = new int[len];
        for (int i = 0; i < len; i++) {
            int cnt = 0;
            for (int j = 0; j < i; j++) if (S[j] > S[i]) cnt++;
            r[i] = cnt;
        }
        return r;
    }

    // LOWRANGE: 当前值是前所有值中第几低
    public static int[] LOWRANGE(double[] S) {
        int len = S.length;
        int[] r = new int[len];
        for (int i = 0; i < len; i++) {
            int cnt = 0;
            for (int j = 0; j < i; j++) if (S[j] < S[i]) cnt++;
            r[i] = cnt;
        }
        return r;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // MACD
    public static double[][] MACD(double[] close,
                                  int shortP,
                                  int longP,
                                  int m) {
        double[] dif = new double[close.length];
        double[] dea;
        double[] macd = new double[close.length];
        double[] emaShort = EMA(close, shortP);
        double[] emaLong = EMA(close, longP);
        for (int i = 0; i < close.length; i++) dif[i] = emaShort[i] - emaLong[i];
        dea = EMA(dif, m);
        for (int i = 0; i < close.length; i++) {
            macd[i] = (dif[i] - dea[i]) * 2;
        }
        return new double[][]{rdArray(dif, 3), rdArray(dea, 3), rdArray(macd, 3)};
    }

    // KDJ
    public static double[][] KDJ(double[] close,
                                 double[] high,
                                 double[] low,
                                 int n,
                                 int m1,
                                 int m2) {
        double[] llv = LLV(low, n);
        double[] hhv = HHV(high, n);
        double[] rsv = new double[close.length];
        for (int i = 0; i < close.length; i++) {
            rsv[i] = (close[i] - llv[i]) / (hhv[i] - llv[i]) * 100;
        }
        double[] k = EMA(rsv, m1 * 2 - 1);
        double[] d = EMA(k, m2 * 2 - 1);
        double[] j = new double[close.length];
        for (int i = 0; i < close.length; i++) j[i] = k[i] * 3 - d[i] * 2;
        return new double[][]{k, d, j};
    }

    // RSI
    public static double[] RSI(double[] close,
                               int n) {
        double[] dif = new double[close.length];
        double[] ref = REF(close, 1);
        for (int i = 0; i < close.length; i++) dif[i] = close[i] - ref[i];
        double[] up = MAX(dif, new double[dif.length]);
        double[] down = ABS(dif);
        double[] rsi = new double[close.length];
        double[] maUp = SMA(up, n, 1);
        double[] maDown = SMA(down, n, 1);
        for (int i = 0; i < close.length; i++) rsi[i] = rd(maUp[i] / maDown[i] * 100, 2);
        return rsi;
    }

    // WR
    public static double[][] WR(double[] close,
                                double[] high,
                                double[] low,
                                int n,
                                int n1) {
        double[] hhvn = HHV(high, n);
        double[] llvn = LLV(low, n);
        double[] wr = new double[close.length];
        for (int i = 0; i < close.length; i++) wr[i] = (hhvn[i] - close[i]) / (hhvn[i] - llvn[i]) * 100;
        double[] hhvn1 = HHV(high, n1);
        double[] llvn1 = LLV(low, n1);
        double[] wr1 = new double[close.length];
        for (int i = 0; i < close.length; i++) wr1[i] = (hhvn1[i] - close[i]) / (hhvn1[i] - llvn1[i]) * 100;
        return new double[][]{rdArray(wr, 2), rdArray(wr1, 2)};
    }

    // BIAS
    public static double[][] BIAS(double[] close,
                                  int l1,
                                  int l2,
                                  int l3) {
        double[] bias1 = new double[close.length];
        double[] bias2 = new double[close.length];
        double[] bias3 = new double[close.length];
        double[] ma1 = MA(close, l1);
        double[] ma2 = MA(close, l2);
        double[] ma3 = MA(close, l3);
        for (int i = 0; i < close.length; i++) {
            bias1[i] = (close[i] - ma1[i]) / ma1[i] * 100;
            bias2[i] = (close[i] - ma2[i]) / ma2[i] * 100;
            bias3[i] = (close[i] - ma3[i]) / ma3[i] * 100;
        }
        return new double[][]{rdArray(bias1, 2), rdArray(bias2, 2), rdArray(bias3, 2)};
    }

    // BOLL
    public static double[][] BOLL(double[] close,
                                  int n,
                                  int p) {
        double[] mid = MA(close, n);
        double[] std = STD(close, n);
        double[] upper = new double[close.length];
        double[] lower = new double[close.length];
        for (int i = 0; i < close.length; i++) {
            upper[i] = mid[i] + std[i] * p;
            lower[i] = mid[i] - std[i] * p;
        }
        return new double[][]{rdArray(upper, 3), rdArray(mid, 3), rdArray(lower, 3)};
    }

    // PSY
    public static double[][] PSY(double[] close,
                                 int n,
                                 int m) {
        boolean[] up = CROSS(close, REF(close, 1));
        int[] cnt = COUNT(up, n);
        double[] psy = new double[close.length];
        for (int i = 0; i < close.length; i++) psy[i] = (double) cnt[i] / n * 100;
        double[] psyma = MA(psy, m);
        return new double[][]{rdArray(psy, 3), rdArray(psyma, 3)};
    }


    // CCI: 平均趋向指数
    public static double[] CCI(double[] close,
                               double[] high,
                               double[] low,
                               int n) {
        int len = close.length;
        double[] tp = new double[len];
        for (int i = 0; i < len; i++) tp[i] = (high[i] + low[i] + close[i]) / 3.0;
        double[] maTp = MA(tp, n);
        double[] adev = AVEDEV(tp, n);
        double[] cci = new double[len];
        for (int i = 0; i < len; i++) {
            cci[i] = (tp[i] - maTp[i]) / (0.015 * adev[i]);
        }
        return cci;
    }

    // ATR: 平均真实波幅
    public static double[] ATR(double[] close,
                               double[] high,
                               double[] low,
                               int n) {
        int len = close.length;
        double[] tr = new double[len];
        double[] refClose = REF(close, 1);
        for (int i = 0; i < len; i++) {
            double v1 = high[i] - low[i];
            double v2 = Math.abs(refClose[i] - high[i]);
            double v3 = Math.abs(refClose[i] - low[i]);
            tr[i] = Math.max(v1, Math.max(v2, v3));
        }
        return MA(tr, n);
    }

    // BBI: 多空指标
    public static double[] BBI(double[] close,
                               int m1,
                               int m2,
                               int m3,
                               int m4) {
        double[] ma1 = MA(close, m1);
        double[] ma2 = MA(close, m2);
        double[] ma3 = MA(close, m3);
        double[] ma4 = MA(close, m4);
        int len = close.length;
        double[] bbi = new double[len];
        for (int i = 0; i < len; i++) {
            bbi[i] = (ma1[i] + ma2[i] + ma3[i] + ma4[i]) / 4.0;
        }
        return bbi;
    }

    // DMI: 动向指标
    public static double[][] DMI(double[] close,
                                 double[] high,
                                 double[] low,
                                 int m1,
                                 int m2) {
        int len = close.length;
        double[] refClose = REF(close, 1);
        double[] tr = new double[len];
        double[] hd = new double[len];
        double[] ld = new double[len];
        for (int i = 0; i < len; i++) {
            tr[i] = high[i] - low[i];
            if (i > 0) {
                tr[i] = Math.max(tr[i], Math.max(Math.abs(high[i] - refClose[i]), Math.abs(low[i] - refClose[i])));
                hd[i] = Math.max(high[i] - high[i - 1], 0);
                ld[i] = Math.max(low[i - 1] - low[i], 0);
            }
        }
        double[] sumTr = SUM(tr, m1);
        double[] dmp = new double[len];
        double[] dmm = new double[len];
        for (int i = 0; i < len; i++) {
            dmp[i] = (hd[i] > ld[i] ? hd[i] : 0);
            dmm[i] = (ld[i] > hd[i] ? ld[i] : 0);
        }
        dmp = SUM(dmp, m1);
        dmm = SUM(dmm, m1);
        double[] pdi = new double[len];
        double[] mdi = new double[len];
        for (int i = 0; i < len; i++) {
            pdi[i] = dmp[i] * 100 / sumTr[i];
            mdi[i] = dmm[i] * 100 / sumTr[i];
        }
        double[] adx = MA(ABS(DIFF(pdi, 0)), m2); // approximate
        double[] refAdx = REF(adx, m2);
        double[] adxr = new double[len];
        for (int i = 0; i < len; i++) adxr[i] = (adx[i] + (Double.isNaN(refAdx[i]) ? adx[i] : refAdx[i])) / 2;
        return new double[][]{pdi, mdi, adx, adxr};
    }

    // TAQ: 唐安奇通道
    public static double[][] TAQ(double[] high,
                                 double[] low,
                                 int n) {
        double[] up = HHV(high, n);
        double[] down = LLV(low, n);
        int len = high.length;
        double[] mid = new double[len];
        for (int i = 0; i < len; i++) mid[i] = (up[i] + down[i]) / 2.0;
        return new double[][]{up, mid, down};
    }

    // KTN: 肯特纳通道
    public static double[][] KTN(double[] close,
                                 double[] high,
                                 double[] low,
                                 int n,
                                 int m) {
        int len = close.length;
        double[] tp = new double[len];
        for (int i = 0; i < len; i++) tp[i] = (high[i] + low[i] + close[i]) / 3.0;
        double[] mid = EMA(tp, n);
        double[] atr = ATR(close, high, low, m);
        double[] upper = new double[len];
        double[] lower = new double[len];
        for (int i = 0; i < len; i++) {
            upper[i] = mid[i] + 2 * atr[i];
            lower[i] = mid[i] - 2 * atr[i];
        }
        return new double[][]{upper, mid, lower};
    }

    // TRIX: 三重指数平滑平均
    public static double[][] TRIX(double[] close,
                                  int m1,
                                  int m2) {
        double[] ema1 = EMA(close, m1);
        double[] ema2 = EMA(ema1, m1);
        double[] ema3 = EMA(ema2, m1);
        int len = close.length;
        double[] trix = new double[len];
        double[] refEma3 = REF(ema3, 1);
        for (int i = 0; i < len; i++) {
            trix[i] = (ema3[i] - refEma3[i]) / refEma3[i] * 100;
        }
        double[] trma = MA(trix, m2);
        return new double[][]{trix, trma};
    }

    // VR: 容量比率
    public static double[] VR(double[] close,
                              double[] vol,
                              int m1) {
        int len = close.length;
        double[] refClose = REF(close, 1);
        double[] upVol = new double[len];
        double[] downVol = new double[len];
        for (int i = 0; i < len; i++) {
            upVol[i] = close[i] > refClose[i] ? vol[i] : 0;
            downVol[i] = close[i] <= refClose[i] ? vol[i] : 0;
        }
        double[] sumUp = SUM(upVol, m1);
        double[] sumDown = SUM(downVol, m1);
        double[] vr = new double[len];
        for (int i = 0; i < len; i++) vr[i] = sumUp[i] / sumDown[i] * 100;
        return vr;
    }

    // CR: 价格动量指标
    public static double[] CR(double[] high,
                              double[] low,
                              double[] close,
                              int n) {
        int len = close.length;
        double[] mid = REF(Arrays.stream(high).map(h -> 0).toArray(), 1); // placeholder
        // 实际: mid = REF((high+low+close),1)/3
        double[] sumHigh = new double[len];
        double[] sumLow = new double[len];
        double[] hm = new double[len];
        for (int i = 0; i < len; i++) {
            double prevSum = (high[i] + low[i] + close[i]) / 3.0;
            if (i > 0) prevSum = (high[i - 1] + low[i - 1] + close[i - 1]) / 3.0;
            sumHigh[i] = Math.max(0, high[i] - prevSum);
            sumLow[i] = Math.max(0, prevSum - low[i]);
        }
        return Arrays.stream(SUM(sumHigh, n)).map(d -> d / Arrays.stream(SUM(sumLow, n)).sum()).toArray();
    }

    // EMV: 简易波动指标
    public static double[][] EMV(double[] high,
                                 double[] low,
                                 double[] vol,
                                 int n,
                                 int m) {
        int len = high.length;
        double[] maVol = MA(vol, n);
        double[] mid = new double[len];
        for (int i = 0; i < len; i++)
            mid[i] = 100 * (high[i] + low[i] - (i > 0 ? (high[i - 1] + low[i - 1]) : (high[i] + low[i]))) / (high[i] + low[i]);
        double[] emv = new double[len];
        for (int i = 0; i < len; i++) {
            int finalI = i;
            emv[i] = MA(Arrays.stream(mid).map(d -> d * maVol[finalI] * (high[finalI] - low[finalI]) / MA(Arrays.stream(high).map(h -> low[finalI]).toArray(), n)[finalI]).toArray(), n)[i];
        }
        double[] maEmv = MA(emv, m);
        return new double[][]{emv, maEmv};
    }


    // DPO: 区间震荡线
    public static double[][] DPO(double[] close,
                                 int m1,
                                 int m2,
                                 int m3) {
        double[] maM1 = MA(close, m1);
        double[] refMa = REF(maM1, m2);
        int len = close.length;
        double[] dpo = new double[len];
        for (int i = 0; i < len; i++) {
            dpo[i] = close[i] - refMa[i];
        }
        double[] maDpo = MA(dpo, m3);
        return new double[][]{dpo, maDpo};
    }

    // BRAR: 情绪指标
    public static double[] BRAR(double[] open,
                                double[] close,
                                double[] high,
                                double[] low,
                                int m1) {

        int len = open.length;


        // 累计   HIGH - OPEN
        double[] sumHighOpen = SUM(IntStream.range(0, len).mapToDouble(i -> high[i] - open[i]).toArray(), m1);
        // 累计   OPEN - LOW
        double[] sumOpenLow = SUM(IntStream.range(0, len).mapToDouble(i -> open[i] - low[i]).toArray(), m1);

        double[] ar = new double[len];
        for (int i = 0; i < len; i++) ar[i] = sumHighOpen[i] / sumOpenLow[i] * 100;
        double[] refClose = REF(close, 1);


        double[] sumBrNum = SUM(IntStream.range(0, len).mapToDouble(i -> Math.max(0, high[i] - refClose[i])).toArray(), m1);
        double[] sumBrDen = SUM(IntStream.range(0, len).mapToDouble(i -> Math.max(0, refClose[i] - low[i])).toArray(), m1);

        double[] br = new double[len];
        for (int i = 0; i < len; i++) br[i] = sumBrNum[i] / sumBrDen[i] * 100;
        return new double[]{ /* 0: unused */, /* placeholder */};
    }

    // DFMA: 平行线差指标
    public static double[][] DFMA(double[] close,
                                  int n1,
                                  int n2,
                                  int m) {
        double[] ma1 = MA(close, n1);
        double[] ma2 = MA(close, n2);
        int len = close.length;
        double[] dif = new double[len];
        for (int i = 0; i < len; i++) dif[i] = ma1[i] - ma2[i];
        double[] difma = MA(dif, m);
        return new double[][]{dif, difma};
    }

    // MTM: 动量指标
    public static double[][] MTM(double[] close,
                                 int n,
                                 int m) {
        double[] refClose = REF(close, n);
        double[] mtm = new double[close.length];
        for (int i = 0; i < close.length; i++) mtm[i] = close[i] - refClose[i];
        double[] mtmma = MA(mtm, m);
        return new double[][]{mtm, mtmma};
    }

    // MASS: 梅斯线
    public static double[][] MASS(double[] high,
                                  double[] low,
                                  int n1,
                                  int n2,
                                  int m) {
        int len = high.length;
        double[] hl = new double[len];
        for (int i = 0; i < len; i++) hl[i] = high[i] - low[i];

        double[] maHl = MA(hl, n1);
        double[] maMaHl = MA(maHl, n1);

        // double[] mass = SUM(Arrays.stream(maHl).map((v, i) -> v / maMaHl[i]).toArray(), n2);

        // 使用 IntStream.range 通过索引来生成 ratio
        double[] ratio = IntStream.range(0, len).mapToDouble(i -> maHl[i] / maMaHl[i]).toArray();

        // 再用 SUM 计算 n2 周期的累积和
        double[] mass = SUM(ratio, n2);
        double[] maMass = MA(mass, m);

        return new double[][]{mass, maMass};
    }


    // ROC: 变动率指标
    public static double[][] ROC(double[] close,
                                 int n,
                                 int m) {
        double[] refClose = REF(close, n);
        double[] roc = new double[close.length];
        for (int i = 0; i < close.length; i++) roc[i] = 100 * (close[i] - refClose[i]) / refClose[i];
        double[] maroC = MA(roc, m);
        return new double[][]{roc, maroC};
    }


    // EXPMA: EMA 指数平均数指标
    public static double[][] EXPMA(double[] close,
                                   int n1,
                                   int n2) {
        double[] ema1 = EMA(close, n1);
        double[] ema2 = EMA(close, n2);
        return new double[][]{ema1, ema2};
    }

    // OBV: 能量潮指标
    public static double[] OBV(double[] close,
                               double[] vol) {
        int len = close.length;
        double[] refClose = REF(close, 1);
        double[] obv = new double[len];
        double cum = 0;
        for (int i = 0; i < len; i++) {
            if (close[i] > refClose[i]) cum += vol[i];
            else if (close[i] < refClose[i]) cum -= vol[i];
            obv[i] = cum / 10000.0;
        }
        return obv;
    }

    // MFI: 资金流向指标
    public static double[] MFI(double[] close,
                               double[] high,
                               double[] low,
                               double[] vol,
                               int n) {
        int len = close.length;
        double[] typ = new double[len];
        for (int i = 0; i < len; i++) typ[i] = (high[i] + low[i] + close[i]) / 3.0;
        double[] refTyp = REF(typ, 1);
        double[] rawV1 = new double[len];
        for (int i = 0; i < len; i++) rawV1[i] = typ[i] > refTyp[i] ? typ[i] * vol[i] : 0;
        double[] rawV2 = new double[len];
        for (int i = 0; i < len; i++) rawV2[i] = typ[i] < refTyp[i] ? typ[i] * vol[i] : 0;
        double[] sumV1 = SUM(rawV1, n);
        double[] sumV2 = SUM(rawV2, n);
        double[] mfi = new double[len];
        for (int i = 0; i < len; i++) mfi[i] = 100 - 100 / (1 + sumV1[i] / sumV2[i]);
        return mfi;
    }

    // ASI: 振动升降指标
    public static double[][] ASI(double[] open,
                                 double[] close,
                                 double[] high,
                                 double[] low,
                                 int m1,
                                 int m2) {
        int len = close.length;
        double[] lc = REF(close, 1);

        double[] aa = ABS(IntStream.range(0, len).mapToDouble(i -> high[i] - lc[i]).toArray());
        double[] bb = ABS(IntStream.range(0, len).mapToDouble(i -> low[i] - lc[i]).toArray());

        // 对于需要用到 i-1 的，给 i==0 一个默认值（比如 0 或 NaN），防止越界
        double[] ccArr = ABS(IntStream.range(0, len).mapToDouble(i -> i > 0 ? high[i] - low[i - 1] : 0.0).toArray());
        double[] dd = ABS(IntStream.range(0, len).mapToDouble(i -> i > 0 ? lc[i] - open[i - 1] : 0.0).toArray());


        double[] r = new double[len];
        for (int i = 0; i < len; i++) {
            if (aa[i] > bb[i] && aa[i] > ccArr[i]) r[i] = aa[i] + bb[i] / 2 + dd[i] / 4;
            else if (bb[i] > ccArr[i] && bb[i] > aa[i]) r[i] = bb[i] + aa[i] / 2 + dd[i] / 4;
            else r[i] = ccArr[i] + dd[i] / 4;
        }
        double[] x = new double[len];
        for (int i = 0; i < len; i++) x[i] = close[i] - lc[i] + (close[i] - open[i]) / 2 + lc[i] - open[i - 1];
        double[] si = new double[len];
        for (int i = 0; i < len; i++) si[i] = 16 * x[i] / r[i] * Math.max(aa[i], bb[i]);
        double[] asi = SUM(si, m1);
        double[] asit = MA(asi, m2);
        return new double[][]{asi, asit};
    }

    // XSII: 薛斯通道 II
    public static double[][] XSII(double[] close,
                                  double[] high,
                                  double[] low,
                                  int n,
                                  int m) {
        int len = close.length;
        double[] tp = new double[len];
        for (int i = 0; i < len; i++) tp[i] = (2 * close[i] + high[i] + low[i]) / 4;
        double[] aaArr = MA(tp, 5);
        double[] td1 = new double[len];
        double[] td2 = new double[len];
        for (int i = 0; i < len; i++) {
            td1[i] = aaArr[i] * n / 100.0;
            td2[i] = aaArr[i] * (200 - n) / 100.0;
        }

        double[] array = IntStream.range(0, len).mapToDouble(i -> Math.abs(tp[i] - MA(close, 20)[i])).toArray();
        double[] diff = DMA(close, array);

        double[] td3 = new double[len];
        double[] td4 = new double[len];
        for (int i = 0; i < len; i++) {
            td3[i] = (1 + m / 100.0) * diff[i];
            td4[i] = (1 - m / 100.0) * diff[i];
        }
        return new double[][]{td1, td2, td3, td4};
    }


    // 更多指标可按此模板依次实现...
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    //   #------------------ 0级：核心工具函数 --------------------------------------------
    //   def RD(N,D=3):   return np.round(N,D)        #四舍五入取3位小数
    //   def RET(S,N=1):  return np.array(S)[-N]      #返回序列倒数第N个值,默认返回最后一个
    //   def ABS(S):      return np.abs(S)            #返回N的绝对值
    //   def LN(S):       return np.log(S)            #求底是e的自然对数,
    //   def POW(S,N):    return np.power(S,N)        #求S的N次方
    //   def SQRT(S):     return np.sqrt(S)           #求S的平方根
    //   def SIN(S):      return np.sin(S)            #求S的正弦值（弧度)
    //   def COS(S):      return np.cos(S)            #求S的余弦值（弧度)
    //   def TAN(S):      return np.tan(S)            #求S的正切值（弧度)
    //   def MAX(S1,S2):  return np.maximum(S1,S2)    #序列max
    //   def MIN(S1,S2):  return np.minimum(S1,S2)    #序列min
    //   def IF(S,A,B):   return np.where(S,A,B)      #序列布尔判断 return=A  if S==True  else  B


    // ------------------ 0级：核心工具函数 -------------------------------------------------------------------------------
    public static double rd(double n,
                            int d) { // 四舍五入取d位小数
        double factor = Math.pow(10, d);
        return Math.round(n * factor) / factor;
    }

    public static double[] RD(double[] N,
                              int D) {
        double[] result = new double[N.length];
        for (int i = 0; i < N.length; i++) result[i] = rd(N[i], D);
        return result;
    }

    public static double RET(double[] S,
                             int N) {
        return S[S.length - N];
    }

    public static double[] ABS(double[] S) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) r[i] = Math.abs(S[i]);
        return r;
    }

    public static double[] LN(double[] S) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) r[i] = Math.log(S[i]);
        return r;
    }

    public static double[] POW(double[] S,
                               double N) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) r[i] = Math.pow(S[i], N);
        return r;
    }

    public static double[] SQRT(double[] S) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) r[i] = Math.sqrt(S[i]);
        return r;
    }

    public static double[] SIN(double[] S) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) r[i] = Math.sin(S[i]);
        return r;
    }

    public static double[] COS(double[] S) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) r[i] = Math.cos(S[i]);
        return r;
    }

    public static double[] TAN(double[] S) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) r[i] = Math.tan(S[i]);
        return r;
    }

    public static double[] MAX(double[] S1,
                               double[] S2) {
        double[] r = new double[S1.length];
        for (int i = 0; i < S1.length; i++) r[i] = Math.max(S1[i], S2[i]);
        return r;
    }

    public static double[] MIN(double[] S1,
                               double[] S2) {
        double[] r = new double[S1.length];
        for (int i = 0; i < S1.length; i++) r[i] = Math.min(S1[i], S2[i]);
        return r;
    }

    public static double[] IF(boolean[] S,
                              double[] A,
                              double[] B) {
        double[] r = new double[S.length];
        for (int i = 0; i < S.length; i++) r[i] = S[i] ? A[i] : B[i];
        return r;
    }


    // -----------------------------------------------------------------------------------------------------------------

    /**
     * 对数组中的每个值进行四舍五入
     *
     * @param arr 原始数值数组
     * @param d   保留的小数位数
     * @return 四舍五入后的新数组
     */
    public static double[] rdArray(double[] arr,
                                   int d) {
        double[] r = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            r[i] = rd(arr[i], d);
        }
        return r;
    }

    // -----------------------------------------------------------------------------------------------------------------


}
