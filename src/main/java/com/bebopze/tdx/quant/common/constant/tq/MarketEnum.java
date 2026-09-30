package com.bebopze.tdx.quant.common.constant.tq;

import lombok.AllArgsConstructor;


/**
 * 市场类型                                     - https://help.tdx.com.cn/quant/docs/markdown/Dict.html#市场类型
 *
 * @author: bebopze
 * @date: 2026/10/1
 */
@AllArgsConstructor
public enum MarketEnum {


    //    名称       类型        数值        说明
    //
    //    .SZ        int        0         深圳交易所
    //    .SH        int        1         上海交易所
    //    .BJ        int        2         北京交易所
    //    .NQ        int        44        新三板
    //    .SHO       int        8         上海个股期权
    //    .SZO       int        9         深圳个股期权
    //    .HK        int        31        香港交易所
    //    .US        int        74        美国股票
    //    .CSI       int        62        中证指数
    //    .CNI       int        102       国证指数
    //    .HG        int        38        国内宏观指标
    //    .CFF       int        47        中金期货
    //    .CZC       int        28        郑州期货
    //    .DCE       int        29        大连期货
    //    .SHF       int        30        上海期货
    //    .GFE       int        66        广州期货
    //    .INE       int        30        上海能源
    //    .HI        int        27        港股指数
    //    .OF        int        33        开放式基金净值
    //    .CFFO      int        7         中金所期权
    //    .CZCO      int        4         郑州期货期权
    //    .DCEO      int        5         大连期货期权
    //    .SHFO      int        6         上海期货期权
    //    .GFEO      int        67        广州期货期权
    //    .QHZ       int        42        期货类指数
    //    .UZ        int        70        扩展市场板块（港股 美股 新加坡等）


    SZ(".SZ", 0, "深圳交易所"),
    SH(".SH", 1, "上海交易所"),
    BJ(".BJ", 2, "北京交易所"),


    NQ(".NQ", 44, "新三板"),


    SHO(".SHO", 8, "上海个股期权"),
    SZO(".SZO", 9, "深圳个股期权"),


    HK(".HK", 31, "香港交易所"),
    US(".US", 74, "美国股票"),


    CSI(".CSI", 62, "中证指数"),
    CNI(".CNI", 102, "国证指数"),
    HG(".HG", 38, "国内宏观指标"),


    CFF(".CFF", 47, "中金期货"),
    CZC(".CZC", 28, "郑州期货"),
    DCE(".DCE", 29, "大连期货"),
    SHF(".SHF", 30, "上海期货"),
    GFE(".GFE", 66, "广州期货"),


    INE(".INE", 30, "上海能源"),


    HI(".HI", 27, "港股指数"),


    OF(".OF", 33, "开放式基金净值"),


    CFFO(".CFFO", 7, "中金所期权"),
    CZCO(".CZCO", 4, "郑州期货期权"),
    DCEO(".DCEO", 5, "大连期货期权"),
    SHFO(".SHFO", 6, "上海期货期权"),
    GFEO(".GFEO", 67, "广州期货期权"),


    QHZ(".QHZ", 42, "期货类指数"),


    UZ(".UZ", 70, "扩展市场板块（港股 美股 新加坡等）");


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 市场标识
     */
    public final String market;

    /**
     * 市场类型
     */
    public final int type;

    /**
     * 描述
     */
    public final String desc;


}