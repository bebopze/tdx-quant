package com.bebopze.tdx.quant.service;

/**
 * @author: bebopze
 * @date: 2025/5/20
 */
public interface StrategyService {

    void buyStockRule(String stockCode);

    void holdingStockRule(String stockCode);

    void breakSell();

}
