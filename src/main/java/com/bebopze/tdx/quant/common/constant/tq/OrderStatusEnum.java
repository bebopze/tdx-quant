package com.bebopze.tdx.quant.common.constant.tq;

import lombok.AllArgsConstructor;


/**
 * 委托单 status 类型                            - https://help.tdx.com.cn/quant/docs/markdown/Dict.html#status类型
 *
 * @author: bebopze
 * @date: 2026/10/1
 */
@AllArgsConstructor
public enum OrderStatusEnum {


    // 名称                 类型      数值      说明
    //
    // WTSTATUS_NULL       int       0       无效单
    // WTSTATUS_NOCJ       int       1       未成交
    // WTSTATUS_PARTCJ     int       2       部分成交
    // WTSTATUS_ALLCJ      int       3       全部成交
    // WTSTATUS_BCBC       int       4       部分成交部分撤单
    // WTSTATUS_ALLCD      int       5       全部撤单


    WTSTATUS_NULL(0, "无效单"),

    WTSTATUS_NOCJ(1, "未成交"),

    WTSTATUS_PARTCJ(2, "部分成交"),

    WTSTATUS_ALLCJ(3, "全部成交"),

    WTSTATUS_BCBC(4, "部分成交部分撤单"),

    WTSTATUS_ALLCD(5, "全部撤单");


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 委托单状态
     */
    public final int status;
    /**
     * 状态说明
     */
    public final String desc;
}