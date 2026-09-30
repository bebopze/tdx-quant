package com.bebopze.tdx.quant.common.constant.tq;

import lombok.AllArgsConstructor;


/**
 * 周期                           - https://help.tdx.com.cn/quant/docs/markdown/Dict.html#period周期入参类型
 *
 * @author: bebopze
 * @date: 2026/10/01
 */
@AllArgsConstructor
public enum PeriodEnum {


    // 名称        类型     周期      说明
    // period     str      1m       1分钟
    // period     str      5m       5分钟
    // period     str      15m      15分钟
    // period     str      30m      30分钟
    // period     str      1h       60分钟（1小时）
    // period     str      1d       1天
    // period     str      1w       1周
    // period     str      1mon     1月
    // period     str      1q       1季
    // period     str      1y       1年
    // period     str      tick     分笔


    _1M("1m", "1分钟"),
    _5M("5m", "5分钟"),
    _15M("15m", "15分钟"),
    _30M("30m", "30分钟"),
    _1H("1h", "60分钟（1小时）"),


    DAY("1d", "1天"),
    WEEK("1w", "1周"),
    MONTH("1mon", "1月"),
    QUARTER("1q", "1季"),
    YEAR("1y", "1年"),


    TICK("tick", "分笔");


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 周期
     */
    public final String period;
    /**
     * 描述
     */
    public final String desc;

}