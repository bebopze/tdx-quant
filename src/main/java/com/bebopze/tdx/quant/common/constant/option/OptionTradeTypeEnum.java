package com.bebopze.tdx.quant.common.constant.option;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * ETF期权 - 交易类型
 *
 * @author: bebopze
 * @date: 2026/11/7
 */
@AllArgsConstructor
public enum OptionTradeTypeEnum {


    // 交易类型（400-买入开仓；401-卖出平仓；402-卖出开仓；403-买入平仓；）


    BUY_OPEN(400, "买入开仓"),
    SELL_CLOSE(401, "卖出平仓"),


    SELL_OPEN(402, "卖出开仓"),
    BUY_CLOSE(403, "买入平仓"),


    ;


    /**
     * 类型
     */
    @Getter
    private final int Zqyw;


    /**
     * 描述
     */
    @Getter
    private final String desc;


}