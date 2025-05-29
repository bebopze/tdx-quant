package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 回测 - 交易类型
 *
 * @author: bebopze
 * @date: 2025/5/4
 */

@AllArgsConstructor
public enum BtTradeTypeEnum {


    BUY(1, "买入"),

    SELL(2, "卖出");


    /**
     * 交易类型
     */
    @Getter
    Integer tradeType;

    /**
     * 描述
     */
    @Getter
    String desc;

}
