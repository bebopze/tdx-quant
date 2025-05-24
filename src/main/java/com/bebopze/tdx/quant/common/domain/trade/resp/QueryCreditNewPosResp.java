package com.bebopze.tdx.quant.common.domain.trade.resp;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;


/**
 * 我的持仓 - 资金持仓 汇总
 *
 * @author: bebopze
 * @date: 2025/5/11
 */
@Data
public class QueryCreditNewPosResp implements Serializable {


    //  {
    //     accessmoney: "0.00",
    //     acreditavl: "1000000.00",
    //     avalmoney: "0.00",
    //     clearCount: "0",
    //     curprofit: "",
    //     dtotaldebts: "0.00",
    //     ftotaldebts: "500000.00",
    //     hitIPOs: [ ],
    //     marginavl: "0.00",
    //     marginrates: "1.2333",
    //     money_type: "RMB",
    //     netasset: "700000.00",
    //     posratio: "1.2345678",
    //     profit: "278900.10",
    //     realmarginavl: "-123456.78",
    //     realrate: "1.2345",
    //     stocks: [],
    //     totalasset: "1200000.00",
    //     totalliability: "500000.00",
    //     totalmkval: "1200000.00"
    //  }


    // 可用资金
    private BigDecimal accessmoney;
    // 剩余额度（融资）
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
    // 融资买入-剩余保证金
    private BigDecimal realmarginavl;
    // 实时担保比例
    private BigDecimal realrate;


    /**
     * 持仓个股 - 详情列表
     */
    private List<CcStockInfo> stocks;


    // 总资产
    private BigDecimal totalasset;
    // 总负债
    private BigDecimal totalliability;
    // 总市值
    private BigDecimal totalmkval;
}