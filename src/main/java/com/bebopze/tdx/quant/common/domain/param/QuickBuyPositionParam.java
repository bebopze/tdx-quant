package com.bebopze.tdx.quant.common.domain.param;

import com.bebopze.tdx.quant.common.util.NumUtil;
import lombok.Data;

import java.math.BigDecimal;


/**
 * 一键买入（调仓换股）
 *
 * @author: bebopze
 * @date: 2025/7/30
 */
@Data
public class QuickBuyPositionParam {


    private String stockCode;

    private String stockName;


    private double price;

    private int quantity;


    /**
     * 仓位占比
     */
    private double posPct;


    // ----------------------------------------------------


    public double getPosRate() {
        // 15%  ->  0.15
        return posPct * 0.01;
    }


    public BigDecimal getMarketValue() {
        return NumUtil.double2Decimal(price * quantity);
    }


}
