package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import lombok.Data;


/**
 * ETF期权 - 最大可交易数量
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Data
public class OptionGetMaxTradeCountResp {


    // [
    //  {
    //    "Cf1hybm": "",
    //    "Cf2hybm": "",
    //    "Cf3hybm": "",
    //    "Cf4hybm": "",
    //    "Hybm": "10011565",
    //    "Jybk": "15",
    //    "Jysc": "1",
    //    "Qqhyzh": "",
    //    "Wtsl": "11",
    //    "Zdwtsl": "11",
    //    "Zqdm": "588000",
    //    "Zqzh": "",
    //    "Zqzhzbm": "888",
    //    "txxxbq": ""
    //  }
    // ]


    private String Cf1hybm;
    private String Cf2hybm;
    private String Cf3hybm;
    private String Cf4hybm;


    // 合约编码（10011565）
    private String Hybm;
    // 交易版块code（05-深交所；15-上交所；）
    private String Jybk;
    private String Jysc;
    private String Qqhyzh;


    //
    private int Wtsl;
    // 最大 -> 可开仓量
    private int Zdwtsl;


    // 标的证券code（ETF代码：588000）
    private String Zqdm;


    // 期权账号
    private String Zqzh;
    private String Zqzhzbm;
    private String txxxbq;


}