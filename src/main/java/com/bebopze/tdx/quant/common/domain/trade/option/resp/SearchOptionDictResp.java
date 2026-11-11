package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import lombok.Data;


/**
 * ETF期权 - 合约检索
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Data
public class SearchOptionDictResp {


    // [
    //  {
    //    "m": "10",
    //    "s": "10011565",
    //    "c": "588000C2612M01900",
    //    "n": "科创50购12月1900",
    //    "cp": "C",
    //    "ep": "1.9000",
    //    "um": "1",
    //    "us": "588000",
    //    "un": "科创50ETF华夏",
    //    "ut": "EBS"
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


    // 合约类型：C-认购；P-认沽；
    private String cp;

    // ETF行权价格（1.9000）
    private String ep;

    // 报价方式？（1）
    private String um;


    // 标的证券code（588000）
    private String us;
    // 标的证券name（科创50ETF华夏）
    private String un;


    // EBS
    private String ut;

}