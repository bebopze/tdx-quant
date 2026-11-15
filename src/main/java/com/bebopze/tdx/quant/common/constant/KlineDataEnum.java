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


    N_DAY(1, "nd", "近N日更新", "拉取近N日K线（短期）-> 用于[近N日更新]"),

    HIS(2, "his", "历史更新", "拉取历史K线（长期）-> 用于[历史更新]"),

    ALL(3, "all", "暂不支持[全部更新（近N日更新 + 历史更新）]", "拉取全部K线（his + nd）-> 用于[回测]");


    /**
     * 更新类型
     */
    private final int updateType;

    /**
     * 字段flag
     */
    private final String klineField;

    /**
     * 更新说明
     */
    private final String updateDesc;

    /**
     * 用途说明
     */
    private final String klineDesc;


}