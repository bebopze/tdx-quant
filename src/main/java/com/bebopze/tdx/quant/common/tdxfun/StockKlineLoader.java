package com.bebopze.tdx.quant.common.tdxfun;

import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bebopze.tdx.quant.parser.tdxdata.LdayParser.KLINE_START_DATE;


/**
 * 股票K线加载器
 *
 * @author: bebopze
 * @date: 2025/12/8
 */
@Slf4j
public class StockKlineLoader {


    private static final ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    /**
     * 优化  ->  idx边界定位[二分查找]  +  subList （避免 全量数据 遍历）
     *
     * @param startDate
     * @param endDate
     * @param stockDOList
     * @param nMonth      往前倒推  N 月（多加载 N月数据，默认：0）    // TODO：并无任何计算 需要往前倒推 N月数据（EXT_DATA [RPS250/MA250/月多/...] 指标计算   ->   有独立的 DataDTO  数据拉取实现，与 BacktestCache 毫不相干！！！）
     *                    后续考虑 彻底废弃此参数！！！
     */
    public static void loadAllStockKline(LocalDate startDate,
                                         LocalDate endDate,
                                         List<BaseStockDO> stockDOList,
                                         int nMonth) {


        if (!startDate.isAfter(KLINE_START_DATE) && !endDate.isBefore(LocalDate.now())) {
            log.info("loadAllStockKline - dateLine 截取（内存爆炸） =>  包含 全部日期 -> 无需截取     >>>     startDate : {}, endDate : {}", startDate, endDate);
            return;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 行情起点（往前倒推 250个交易日 -> 1年[365个自然日]）    // TODO：并无任何计算 需要往前倒推 N月数据（EXT_DATA [RPS250/MA250/月多/...] 指标计算   ->   有独立的 DataDTO  数据拉取实现，与 BacktestCache 毫不相干！！！）
        LocalDate dateLine_start = startDate.minusMonths(nMonth).minusDays(10);
        LocalDate dateLine_end = endDate;

        log.info("loadAllStockKline - dateLine 截取（内存爆炸）    >>>     startDate : {}, endDate : {}", startDate, endDate);


        // -------------------------------------------------------------------------------------------------------------


        long start = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);


        List<Future<?>> futures = new ArrayList<>();

        for (BaseStockDO e : stockDOList) {
            futures.add(pool.submit(() -> processOneStock(e, dateLine_start, dateLine_end)));
        }


        // 等全部线程执行完成
        futures.forEach(f -> {
            try {
                long start_2 = System.currentTimeMillis();
                f.get();
                log.info("loadAllStockKline - dateLine 截取（内存爆炸）    >>>     count : {} , [{} ~ {}] , stockTime : {} , totalTime : {}",
                         count.incrementAndGet(), /*e.getCode(), e.getName(),*/ dateLine_start, dateLine_end, DateTimeUtil.formatNow2Hms(start_2), DateTimeUtil.formatNow2Hms(start));
            } catch (Exception ex) {
                log.error("loadAllStockKline - dateLine 截取（内存爆炸）    >>>     error: {}", ex.getMessage(), ex);
            }
        });


        log.info("loadAllStockKline - dateLine 截取（内存爆炸）    >>>     count : {} , startDate : {}, endDate : {} , totalTime : {}",
                 count.get(), startDate, endDate, DateTimeUtil.formatNow2Hms(start));
    }


    /**
     * 单支股票优化处理
     **/
    private static void processOneStock(BaseStockDO e, LocalDate start, LocalDate end) {

        // ---------------------- KLINE 部分（使用二分界定区间） ----------------------
        List<KlineDTO> klineList = e.getKlineDTOList();

        int left = lowerBoundForKline(klineList, start);
        int right = upperBoundForKline(klineList, end);

        if (left > right) {
            e.setKlineDTOList(Collections.emptyList());
            e.setExtDataDTOList(Collections.emptyList());
            return;
        }

        // 取 subList（注意：不会复制数据 O(1)）
        List<KlineDTO> subKline = new ArrayList<>();
        for (int i = left; i <= right; i++) {
            KlineDTO k = klineList.get(i);
            if (k.getClose() > 0) { // 保留正价格
                subKline.add(k);
            }
        }

        e.setKlineDTOList(subKline);

        if (subKline.isEmpty()) {
            e.setExtDataDTOList(Collections.emptyList());
            return;
        }

        // ---------------------- EXT DATA 部分（二分区间 + 双指针对齐） ----------------------
        List<ExtDataDTO> extList = e.getExtDataDTOList();
        if (extList == null || extList.isEmpty()) {
            e.setExtDataDTOList(Collections.emptyList());
            return;
        }

        int l2 = lowerBoundForExt(extList, start);
        int r2 = upperBoundForExt(extList, end);

        if (l2 > r2) {
            e.setExtDataDTOList(Collections.emptyList());
            return;
        }

        // 取 sub
        List<ExtDataDTO> subExt = extList.subList(l2, r2 + 1);

        // ---------------------- 双指针对齐（保证 ext 与 kline 日期完全一致） ----------------------
        List<ExtDataDTO> alignedExt = new ArrayList<>(subExt.size());
        int i = 0, j = 0;

        while (i < subKline.size() && j < subExt.size()) {
            LocalDate d1 = subKline.get(i).getDate();
            LocalDate d2 = subExt.get(j).getDate();

            if (d1.isEqual(d2)) {
                alignedExt.add(subExt.get(j));
                i++;
                j++;
            } else if (d1.isBefore(d2)) {
                i++;
            } else {
                j++;
            }
        }

        e.setExtDataDTOList(alignedExt);
    }


// ============================== 二分查找函数 ==============================

    private static int lowerBoundForKline(List<KlineDTO> list, LocalDate target) {
        int l = 0, r = list.size() - 1, ans = list.size();
        while (l <= r) {
            int mid = (l + r) >>> 1;
            if (!list.get(mid).getDate().isBefore(target)) {
                ans = mid;
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }
        return ans;
    }

    private static int upperBoundForKline(List<KlineDTO> list, LocalDate target) {
        int l = 0, r = list.size() - 1, ans = -1;
        while (l <= r) {
            int mid = (l + r) >>> 1;
            if (!list.get(mid).getDate().isAfter(target)) {
                ans = mid;
                l = mid + 1;
            } else {
                r = mid - 1;
            }
        }
        return ans;
    }


    private static int lowerBoundForExt(List<ExtDataDTO> list, LocalDate target) {
        int l = 0, r = list.size() - 1, ans = list.size();
        while (l <= r) {
            int mid = (l + r) >>> 1;
            if (!list.get(mid).getDate().isBefore(target)) {
                ans = mid;
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }
        return ans;
    }

    private static int upperBoundForExt(List<ExtDataDTO> list, LocalDate target) {
        int l = 0, r = list.size() - 1, ans = -1;
        while (l <= r) {
            int mid = (l + r) >>> 1;
            if (!list.get(mid).getDate().isAfter(target)) {
                ans = mid;
                l = mid + 1;
            } else {
                r = mid - 1;
            }
        }
        return ans;
    }


}