package com.bebopze.tdx.quant.common.tdxfun;

import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
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
 * 通达信“月多”量化公式：                                     // 未来函数 bug
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
 *
 *
 *
 *
 * 最终输出：按日序列返回 boolean[]，每个交易日 i 若为“月多”则为 true，否则为 false。
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


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 整体计算“月多”信号   并返回布尔数组
     *
     * @param dailyKlines 日K 序列
     * @return
     */
    public static boolean[] computeMonthlyBull(List<KlineBar> dailyKlines) {
        int nDays = dailyKlines.size();


        // 1. 提取日线 close, high, low 数组
        double[] dayClose = new double[nDays];
        double[] dayHigh = new double[nDays];
        double[] dayLow = new double[nDays];
        LocalDate[] dayDate = new LocalDate[nDays];
        for (int i = 0; i < nDays; i++) {
            KlineBar bar = dailyKlines.get(i);
            dayDate[i] = bar.date;
            dayClose[i] = bar.close;
            dayHigh[i] = bar.high;
            dayLow[i] = bar.low;
        }


        // 2. 聚合到周线和月线
        List<KlineBar> weeklyBars = aggregateToWeekly(dailyKlines);
        int nWeeks = weeklyBars.size();
        double[] weekClose = new double[nWeeks];
        double[] weekHigh = new double[nWeeks];
        double[] weekLow = new double[nWeeks];
        LocalDate[] weekDate = new LocalDate[nWeeks];
        for (int i = 0; i < nWeeks; i++) {
            KlineBar bar = weeklyBars.get(i);
            weekDate[i] = bar.date;
            weekClose[i] = bar.close;
            weekHigh[i] = bar.high;
            weekLow[i] = bar.low;
        }


        List<KlineBar> monthlyBars = aggregateToMonthly(dailyKlines);
        int nMonths = monthlyBars.size();
        double[] monthClose = new double[nMonths];
        LocalDate[] monthDate = new LocalDate[nMonths];
        for (int i = 0; i < nMonths; i++) {
            KlineBar bar = monthlyBars.get(i);
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


        // 辅助：在 日→周/日→月 映射时，找到“最近一个 ≥ 当前日的周/月索引”
        int[] dayToWeek = new int[nDays];
        int wPointer = 0;
        for (int i = 0; i < nDays; i++) {
            LocalDate d = dayDate[i];
            while (wPointer < nWeeks - 1 && weeklyBars.get(wPointer).date.isBefore(d)) {
                wPointer++;
            }
            dayToWeek[i] = wPointer;
        }


        int[] dayToMonth = new int[nDays];
        int mPointer = 0;
        for (int i = 0; i < nDays; i++) {
            LocalDate d = dayDate[i];
            while (mPointer < nMonths - 1 && monthlyBars.get(mPointer).date.isBefore(d)) {
                mPointer++;
            }
            dayToMonth[i] = mPointer;
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
        // MACD_D -> 10日的最高值
        double[] HHV_MACD_10 = HHV(MACD_D, 10);

        // 4.3 周线 MACD
        double[][] macdWeekArr = MACD(weekClose);
        double[] MACD_W = macdWeekArr[2];

        // 4.4 月线 MACD
        double[][] macdMonthArr = MACD(monthClose);
        double[] DIF_M = macdMonthArr[0];
        double[] DEA_M = macdMonthArr[1];
        double[] MACD_M = macdMonthArr[2];
        // MACD_M -> 9个月的最高值
        double[] HHV_MACD_M__9 = HHV(MACD_M, 9);

        // 4.5 SAR 计算（周线）
        double[] SAR_W = TDX_SAR(weekHigh, weekLow);

        // 4.6 日线“均线预萌出”布尔序列
        boolean[] preBreak = 均线预萌出(dayClose);


        // -------------------------------------------------------------------------------------------------------------

        // BARSLASTCOUNT(DIF_M >= REF(DIF_M, 1 *20))               ->          本月DIF >= 上月DIF
        // BARSLASTCOUNT(DIF_M >  REF(DIF_M, 1 *20))               ->          本月DIF >  上月DIF


        // 先生成 DIF_M >= REF(DIF_M, 1*20) 的布尔数组，再调用 BARSLASTCOUNT
        double[] LAST_DIF_M = REF(DIF_M, 1);

        boolean[] DIF_M_bull1 = new boolean[DIF_M.length];
        boolean[] DIF_M_bull2 = new boolean[DIF_M.length];

        for (int i = 0; i < DIF_M.length; i++) {
            // 如果 REF(DIF_M,20)[i] 是 NaN，那么 cond 直接为 false

            // DIF_M >= REF(DIF_M, 1 *20)
            DIF_M_bull1[i] = !Double.isNaN(LAST_DIF_M[i]) && DIF_M[i] >= LAST_DIF_M[i];
            // DIF_M >  REF(DIF_M, 1 *20)
            DIF_M_bull2[i] = !Double.isNaN(LAST_DIF_M[i]) && DIF_M[i] > LAST_DIF_M[i];
        }

        // 这样就可以传入 boolean[] 了
        int[] barsLastCount__DIF_M_bull_1 = BARSLASTCOUNT(DIF_M_bull1);
        int[] barsLastCount__DIF_M_bull_2 = BARSLASTCOUNT(DIF_M_bull2);

        // -------------------------------------------------------------------------------------------------------------


        // 5. 逐日计算“月多”信号
        boolean[] monthlyBull = new boolean[nDays];


        for (int i = 0; i < nDays; i++) {
            int wi = dayToWeek[i];
            int mi = dayToMonth[i];


            // --- 5.1 计算 MACD日上0轴 ---
            // MACD_日上0轴 :   (MACD.DIF>=0 AND MACD.DEA>=0)   ||   (MACD>=0 AND MACD=HHV(MACD,10))
            boolean macdDayAbove0 = (DIF_D[i] >= 0 && DEA_D[i] >= 0)
                    || (MACD_D[i] >= 0 && MACD_D[i] == HHV_MACD_10[i]);


            // --- 5.2 计算 MACD周金叉 ---
            boolean macdWeekCross = MACD_W[wi] >= 0;


            // --- 5.3 计算 MACD月多 ---

            double absDIF_M = Math.abs(DIF_M[mi]);
            double absDEA_M = Math.abs(DEA_M[mi]);
            // MACD月_比率
            double ratio = (absDEA_M == 0 && absDIF_M == 0)
                    ? 0
                    : Math.min(absDEA_M, absDIF_M) / Math.max(absDEA_M, absDIF_M);

            // MACD月_接近金叉 :   (BARSLASTCOUNT(DIF_M >= REF(DIF_M, 1 *20))>=1.2*20   AND   MACD月_比率>=0.9 )
            //              ||   (BARSLASTCOUNT(DIF_M >  REF(DIF_M, 1 *20))>=1        AND   MACD月_比率>=0.95)
            boolean MACD_M__nearCross = (barsLastCount__DIF_M_bull_1[mi] >= (int) (1.2 * 20) && ratio >= 0.9)
                    || (barsLastCount__DIF_M_bull_2[mi] >= 1 && ratio >= 0.95);

            // MACD_月金叉 :   MACD_M>=0     ||     ( MACD_M = HH_MACD(9)   AND   MACD月_接近金叉 )
            boolean macdMonthCross = MACD_M[mi] >= 0 || (MACD_M[mi] == HHV_MACD_M__9[mi] && MACD_M__nearCross);


            //  MACD月多 :   MACD_月金叉  AND  MACD_周金叉  AND  MACD_日上0轴
            boolean macdMonthBull = macdMonthCross && macdWeekCross && macdDayAbove0;


            // --- 5.4 计算 SAR 周多 ---
            // SAR周多 :   C >= SAR.SAR#WEEK
            boolean sarWeekBull = dayClose[i] >= SAR_W[wi];


            // --- 5.5 计算最终“月多” ---
            // 月多 :   MACD月多   AND   (SAR周多 || 均线预萌出)
            monthlyBull[i] = macdMonthBull && (sarWeekBull || preBreak[i]);


            // --------------------------------------------------------------------------------- debug


//            if (dayDate[i].isEqual(LocalDate.of(2024, 9, 6))) {
//                log.debug("MACD月_比率 : {}", ratio);
//                log.debug("DIF : {} , DEA : {} , MACD : {}", DIF_D[i], DEA_D[i], MACD_D[i]);
//                log.debug("MACD_月金叉 : {} , MACD_周金叉 : {} , MACD_日上0轴 : {} , MACD月多 : {} , SAR周多 : {} , 均线预萌出 : {} , 月多 : {}",
//                          bool2Int(macdMonthCross), bool2Int(macdWeekCross), bool2Int(macdDayAbove0), bool2Int(macdMonthBull), bool2Int(sarWeekBull), bool2Int(preBreak[i]), bool2Int(monthlyBull[i]));
//
//                System.out.println();
//            }
        }


        return monthlyBull;
    }


    /**
     * 将日线数据聚合成周线数据（取 周一到周五 为同一周期）
     *
     * @param dailyKlines
     * @return
     */
    public static List<KlineBar> aggregateToWeekly(List<KlineBar> dailyKlines) {
        // 使用 ISO 周为准：一周 从周一到周日
        WeekFields wf = WeekFields.ISO;


        return dailyKlines.stream()
                          .collect(Collectors.groupingBy(
                                  // 构造 key = "YYYY-WW" 形式，保证 不同年份同周号 不会冲突
                                  bar -> bar.date.get(wf.weekBasedYear()) + "-" + bar.date.get(wf.weekOfWeekBasedYear()),
                                  LinkedHashMap::new, // 保留插入顺序
                                  Collectors.toList()
                          ))
                          .values().stream()
                          .map(group -> {

                              // 对每个周的列表，取 High 的最大、Low 的最小、Open 为首、Close 为尾
                              group.sort(Comparator.comparing(b -> b.date));

                              // 用周最后一个交易日的日期代表
                              LocalDate date = group.getLast().date;

                              double open = group.getFirst().open;
                              double close = group.getLast().close;
                              double high = group.stream().mapToDouble(b -> b.high).max().orElse(Double.NaN);
                              double low = group.stream().mapToDouble(b -> b.low).min().orElse(Double.NaN);

                              return new KlineBar(date, open, high, low, close);

                          })
                          .sorted(Comparator.comparing(w -> w.date))
                          .collect(Collectors.toList());
    }

    /**
     * 将日线数据聚合成月线数据（以自然月为周期）
     *
     * @param dailyKlines
     * @return
     */
    public static List<KlineBar> aggregateToMonthly(List<KlineBar> dailyKlines) {

        return dailyKlines.stream()
                          .collect(Collectors.groupingBy(
                                  // key  ->  yyyy-MM
                                  bar -> YearMonth.from(bar.date),
                                  LinkedHashMap::new,
                                  Collectors.toList()
                          ))
                          .values().stream()
                          .map(group -> {


                              // 对每个自然月，取 High 最大、Low 最小、Open 首、Close 尾
                              group.sort(Comparator.comparing(b -> b.date));

                              // 用当月最后一个交易日的日期代表
                              LocalDate date = group.getLast().date;

                              double open = group.getFirst().open;
                              double close = group.getLast().close;
                              double high = group.stream().mapToDouble(b -> b.high).max().orElse(Double.NaN);
                              double low = group.stream().mapToDouble(b -> b.low).min().orElse(Double.NaN);

                              return new KlineBar(date, open, high, low, close);

                          })
                          .sorted(Comparator.comparing(m -> m.date))
                          .collect(Collectors.toList());
    }


    /**
     * 日 - 周idx
     *
     * @param dailyKlines
     * @return
     */
    public static Map<LocalDate, Integer> weekIndexMap(List<KlineBar> dailyKlines) {
        Map<LocalDate, Integer> weekIndexMap = Maps.newLinkedHashMap();


        // 2. 聚合到周线和月线
        List<KlineBar> weeklyBars = aggregateToWeekly(dailyKlines);
        int nWeeks = weeklyBars.size();


        // 辅助：在 日→周/日→月 映射时，找到“最近一个 ≥ 当前日的周/月索引”
        int wPointer = 0;
        for (KlineBar klineBar : dailyKlines) {
            LocalDate d = klineBar.date;

            while (wPointer < nWeeks - 1 && weeklyBars.get(wPointer).date.isBefore(d)) {
                wPointer++;
            }
            weekIndexMap.put(d, wPointer);
        }


        return weekIndexMap;
    }


    /**
     * 日 - 月idx
     *
     * @param dailyKlines
     * @return
     */
    public static Map<LocalDate, Integer> monthIndexMap(List<KlineBar> dailyKlines) {
        Map<LocalDate, Integer> monthIndexMap = Maps.newLinkedHashMap();


        // 2. 聚合到周线和月线
        List<KlineBar> monthlyBars = aggregateToMonthly(dailyKlines);
        int nMonths = monthlyBars.size();


        // 辅助：在 日→周/日→月 映射时，找到“最近一个 ≥ 当前日的周/月索引”
        int mPointer = 0;
        for (KlineBar klineBar : dailyKlines) {
            LocalDate d = klineBar.date;

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
     * K线   数据结构
     */
    @Data
    @AllArgsConstructor
    public static class KlineBar {
        public LocalDate date;
        public double open, high, low, close;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 示例主方法：打印最后 20 天的 “月多” 信号
     */
    public static void main(String[] args) {

        String stockCode = "300059";


        List<KlineBar> dailyKlines = loadDailyBars(stockCode);
        boolean[] signals = computeMonthlyBull(dailyKlines);


        int n = signals.length;
        int limit = 300;


        System.out.printf("最后%s个交易日的月多信号：", limit);
        System.out.println();


        for (int i = n - limit; i < n; i++) {
            System.out.printf("%s -> %s%n", dailyKlines.get(i).date, signals[i] ? "月多" : "---");
        }
    }


    /**
     * 加载日线数据（示例：请改为你的数据库加载逻辑）
     */
    private static List<KlineBar> loadDailyBars(String stockCode) {

        BaseStockMapper mapper = MybatisPlusUtil.getMapper(BaseStockMapper.class);
        BaseStockDO stockDO = mapper.getByCode(stockCode);


        List<KlineDTO> klineDTOList = stockDO.getKlineDTOList();


        // 从 DB 读取
        List<KlineBar> dailyBars = klineDTOList.stream().map(e -> {
            return new KlineBar(e.getDate(), e.getOpen(), e.getHigh(), e.getLow(), e.getClose());
        }).collect(Collectors.toList());

        return dailyBars;
    }


}