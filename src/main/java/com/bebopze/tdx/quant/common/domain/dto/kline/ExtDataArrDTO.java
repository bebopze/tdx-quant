package com.bebopze.tdx.quant.common.domain.dto.kline;

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


    public double[] MA5;
    public double[] MA10;
    public double[] MA20;
    public double[] MA30;
    public double[] MA50;
    public double[] MA60;
    public double[] MA100;
    public double[] MA120;
    public double[] MA150;
    public double[] MA200;
    public double[] MA250;


    // ---------------------------------------------------


    public double[] SSF;
    public double[] SAR;


    // ---------------------------------------------------


    public double[] RPS三线和;
    public double[] RPS五线和;


    // ---------------------------------------------------


    public double[] 中期涨幅N5;
    public double[] 中期涨幅N10;
    public double[] 中期涨幅N20;
    public double[] 中期涨幅N30;
    public double[] 中期涨幅N50;
    public double[] 中期涨幅N60;
    public double[] 中期涨幅N100;
    public double[] 中期涨幅N120;
    public double[] 中期涨幅N150;
    public double[] 中期涨幅N200;
    public double[] 中期涨幅N250;


    // ---------------------------------------------------


    public double[] N3日涨幅;
    public double[] N5日涨幅;
    public double[] N10日涨幅;
    public double[] N20日涨幅;
    public double[] N30日涨幅;
    public double[] N50日涨幅;
    public double[] N60日涨幅;
    public double[] N100日涨幅;
    public double[] N120日涨幅;
    public double[] N150日涨幅;
    public double[] N200日涨幅;
    public double[] N250日涨幅;


    // ---------------------------------------------------


    public double[] 中期调整幅度;
    public double[] 中期调整天数;
    public double[] 中期调整幅度2;
    public double[] 中期调整天数2;


    // ---------------------------------------------------


    public int[] 短期支撑线;
    public int[] 中期支撑线;
    public int[] 长期支撑线;


    // ---------------------------------------------------


    public double[] C_SSF_偏离率;   // TODO   B -> C_SSF（+ L_SSF）
    public double[] H_SSF_偏离率;   //        S -> H_SSF


    // ---------------------------------------------------


    public double[] C_MA5_偏离率;
    public double[] H_MA5_偏离率;

    public double[] C_MA10_偏离率;
    public double[] H_MA10_偏离率;

    public double[] C_MA20_偏离率;
    public double[] H_MA20_偏离率;

    public double[] C_MA30_偏离率;
    public double[] H_MA30_偏离率;

    public double[] C_MA50_偏离率;
    public double[] H_MA50_偏离率;

    public double[] C_MA60_偏离率;
    public double[] H_MA60_偏离率;

    public double[] C_MA100_偏离率;
    public double[] H_MA100_偏离率;

    public double[] C_MA120_偏离率;
    public double[] H_MA120_偏离率;

    public double[] C_MA150_偏离率;
    public double[] H_MA150_偏离率;

    public double[] C_MA200_偏离率;
    public double[] H_MA200_偏离率;

    public double[] C_MA250_偏离率;
    public double[] H_MA250_偏离率;


    // ---------------------------------------------------


    public boolean[] 上影大阴;
    public boolean[] 高位爆量上影大阴;   // 高位-爆量/上影/大阴


    public boolean[] 涨停;
    public boolean[] 跌停;


    // ---------------------------------------------------


    public boolean[] XZZB;
    public boolean[] BSQJ;


    // ---------------------------------------------------


    public boolean[] MA5多;
    public boolean[] MA5空;
    public boolean[] MA10多;
    public boolean[] MA10空;
    public boolean[] MA20多;
    public boolean[] MA20空;
    public boolean[] SSF多;
    public boolean[] SSF空;

    public boolean[] 上MA20;
    public boolean[] 下MA20;
    public boolean[] 上SSF;
    public boolean[] 下SSF;


    // ---------------------------------------------------


    public boolean[] N60日新高;
    public boolean[] N100日新高;
    public boolean[] 历史新高;


    public boolean[] 百日新高;   // 近5日内创百日新高，并且未大幅回落


    // ---------------------------------------------------


    public boolean[] 月多;
    public boolean[] 均线预萌出;
    public boolean[] 均线萌出;
    public boolean[] 小均线多头;
    public boolean[] 大均线多头;
    public boolean[] 均线大多头;
    public boolean[] 均线极多头;


    // ---------------------------------------------------


    public boolean[] RPS红;
    public boolean[] RPS一线红;
    public boolean[] RPS双线红;
    public boolean[] RPS三线红;


    // --------------------------------------------------- 经典买点


    public boolean[] 首次三线红;
    public boolean[] 口袋支点;


    // ---------------------------------------------------


    public int[] klineType;


    // ---------------------------------------------------


    public boolean 月空(int idx) {
        return !月多[idx];
    }


    // ---------------------------------------------------


    public ExtDataArrDTO(int size) {


        // ---------------------------------------------------


        this.date = new LocalDate[size];


        // ---------------------------------------------------


        this.rps10 = new double[size];
        this.rps20 = new double[size];
        this.rps50 = new double[size];
        this.rps120 = new double[size];
        this.rps250 = new double[size];


        // ---------------------------------------------------


        this.MA5 = new double[size];
        this.MA10 = new double[size];
        this.MA20 = new double[size];
        this.MA30 = new double[size];
        this.MA50 = new double[size];
        this.MA60 = new double[size];
        this.MA100 = new double[size];
        this.MA120 = new double[size];
        this.MA150 = new double[size];
        this.MA200 = new double[size];
        this.MA250 = new double[size];


        // ---------------------------------------------------


        this.SSF = new double[size];
        this.SAR = new double[size];


        // ---------------------------------------------------


        this.RPS三线和 = new double[size];
        this.RPS五线和 = new double[size];


        // ---------------------------------------------------


        this.中期涨幅N5 = new double[size];
        this.中期涨幅N10 = new double[size];
        this.中期涨幅N20 = new double[size];
        this.中期涨幅N30 = new double[size];
        this.中期涨幅N50 = new double[size];
        this.中期涨幅N60 = new double[size];
        this.中期涨幅N100 = new double[size];
        this.中期涨幅N120 = new double[size];
        this.中期涨幅N150 = new double[size];
        this.中期涨幅N200 = new double[size];
        this.中期涨幅N250 = new double[size];


        // ---------------------------------------------------


        this.N3日涨幅 = new double[size];
        this.N5日涨幅 = new double[size];
        this.N10日涨幅 = new double[size];
        this.N20日涨幅 = new double[size];
        this.N30日涨幅 = new double[size];
        this.N50日涨幅 = new double[size];
        this.N60日涨幅 = new double[size];
        this.N100日涨幅 = new double[size];
        this.N120日涨幅 = new double[size];
        this.N150日涨幅 = new double[size];
        this.N200日涨幅 = new double[size];
        this.N250日涨幅 = new double[size];


        // ---------------------------------------------------


        this.中期调整幅度 = new double[size];
        this.中期调整天数 = new double[size];
        this.中期调整幅度2 = new double[size];
        this.中期调整天数2 = new double[size];


        // ---------------------------------------------------


        this.短期支撑线 = new int[size];
        this.中期支撑线 = new int[size];
        this.长期支撑线 = new int[size];


        // ---------------------------------------------------


        this.C_SSF_偏离率 = new double[size];
        this.H_SSF_偏离率 = new double[size];


        // ---------------------------------------------------


        this.C_MA5_偏离率 = new double[size];
        this.H_MA5_偏离率 = new double[size];

        this.C_MA10_偏离率 = new double[size];
        this.H_MA10_偏离率 = new double[size];

        this.C_MA20_偏离率 = new double[size];
        this.H_MA20_偏离率 = new double[size];

        this.C_MA30_偏离率 = new double[size];
        this.H_MA30_偏离率 = new double[size];

        this.C_MA50_偏离率 = new double[size];
        this.H_MA50_偏离率 = new double[size];

        this.C_MA60_偏离率 = new double[size];
        this.H_MA60_偏离率 = new double[size];

        this.C_MA100_偏离率 = new double[size];
        this.H_MA100_偏离率 = new double[size];

        this.C_MA120_偏离率 = new double[size];
        this.H_MA120_偏离率 = new double[size];

        this.C_MA150_偏离率 = new double[size];
        this.H_MA150_偏离率 = new double[size];

        this.C_MA200_偏离率 = new double[size];
        this.H_MA200_偏离率 = new double[size];

        this.C_MA250_偏离率 = new double[size];
        this.H_MA250_偏离率 = new double[size];


        // ---------------------------------------------------


        this.上影大阴 = new boolean[size];
        this.高位爆量上影大阴 = new boolean[size];


        this.涨停 = new boolean[size];
        this.跌停 = new boolean[size];


        // ---------------------------------------------------


        this.XZZB = new boolean[size];
        this.BSQJ = new boolean[size];


        // ---------------------------------------------------


        this.MA5多 = new boolean[size];
        this.MA5空 = new boolean[size];
        this.MA10多 = new boolean[size];
        this.MA10空 = new boolean[size];
        this.MA20多 = new boolean[size];
        this.MA20空 = new boolean[size];
        this.SSF多 = new boolean[size];
        this.SSF空 = new boolean[size];


        this.上MA20 = new boolean[size];
        this.下MA20 = new boolean[size];
        this.上SSF = new boolean[size];
        this.下SSF = new boolean[size];


        // ---------------------------------------------------


        this.N60日新高 = new boolean[size];
        this.N100日新高 = new boolean[size];
        this.历史新高 = new boolean[size];


        this.百日新高 = new boolean[size];


        // ---------------------------------------------------


        this.月多 = new boolean[size];
        this.均线预萌出 = new boolean[size];
        this.均线萌出 = new boolean[size];
        this.小均线多头 = new boolean[size];
        this.大均线多头 = new boolean[size];
        this.均线大多头 = new boolean[size];
        this.均线极多头 = new boolean[size];


        // ---------------------------------------------------


        this.RPS红 = new boolean[size];
        this.RPS一线红 = new boolean[size];
        this.RPS双线红 = new boolean[size];
        this.RPS三线红 = new boolean[size];


        // ---------------------------------------------------


        this.首次三线红 = new boolean[size];
        this.口袋支点 = new boolean[size];


        // ---------------------------------------------------


        this.klineType = new int[size];


        // ---------------------------------------------------
    }

}