package com.bebopze.tdx.quant.common.domain.dto.tq;

import com.bebopze.tdx.quant.common.util.NumUtil;

import java.io.Serializable;


/**
 * 通达信 盘中 实时行情快照
 *
 *
 * <p> 个股/ETF 的 VOL 单位为手，指数/板块 沿用通达信快照口径；AMO 单位为万元，价格单位为元，涨跌幅单位为 % </p>
 *
 * @author bebopze
 * @date 2026/9/26
 */
public record TdxRealtimeQuoteDTO(String code,
                                  double open,
                                  double high,
                                  double low,
                                  double close,
                                  double vol,
                                  double amo,
                                  double prevClose) implements Serializable {


    public double getVol() {
        // 手 -> 股
        return vol * 100;
    }

    public double amo() {
        // 万元 -> 元
        return NumUtil.of(amo * 1_0000, 2);
    }


    // 涨跌额（元）
    public double getChangePrice() {
        return !Double.isNaN(close) && !Double.isNaN(prevClose) ? NumUtil.of(close - prevClose, 2) : Double.NaN;
    }

    // 涨跌幅（%）
    public double getChangePct() {
        // (close - prevClose) / prevClose * 100%
        return !Double.isNaN(close) && !Double.isNaN(prevClose) ? NumUtil.of(getChangePrice() / prevClose * 100, 2) : Double.NaN;
    }


}