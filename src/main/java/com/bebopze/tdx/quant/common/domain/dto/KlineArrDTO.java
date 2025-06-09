package com.bebopze.tdx.quant.common.domain.dto;

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


    public KlineArrDTO(int size) {
        this.date = new LocalDate[size];
        this.open = new double[size];
        this.high = new double[size];
        this.low = new double[size];
        this.close = new double[size];
        this.vol = new long[size];
        this.amo = new double[size];
    }


    // ----------------------------------------------


    private TreeMap<LocalDate, Double> dateCloseMap = new TreeMap<>();


    public TreeMap<LocalDate, Double> getDateCloseMap() {
        if (dateCloseMap.size() == date.length) return dateCloseMap;


        for (int i = 0; i < date.length; i++) {
            dateCloseMap.put(date[i], close[i]);
        }
        return dateCloseMap;
    }

}