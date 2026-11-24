package com.bebopze.tdx.quant.common.domain.dto.option;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * 期权 - 标的（ETF）列表 + 该日期合约概况
 *
 * @author: bebopze
 * @date: 2026/11/24
 */
@Data
public class OptionUnderlyingDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    @Schema(description = "标的证券code", example = "510300")
    private String underlyingCode;

    @Schema(description = "标的证券name", example = "300ETF")
    private String underlyingName;

    @Schema(description = "标的ETF 全名（base_stock.name）", example = "沪深300ETF华泰柏瑞")
    private String etfName;

    @Schema(description = "该日期 存续合约数量", example = "78")
    private Integer contractCount;

    @Schema(description = "该日期 存续合约月份数量", example = "4")
    private Integer monthCount;

    @Schema(description = "最早合约开始日期")
    private LocalDate startDate;

    @Schema(description = "最晚合约到期日期")
    private LocalDate endDate;


    // ---------------------------------------------------------------------------------------------------------------
    //                                                 标的ETF 行情
    // ---------------------------------------------------------------------------------------------------------------


    @Schema(description = "标的ETF 行情日期")
    private LocalDate tradeDate;

    @Schema(description = "标的ETF 收盘价")
    private BigDecimal close;

    @Schema(description = "标的ETF 昨收价")
    private BigDecimal prevClose;

    @Schema(description = "标的ETF 涨跌额")
    private BigDecimal changePrice;

    @Schema(description = "标的ETF 涨跌幅（%）")
    private BigDecimal changePct;

    @Schema(description = "是否缺失 标的ETF 行情（base_stock 无该 ETF）", example = "false")
    private Boolean missingEtfKline;

}
