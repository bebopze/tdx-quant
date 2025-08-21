package com.bebopze.tdx.quant.common.domain.dto.backtest;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * 每日收益率 - 最大回撤
 *
 * @author: bebopze
 * @date: 2025/8/17
 */
@Data
public class MaxDrawdownPctDTO {


    /**
     * 当日
     */
    private LocalDate tradeDate;
    /**
     * 当日 - 回撤（%）
     */
    private BigDecimal drawdownPct;


    /**
     * 最大净值
     */
    private BigDecimal maxNav;
    /**
     * 最大净值 - 日期
     */
    private LocalDate maxNavDate;


    /**
     * 最大回撤（%）
     */
    private BigDecimal maxDrawdownPct;
    /**
     * 最大回撤 - 日期
     */
    private LocalDate maxDrawdownDate;
    /**
     * 最大回撤 - 净值
     */
    private BigDecimal maxDrawdownNav;
}