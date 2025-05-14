package com.bebopze.tdx.quant.common.domain.dto;

import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * K线
 *
 * @author: bebopze
 * @date: 2025/5/15
 */
@Data
public class KlineDTO implements Serializable {


    // 日期,O,C,H,L,VOL,AMO,振幅,涨跌幅,涨跌额,换手率


    // 20250501
    private String date;

    private BigDecimal open;

    private BigDecimal close;

    private BigDecimal high;

    private BigDecimal low;

    private Long vol;

    private BigDecimal amo;


    // 振幅       H/L   x100-100
    private BigDecimal range_pct;

    // 涨跌幅       C/pre_C   x100-100
    private BigDecimal change_pct;

    // 涨跌额       C - pre_C
    private BigDecimal change_price;

    // 换手率
    private BigDecimal turnover_pct;


    // ---------------------------------------------------


    public LocalDate getDate() {
        return DateTimeUtil.parseDate_yyyy_MM_dd(date);
    }
}
