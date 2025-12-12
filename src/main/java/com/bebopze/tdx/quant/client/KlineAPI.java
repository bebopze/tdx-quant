package com.bebopze.tdx.quant.client;

import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.INDEX_STOCK;


/**
 * 各平台   行情API     -     通用封装（差异化屏蔽）
 *
 * @author: bebopze
 * @date: 2025/10/21
 */
@Slf4j
public class KlineAPI {


    public static final Cache<String, StockSnapshotKlineDTO> klineCache = Caffeine.newBuilder()
                                                                                  .maximumSize(1_000)
                                                                                  // .expireAfterWrite(10, TimeUnit.MINUTES)
                                                                                  .expireAfterAccess(5, TimeUnit.MINUTES)
                                                                                  .recordStats()
                                                                                  // .removalListener(createStatsRemovalListener("klineCache", () -> BacktestCache.marketCache))
                                                                                  .scheduler(Scheduler.systemScheduler())
                                                                                  .build();


    public static StockSnapshotKlineDTO klineCache(String stockCode) {
        return klineCache.get(stockCode, k -> klineWait(stockCode));
    }


    public static StockSnapshotKlineDTO klineWait(String stockCode) {
        SleepUtils.randomSleep(1, 20);
        return kline(stockCode);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 获取  最新交易日
     *
     * @return
     */
    public static LocalDate lastTradeDate() {
        LocalDate date = EastMoneyTradeAPI.SHSZQuoteSnapshot(INDEX_STOCK).getRealtimequote().getDate();
        log.info("lastTradeDate     >>>     {}", date);
        return date;
    }


    // -----------------------------------------------------------------------------------------------------------------

    /**
     * 获取  多个股票/ETF  行情快照
     *
     * @param stockCodeList 股票code列表
     * @return
     */
    public static List<StockSnapshotKlineDTO> kline(Collection<String> stockCodeList) {
        return stockCodeList.parallelStream().map(stockCode -> kline(stockCode)).collect(Collectors.toList());
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 获取  指定个股/ETF  行情快照
     *
     * @param stockCode 股票code
     * @return
     */
    public static StockSnapshotKlineDTO kline(String stockCode) {
        StockSnapshotKlineDTO klineDTO = null;


        // 东方财富
        try {
            SHSZQuoteSnapshotResp resp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
            klineDTO = convert2DTO__eastmoney(resp);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }


        // 同花顺
        if (klineDTO == null) {

        }


        // 雪球
        if (klineDTO == null) {

        }


        // ...
        if (klineDTO == null) {

        }


        return klineDTO;
    }


    /**
     * 拉取  所有股票（A股） 行情快照
     *
     * @return
     */
    public static List<StockSnapshotKlineDTO> pullAllStockSnapshotKline() {
        List<StockSnapshotKlineDTO> klineDTOList = null;


//        // 东方财富
//        try {
//            klineDTOList = EastMoneyKlineAPI.pullAllStockSnapshotKline();
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }


        // 新浪财经
        if (klineDTOList == null) {
            log.info("pullAllStockSnapshotKline     >>>     使用 新浪财经 API");
            klineDTOList = SinaKlineAPI.pullAllStockSnapshotKline();
        }


        // 雪球
        if (klineDTOList == null) {
            log.info("pullAllStockSnapshotKline     >>>     使用 雪球 API");
        }


        // ...
        if (klineDTOList == null) {
            log.info("pullAllStockSnapshotKline     >>>     使用 xxx API");

        }


        return klineDTOList;
    }

    /**
     * 拉取  所有ETF  行情快照
     *
     * @return
     */
    public static List<StockSnapshotKlineDTO> pullAllETFSnapshotKline() {
        List<StockSnapshotKlineDTO> klineDTOList = null;


        // 东方财富
        try {
            klineDTOList = EastMoneyKlineAPI.pullAllETFSnapshotKline();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }


        // 新浪财经
        if (klineDTOList == null) {
            log.info("pullAllETFSnapshotKline     >>>     使用 新浪财经 API");
            klineDTOList = SinaKlineAPI.pullAllETFSnapshotKline();
        }


        // 雪球
        if (klineDTOList == null) {

            log.info("pullAllETFSnapshotKline     >>>     使用 雪球 API");
        }


        // ...
        if (klineDTOList == null) {
            log.info("pullAllETFSnapshotKline     >>>     使用 xxx API");

        }


        return klineDTOList;
    }


    /**
     * 拉取  所有股票/ETF  行情快照
     *
     * @return
     */
    public static List<StockSnapshotKlineDTO> pullAllStockETFSnapshotKline() {
        long start = System.currentTimeMillis();


        List<StockSnapshotKlineDTO> stockDTOList = pullAllStockSnapshotKline();
        // List<StockSnapshotKlineDTO> etfDTOList = pullAllETFSnapshotKline();

        // stockDTOList.addAll(etfDTOList);


        log.info("pullAllStockETFSnapshotKline     >>>     size : {} , time : {}", stockDTOList.size(), DateTimeUtil.formatNow2Hms(start));

        return stockDTOList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 行情快照（东方财富） ->  行情DTO
     *
     * @param r
     * @return
     */
    private static StockSnapshotKlineDTO convert2DTO__eastmoney(SHSZQuoteSnapshotResp r) {
        StockSnapshotKlineDTO dto = new StockSnapshotKlineDTO();


        // -------------------------------

        SHSZQuoteSnapshotResp.FivequoteDTO fivequote = r.getFivequote();
        SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequote = r.getRealtimequote();


        // -------------------------------


        dto.setDate(realtimequote.getDate());


        dto.setStockCode(r.getCode());
        dto.setStockName(r.getName());


        dto.setPrevClose(fivequote.getYesClosePrice());

        dto.setZtPrice(r.getTopprice());
        dto.setDtPrice(r.getBottomprice());


        // -------------------------------


        dto.setOpen(realtimequote.getOpen());
        dto.setHigh(realtimequote.getHigh());
        dto.setLow(realtimequote.getLow());
        dto.setClose(realtimequote.getCurrentPrice());


        dto.setVol(realtimequote.getVolume());
        dto.setAmo(realtimequote.getAmount());


        dto.setRange_pct(realtimequote.getRangePct());
        dto.setChange_pct(realtimequote.getZdf());
        dto.setChange_price(realtimequote.getZd());
        dto.setTurnover_pct(realtimequote.getTurnover());


        return dto;
    }


}