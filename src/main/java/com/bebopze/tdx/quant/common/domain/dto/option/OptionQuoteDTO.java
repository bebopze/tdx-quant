package com.bebopze.tdx.quant.common.domain.dto.option;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * 期权 T型报价 - 单个合约（T型报价 单元格）
 *
 * <p>
 * 行情字段取自 {@code base_option.kline_his} 中「所选日期当日（或之前最近一根）」的日K，
 * 因此同一响应的不同合约，其 {@code barDate} 可能不同（停牌/无成交合约 取最近行情日，{@code stale=true}）。
 *
 * @author: bebopze
 * @date: 2026/11/24
 */
@Data
public class OptionQuoteDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    // ---------------------------------------------------------------------------------------------------------------
    //                                                 合约要素
    // ---------------------------------------------------------------------------------------------------------------


    @Schema(description = "期权合约编码", example = "10012853")
    private String code;

    @Schema(description = "期权合约名称", example = "50ETF购12月2800")
    private String name;

    @Schema(description = "合约类型：C-认购；P-认沽", example = "C")
    private String callPutType;

    @Schema(description = "行权价格", example = "2.8000")
    private BigDecimal strikePrice;

    @Schema(description = "合约月份（yyyyMM）", example = "202612")
    private Integer contractMonth;

    @Schema(description = "合约开始日期")
    private LocalDate startDate;

    @Schema(description = "合约结束日期（表内原值）")
    private LocalDate endDate;

    @Schema(description = "到期日（有效到期日 = max(表内end_date, 合约月份第4个星期三 顺延)）")
    private LocalDate expiryDate;

    @Schema(description = "剩余自然日（相对所选日期）", example = "26")
    private Integer daysToExpiry;


    // ---------------------------------------------------------------------------------------------------------------
    //                                                 行情（所选日期）
    // ---------------------------------------------------------------------------------------------------------------


    @Schema(description = "行情日期：该合约在所选日期当日（或之前最近一根）的K线日期")
    private LocalDate barDate;

    @Schema(description = "是否滞后行情：barDate != 所选日期（当日无成交/停牌）", example = "false")
    private Boolean stale;

    private BigDecimal open;

    private BigDecimal high;

    private BigDecimal low;

    @Schema(description = "收盘价（最新价）", example = "0.0714")
    private BigDecimal close;

    @Schema(description = "昨收价（= close - changePrice）", example = "0.0709")
    private BigDecimal prevClose;

    @Schema(description = "成交量（张）", example = "16822")
    private Long volume;

    @Schema(description = "成交额（元）")
    private BigDecimal amount;

    @Schema(description = "涨跌额", example = "0.0005")
    private BigDecimal changePrice;

    @Schema(description = "涨跌幅（%）", example = "0.71")
    private BigDecimal changePct;

    @Schema(description = "振幅（%）")
    private BigDecimal rangePct;


    // ---------------------------------------------------------------------------------------------------------------
    //                                                 派生指标
    // ---------------------------------------------------------------------------------------------------------------


    @Schema(description = "内在价值（认购 max(0,S-K)；认沽 max(0,K-S)）")
    private BigDecimal intrinsicValue;

    @Schema(description = "时间价值（= close - intrinsicValue）")
    private BigDecimal timeValue;

    @Schema(description = "盈亏平衡点（认购 K+close；认沽 K-close）")
    private BigDecimal breakEven;

    @Schema(description = "价格杠杆（= 标的价 / close）")
    private BigDecimal leverage;

    @Schema(description = "价值状态：ITM-实值；ATM-平值；OTM-虚值", example = "OTM")
    private String moneyness;

    @Schema(description = "距平值档数（0=平值，正数=虚值档数，负数=实值档数）", example = "3")
    private Integer otmLevel;

}
