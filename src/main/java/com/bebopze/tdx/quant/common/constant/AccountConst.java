package com.bebopze.tdx.quant.common.constant;


/**
 * 交易账户   -   全局控制 总开关
 *
 * @author: bebopze
 * @date: 2025/8/13
 */
public class AccountConst {


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 融资/普通  账户     ->     总开关
     */
    public static final boolean RZ_ACCOUNT = true;


    /**
     * 最大 融资比例     ->     总开关
     */
    public static final double MAX_RZ_RATE = RZ_ACCOUNT ? 2.1 : 1.0;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 账户 最大仓位限制（%）
     */
    public static double ACCOUNT__POS_PCT_LIMIT = RZ_ACCOUNT ? 100.0 * MAX_RZ_RATE : 100.0;


    /**
     * 单只个股 最大仓位限制（%）  ->   最大5%
     */
    public static final double STOCK__POS_PCT_LIMIT = 5.0;


}