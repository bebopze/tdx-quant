package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 东方财富 - K线   类型
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@AllArgsConstructor
public enum KlineTypeEnum {


    DAY(101, "日线"),

    WEEK(102, "周线"),

    MONTH(103, "月线"),

    QUAARTER(104, "季度"),

    HALF_YEAR(105, "半年线"),

    YEAR(106, "年线"),

    ;


    @Getter
    private Integer type;

    @Getter
    private String desc;


}