package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import lombok.Data;


/**
 * ETF期权   -   期权持仓（列表）
 *
 * @author: bebopze
 * @date: 2026/11/9
 */
@Data
public class OptionGetGGQQPositionResp {


    //   {
    //    "Status": 0,
    //    "Count": 44,
    //    "Errcode": 0,
    //    "Data": [
    //      {
    //        "Bdbz": "非备兑合约",            // 备兑描述
    //        "Bdgfsl": "0",                 //
    //        "Bzj": "0.0000",               //
    //        "Ccfx": "权利仓",               // 持仓方向（权利仓/义务仓）
    //        "Drljkqsl": "0",
    //        "Drpcyk": "0.0000",
    //        "Dwc": "",
    //        "Fdyk": "-585.0000",           // 浮动盈亏
    //        "Hbdm": "0",
    //
    //	  	  "Hybm": "10011565",            // 合约编码
    //	  	  "Hydm": "588000C2612M01900",   // 合约代码
    //	  	  "Hyjc": "科创50购12月1900",     // 合约名称
    //
    //        "Hydjsl": "0",
    //        "Hyjdsl": "0",
    //        "Hyjydjsl": "0",
    //        "Hyjyjdsl": "0",
    //        "Hyjyzcsl": "0",
    //        "Hyjyztsl": "0",
    //        "Hykysl": "21",              // 可用数量
    //        "Hylx": "认购",               // 合约类别
    //        "Hymrcb": "3335.0000",       // 合约买入成本
    //        "Hymrcbss": "3335.0000",     //
    //        "Hypcgdsl": "0",
    //        "Hyqsdjsl": "0",
    //        "Hyqsjdsl": "0",
    //        "Hyqsztsl": "0",
    //        "Hysjcc": "21",               // 持仓数量
    //        "Hysz": "2750.0000",          // 合约市值
    //        "Hyye": "21",                 //
    //        "Hyykje": "0.0000",
    //        "Hyykjess": "0.0000",
    //        "Hyzrye": "21",                //
    //        "Jybk": "15",                  // 交易版块code（固定）
    //        "Jybkmc": "上海个股期权",        // 交易版块name（固定）
    //        "Jydy": "34081",               // 交易dy（固定）
    //        "Jysc": "上海交易所",            // 交易所（固定）
    //        "Kcjj": "151.59",              //
    //        "Ljpcyk": "0.0000",
    //        "Mm": "买",                    // 买卖方向
    //        "Nbjg": "5408",                //
    //        "Qlj": "0.0000",
    //        "Yzhhysl": "0",
    //        "Zqzhzbm": "888",               //
    //        "Zyk": "-585.0000",             // 总盈亏
    //        "bdzqdm": "588000",             // 标的证券code（ETF代码）
    //        "hydw": "10000",                //
    //        "qqxqr": "20261223",            // 行权日
    //        "syts": "39",                   // 到期天数（自然日）
    //        "xqj": "1.7000"                 // 行权价
    //      }
    //    ]
    //   }


    private String Bdbz;

    private double Bdgfsl;
    private double Bzj;

    private String Ccfx;

    private double Drljkqsl;
    private double Drpcyk;
    private double Dwc;
    private double Fdyk;


    private String Hbdm;


    // 合约编码
    private String Hybm;
    // 合约代码
    private String Hydm;
    // 合约名称
    private String Hyjc;


    private double Hydjsl;
    private double Hyjdsl;
    private double Hyjydjsl;
    private double Hyjyjdsl;
    private double Hyjyzcsl;
    private double Hyjyztsl;
    private int Hykysl;

    private String Hylx;


    // 合约买入成本
    private double Hymrcb;
    private double Hymrcbss;
    private double Hypcgdsl;
    private double Hyqsdjsl;
    private double Hyqsjdsl;
    private double Hyqsztsl;


    // 持仓数量
    private int Hysjcc;

    // 合约市值
    private double Hysz;

    private int Hyye;

    private double Hyykje;
    private double Hyykjess;

    private int Hyzrye;

    private String Jybk;
    private String Jybkmc;
    private String Jydy;
    private String Jysc;

    private double Kcjj;
    private double Khdm;
    private double Ljpcyk;

    private String Mm;
    private String Nbjg;

    private double Qlj;

    private String Qqhyzh;

    private double Yzhhysl;

    private String Zczh;
    private String Zqzh;

    private String Zqzhzbm;

    private double Zyk;


    // 标的证券code（ETF代码）
    private String bdzqdm;

    private double hydw;


    // 行权日
    private String qqxqr;
    // 到期天数（自然日）
    private int syts;
    // 行权价
    private double xqj;


    // ---------------------------------------------- 自定义 字段---------------------------------------------------------


    // 买入单价 = 合约买入成本 / 持仓数量
    private double buyPrice;
    // 当前价格 = 合约市值 / 持仓数量
    private double currentPrice;

    // 持仓比例 = 合约市值 / 账户总资金
    private double posRatio;
    // 账户总资金
    private double accountAssets;


    public double getBuyPrice() {
        buyPrice = Hymrcb / Hysjcc;
        return buyPrice;
    }

    public double getCurrentPrice() {
        currentPrice = Hysz / Hysjcc;
        return currentPrice;
    }

    public double getPosRatio() {
        posRatio = Hysz / accountAssets;
        return posRatio;
    }

}