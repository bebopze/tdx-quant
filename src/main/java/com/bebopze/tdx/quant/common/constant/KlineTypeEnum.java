package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * K线 类型   -   东方财富/同花顺/雪球/腾讯/新浪/...
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@AllArgsConstructor
public enum KlineTypeEnum {


    M1(1, "1m", "1分钟"),
    M5(5, "5m", "5分钟"),
    M15(15, "15m", "15分钟"),
    M30(30, "30m", "30分钟"),
    M60(60, "60m", "60分钟"),
    M120(120, "120m", "120分钟"),


    DAY(101, "day", "日线"),

    WEEK(102, "week", "周线"),

    MONTH(103, "month", "月线"),

    QUARTER(104, "quarter", "季度"),

    HALF_YEAR(105, "-", "半年线"),

    YEAR(106, "year", "年线"),

    ;


    /**
     * klt
     */
    @Getter
    private Integer eastMoneyType;


    /**
     * period
     */
    @Getter
    private String xueqiuType;


    @Getter
    private String desc;

}