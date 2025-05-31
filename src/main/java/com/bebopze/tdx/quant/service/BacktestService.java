package com.bebopze.tdx.quant.service;


/**
 * @author: bebopze
 * @date: 2025/5/20
 */
public interface BacktestService {


    void backtest();

    void holdingStockRule(String stockCode);

}