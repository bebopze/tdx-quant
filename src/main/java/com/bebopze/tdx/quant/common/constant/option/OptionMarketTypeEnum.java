package com.bebopze.tdx.quant.common.constant.option;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * ETF期权 - 交易所类型
 *
 * @author: bebopze
 * @date: 2026/11/7
 */
@AllArgsConstructor
public enum OptionMarketTypeEnum {


    // 交易所（05-深交所；15-上交所；）


    SZ("05", "深交所"),

    SH("15", "上交所"),

    ;


    /**
     * 类型
     */
    @Getter
    private final String Jybk;


    /**
     * 描述
     */
    @Getter
    private final String desc;
}