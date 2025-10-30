package com.bebopze.tdx.quant.common.domain.dto.backtest;

import lombok.Data;


/**
 * 回测对照
 *
 * @author: bebopze
 * @date: 2025/9/7
 */
@Data
public class BacktestCompareDTO {


    /**
     * 买入前N支
     */
    private int scoreSortN = 100;


    /**
     * 单一个股   单次最大买入  剩余资金 x 10%
     */
    private int stockPosPctLimit = 10;

}