package com.bebopze.tdx.quant.common.domain.trade.option.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * ETF期权 - 最大可交易数量
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Data
public class OptionGetMaxTradeCountReq {


    // zqyw     400
    // ywxw     130
    // jybk     15
    //
    // hybm     10011565
    // wtjg     0.0037


    // 交易类型（400-买入开仓；401-卖出平仓；402-卖出开仓；403-买入平仓；）
    @Schema(description = "交易类型（400-买入开仓；401-卖出平仓；402-卖出开仓；403-买入平仓；）",
            example = "400", requiredMode = Schema.RequiredMode.REQUIRED)
    private int zqyw;


    // 报价方式（130-LMT限价；131-LOK限价即全成否则撤；132-FAL市价剩转限价；133-FOK市价即全成否则撤；134-FAK市价即成剩撤；）
    @Schema(description = "报价方式（130-LMT限价；131-LOK限价即全成否则撤；132-FAL市价剩转限价；133-FOK市价即全成否则撤；134-FAK市价即成剩撤；）",
            example = "130", requiredMode = Schema.RequiredMode.REQUIRED)
    private int ywxw;


    // 交易所（05-深交所；15-上交所；）
    @Schema(description = "交易所（05-深交所；15-上交所；）", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jybk;


    // 委托编号（10011565）
    @Schema(description = "委托编号（10011565）", example = "10011565", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hybm;

    // 委托价格（0.0037）
    @Schema(description = "委托价格", example = "0.0037", requiredMode = Schema.RequiredMode.REQUIRED)
    private double wtjg;

}