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
     * 增量更新  ->  近N（>=250）日 行情Cache
     *
     * @return
     */
    BacktestCache incrUpdateInitData();

    /**
     * 指定日期范围   ->   行情Cache
     *
     * @param startDate
     * @param endDate
     * @param refresh
     * @return
     */
    BacktestCache initData(LocalDate startDate, LocalDate endDate, boolean refresh);


    void deleteCache();

    void refreshCache();

}