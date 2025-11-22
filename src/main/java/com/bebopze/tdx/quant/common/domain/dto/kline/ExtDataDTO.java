package com.bebopze.tdx.quant.common.domain.dto.kline;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

import static com.bebopze.tdx.quant.common.util.NumUtil.of;


/**
 * 扩展数据（预计算 指标） -  RPS/...
 *
 * @author: bebopze
 * @date: 2025/5/15
 */
@Data
public class ExtDataDTO implements Serializable {


    // 2025-05-01
    private LocalDate date;


    // ---------------------------------------------------


    private Double rps10;  // -> 板块 rps5
    private Double rps20;  // -> 板块 rps10
    private Double rps50;  // -> 板块 rps15
    private Double rps120; // -> 板块 rps20
    private Double rps250; // -> 板块 rps50


    // ---------------------------------------------------


//    private Double MA5;
//    private Double MA10;
//    private Double MA20;
//    private Double MA25;
//    private Double MA30;
//    private Double MA50;
//    private Double MA60;
//    private Double MA100;
//    private Double MA120;
//    private Double MA150;
//    private Double MA200;
//    private Double MA250;


    // ---------------------------------------------------


    private Double SSF;
    private Double SAR;


    // ---------------------------------------------------


    private Double RPS三线和;
    private Double RPS五线和;


    // ---------------------------------------------------


    private Double 中期涨幅;
    private Double N3日涨幅;
    private Double N5日涨幅;
    private Double N10日涨幅;
    private Double N20日涨幅;


    // ---------------------------------------------------


    private Integer 短期趋势支撑线 = 20;   // 默认支撑线：MA20
    private Integer 中期趋势支撑线 = 50;
    // TODO   private Integer 长期趋势支撑线 = 100;


    private Double C_SSF_偏离率;
    private Double H_SSF_偏离率;


    private Double C_MA5_偏离率;   // TODO   DEL C_MA_偏离率
    private Double H_MA5_偏离率;   // TODO   保留 H_MA_偏离率（高抛S）

    private Double C_MA10_偏离率;
    private Double C_MA15_偏离率;
    private Double C_MA20_偏离率;
    private Double H_MA20_偏离率;
    private Double C_MA25_偏离率;
    private Double C_MA30_偏离率;
    private Double C_MA40_偏离率;
    private Double C_MA50_偏离率;
    private Double C_MA60_偏离率;
    private Double C_MA100_偏离率;
    private Double C_MA120_偏离率;
    private Double C_MA150_偏离率;
    private Double C_MA200_偏离率;
    private Double C_MA250_偏离率;


    public Boolean 高位爆量上影大阴;   // 高位-爆量/上影/大阴


    public Boolean 涨停;
    public Boolean 跌停;


    // ---------------------------------------------------


    // ------- 多空

    private Boolean XZZB;
    private Boolean BSQJ;


    private Boolean MA20多;
    private Boolean MA20空;
    private Boolean SSF多;
    private Boolean SSF空;


    private Boolean 上MA20;
    private Boolean 下MA20;
    private Boolean 上SSF;
    private Boolean 下SSF;


    // ------- 新高

    private Boolean N60日新高;
    private Boolean N100日新高;
    private Boolean 历史新高;


    private Boolean 百日新高;


    // ------- 均线形态

    private Boolean 月多;
    private Boolean 均线预萌出;
    private Boolean 均线萌出;
    private Boolean 小均线多头;
    private Boolean 大均线多头;
    private Boolean 均线大多头;
    private Boolean 均线极多头;


    // ------- RPS强度

    private Boolean RPS红;
    private Boolean RPS一线红;
    private Boolean RPS双线红;
    private Boolean RPS三线红;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 获取 C_MA偏离率 （短期）    ->     B（低吸）
     *
     * @return
     */
    public double getC_短期MA_偏离率() {
        return getC_MA_偏离率(短期趋势支撑线);
    }

    /**
     * 获取 C_MA偏离率 （中期）    ->     S（高抛）
     *
     * @return
     */
    public double getC_中期MA_偏离率() {
        return getC_MA_偏离率(中期趋势支撑线);
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
        } else if (趋势支撑线 == 15) {
            return of(C_MA15_偏离率);
        } else if (趋势支撑线 == 20) {
            return of(C_MA20_偏离率);
        } else if (趋势支撑线 == 25) {
            return of(C_MA25_偏离率);
        } else if (趋势支撑线 == 30) {
            return of(C_MA30_偏离率);
        } else if (趋势支撑线 == 40) {
            return of(C_MA40_偏离率);
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