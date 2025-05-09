package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 东方财富 - 交易API 参数
 *
 * @author: bebopze
 * @date: 2025/5/4
 */

@AllArgsConstructor
public enum TradeTypeEnum {


    // B：买入 / S：卖出
    // 担保买入-6; 卖出-7; 融资买入-a;   [融券卖出-A];
    RONGZI_BUY("B", "a", "融资买入"),

    DANBAO_BUY("B", "6", "担保买入"),

    DANBAO_SELL("S", "7", "担保卖出");


    @Getter
    private String tradeType;

    @Getter
    private String xyjylx;

    @Getter
    private String desc;
}