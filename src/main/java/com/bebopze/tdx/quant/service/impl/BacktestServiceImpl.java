package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.service.BacktestService;
import com.bebopze.tdx.quant.service.StrategyService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 回测
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
@Slf4j
@Service
public class BacktestServiceImpl implements BacktestService {


    @Autowired
    private BacktestStrategy backTestStrategy;

    @Autowired
    private StrategyService strategyService;


    @Override
    public void backtest() {

        backTestStrategy.backtest();
    }

    @Override
    public void holdingStockRule(String stockCode) {

        // 买入    - 总金额

        // 当前/S  - 总金额


        // 差价 = 当前/S - B


        // 所有个股  差价累加


        strategyService.holdingStockRule(stockCode);
    }

}
