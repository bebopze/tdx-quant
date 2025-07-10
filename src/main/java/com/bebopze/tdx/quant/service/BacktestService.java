package com.bebopze.tdx.quant.service;


import java.time.LocalDate;

/**
 * @author: bebopze
 * @date: 2025/5/20
 */
public interface BacktestService {


    void backtest(LocalDate startDate, LocalDate endDate);

    void holdingStockRule(String stockCode);

}