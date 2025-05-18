package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 通达信 - 基础 股票池（选股公式）
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@AllArgsConstructor
public enum StockPoolBlockEnum {


    _60日新高("60RXG", 1, "60日新高"),

    RPS三线翻红("SXFH", 2, "RPS三线翻红"),


    口袋支点("KD", 3, "口袋支点"),

    月多("YD", 4, "月多"),

    大均线多头("DJXDT", 5, "大均线多头"),

    中期池子("ZQCZ", 6, "中期池子"),


    ;


    @Getter
    private String blockNewCode;

    @Getter
    private Integer status;

    @Getter
    private String desc;
}