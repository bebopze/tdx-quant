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
public enum StockPoolEnum {


    _60日新高("60RXG(ZD)", 1, "60日新高"),

    RPS三线翻红("SXFH(ZD)", 2, "RPS三线翻红"),


    口袋支点("KD(ZD)", 3, "口袋支点"),

    月多("YD(ZD)", 4, "月多"),

    大均线多头("DJXDT(ZD)", 5, "大均线多头"),

    中期池子("ZQCZ(ZD)", 6, "中期池子"),


    ;


    @Getter
    private String blockNewCode;

    @Getter
    private Integer type;

    @Getter
    private String desc;
}