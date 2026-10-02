package com.bebopze.tdx.quant.common.domain.dto.kline;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.TreeMap;


/**
 * K线 - 序列
 *
 * @author: bebopze
 * @date: 2025/6/10
 */
@Data
public class KlineArrDTO implements Serializable {


    public LocalDate[] date;

    public double[] open;
    public double[] high;
    public double[] low;
    public double[] close;

    public long[] vol;
    public double[] amo;


    // 振幅（%）       H/L   x100-100
    public double[] rangePct;
    // 涨跌幅（%）     C/prev_C   x100-100
    public double[] changePct;
    // 涨跌额         C - prev_C
    public double[] changePrice;
    // 换手率（%）
    public double[] turnoverPct;


    // 流通股本（股）
    public long[] ltgb;
    // 总股本（股）
    public long[] zgb;


    // -----------------------------------------------------------------------------------------------------------------


    public KlineArrDTO(int size) {
        this.date = new LocalDate[size];
        this.open = new double[size];
        this.high = new double[size];
        this.low = new double[size];
        this.close = new double[size];
        this.vol = new long[size];
        this.amo = new double[size];

        this.rangePct = new double[size];
        this.changePct = new double[size];
        this.changePrice = new double[size];
        this.turnoverPct = new double[size];

        this.ltgb = new long[size];
        this.zgb = new long[size];
    }


    // ----------------------------------------------


    public /*private*/ TreeMap<LocalDate, Double> dateCloseMap = new TreeMap<>();


    public TreeMap<LocalDate, Double> getDateCloseMap() {
        if (null == dateCloseMap) {
            dateCloseMap = new TreeMap<>();
        }

        if (dateCloseMap.size() == date.length) {
            return dateCloseMap;
        }


        for (int i = 0; i < date.length; i++) {
            dateCloseMap.put(date[i], close[i]);
        }
        return dateCloseMap;
    }

}