package com.bebopze.tdx.quant.common.constant.tq;

import lombok.AllArgsConstructor;


/**
 * 获取 系统分类 成份股   get_stock_list                 - https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhttn72svo/mindoc-1h10qo3uj48fg.html
 *
 * @author: bebopze
 * @date: 2026/10/1
 */
@AllArgsConstructor
public enum GetStockListEnum {


    // 默认为全部A股
    //
    //    0:自选股 1:持仓股
    //    5:所有A股 6:上证指数成份股 7:上证主板 8:深证主板 9:重点指数
    //    10:所有板块指数 11:缺省行业板块 12:概念板块 13:风格板块 14:地区板块 15:缺省行业分类+概念板块 16:研究行业一级 17:研究行业二级 18:研究行业三级
    //    21:含H股 22:含可转债 23:沪深300 24:中证500 25:中证1000 26:国证2000 27:中证2000 28:中证A500
    //    30:REITs 31:ETF基金 32:可转债 33:LOF基金 34:所有可交易基金 35:所有沪深基金 36:T+0基金
    //    49:金融类企业 50:沪深A股 51:创业板 52:科创板 53:北交所 56:沪深股通 57:融资融券
    //    101:国内期货 102:港股 103:美股 110:扩展板块指数
    //	  91:ETF追踪的指数
    //	  92:国内期货主力合约


    A_STOCK(5, "全部A股"),

    BLOCK(10, "所有板块指数"),

    ETF(35, "所有沪深基金"),

    HK(102, "港股"),

    US(103, "美股");


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 市场类型
     */
    public final int market;

    /**
     * 描述
     */
    public final String desc;
}