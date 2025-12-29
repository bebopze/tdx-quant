package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BSStrategyInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;


/**
 * @author: bebopze
 * @date: 2025/5/20
 */
public interface StrategyService {


    BSStrategyInfoDTO bsTradeMultiStrategy(TopBlockStrategyEnum topBlockStrategyEnum,
                                           Set<String> buyConSet,
                                           Set<String> sellConSet,
                                           LocalDate tradeDate);


    BSStrategyInfoDTO bsTrade(TopBlockStrategyEnum topBlockStrategyEnum,
                              Set<String> buyConSet,
                              Set<String> sellConSet,
                              LocalDate tradeDate);

    BSStrategyInfoDTO bsTradeRead();

    void bsTopStockList();


    List<String> sellCodeList();

    List<StockSnapshotKlineDTO> sellList();
}