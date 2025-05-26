package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 通达信 - B策略 股票池（自动交易 - B）
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@AllArgsConstructor
public enum BuyStrategyStockPoolEnum {


    CL_DBMR("CL-DBMR", "策略-等比买入"),


    ;


    @Getter
    private String blockNewCode;

    @Getter
    private String desc;
}