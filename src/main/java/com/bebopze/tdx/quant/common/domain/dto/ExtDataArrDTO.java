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


    public double[] rps10;
    public double[] rps20;
    public double[] rps50;
    public double[] rps120;
    public double[] rps250;


    // ---------------------------------------------------


    public double[] SSF;


    public double[] 中期涨幅;
    public int[] 趋势支撑线;


    public boolean[] 高位爆量上影大阴;   // 高位-爆量/上影/大阴


    // ---------------------------------------------------


    public boolean[] MA20多;
    public boolean[] MA20空;

    public boolean[] SSF多;
    public boolean[] SSF空;


    public boolean[] N60日新高;
    public boolean[] N100日新高;
    public boolean[] 历史新高;


    public boolean[] 月多;
    public boolean[] 均线预萌出;
    public boolean[] 均线萌出;
    public boolean[] 大均线多头;


    public boolean[] RPS红;
    public boolean[] RPS一线红;
    public boolean[] RPS双线红;
    public boolean[] RPS三线红;


    // ---------------------------------------------------


    // ---------------------------------------------------


    public ExtDataArrDTO(int size) {
        this.date = new LocalDate[size];


        this.rps10 = new double[size];
        this.rps20 = new double[size];
        this.rps50 = new double[size];
        this.rps120 = new double[size];
        this.rps250 = new double[size];


        this.SSF = new double[size];


        this.中期涨幅 = new double[size];
        this.趋势支撑线 = new int[size];


        this.高位爆量上影大阴 = new boolean[size];


        this.MA20多 = new boolean[size];
        this.MA20空 = new boolean[size];
        this.SSF多 = new boolean[size];
        this.SSF空 = new boolean[size];


        this.N60日新高 = new boolean[size];
        this.N100日新高 = new boolean[size];
        this.历史新高 = new boolean[size];


        this.月多 = new boolean[size];
        this.均线预萌出 = new boolean[size];
        this.均线萌出 = new boolean[size];
        this.大均线多头 = new boolean[size];


        this.RPS红 = new boolean[size];
        this.RPS一线红 = new boolean[size];
        this.RPS双线红 = new boolean[size];
        this.RPS三线红 = new boolean[size];
    }

}