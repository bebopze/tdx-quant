package com.bebopze.tdx.quant.common.domain.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * ETF期权 - 撤单
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Data
public class OptionTradeRevokeOrdersParam implements Serializable {


    @Schema(description = "交易所（05-深交所；15-上交所；）", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String Jybk;

    @Schema(description = "委托编号", example = "9000028708", requiredMode = Schema.RequiredMode.REQUIRED)
    private String Htxh;

}