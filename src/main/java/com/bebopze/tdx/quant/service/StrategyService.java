package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BSStrategyInfoDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * @author: bebopze
 * @date: 2025/5/20
 */
public interface StrategyService {

    BSStrategyInfoDTO bsTrade(TopBlockStrategyEnum topBlockStrategyEnum,
                              List<String> buyConList, List<String> sellConList, LocalDate tradeDate);

    BSStrategyInfoDTO bsTradeRead();

    void holdingStockRule(String stockCode);

    void breakSell();
}
