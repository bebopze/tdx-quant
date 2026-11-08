package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import lombok.Data;


/**
 * ETF期权   -   客户结单-资金明细
 *
 * @author: bebopze
 * @date: 2026/11/9
 */
@Data
public class OptionGetLastSettlementSheetResp {


    //   {
    //    "Status": 0,
    //    "Count": 0,
    //    "Data": [
    //      {
    //        "Add_margin_amt": "0.000",
    //        "Add_margin_notice": "",
    //        "Confirm_flag": "1",
    //        "Confirm_time": "2026-11-07 15:38:22",
    //        "Cuacct_code": "",
    //        "Currency": "0",
    //        "Cust_code": "",
    //        "Cust_name": "张三",
    //        "Dute_mkt_val": "0.000",
    //        "Exec_pay": "0.000",
    //        "Expect_pnint": "0",
    //        "Fund_avl": "36405.860",               // 期末余额
    //        "Fund_bln": "36405.860",               // 可用资金
    //        "Fund_exe_frz": "0.000",
    //        "Fund_exe_margin": "0.000",
    //        "Fund_prebln": "67115.860",             // 期初余额
    //        "Fund_used_margin": "0.000",
    //        "In_amt": "0.000",
    //        "Int_org": "5408",
    //        "Interst_income": "0.000",
    //        "Interst_income_tax": "0.000",
    //        "Jsf_fee": "148.200",
    //        "Jys_jsf_fee": "0.000",
    //        "Margin_risk_rate": "0.0000",
    //        "Opt_float_profit": "-2799.610",        //
    //        "Out_amt": "0.000",
    //        "Paylater": "0.000",
    //        "Premium_pay": "-30140.000",            // 权利金收付
    //        "Qsf_fee": "34.200",
    //        "Quota_val": "400000.000",              //
    //        "Quota_val_used": "147013.990",         //
    //        "Remit_pnint": "0.000",
    //        "Repay_paylater": "0.000",
    //        "Repay_pnint": "0.000",
    //        "Right_mkt_val": "146164.380",         // 权利仓市值
    //        "Sxf_fee": "387.600",                  // 手续费
    //        "Trd_date": "20261106",                // 交易日期
    //        "Xqgh_fee": "0.000",
    //        "Xqjsf_fee": "0.000",
    //        "Xqsxf_fee": "0.000"
    //      }
    //    ],
    //    "Errcode": 0
    //   }


    private double Add_margin_amt;
    private String Add_margin_notice;
    private int Confirm_flag;

    private String Confirm_time;
    private String Cuacct_code;
    private String Currency;
    private String Cust_code;
    private String Cust_name;
    private String Dute_mkt_val;
    private String Exec_pay;
    private int Expect_pnint;
    private String Fund_avl;
    private String Fund_bln;

    private String Fund_exe_frz;
    private String Fund_exe_margin;
    private String Fund_prebln;
    private String Fund_used_margin;
    private String In_amt;
    private String Int_org;
    private String Interst_income;
    private String Interst_income_tax;
    private String Jsf_fee;
    private String Jys_jsf_fee;
    private String Margin_risk_rate;
    private String Opt_float_profit;
    private String Out_amt;
    private String Paylater;
    private String Premium_pay;
    private String Qsf_fee;
    private String Quota_val;
    private String Quota_val_used;
    private String Remit_pnint;
    private String Repay_paylater;
    private String Repay_pnint;
    private String Right_mkt_val;
    private String Sxf_fee;
    private String Trd_date;
    private String Xqgh_fee;
    private String Xqjsf_fee;
    private String Xqsxf_fee;


}