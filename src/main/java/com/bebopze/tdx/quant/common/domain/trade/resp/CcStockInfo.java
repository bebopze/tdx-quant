package com.bebopze.tdx.quant.common.domain.trade.resp;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * 我的持仓 - 持仓个股 详情列表
 *
 * @author: bebopze
 * @date: 2025/5/10
 */
@Data
public class CcStockInfo implements Serializable {


    //  {
    //     costprice: "34.593",
    //     curProfitratio: 0.02030145,
    //     curprofit: 66,
    //     isApplyStg: "1",
    //     lastprice: "32.510",
    //
    //     market: "SA",
    //     mktval: "3251.00",
    //     needGyjg: "0",
    //     needJzd: "0",
    //     posratio: "0.0106592",
    //
    //     profit: "-208.30",
    //     profitratio: "-0.060214",
    //     rzDebt: "1",
    //     secuid: "0808088888",
    //     stkRzBal: "0",
    //
    //     stkZyBal: "100",
    //     stkavl: "100",
    //     stkbal: "100",
    //     stkclasses: "",
    //     stkcode: "000063",

    //     stkname: "中兴通讯",
    //     stktype: "股票",
    //     stktype_ex: "0"
    //  }


    // 成本价
    private BigDecimal costprice;
    // 当日盈亏比例（0.02030145   ->   2.030%）
    private BigDecimal curProfitratio;
    // 当日盈亏
    private BigDecimal curprofit;
    // 1
    private Integer isApplyStg;
    // 当前价
    private BigDecimal lastprice;

    // 交易所（SA / HA / B）
    private String market;
    // 最新市值
    private BigDecimal mktval;
    // -
    private Integer needGyjg;
    // -
    private Integer needJzd;
    // 个股仓位（0.0106592   ->   1.07%）
    private BigDecimal posratio;

    // 持仓盈亏
    private BigDecimal profit;
    // 持仓盈亏比例
    private BigDecimal profitratio;
    // 0/1
    private String rzDebt;
    // 股东账号（ 0808088888 ）
    private String secuid;
    // 持仓数量-融资（0 / --）
    private String stkRzBal;

    // 持仓数量-担保
    private Integer stkZyBal;
    // 可用数量
    private Integer stkavl;
    // 持仓数量
    private Integer stkbal;
    // 融资(E组 / F组 / G组、关注类)
    private String stkclasses;
    // 证券代码
    private String stkcode;

    // 证券名称
    private String stkname;
    // 证券类型 - desc（ 股票 / ETF / 创业板 / 科创板 / ... ）
    private String stktype;
    // 证券类型 - 代码（ 0-股票 / E-ETF / R-创业板 / W-科创板 / J-北交所 / ... ）
    private String stktype_ex;
}