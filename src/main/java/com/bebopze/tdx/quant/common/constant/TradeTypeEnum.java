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


    // B-买入 / S-卖出
    // 6-担保买入; 7-卖出; a-融资买入;   [A-融券卖出];
    RZ_BUY(1, "B", "a", "融资买入"),

    ZY_BUY(2, "B", "6", "担保买入"),

    SELL(3, "S", "7", "担保卖出");


    /**
     * 自定义（web层 参数枚举）
     */
    @Getter
    private Integer tradeType;


    /**
     * 东方财富 - tradeType（B-买入；S-卖出；）
     */
    @Getter
    private String eastMoneyTradeType;
    /**
     * 东方财富 - 信用交易类型（6-担保买入; 7-卖出; a-融资买入;   [A-融券卖出];）
     */
    @Getter
    private String xyjylx;


    /**
     * 描述
     */
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