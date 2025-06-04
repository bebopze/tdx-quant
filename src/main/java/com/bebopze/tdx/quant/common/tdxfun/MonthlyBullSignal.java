package com.bebopze.tdx.quant.common.tdxfun;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.tdxfun.TdxExtFun.*;
import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.*;


/**
 * 完整实现通达信“月多”量化公式：
 *
 * MA20 := MA(C,20);
 * MA50 := IF(MA(C,50)=DRAWNULL,0,MA(C,50));
 * MA100:= IF(MA(C,100)=DRAWNULL,0,MA(C,100));
 * MA250:= IF(MA(C,250)=DRAWNULL,0,MA(C,250));
 *
 * 均线萌出   :=  maBreakout(dayClose)  // 并调用内部函数完成
 * 均线预萌出 :=  maPreBreakout(dayClose)
 *
 * MACD日线  := daily MACD
 * MACD周线  := weekly MACD
 * MACD月线  := monthly MACD
 *
 * MACD月比率     := MIN(|DEA_M|, |DIF_M|) / MAX(|DEA_M|, |DIF_M|)
 * MACD月接近金叉 := (BARSLASTCOUNT(DIF_M >= REF(DIF_M,20)) >= 1.2*20 && MACD月比率>=0.9)
 * || (BARSLASTCOUNT(DIF_M >  REF(DIF_M,20)) >= 1      && MACD月比率>=0.95)
 * MACD月金叉     := (MACD_M >= 0) || (MACD_M == HHV(MACD_M,9) && MACD月接近金叉)
 *
 * MACD周金叉 := weekly MACD >= 0
 *
 * MACD日上0轴 := (DIF_D>=0 && DEA_D>=0) || (MACD_D >=0 && MACD_D==HHV(MACD_D,10))
 *
 * MACD月多 := MACD月金叉 && MACD周金叉 && MACD日上0轴
 *
 * SAR周多 := dayClose >= weeklySAR
 *
 * 日线“均线萌出或预萌出”序列通过前两步获得，用于 BARSSINCEN 计算。
 *
 * 月多 := MACD月多 && (SAR周多 || BARSSINCEN(“均线萌出”||“均线预萌出”,2) == 0)
 *
 * 最终输出：按日序列返回 boolean[]，每个交易日 i 若为“月多”则为 true，否则为 false。
 *
 *
 *
 *
 *
 * -
 *
 * @author: bebopze
 * @date: 2025/6/3
 */
@Slf4j
public class MonthlyBullSignal {


    /**
     * 整体计算“月多”信号并返回布尔数组
     */
    public static List<DailyBar> computeMonthlyBull2(List<DailyBar> dailyBars) {
        int nDays = dailyBars.size();


        // 1. 提取日线 close, high, low 数组
        double[] dayClose = new double[nDays];
        double[] dayHigh = new double[nDays];
        double[] dayLow = new double[nDays];
        LocalDate[] dayDate = new LocalDate[nDays];
        for (int i = 0; i < nDays; i++) {
            DailyBar bar = dailyBars.get(i);
            dayDate[i] = bar.date;
            dayClose[i] = bar.close;
            dayHigh[i] = bar.high;
            dayLow[i] = bar.low;
        }


        // 2. 聚合到周线和月线
        List<DailyBar> weeklyBars = aggregateToWeekly(dailyBars);
        int nWeeks = weeklyBars.size();
        double[] weekClose = new double[nWeeks];
        double[] weekHigh = new double[nWeeks];
        double[] weekLow = new double[nWeeks];
        LocalDate[] weekDate = new LocalDate[nWeeks];
        for (int i = 0; i < nWeeks; i++) {
            DailyBar bar = weeklyBars.get(i);
            weekDate[i] = bar.date;
            weekClose[i] = bar.close;
            weekHigh[i] = bar.high;
            weekLow[i] = bar.low;
        }


        List<DailyBar> monthlyBars = aggregateToMonthly(dailyBars);
        int nMonths = monthlyBars.size();
        double[] monthClose = new double[nMonths];
        LocalDate[] monthDate = new LocalDate[nMonths];
        for (int i = 0; i < nMonths; i++) {
            DailyBar bar = monthlyBars.get(i);
            monthDate[i] = bar.date;
            monthClose[i] = bar.close;
        }


        // 3. 构建日期到索引的映射，方便 日→周、日→月 索引对齐
        Map<LocalDate, Integer> dayIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < nDays; i++) {
            dayIndexMap.put(dayDate[i], i);
        }
        Map<LocalDate, Integer> weekIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < nWeeks; i++) {
            weekIndexMap.put(weekDate[i], i);
        }
        Map<LocalDate, Integer> monthIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < nMonths; i++) {
            monthIndexMap.put(monthDate[i], i);
        }


        // 辅助：在 日→周/日→月 映射时，找到“最近一个不大于当前日的周/月索引”
        int[] dayToWeek = new int[nDays];
        int wPointer = 0;
        for (int i = 0; i < nDays; i++) {
            LocalDate d = dayDate[i];
            // while (wPointer + 1 < nWeeks && !weeklyBars.get(wPointer + 1).date.isAfter(d)) {

            if (LocalDate.of(2012, 12, 1).isBefore(d)) {
                log.debug("----- " + d);
            }
//            LocalDate dateWeek = weeklyBars.get(wPointer).date;
//            while (wPointer < nWeeks - 1 && weeklyBars.get(wPointer).date.isBefore(d)) {
//                wPointer++;
//            }
//            dayToWeek[i] = wPointer;


            while (wPointer < nWeeks) {
                LocalDate dateWeek = weeklyBars.get(wPointer).date;

                if (!d.isAfter(dateWeek)) {
                    dayToWeek[i] = wPointer;
                    break;
                } else {
                    wPointer++;
                }
            }
        }


        for (int i = 0; i < nDays; i++) {
            int d_idx = i;
            int w_idx = dayToWeek[d_idx];


            LocalDate day = dayDate[d_idx];
            DailyBar week = weeklyBars.get(w_idx);


            System.out.printf("d_idx : %s , day : %s , d : %s       -       w_idx : %s , w : %s", d_idx, day, dailyBars.get(d_idx), w_idx, week);
            System.out.println();

            if (day.isEqual(week.date)) {
                System.out.println();
            }
        }


        int[] dayToMonth = new int[nDays];
        int mPointer = 0;
        for (int i = 0; i < nDays; i++) {
            LocalDate d = dayDate[i];
            while (mPointer < nMonths - 1 && monthlyBars.get(mPointer).date.isBefore(d)) {
                mPointer++;
            }
            dayToMonth[i] = mPointer;


//            while (mPointer < nMonths) {
//                LocalDate dateDay = d;
//                LocalDate dateMonth = monthlyBars.get(mPointer).date;
//
//                if (!dateDay.isAfter(dateMonth)) {
//                    dayToMonth[i] = mPointer;
//                    break;
//                } else {
//                    mPointer++;
//                }
//            }
        }


        System.out.println();
        System.out.println();
        System.out.println();


        for (int i = 0; i < nDays; i++) {
            int d_idx = i;
            int m_idx = dayToMonth[d_idx];


            LocalDate day = dayDate[d_idx];
            DailyBar month = monthlyBars.get(m_idx);


            System.out.printf("d_idx : %s , day : %s , d : %s       -       m_idx : %s , m : %s", d_idx, day, dailyBars.get(d_idx), m_idx, month);
            System.out.println();

            if (day.isEqual(month.date)) {
                System.out.println();
            }
        }


        // 4. 计算各种指标所需序列
        // 4.1 日线：MA10, MA20, MA50, MA100, MA200, MA250   ->   计算 周多
        // double[] MA10 = MA(dayClose, 10);
        // double[] MA20 = MA(dayClose, 20);
        // double[] MA50 = replaceNaNWithZero(MA(dayClose, 50));
        // double[] MA100 = replaceNaNWithZero(MA(dayClose, 100));
        // double[] MA200 = replaceNaNWithZero(MA(dayClose, 200));
        // double[] MA250 = replaceNaNWithZero(MA(dayClose, 250));

        // 4.2 日线 MACD
        double[][] macdDayArr = MACD(dayClose);
        double[] DIF_D = macdDayArr[0];
        double[] DEA_D = macdDayArr[1];
        double[] MACD_D = macdDayArr[2];
        double[] HHV_MACD_10 = HHV(MACD_D, 10);

        // 4.3 周线 MACD
        double[][] macdWeekArr = MACD(weekClose);
        double[] MACD_W = macdWeekArr[2];

        // 4.4 月线 MACD
        double[][] macdMonthArr = MACD(monthClose);
        double[] DIF_M = macdMonthArr[0];
        double[] DEA_M = macdMonthArr[1];
        double[] MACD_M = macdMonthArr[2];
        double[] HHV_MACD_9 = HHV(MACD_M, 9);

        // 4.5 SAR 计算（周线）
        double[] SAR_W = TDX_SAR(weekHigh, weekLow);

        // 4.6 日线“均线萌出/预萌出”布尔序列
        boolean[] breakout = 均线萌出(dayClose);
        boolean[] preBreak = 均线预萌出(dayClose);
        boolean[] orBreak = new boolean[nDays];
        for (int i = 0; i < nDays; i++) {
            orBreak[i] = breakout[i] || preBreak[i];
        }


        // -------------------------------------------------------------------------------------------------------------


        // 5. 逐日计算“月多”信号
        boolean[] monthlyBull = new boolean[nDays];
        int[] barsSinceBreak = BARSSINCEN(orBreak, 2);

        // 先生成 DIF_M >= REF(DIF_M,20) 的布尔数组，再调用 BARSLASTCOUNT
        double[] refDIF_M = REF(DIF_M, 20);
        boolean[] condDIF_GTE_REF = new boolean[DIF_M.length];
        for (int i = 0; i < DIF_M.length; i++) {
            // 如果 REF(DIF_M,20)[i] 是 NaN，那么 cond 直接为 false
            condDIF_GTE_REF[i] = !Double.isNaN(refDIF_M[i]) && DIF_M[i] >= refDIF_M[i];
        }
        // 这样就可以传入 boolean[] 了
        int[] barsLastCountMonthDIF = BARSLASTCOUNT(condDIF_GTE_REF);


        for (int i = 0; i < nDays; i++) {
            int wi = dayToWeek[i];
            int mi = dayToMonth[i];

            // --- 5.1 计算 MACD日上0轴 ---
            boolean macdDayAbove0 = (DIF_D[i] >= 0 && DEA_D[i] >= 0)
                    || (MACD_D[i] >= 0 && MACD_D[i] == HHV_MACD_10[i]);

            // --- 5.2 计算 MACD周金叉 ---
            boolean macdWeekCross = MACD_W[wi] >= 0;

            // --- 5.3 计算 MACD月度信号 ---
            double absDIF_M = Math.abs(DIF_M[mi]);
            double absDEA_M = Math.abs(DEA_M[mi]);
            double ratio = (absDEA_M == 0 && absDIF_M == 0)
                    ? 0
                    : Math.min(absDEA_M, absDIF_M) / Math.max(absDEA_M, absDIF_M);
            // “接近金叉”条件
            boolean nearGolden = (barsLastCountMonthDIF[mi] >= (int) (1.2 * 20) && ratio >= 0.9)
                    || (barsLastCountMonthDIF[mi] >= 1 && ratio >= 0.95);
            boolean macdMonthCross = MACD_M[mi] >= 0
                    || (MACD_M[mi] == HHV_MACD_9[mi] && nearGolden);

            boolean macdMonthBull = macdMonthCross && macdWeekCross && macdDayAbove0;

            // --- 5.4 计算 SAR 周多 ---
            boolean sarWeekBull = dayClose[i] >= SAR_W[wi];

            // --- 5.5 计算最终“月多” ---
            monthlyBull[i] = macdMonthBull && (sarWeekBull || barsSinceBreak[i] == 0);
        }


        // return monthlyBull;


        // ----------------------------------


        return weeklyBars;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 整体计算“月多”信号并返回布尔数组
     */
    public static boolean[] computeMonthlyBull(List<DailyBar> dailyBars) {
        int nDays = dailyBars.size();


        // 1. 提取日线 close, high, low 数组
        double[] dayClose = new double[nDays];
        double[] dayHigh = new double[nDays];
        double[] dayLow = new double[nDays];
        LocalDate[] dayDate = new LocalDate[nDays];
        for (int i = 0; i < nDays; i++) {
            DailyBar bar = dailyBars.get(i);
            dayDate[i] = bar.date;
            dayClose[i] = bar.close;
            dayHigh[i] = bar.high;
            dayLow[i] = bar.low;
        }


        // 2. 聚合到周线和月线
        List<DailyBar> weeklyBars = aggregateToWeekly(dailyBars);
        int nWeeks = weeklyBars.size();
        double[] weekClose = new double[nWeeks];
        double[] weekHigh = new double[nWeeks];
        double[] weekLow = new double[nWeeks];
        LocalDate[] weekDate = new LocalDate[nWeeks];
        for (int i = 0; i < nWeeks; i++) {
            DailyBar bar = weeklyBars.get(i);
            weekDate[i] = bar.date;
            weekClose[i] = bar.close;
            weekHigh[i] = bar.high;
            weekLow[i] = bar.low;
        }


        List<DailyBar> monthlyBars = aggregateToMonthly(dailyBars);
        int nMonths = monthlyBars.size();
        double[] monthClose = new double[nMonths];
        LocalDate[] monthDate = new LocalDate[nMonths];
        for (int i = 0; i < nMonths; i++) {
            DailyBar bar = monthlyBars.get(i);
            monthDate[i] = bar.date;
            monthClose[i] = bar.close;
        }


        // 3. 构建日期到索引的映射，方便 日→周、日→月 索引对齐
        Map<LocalDate, Integer> dayIndexMap = new HashMap<>();
        for (int i = 0; i < nDays; i++) {
            dayIndexMap.put(dayDate[i], i);
        }
        Map<LocalDate, Integer> weekIndexMap = new HashMap<>();
        for (int i = 0; i < nWeeks; i++) {
            weekIndexMap.put(weekDate[i], i);
        }
        Map<LocalDate, Integer> monthIndexMap = new HashMap<>();
        for (int i = 0; i < nMonths; i++) {
            monthIndexMap.put(monthDate[i], i);
        }

        // 辅助：在 日→周/日→月 映射时，找到“最近一个不大于当前日的周/月索引”
        int[] dayToWeek = new int[nDays];
        int wPointer = 0;
        for (int i = 0; i < nDays; i++) {
            LocalDate d = dayDate[i];
            while (wPointer < nWeeks - 1 && weeklyBars.get(wPointer).date.isBefore(d)) {
                wPointer++;
            }
            dayToWeek[i] = wPointer;


//            while (wPointer < nWeeks) {
//                LocalDate dateWeek = weeklyBars.get(wPointer).date;
//
//                if (!d.isAfter(dateWeek)) {
//                    dayToWeek[i] = wPointer;
//                    break;
//                } else {
//                    wPointer++;
//                }
//            }
        }


        int[] dayToMonth = new int[nDays];
        int mPointer = 0;
        for (int i = 0; i < nDays; i++) {
            LocalDate d = dayDate[i];
//            while (mPointer + 1 < nMonths && monthlyBars.get(mPointer + 1).date.isAfter(d)) {
            while (mPointer < nMonths - 1 && monthlyBars.get(mPointer).date.isBefore(d)) {
                mPointer++;
            }
            dayToMonth[i] = mPointer;


//            while (mPointer < nMonths) {
//                LocalDate dateMonth = monthlyBars.get(mPointer).date;
//
//                if (!d.isAfter(dateMonth)) {
//                    dayToMonth[i] = mPointer;
//                    break;
//                } else {
//                    mPointer++;
//                }
//            }
        }


        // 4. 计算各种指标所需序列
        // 4.1 日线：MA10, MA20, MA50, MA100, MA200, MA250   ->   计算 周多
        // double[] MA10 = MA(dayClose, 10);
        // double[] MA20 = MA(dayClose, 20);
        // double[] MA50 = replaceNaNWithZero(MA(dayClose, 50));
        // double[] MA100 = replaceNaNWithZero(MA(dayClose, 100));
        // double[] MA200 = replaceNaNWithZero(MA(dayClose, 200));
        // double[] MA250 = replaceNaNWithZero(MA(dayClose, 250));

        // 4.2 日线 MACD
        double[][] macdDayArr = MACD(dayClose);
        double[] DIF_D = macdDayArr[0];
        double[] DEA_D = macdDayArr[1];
        double[] MACD_D = macdDayArr[2];
        double[] HHV_MACD_10 = HHV(MACD_D, 10);

        // 4.3 周线 MACD
        double[][] macdWeekArr = MACD(weekClose);
        double[] MACD_W = macdWeekArr[2];

        // 4.4 月线 MACD
        double[][] macdMonthArr = MACD(monthClose);
        double[] DIF_M = macdMonthArr[0];
        double[] DEA_M = macdMonthArr[1];
        double[] MACD_M = macdMonthArr[2];
        double[] HHV_MACD_9 = HHV(MACD_M, 9);

        // 4.5 SAR 计算（周线）
        double[] SAR_W = TDX_SAR(weekHigh, weekLow);

        // 4.6 日线“均线萌出/预萌出”布尔序列
        boolean[] breakout = 均线萌出(dayClose);
        boolean[] preBreak = 均线预萌出(dayClose);
        boolean[] orBreak = new boolean[nDays];
        for (int i = 0; i < nDays; i++) {
            orBreak[i] = breakout[i] || preBreak[i];
        }

        // 5. 逐日计算“月多”信号
        boolean[] monthlyBull = new boolean[nDays];
        int[] barsSinceBreak = BARSSINCEN(orBreak, 2);

        // 先生成 DIF_M >= REF(DIF_M,20) 的布尔数组，再调用 BARSLASTCOUNT
        double[] refDIF_M = REF(DIF_M, 20);
        boolean[] condDIF_GTE_REF = new boolean[DIF_M.length];
        for (int i = 0; i < DIF_M.length; i++) {
            // 如果 REF(DIF_M,20)[i] 是 NaN，那么 cond 直接为 false
            condDIF_GTE_REF[i] = !Double.isNaN(refDIF_M[i]) && DIF_M[i] >= refDIF_M[i];
        }
        // 这样就可以传入 boolean[] 了
        int[] barsLastCountMonthDIF = BARSLASTCOUNT(condDIF_GTE_REF);


        for (int i = 0; i < nDays; i++) {
            int wi = dayToWeek[i];
            int mi = dayToMonth[i];

            // --- 5.1 计算 MACD日上0轴 ---
            boolean macdDayAbove0 = (DIF_D[i] >= 0 && DEA_D[i] >= 0)
                    || (MACD_D[i] >= 0 && MACD_D[i] == HHV_MACD_10[i]);

            // --- 5.2 计算 MACD周金叉 ---
            boolean macdWeekCross = MACD_W[wi] >= 0;

            // --- 5.3 计算 MACD月度信号 ---
            double absDIF_M = Math.abs(DIF_M[mi]);
            double absDEA_M = Math.abs(DEA_M[mi]);
            double ratio = (absDEA_M == 0 && absDIF_M == 0)
                    ? 0
                    : Math.min(absDEA_M, absDIF_M) / Math.max(absDEA_M, absDIF_M);
            // “接近金叉”条件
            boolean nearGolden = (barsLastCountMonthDIF[mi] >= (int) (1.2 * 20) && ratio >= 0.9)
                    || (barsLastCountMonthDIF[mi] >= 1 && ratio >= 0.95);
            boolean macdMonthCross = MACD_M[mi] >= 0
                    || (MACD_M[mi] == HHV_MACD_9[mi] && nearGolden);

            boolean macdMonthBull = macdMonthCross && macdWeekCross && macdDayAbove0;

            // --- 5.4 计算 SAR 周多 ---
            boolean sarWeekBull = dayClose[i] >= SAR_W[wi];

            // --- 5.5 计算最终“月多” ---
            monthlyBull[i] = macdMonthBull && (sarWeekBull || barsSinceBreak[i] == 0);
        }


        return monthlyBull;
    }


    /**
     * 将日线数据聚合成周线数据（取周一到周五为同一周期）
     */
    public static List<DailyBar> aggregateToWeekly(List<DailyBar> dailyBars) {
        // 使用 ISO 周为准：一周从周一到周日
        WeekFields wf = WeekFields.ISO;


        return dailyBars.stream()
                        .collect(Collectors.groupingBy(
                                bar -> bar.date.getYear() * 100 + bar.date.get(wf.weekOfWeekBasedYear()),
                                LinkedHashMap::new, // 保留插入顺序
                                Collectors.toList()
                        ))
                        .values().stream()
                        .map(group -> {

                            // 对每个周的列表，取 High 的最大、Low 的最小、Open 为首、Close 为尾
                            group.sort(Comparator.comparing(b -> b.date));

                            // 用周最后一个交易日的日期代表
                            LocalDate date = group.get(group.size() - 1).date;

                            double open = group.get(0).open;
                            double close = group.get(group.size() - 1).close;
                            double high = group.stream().mapToDouble(b -> b.high).max().orElse(Double.NaN);
                            double low = group.stream().mapToDouble(b -> b.low).min().orElse(Double.NaN);

                            return new DailyBar(date, open, high, low, close);

                        })
                        .sorted(Comparator.comparing(w -> w.date))
                        .collect(Collectors.toList());
    }

    /**
     * 将日线数据聚合成月线数据（以自然月为周期）
     */
    public static List<DailyBar> aggregateToMonthly(List<DailyBar> dailyBars) {

        return dailyBars.stream()
                        .collect(Collectors.groupingBy(
                                bar -> YearMonth.from(bar.date),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ))
                        .values().stream()
                        .map(group -> {


                            // 对每个自然月，取 High 最大、Low 最小、Open 首、Close 尾
                            group.sort(Comparator.comparing(b -> b.date));

                            // 用当月最后一个交易日的日期代表
                            LocalDate date = group.get(group.size() - 1).date;

                            double open = group.get(0).open;
                            double close = group.get(group.size() - 1).close;
                            double high = group.stream().mapToDouble(b -> b.high).max().orElse(Double.NaN);
                            double low = group.stream().mapToDouble(b -> b.low).min().orElse(Double.NaN);

                            return new DailyBar(date, open, high, low, close);

                        })
                        .sorted(Comparator.comparing(m -> m.date))
                        .collect(Collectors.toList());
    }


    public static Map<LocalDate, Integer> weekIndexMap(List<DailyBar> dailyBars) {
        Map<LocalDate, Integer> weekIndexMap = Maps.newLinkedHashMap();


        // 2. 聚合到周线和月线
        List<DailyBar> weeklyBars = aggregateToWeekly(dailyBars);
        int nWeeks = weeklyBars.size();


        int nDays = dailyBars.size();


        // 辅助：在 日→周/日→月 映射时，找到“最近一个不大于当前日的周/月索引”
        int wPointer = 0;
        for (int i = 0; i < nDays; i++) {
            LocalDate d = dailyBars.get(i).date;

//            while (wPointer < nWeeks - 1 && weeklyBars.get(wPointer).date.isBefore(d)) {
//                wPointer++;
//            }
//            weekIndexMap.put(d, wPointer);


            while (wPointer < nWeeks) {
                LocalDate dateWeek = weeklyBars.get(wPointer).date;

                if (!d.isAfter(dateWeek)) {
                    // dayToWeek[i] = wPointer;
                    weekIndexMap.put(d, wPointer);
                    break;
                } else {
                    wPointer++;
                }
            }
        }


        return weekIndexMap;
    }


    public static Map<LocalDate, Integer> monthIndexMap(List<DailyBar> dailyBars) {
        Map<LocalDate, Integer> monthIndexMap = Maps.newLinkedHashMap();


        // 2. 聚合到周线和月线
        List<DailyBar> monthlyBars = aggregateToMonthly(dailyBars);
        int nMonths = monthlyBars.size();


        int nDays = dailyBars.size();


        // 辅助：在 日→周/日→月 映射时，找到“最近一个不大于当前日的周/月索引”
        int mPointer = 0;
        for (int i = 0; i < nDays; i++) {
            LocalDate d = dailyBars.get(i).date;

            while (mPointer < nMonths - 1 && monthlyBars.get(mPointer).date.isBefore(d)) {
                mPointer++;
            }
            monthIndexMap.put(d, mPointer);
        }


        return monthIndexMap;
    }


    /**
     * 将 NaN 视作 DRAWNULL 并替换为 0
     */
    private static double[] replaceNaNWithZero(double[] arr) {
        return Arrays.stream(arr).map(v -> Double.isNaN(v) ? 0.0 : v).toArray();
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 日线 K 线数据结构
     */
    @Data
    @AllArgsConstructor
    public static class DailyBar {
        public LocalDate date;
        public double open, high, low, close;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 示例主方法：打印最后 20 天的 “月多” 信号
     */
    public static void main(String[] args) {

//        String stockCode = "300468";
        String stockCode = "300059";


        List<DailyBar> dailyBars = loadDailyBars(stockCode);
        boolean[] signals = computeMonthlyBull(dailyBars);


        int n = signals.length;
        int limit = 300;


        System.out.printf("最后%s个交易日的月多信号：", limit);
        System.out.println();


        for (int i = n - limit; i < n; i++) {
            System.out.printf("%s -> %s%n", dailyBars.get(i).date,
                              signals[i] ? "月多" : "---");
        }
    }


    /**
     * 加载日线数据（示例：请改为你的数据库加载逻辑）
     */
    private static List<DailyBar> loadDailyBars(String stockCode) {

        BaseStockMapper mapper = MybatisPlusUtil.getMapper(BaseStockMapper.class);
        BaseStockDO stockDO = mapper.getByCode(stockCode);


        List<KlineDTO> klineDTOList = ConvertStockKline.str2DTOList(stockDO.getKLineHisOriginal(), 5000);


        // 从 DB 读取
        List<DailyBar> bars = klineDTOList.stream().map(e -> {
            DailyBar dailyBar = new DailyBar(e.getDate(), e.getOpen().doubleValue(), e.getHigh().doubleValue(), e.getLow().doubleValue(), e.getClose().doubleValue());
            return dailyBar;
        }).collect(Collectors.toList());


//        String[] date = ConvertStockKline.strFieldValArr(klineDTOList, "date");
//
//        double[] close = ConvertStockKline.fieldValArr(klineDTOList, "close");
//        double[] high = ConvertStockKline.fieldValArr(klineDTOList, "high");
//        double[] low = ConvertStockKline.fieldValArr(klineDTOList, "low");
//
//
//        double[] sar = TDX_SAR(high, low);
//
//        for (int i = 0; i < date.length; i++) {
//            System.out.printf("%s     %.2f - %s       %.2f - %s     \n",
//                              date[i],
//                              sar[i], close[i] > sar[i] ? "多" : "空",
//                              sar[i], close[i] > sar[i] ? "多" : "空");
//        }
//
//
//        double[][] macd = MACD(close);
//        double[] dif = macd[0];
//        double[] dea = macd[1];
//        double[] _macd = macd[2];
//
//
//        for (int i = 100; i < date.length; i++) {
//            System.out.printf("%s     %s     %s     %s     %s     %s     %s     %s", date[i], close[i], high[i], low[i], NumUtil.double2Decimal(sar[i]), dif[i], dea[i], _macd[i]);
//            System.out.println();
//        }

        return bars;
    }


}