package com.bebopze.tdx.quant.common.domain.trade.resp;

import com.bebopze.tdx.quant.common.util.NumUtil;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.AccountConst.*;


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


    // 可取资金
    private BigDecimal accessmoney;
    // 剩余额度（ = 融资授信 - 已融资 ）
    private BigDecimal acreditavl;
    // 可用资金（担）
    private BigDecimal avalmoney;
    private BigDecimal clearCount;
    // 当日盈亏
    private BigDecimal curprofit;
    // 融券负债
    private BigDecimal dtotaldebts;
    // 融资负债
    private BigDecimal ftotaldebts;


    private List<Object> hitIPOs;


    // 可用保证金（可融）
    private BigDecimal marginavl;
    // 维持担保比例
    private BigDecimal marginrates;
    // 币种（RMB）
    private String money_type;
    // 净资产
    private BigDecimal netasset;
    // 总仓位（担保[净资产]）     2.3567123   ->   235.67%
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


    // -----------------------------------------------------------------------------------------------------------------


    // ---------------------------------------------- 融资账户（总资金标准 = 净资产 x 2   ->   为100%基准）


    /**
     * 融资/普通  账户     ->     总开关
     */
    private boolean rzAccount = RZ_ACCOUNT;


    /**
     * 剩余 可买仓位（%）  =   最大总仓位限制（%）  -   当前 总仓位（%）
     */
    private double max_buyPosPct; // = ACCOUNT__POS_PCT_LIMIT -  posratio.doubleValue() * 100;


    public double getMax_buyPosPct() {
        return ACCOUNT__POS_PCT_LIMIT - posratio.doubleValue() * 100;
    }


    /**
     * （当前 不清仓）可用总资金  =  可用保证金（可融）  +   可用资金（担）
     */
    private double max_buyCap;//  = marginavl.doubleValue() + avalmoney.doubleValue();

    public double getMax_buyCap() {
        return getMarginavl().doubleValue() + getAvalmoney().doubleValue();
    }


    /**
     * （清仓）总资金
     */
    private double max_TotalCap;

    public double getMax_TotalCap() {
        // （清仓）总资金  =  融资上限 = 净资产 x 2.1                理论上最大融资比例 125%  ->  这里取 110%（实际最大可融比例 110%~115%）
        return netasset.doubleValue() * MAX_RZ_RATE;
    }

    // -----------------------------------------------------------------------------------------------------------------


    // TODO   DEL

    // 账户   实际总资金（融+担）
    private double totalAccount__actTotalMoney;


    // 账户   实际总仓位（融+担）     0.9567123   ->   95.67%
    private double totalAccount__actTotalPosRatio;


    /**
     * 账户   实际总资金（融+担） =  净资产 x 2
     *
     * @return
     */
    @Deprecated
    public double getTotalAccount__actTotalMoney() {
        // 融+担 = 净资产 x 2
        double actTotalMoney = rzAccount ? netasset.doubleValue() : netasset.doubleValue() * MAX_RZ_RATE;

        return NumUtil.of(actTotalMoney, 5);
    }


    /**
     * 账户   实际总仓位（融+担）     0.9567123   ->   95.67%
     *
     * @return
     */
    @Deprecated
    public double getTotalAccount__actTotalPosRatio() {

        // 融+担 = 净资产 x 2


        // 总仓位（融+担）  =   总市值 / （净资产 x 2）
        double actTotalPosRatio = totalmkval.doubleValue() / getTotalAccount__actTotalMoney();

        return NumUtil.of(actTotalPosRatio, 5);
    }


}