package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.cache.BacktestCache;

import java.time.LocalDate;

/**
 * @author: bebopze
 * @date: 2025/7/11
 */
public interface InitDataService {


    BacktestCache initData();

    BacktestCache initData(LocalDate startDate, LocalDate endDate, boolean refresh);

}