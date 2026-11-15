package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * kline 数据范围
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
@Getter
@AllArgsConstructor
public enum KlineDataEnum {


    N_DAY(1, "N日K线"),
    HIS(2, "历史K线"),
    ALL(3, "全部K线");


    private final int type;

    private final String desc;

}