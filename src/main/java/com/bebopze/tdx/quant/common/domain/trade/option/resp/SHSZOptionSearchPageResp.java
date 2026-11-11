package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import lombok.Data;


/**
 * ETF期权 - 可交易合约 列表检索
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Data
public class SHSZOptionSearchPageResp {


    // [
    //  {
    //    "m" : "10",
    //    "s" : "10011565",
    //    "c" : "588000C2612M01900",
    //    "n" : "科创50购12月1900",
    //    "um" : "1",
    //    "us" : "588000",
    //    "un" : "科创50ETF华夏",
    //    "ut" : "EBS",
    //    "upc" : "1.3370",
    //    "ot" : "E",
    //    "cp" : "C",
    //    "cu" : "10000",
    //    "ep" : "1.9000",
    //    "sd" : "20260507",
    //    "ed" : "20261223",
    //    "ecd" : "20261223",
    //    "dld" : "20261224",
    //    "epd" : "20261223",
    //    "uv" : "0",
    //    "tp" : "14492",
    //    "pc" : "0.0033",
    //    "ps" : "0.0033",
    //    "plt" : "N",
    //    "dpu" : "0.1581",
    //    "dpd" : "0.0001",
    //    "mu" : "968.9000",
    //    "mr1" : "12.00",
    //    "mr2" : "7.00",
    //    "rl" : "1",
    //    "lnf" : "1",
    //    "lxf" : "50",
    //    "mnf" : "1",
    //    "mxf" : "10",
    //    "ts" : "0.0001",
    //    "ad" : "20261221",
    //    "sf" : "0000E0"
    //  }
    // ]


    // 10
    private String m;


    // 合约编码（10011565）
    private String s;
    // 合约代码（588000C2612M01900）
    private String c;
    // 合约名称（科创50购12月1900）
    private String n;


    // 1
    private String um;


    // 标的证券code（588000）
    private String us;
    // 标的证券name（科创50ETF华夏）
    private String un;


    // EBS
    private String ut;

    // ETF标的证券 昨日收盘价（1.3370）
    private String upc;
    // E
    private String ot;


    // 合约类型：C-认购；P-认沽；
    private String cp;
    // 合约单位（10000）
    private String cu;
    // 行权价格（1.9000）
    private String ep;


    // 合约开始日期（20260507）
    private String sd;
    // 合约结束日期（20261223）
    private String ed;
    // 合约到期日期（20261223）
    private String ecd;
    // 到期日（20261224）
    private String dld;
    // 行权日（20261223）
    private String epd;


    // 0
    private String uv;
    // 昨日成交量（14492）
    private String tp;
    // 昨日收盘价（0.0033）
    private String pc;
    // 昨日最低价（0.0033）
    private String ps;
    // N
    private String plt;


    // 涨停价（0.1581）
    private String dpu;
    // 跌停价（0.0001）
    private String dpd;


    // 968.9000
    private String mu;
    // 12.00
    private String mr1;
    // 7.00
    private String mr2;
    // 1
    private String rl;
    // 1
    private String lnf;


    // 可开仓量（50）
    private String lxf;
    // 1
    private String mnf;
    // 10
    private String mxf;
    // 0.0001
    private String ts;
    // 20261221
    private String ad;
    // 0000E0
    private String sf;


}