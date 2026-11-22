package com.bebopze.tdx.quant.common.domain.tq;

import lombok.Data;

import java.math.BigDecimal;


/**
 * 获取 证券基本信息
 *
 * @author: bebopze
 * @date: 2026/11/22
 */
@Data
public class GetStockInfoDTO {


    //   {
    //        "ActiveCapital": "0.00",
    //        "BelongHS300": "0",
    //        "BelongHSGT": "0",
    //        "BelongHasKQZ": "0",
    //        "BelongRZRQ": "0",
    //        "DelayMin": "0",
    //        "ErrorId": "0",
    //        "Fz": [
    //          "570",
    //          "690",
    //          "780",
    //          "900",
    //          "900",
    //          "900",
    //          "900",
    //          "900"
    //        ],
    //        "HKVolBaseRate": "990057071",
    //        "HSStockKind": "10",
    //        "IsHKGP": "0",
    //        "IsQH": "0",
    //        "IsQQ": "1",
    //        "IsQuitGP": "0",
    //        "IsSTGP": "0",
    //        "J_HalfYearFlag": "0",
    //        "J_bg": "0.00",
    //        "J_ch": "0.00",
    //        "J_cqfz": "0.00",
    //        "J_gdqyb": "0.00",
    //        "J_gdrs": "0.00",
    //        "J_gdzc": "0.00",
    //        "J_hg": "0.00",
    //        "J_jly": "0.00",
    //        "J_jyl": "31.00",
    //        "J_jyxjl": "0.00",
    //        "J_jzc": "0.00",
    //        "J_ldfz": "0.00",
    //        "J_ldzc": "0.00",
    //        "J_lyze": "0.00",
    //        "J_mggjj": "46.31",
    //        "J_mgjzc": "0.00",
    //        "J_mgjzc2": "1.90",
    //        "J_mgsy": "0.00",
    //        "J_mgsy2": "0.00",
    //        "J_mgwfp": "650.00",
    //        "J_shly": "0.00",
    //        "J_start": "20260507",
    //        "J_tzsy": "0.00",
    //        "J_wfply": "1.00",
    //        "J_wxzc": "0.00",
    //        "J_yszk": "0.00",
    //        "J_yycb": "0.00",
    //        "J_yyly": "0.00",
    //        "J_yysy": "0.00",
    //        "J_zbgjj": "0.00",
    //        "J_zgb": "0.00",
    //        "J_zxjl": "0.00",
    //        "J_zzc": "0.00",
    //        "MinPrice": "0.00",
    //        "Name": "科创50购12月1900",
    //        "QHVolBaseRate": "10000",
    //        "TodayDRFlag": "0",
    //        "Unit": "1",
    //        "VolBase": "7.56",
    //        "XsFlag": "4",
    //        "blockzscode": "0",
    //        "rs_hycode_sim": "",
    //        "rs_hyname": "",
    //        "tdx_dycode": "-1",
    //        "tdx_dyname": "",
    //        "underly_code": "588000",
    //        "underly_setcode": "1"
    //   }


    // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhthaq5qmg/mindoc-1h10jj7r7jol4.html#返回数据


    private String ActiveCapital;   // 0.00
    private String BelongHS300;   // "0",
    private String BelongHSGT;   // "0",
    private String BelongHasKQZ; // "0",
    private String BelongRZRQ;   // "0",
    private String DelayMin;     // "0",
    private String ErrorId;      // "0",
    private String Fz;
    private String HKVolBaseRate;   // "990057071",
    private String HSStockKind;   // "10",
    private String IsHKGP;   // "0",
    private String IsQH;   // "0",
    private String IsQQ;   // "1",
    private String IsQuitGP;   // "0",
    private String IsSTGP;   // "0",
    private String J_HalfYearFlag;   // "0",
    private String J_bg;   // "0.00",
    private String J_ch;   // "0.00",
    private String J_cqfz;   // "0.00",
    private String J_gdqyb;   // "0.00",
    private String J_gdrs;   // "0.00",
    private String J_gdzc;   // "0.00",
    private String J_hg;   // "0.00",
    private String J_jly;   // "0.00",
    private String J_jyl;   // "31.00",
    private String J_jyxjl;   // "0.00",
    private String J_jzc;   // "0.00",
    private String J_ldfz;   // "0.00",
    private String J_ldzc;   // "0.00",
    private String J_lyze;   // "0.00",
    private String J_mggjj;   // "46.31",
    private String J_mgjzc;   // "0.00",
    private BigDecimal J_mgjzc2;   // "1.90",
    private String J_mgsy;   // "0.00",
    private String J_mgsy2;   // "0.00",
    private String J_mgwfp;   // "650.00",
    private String J_shly;   // "0.00",
    private String J_start;   // "20260507",
    private String J_tzsy;   // "0.00",
    private String J_wfply;   // "1.00",
    private String J_wxzc;   // "0.00",
    private String J_yszk;   // "0.00",
    private String J_yycb;   // "0.00",
    private String J_yyly;   // "0.00",
    private String J_yysy;   // "0.00",
    private String J_zbgjj;   // "0.00",
    private String J_zgb;   // "0.00",
    private String J_zxjl;   // "0.00",
    private String J_zzc;   // "0.00",
    private String MinPrice;   // "0.00",
    private String Name;   // "科创50购12月1900",
    private int QHVolBaseRate;   // "10000",
    private String TodayDRFlag;   // "0",
    private String Unit;   // "1",
    private String VolBase;   // "7.56",
    private String XsFlag;   // "4",
    private String blockzscode;   // "0",
    private String rs_hycode_sim;   // "",
    private String rs_hyname;   // "",
    private String tdx_dycode;   // "-1",
    private String tdx_dyname;   // "",
    private String underly_code;   // "588000",
    private String underly_setcode;   // "1"


}