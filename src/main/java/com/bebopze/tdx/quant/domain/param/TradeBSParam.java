package com.bebopze.tdx.quant.domain.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * @author: bebopze
 * @date: 2025/5/10
 */
@Data
public class TradeBSParam implements Serializable {


    // stockCode: 588050       股票代码
    // stockName: 科创ETF       股票名称
    // price: 1.031            价格
    // amount: 100             数量
    // tradeType: B            B：买入 / S：卖出
    // xyjylx: 6               6-担保买入; 7-卖出; a-融资买入;   [A-融券卖出];
    // market: HA              市场（HA：沪A / SA：深A / B：北交所）


    @Schema(description = "证券代码", example = "300750", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stockCode;

    @Schema(description = "证券名称", example = "宁德时代", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stockName;

    @Schema(description = "价格", example = "12.34", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Schema(description = "数量", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer amount;


    @Schema(description = "交易类型：1-融资买入；2-担保买入；3-卖出", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer tradeType;
}
