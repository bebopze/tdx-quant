package com.bebopze.tdx.quant.common.domain.param;

import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 买入 - pre check
 *
 * @author: bebopze
 * @date: 2025/5/11
 */
@Data
public class TradeQueryCreditStkInfoWEBParam implements Serializable {


    // zqdm: 300059
    // market: SA
    // jylb: B
    // xyjylx: 6


    @Schema(description = "证券代码", example = "300059", requiredMode = Schema.RequiredMode.REQUIRED)
    private String zqdm;


    /**
     * @see StockMarketEnum
     */
    @Schema(description = "交易所（SA/HA/B）", example = "SA", requiredMode = Schema.RequiredMode.REQUIRED)
    private String market;


    @Schema(description = "交易类别（B/S）", example = "B", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jylb;


    @Schema(description = "6-担保买入; 7-卖出; a-融资买入;   [A-融券卖出];", example = "6", requiredMode = Schema.RequiredMode.REQUIRED)
    private String xyjylx;
}