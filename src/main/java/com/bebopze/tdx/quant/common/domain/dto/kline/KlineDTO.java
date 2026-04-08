package com.bebopze.tdx.quant.common.domain.dto.kline;

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


    // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率


    // 2025-05-01
    private LocalDate date;


    // --------------- price 规则（ 2位小数 ）
    //
    // A股 股票真实成交价 在交易所层面   统一精确到  小数点后 2位（分）
    // 券商线上系统 或 API 虽可能对参数格式 或 成本测算展示更多小数位，但最终的 撮合成交价 均以  2位小数  上报并成交

    private double open;
    private double high;
    private double low;
    private double close;


    private long vol;

    private double amo;


    // 振幅（%）       H/L   x100-100
    private double rangePct;
    // 涨跌幅（%）     C/prev_C   x100-100
    private double changePct;
    // 涨跌额（元）    C - prev_C
    private double changePrice;
    // 换手率（%）
    private double turnoverPct;
}