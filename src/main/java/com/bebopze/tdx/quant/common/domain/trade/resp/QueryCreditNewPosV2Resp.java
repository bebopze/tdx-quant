package com.bebopze.tdx.quant.common.domain.trade.resp;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;


/**
 * 我的持仓 - 资金持仓 汇总
 *
 * @author: bebopze
 * @date: 2025/5/10
 */
@Data
public class QueryCreditNewPosV2Resp implements Serializable {


    //  {
    //     accessmoney: "0.00",
    //     acreditavl: "946994.90",
    //     avalmoney: "0.00",
    //     clearCount: "0",
    //     curprofit: "",
    //     dtotaldebts: "0.00",
    //     ftotaldebts: "415933.89",
    //     hitIPOs: [ ],
    //     marginavl: "0.00",
    //     marginrates: "1.7333",
    //     money_type: "RMB",
    //     netasset: "304994.51",
    //     posratio: "2.3637425",
    //     profit: "-73907.38",
    //     realmarginavl: "-282909.52",
    //     realrate: "1.7329",
    //     stocks: [],
    //     totalasset: "720928.40",
    //     totalliability: "415933.89",
    //     totalmkval: "720928.40"
    //  }


    // 可用资金
    private BigDecimal accessmoney;
    // 剩余额度
    private BigDecimal acreditavl;
    // 可取资金
    private BigDecimal avalmoney;
    private BigDecimal clearCount;
    // 当日盈亏
    private BigDecimal curprofit;
    // 融券负债
    private BigDecimal dtotaldebts;
    // 融资负债
    private BigDecimal ftotaldebts;


    private List<Object> hitIPOs;


    // 可用保证金
    private BigDecimal marginavl;
    // 维持担保比例
    private BigDecimal marginrates;
    // 币种（RMB）
    private String money_type;
    // 净资产
    private BigDecimal netasset;
    // 总仓位     2.3567123   ->   235.67%
    private BigDecimal posratio;
    // 持仓盈亏
    private BigDecimal profit;
    private BigDecimal realmarginavl;
    // 实时担保比例
    private BigDecimal realrate;


    /**
     * 持仓个股 - 详情列表
     */
    private List<QueryCreditNewPosV2StockResp> stocks;


    // 总资产
    private BigDecimal totalasset;
    // 总负债
    private BigDecimal totalliability;
    // 总市值
    private BigDecimal totalmkval;
}