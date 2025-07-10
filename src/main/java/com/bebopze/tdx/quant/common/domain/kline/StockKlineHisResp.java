package com.bebopze.tdx.quant.common.domain.kline;


import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 个股 - 历史行情          日/周/月/季/半年/年
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@Data
public class StockKlineHisResp implements Serializable {


    //   code: "300059",
    //   market: 0,
    //   name: "东方财富",
    //   decimal: 2,
    //   dktotal: 3613,
    //   preKPrice: 0.2,
    //   prePrice: 21.72,
    //   qtMiscType: 7,
    //   version: 0,
    //
    // klines: [
    //     "2010-03-19,0.46,0.43,0.48,0.42,197373,1182393994.00,30.00,115.00,0.23,70.49",
    //     "2010-03-22,0.46,0.50,0.50,0.45,110104,693595698.00,11.63,16.28,0.07,39.32",
    //     "2010-03-23,0.49,0.51,0.52,0.48,85522,547135876.00,8.00,2.00,0.01,30.54"
    // ]


    // 证券代码
    private String code;
    private String market;
    // 证券名称
    private String name;
    // 小数位数
    private String decimal;
    // klines - 总数           （有bug，不可用       日/周/月 -> 全部返回的 日K 总数）
    private String dktotal;
    //
    private String preKPrice;
    // 昨日-收盘价
    private String prePrice;
    // xx类型（7-日线 ？？？）
    private String qtMiscType;
    //
    private String version;


    // 2025-05-06,20.70,20.99,21.08,20.61,3281601,6867607336.00,2.28,2.04,0.42,2.46

    // 2025-05-13,21.06,21.45,21.97,20.89,8455131,18181107751.03,5.18,2.98,0.62,6.33
    // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率
    private List<String> klines;

}
