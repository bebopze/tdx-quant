package com.bebopze.tdx.quant.common.domain.dto.kline;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

import static com.bebopze.tdx.quant.common.util.NumUtil.of;


/**
 * 扩展数据（预计算 指标） -  RPS/MA/中期涨幅/中期调整/支撑线/偏离率/...
 *
 * @author: bebopze
 * @date: 2025/5/15
 */
@Data
public class ExtDataDTO implements Serializable {


    // =================================================================================================================


    // boolean      ->        1 bytes                 栈/堆(作为字段)               1（基础值大小）
    // Boolean      ->       16 bytes                 堆(对象)                     1（基础值大小） + 12（对象头）+ 3（对齐填充）

    // int          ->        4 bytes                 栈/堆(作为字段)               4（基础值大小）
    // Integer      ->       16 bytes                 堆(对象)                     4（基础值大小） + 12（对象头）+ 0（对齐填充）

    // float        ->        4 bytes                 栈/堆(作为字段)               4（基础值大小）
    // Float        ->       16 bytes                 堆(对象)                     4（基础值大小） + 12（对象头）+ 0（对齐填充）

    // long         ->        8 bytes                 栈/堆(作为字段)               8（基础值大小）
    // Long         ->       24 bytes                 堆(对象)                     8（基础值大小） + 12（对象头）+ 4（对齐填充）

    // double       ->        8 bytes                 栈/堆(作为字段)               8（基础值大小）
    // Double       ->       24 bytes                 堆(对象)                     8（基础值大小） + 12（对象头）+ 4（对齐填充）


    // =================================================================================================================


    // 2025-05-01
    private LocalDate date;


    // --------------------------------------------------- RPS


    private double rps10;  // -> 板块 rps5
    private double rps20;  // -> 板块 rps10
    private double rps50;  // -> 板块 rps15
    private double rps120; // -> 板块 rps20
    private double rps250; // -> 板块 rps50


    // --------------------------------------------------- MA（日）


    private double MA5;
    private double MA10;
    private double MA20;
    private double MA30;
    private double MA50;
    private double MA60;
    private double MA100;
    private double MA120;
    private double MA150;
    private double MA200;
    private double MA250;


//    // --------------------------------------------------- MA（周）
//
//
//    private double MA5_week;
//    private double MA10_week;
//    private double MA20_week;
//    private double MA30_week;
//    private double MA40_week;
//    private double MA50_week;
//
//
//    // --------------------------------------------------- MA（月）
//
//
//    private double MA5_month;  //  5 x20
//    private double MA10_month; // 10 x20


    // ---------------------------------------------------


    private double SSF;
    private double SAR;


    // ---------------------------------------------------


    private double RPS三线和;
    private double RPS五线和;


    // --------------------------------------------------- 中期涨幅


    private double 中期涨幅N5;
    private double 中期涨幅N10;
    private double 中期涨幅N20;
    private double 中期涨幅N30;
    private double 中期涨幅N50;
    private double 中期涨幅N60;
    private double 中期涨幅N100;
    private double 中期涨幅N120;
    private double 中期涨幅N150;
    private double 中期涨幅N200;
    private double 中期涨幅N250;


    // --------------------------------------------------- N日涨幅


    private double N3日涨幅;
    private double N5日涨幅;
    private double N10日涨幅;
    private double N20日涨幅;
    private double N30日涨幅;
    private double N50日涨幅;
    private double N60日涨幅;
    private double N100日涨幅;
    private double N120日涨幅;
    private double N150日涨幅;
    private double N200日涨幅;
    private double N250日涨幅;


    // --------------------------------------------------- 中期调整 幅度/天数


    private double 中期调整幅度;
    private double 中期调整天数;
    private double 中期调整幅度2;
    private double 中期调整天数2;


    // --------------------------------------------------- 支撑线（日）


    private int 短期支撑线 = 20;   // 默认支撑线：MA20
    private int 中期支撑线 = 50;
    private int 长期支撑线 = 100;

//    private int 短期支撑线_week = Math.max(短期支撑线 / 5, 5);
//
//    private int 短期支撑线_month = Math.max(短期支撑线 / 20, 5);


    // ---------------------------------------------------


    private double C_SSF_偏离率;
    private double H_SSF_偏离率;


    // --------------------------------------------------- C_MA_偏离率 / H_MA_偏离率


    private double C_MA5_偏离率;
    private double H_MA5_偏离率;

    private double C_MA10_偏离率;
    private double H_MA10_偏离率;

    private double C_MA20_偏离率;
    private double H_MA20_偏离率;

    private double C_MA30_偏离率;
    private double H_MA30_偏离率;

    private double C_MA50_偏离率;
    private double H_MA50_偏离率;

    private double C_MA60_偏离率;
    private double H_MA60_偏离率;

    private double C_MA100_偏离率;
    private double H_MA100_偏离率;

    private double C_MA120_偏离率;
    private double H_MA120_偏离率;

    private double C_MA150_偏离率;
    private double H_MA150_偏离率;

    private double C_MA200_偏离率;
    private double H_MA200_偏离率;

    private double C_MA250_偏离率;
    private double H_MA250_偏离率;


    // ---------------------------------------------------


    public boolean 上影大阴;
    public boolean 高位爆量上影大阴;   // 高位-爆量/上影/大阴


    public boolean 涨停;
    public boolean 跌停;


    // --------------------------------------------------- 多空


    private boolean XZZB;
    private boolean BSQJ;


    // ---------------------------------------------------


    private boolean MA5多;
    private boolean MA5空;
    private boolean MA10多;
    private boolean MA10空;
    private boolean MA20多;
    private boolean MA20空;
    private boolean SSF多;
    private boolean SSF空;


//    private boolean MA200多;
//    private boolean MA200空;


    private boolean 上MA20;
    private boolean 下MA20;
    private boolean 上SSF;
    private boolean 下SSF;


    // --------------------------------------------------- 新高


    private boolean N60日新高;
    private boolean N100日新高;
    private boolean 历史新高;


    private boolean 百日新高;


    // --------------------------------------------------- 均线形态


    private boolean 月多;
    private boolean 均线预萌出;
    private boolean 均线萌出;
    private boolean 小均线多头;
    private boolean 大均线多头;
    private boolean 均线大多头;
    private boolean 均线极多头;


    // --------------------------------------------------- RPS强度


    private boolean RPS红;
    private boolean RPS一线红;
    private boolean RPS双线红;
    private boolean RPS三线红;


    // --------------------------------------------------- 经典买点


    private boolean 首次三线红;
    private boolean 口袋支点;


    // --------------------------------------------------- K线形态


    private int klineType;   // K线形态（1-慢牛股；2-趋势股；3-动量股；4-妖股；）


    // ---------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 获取 C_MA偏离率 （短期）    ->     B（低吸/高抛）
     *
     * @return
     */
    public double getC_短期MA_偏离率() {
        return getC_MA_偏离率(短期支撑线);
    }

    /**
     * 获取 C_MA偏离率 （中期）    ->     S（高抛）
     *
     * @return
     */
    public double getC_中期MA_偏离率() {
        return getC_MA_偏离率(中期支撑线);
    }

    /**
     * 获取 C_MA偏离率 （长期）    ->     S（高抛）
     *
     * @return
     */
    public double getC_长期MA_偏离率() {
        return getC_MA_偏离率(长期支撑线);
    }


    /**
     * 获取 C_MA偏离率
     *
     * @param 趋势支撑线
     * @return
     */
    private double getC_MA_偏离率(Integer 趋势支撑线) {


        // TODO   DEL C_MA_偏离率
        // TODO   保留 H_MA_偏离率（高抛S）


        if (趋势支撑线 == 5) {
            return of(C_MA5_偏离率);
        } else if (趋势支撑线 == 10) {
            return of(C_MA10_偏离率);
        } else if (趋势支撑线 == 20) {
            return of(C_MA20_偏离率);
        } else if (趋势支撑线 == 30) {
            return of(C_MA30_偏离率);
        } else if (趋势支撑线 == 50) {
            return of(C_MA50_偏离率);
        } else if (趋势支撑线 == 60) {
            return of(C_MA60_偏离率);
        } else if (趋势支撑线 == 100) {
            return of(C_MA100_偏离率);
        } else if (趋势支撑线 == 120) {
            return of(C_MA120_偏离率);
        } else if (趋势支撑线 == 150) {
            return of(C_MA150_偏离率);
        } else if (趋势支撑线 == 200) {
            return of(C_MA200_偏离率);
        } else if (趋势支撑线 == 250) {
            return of(C_MA250_偏离率);
        }


        return Double.NaN;
    }


}