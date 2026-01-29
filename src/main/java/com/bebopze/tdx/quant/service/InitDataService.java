package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.cache.BacktestCache;

import java.time.LocalDate;


/**
 * 个股/板块  -  全量行情 Cache
 *
 * @author: bebopze
 * @date: 2025/7/11
 */
public interface InitDataService {


    /**
     * 全量更新  ->  近10年 行情Cache
     *
     * @return
     */
    BacktestCache initData();

    /**
     * 增量更新
     *
     * @return
     */
    BacktestCache incrUpdateInitData();

    /**
     * 指定日期范围   ->   行情Cache
     *
     * @param startDate
     * @param endDate
     * @param stockType 股票类型：null-全部（A股+ETF）；1-A股；2-ETF；
     * @param refresh
     * @return
     */
    BacktestCache initData(LocalDate startDate, LocalDate endDate, Integer stockType, boolean refresh);

    /**
     * 指定日期范围   ->   行情Cache
     *
     * @param startDate
     * @param endDate
     * @param stockType 股票类型：null-全部（A股+ETF）；1-A股；2-ETF；
     * @param refresh
     * @param nMonth    往前倒推  N 月（多加载 N月数据，默认：0）    // TODO：并无任何计算 需要往前倒推 N月数据（EXT_DATA [RPS250/MA250/月多/...] 指标计算   ->   有独立的 DataDTO  数据拉取实现，与 BacktestCache 毫不相干！！！）
     *                  后续考虑 彻底废弃此参数！！！
     * @return
     */
    BacktestCache initData(LocalDate startDate, LocalDate endDate, Integer stockType, boolean refresh, int nMonth);


    void clearData();


    void deleteDiskCache();

    void refreshDiskCache();
}