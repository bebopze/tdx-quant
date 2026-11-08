package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import lombok.Data;


/**
 * ETF期权   -   委托单（列表）
 *
 * @author: bebopze
 * @date: 2026/11/7
 */
@Data
public class OptionGetHisOrderResp {


    //   {
    //	    "Status": 0,
    //	    "Count": 0,
    //	    "Errcode": 0,
    //	    "Data": [
    //	      {
    //	  	    "Bdqwtdjsl": "0",
    //	  	    "Bdqwtjdsl": "0",
    //	  	    "Bdqycdsl": "0",
    //	  	    "Bdzqdm": "588000",
    //	  	    "Bdzqlb": "ETF基金",
    //	  	    "Bdzqmc": "科创50",            // 标的证券名称
    //	  	    "Bs": "非备兑",                // 备兑描述
    //	  	    "Cdbz": "F",
    //	  	    "Cfehybm": "",
    //	  	    "Cfsahybm": "",
    //	  	    "Cfsihybm": "",
    //	  	    "Cfyhybm": "",
    //	  	    "Ddsylx": "1",
    //	  	    "Dwc": "1",
    //	  	    "Hbdm": "0",
    //	  	    "Htxh": "9000012345",          // 合同编号
    //	  	    "Hybm": "10011479",            // 合约编号
    //	  	    "Hydm": "588000C2612M01350",   // 合约交易代码
    //	  	    "Hyjc": "科创50购12月1350",     // 合约名称
    //	  	    "Hylx": "认购",
    //	  	    "Jlxh": "1",
    //	  	    "Jybk": "15",                // 交易版块code
    //	  	    "Jybkmc": "上海个股期权",      // 交易版块名称
    //	  	    "Jydy": "34081",             // 交易
    //	  	    "Jyrq": "20261029",          // 交易日期
    //	  	    "Jysc": "上海交易所",          // 交易所
    //	  	    "Khdm": "",                  // 客户代码（信用账户邮箱）
    //	  	    "Kp": "开仓",                // 开平
    //	  	    "Mm": "买入开仓",             // 操作
    //	  	    "Nbjg": "5408",              //
    //	  	    "Qqhyzh": "",                // 期权合约账户（沪/深市衍生品合约账号 = Zqzh + Zqzhzbm）
    //	  	    "Sbfhxx": "",
    //	  	    "Sbsj": "102816",            // 时间
    //	  	    "Sbsl": "1",                 // 数量
    //	  	    "Wtdjje": "717.000",         // 委托金额 + 佣金（5元/张）
    //	  	    "Wtjdje": "0.000",
    //	  	    "Wtje": "712.000",           // 委托金额
    //	  	    "Wtjg": "0.0712",            // 委托价格
    //	  	    "Wtph": "1",
    //	  	    "Wtrq": "20261029",          // 委托日期
    //	  	    "Wtsj": "10:28:16",          // 委托时间
    //	  	    "Wtsl": "1",                 // 委托数量
    //	  	    "Wtyxbz": "1",
    //	  	    "Wtzt": "已成",               // 委托状态
    //	  	    "Ycdbz": "0",
    //	  	    "Ycdsl": "0",
    //	  	    "Ycjje": "712.000",          // 成交金额
    //	  	    "Ycjsl": "1",                // 成交数量
    //	  	    "Zczh": "",                  // 期权账号
    //	  	    "Zhbm": "",
    //	  	    "Zhcldm": "",
    //	  	    "Zqyw": "买入开仓|非备兑|开仓| ",     // 证券业务
    //	  	    "Zqywcode": "400",                 // 证券业务code
    //	  	    "Zqywxw": "限价GFD",                // 证券业务xw
    //	  	    "Zqzh": "A123456",                 // 期权合约账户
    //	  	    "Zqzhzbm": "888"                   // 证券账户子编码
    //         }
    //      ]
    //   }


    private double Bdqwtdjsl;
    private double Bdqwtjdsl;
    private double Bdqycdsl;

    private String Bdzqdm;
    private String Bdzqlb;
    private String Bdzqmc;
    private String Bs;
    private String Cdbz;

    private double Cfehybm;
    private double Cfsahybm;
    private double Cfsihybm;
    private double Cfyhybm;
    private double Ddsylx;
    private double Dwc;

    private String Hbdm;
    private String Htxh;
    private String Hybm;
    private String Hydm;
    private String Hyjc;

    private String Hylx;
    private String Jlxh;
    private String Jybk;
    private String Jybkmc;
    private String Jydy;
    private String Jyrq;
    private String Jysc;
    private String Khdm;

    private double Kp;

    private String Mm;
    private String Nbjg;
    private String Qqhyzh;
    private String Sbfhxx;
    private String Sbsj;

    private int Sbsl;

    private double Wtdjje;
    private double Wtjdje;
    private double Wtje;
    private double Wtjg;

    private String Wtph;
    private String Wtrq;
    private String Wtsj;

    private int Wtsl;

    private String Wtyxbz;
    private String Wtzt;

    private String Ycdbz;
    private String Ycdsl;

    private double Ycjje;
    private int Ycjsl;

    private String Zczh;
    private String Zhbm;
    private String Zhcldm;
    private String Zqyw;
    private String Zqywcode;
    private String Zqywxw;
    private String Zqzh;
    private String Zqzhzbm;


}