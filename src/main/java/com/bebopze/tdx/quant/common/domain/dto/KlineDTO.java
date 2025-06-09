package com.bebopze.tdx.quant.common.domain.dto;

import lombok.Data;

import java.io.Serializable;
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


    // 2025-05-01
    private LocalDate date;


    // --------------- price 规则（ 2位小数 ）
    //
    // A股 股票真实成交价 在交易所层面   统一精确到  小数点后 2位（分）
    // 券商线上系统 或 API 虽可能对参数格式 或 成本测算展示更多小数位，但最终的 撮合成交价 均以  2位小数  上报并成交

    private Double open;
    private Double high;
    private Double low;
    private Double close;


    private Long vol;

    private Double amo;


    // 振幅       H/L   x100-100
    private Double range_pct;

    // 涨跌幅       C/pre_C   x100-100
    private Double change_pct;

    // 涨跌额       C - pre_C
    private Double change_price;

    // 换手率
    private Double turnover_pct;


}