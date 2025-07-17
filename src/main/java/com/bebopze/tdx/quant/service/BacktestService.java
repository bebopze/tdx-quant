package com.bebopze.tdx.quant.service;

import java.time.LocalDate;
import java.util.Map;


/**
 * @author: bebopze
 * @date: 2025/5/20
 */
public interface BacktestService {

    Long backtest(LocalDate startDate, LocalDate endDate);

    void checkBacktest(Long taskId);

    Map analysis(Long taskId);

    void holdingStockRule(String stockCode);

}
