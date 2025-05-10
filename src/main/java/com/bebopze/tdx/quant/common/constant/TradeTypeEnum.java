package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;


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
    RZ_BUY(1, "B", "a", "融资买入"),

    ZY_BUY(2, "B", "6", "担保买入"),

    SELL(3, "S", "7", "担保卖出");


    @Getter
    private Integer tradeType;

    /**
     * 东方财富 - tradeType
     */
    @Getter
    private String eastMoneyTradeType;

    @Getter
    private String xyjylx;

    @Getter
    private String desc;


    public static TradeTypeEnum getByTradeType(Integer tradeType) {
        for (TradeTypeEnum value : TradeTypeEnum.values()) {
            if (value.getTradeType().equals(tradeType)) {
                return value;
            }
        }
        return null;
    }


    public static TradeTypeEnum getByTradeType(String eastMoneyTradeType,
                                               String xyjylx) {

        for (TradeTypeEnum value : TradeTypeEnum.values()) {
            if (value.eastMoneyTradeType.equals(eastMoneyTradeType) && value.xyjylx.equals(xyjylx)) {
                return value;
            }
        }
        return null;
    }
}