package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;


/**
 * 股票类型：1-A股；2-ETF；
 *
 * @author: bebopze
 * @date: 2025/7/29
 */
@AllArgsConstructor
public enum StockTypeEnum {


    A_STOCK(1, "A股"),

    ETF(2, "ETF"),


    ;


    public final Integer type;

    public final String desc;


    public static String getDescByType(Integer type) {
        for (StockTypeEnum value : StockTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value.desc;
            }
        }
        return null;
    }

}
