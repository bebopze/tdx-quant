package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import lombok.Data;


/**
 * ETF期权   -   账户资金（汇总）
 *
 * @author: bebopze
 * @date: 2026/11/9
 */
@Data
public class OptionGetAccountFundsResp {


    //   {
    //     "Status": 0,
    //     "Count": 1,
    //     "Errcode": 0,
    //     "Data": [
    //       {
    //         "Ccyk": "-2890.6060",         //
    //         "Dfzj": "0.0000",             //
    //         "Dtzybzj": "0.0000",          //
    //         "Fdyk": "-2890.6060",         // 浮动盈亏
    //         "Hbdm": "0",
    //         "Khdm": "",                   // 客户代码（信用账户邮箱）
    //         "Nbjg": "5408",
    //         "Sz": "146073.3840",           // 市值
    //         "Xqsdbzj": "0.0000",
    //         "Xqzjdjje": "0.0000",
    //         "Yjfzlx": "0.0000",
    //         "Zczh": "",                     // 期权账号
    //         "Zczz": "182479.2440",          // 总资产
    //         "Zjdjje": "0.0000",
    //         "Zjdtky": "36405.8600",         //
    //         "Zjfydjje": "0.0000",
    //         "Zjjdje": "0.0000",
    //         "Zjjydjje": "0.0000",
    //         "Zjjyjdje": "0.0000",
    //         "Zjjyzcje": "0.0000",
    //         "Zjjyztje": "0.0000",
    //         "Zjkqje": "36404.8600",
    //         "Zjkyje": "36405.8600",         // 可用资金（现金资产/可取金额）
    //         "Zjsjky": "36405.8600",         //
    //         "Zjye": "36405.8600",           //
    //         "Zjzc": "36405.8600",           //
    //         "Zjzrye": "36405.8600",         //
    //         "Zjzt": "0",
    //         "Zybzj": "0.0000",
    //         "ZybzjEX": "0.0000"
    //       }
    //     ]
    //   }


    private double Ccyk;
    private double Dfzj;
    private double Dtzybzj;
    private double Fdyk;
    private double Hbdm;
    private double Khdm;

    private String Nbjg;

    private double Sz;
    private double Xqsdbzj;
    private double Xqzjdjje;
    private double Yjfzlx;
    private double Zczh;
    private double Zczz;
    private double Zjdjje;
    private double Zjdtky;
    private double Zjfydjje;
    private double Zjjdje;
    private double Zjjydjje;
    private double Zjjyjdje;
    private double Zjjyzcje;
    private double Zjjyztje;
    private double Zjkqje;
    private double Zjkyje;
    private double Zjsjky;
    private double Zjye;
    private double Zjzc;
    private double Zjzrye;
    private double Zjzt;
    private double Zybzj;
    private double ZybzjEX;


}