package com.bebopze.tdx.quant.common.domain.dto.option;

import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/**
 * 期权 - 标的ETF K线（所选日期 前后 N 个月）
 *
 * @author: bebopze
 * @date: 2026/11/24
 */
@Data
public class OptionUnderlyingKlineDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    @Schema(description = "标的证券code", example = "510300")
    private String underlyingCode;

    @Schema(description = "标的证券name", example = "300ETF")
    private String underlyingName;

    @Schema(description = "标的ETF 全名", example = "沪深300ETF华泰柏瑞")
    private String etfName;

    @Schema(description = "所选日期")
    private LocalDate date;

    @Schema(description = "K线区间 起始")
    private LocalDate from;

    @Schema(description = "K线区间 结束")
    private LocalDate to;

    @Schema(description = "前后月份数", example = "3")
    private Integer months;

    @Schema(description = "行情日期（<= 所选日期 的最近一根）")
    private LocalDate quoteDate;

    @Schema(description = "收盘价（行情日）")
    private BigDecimal close;

    @Schema(description = "昨收价（行情日）")
    private BigDecimal prevClose;

    @Schema(description = "涨跌额（行情日）")
    private BigDecimal changePrice;

    @Schema(description = "涨跌幅（%）（行情日）")
    private BigDecimal changePct;

    @Schema(description = "K线数量")
    private Integer barCount;

    @Schema(description = "日K线列表（升序）")
    private List<KlineDTO> klineList;

}
