package com.bebopze.tdx.quant.common.domain.dto.option;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/**
 * 期权 T型报价 - 合约月份分组（合约月份 - 认购列表 / 认沽列表）
 *
 * @author: bebopze
 * @date: 2026/11/24
 */
@Data
public class MonthOptionDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    @Schema(description = "分组唯一键（underlyingCode-contractMonth）", example = "510050-202612")
    private String key;

    @Schema(description = "标的证券code", example = "510050")
    private String underlyingCode;

    @Schema(description = "标的证券name", example = "50ETF")
    private String underlyingName;

    @Schema(description = "合约月份（yyyyMM）", example = "202612")
    private Integer contractMonth;

    @Schema(description = "合约月份（yyyy-MM）", example = "2026-12")
    private String monthLabel;

    @Schema(description = "到期日")
    private LocalDate expiryDate;

    @Schema(description = "剩余自然日（相对所选日期）", example = "26")
    private Integer daysToExpiry;


    // ---------------------------------------------------------------------------------------------------------------
    //                                            所选日期 / 行情日
    // ---------------------------------------------------------------------------------------------------------------


    @Schema(description = "所选日期")
    private LocalDate date;

    @Schema(description = "实际行情日期（该月份内 最新一根K线日期；无行情时为 null）")
    private LocalDate quoteDate;

    @Schema(description = "该月份在所选日期当日是否有行情", example = "true")
    private Boolean hasData;


    // ---------------------------------------------------------------------------------------------------------------
    //                                                 标的行情
    // ---------------------------------------------------------------------------------------------------------------


    @Schema(description = "标的ETF 收盘价（所选日期当日/之前最近一根）")
    private BigDecimal underlyingClose;

    @Schema(description = "标的ETF 涨跌幅（%）")
    private BigDecimal underlyingChangePct;

    @Schema(description = "标的ETF 行情日期")
    private LocalDate underlyingTradeDate;


    // ---------------------------------------------------------------------------------------------------------------
    //                                                 分组明细
    // ---------------------------------------------------------------------------------------------------------------


    @Schema(description = "认购 数量")
    private Integer callCount;

    @Schema(description = "认沽 数量")
    private Integer putCount;

    @Schema(description = "行权价列表（认购+认沽 去重升序，T型报价 行对齐用）")
    private List<BigDecimal> strikeList;

    @Schema(description = "平值行权价（最接近标的价）")
    private BigDecimal atmStrike;

    @Schema(description = "认购 期权列表（按行权价升序）")
    private List<OptionQuoteDTO> callDtoList;

    @Schema(description = "认沽 期权列表（按行权价升序）")
    private List<OptionQuoteDTO> putDtoList;


    // ---------------------------------------------------------------------------------------------------------------
    //                                                 月度统计
    // ---------------------------------------------------------------------------------------------------------------


    @Schema(description = "认购 成交量合计（张）")
    private Long callVolume;

    @Schema(description = "认沽 成交量合计（张）")
    private Long putVolume;

    @Schema(description = "认购 成交额合计（元）")
    private BigDecimal callAmount;

    @Schema(description = "认沽 成交额合计（元）")
    private BigDecimal putAmount;

    @Schema(description = "认沽/认购 成交量 PCR")
    private BigDecimal pcrVolume;

    @Schema(description = "认沽/认购 成交额 PCR")
    private BigDecimal pcrAmount;

}
