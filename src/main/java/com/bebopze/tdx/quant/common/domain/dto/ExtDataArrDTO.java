package com.bebopze.tdx.quant.common.domain.dto;

import java.io.Serializable;
import java.time.LocalDate;


/**
 * 扩展数据 - 序列
 *
 * @author: bebopze
 * @date: 2025/6/10
 */
public class ExtDataArrDTO implements Serializable {


    public LocalDate[] date;


    // ---------------------------------------------------


    public double[] rps10_arr;
    public double[] rps20_arr;
    public double[] rps50_arr;
    public double[] rps120_arr;
    public double[] rps250_arr;


    // ---------------------------------------------------


    public double[] ssf_arr;


    public double[] 中期涨幅;


    // ---------------------------------------------------


    public boolean[] MA20多;
    public boolean[] MA20空;

    public boolean[] SSF多;
    public boolean[] SSF空;


    public boolean[] N日新高;
    public boolean[] 均线预萌出;
    public boolean[] 均线萌出;
    public boolean[] 大均线多头;


    public boolean[] 月多;
    public boolean[] RPS三线红;


    public ExtDataArrDTO(int size) {
        this.date = new LocalDate[size];


        this.rps10_arr = new double[size];
        this.rps20_arr = new double[size];
        this.rps50_arr = new double[size];
        this.rps120_arr = new double[size];
        this.rps250_arr = new double[size];


        this.ssf_arr = new double[size];
        this.中期涨幅 = new double[size];


        this.MA20多 = new boolean[size];
        this.MA20空 = new boolean[size];
        this.SSF多 = new boolean[size];
        this.SSF空 = new boolean[size];

        this.N日新高 = new boolean[size];
        this.均线预萌出 = new boolean[size];
        this.均线萌出 = new boolean[size];
        this.大均线多头 = new boolean[size];

        this.月多 = new boolean[size];
        this.RPS三线红 = new boolean[size];
    }


}
