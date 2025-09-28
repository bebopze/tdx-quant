package com.bebopze.tdx.quant.common.domain.dto.kline;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;


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


    private Double SSF;


    private Double 中期涨幅;
    private Integer 趋势支撑线 = 20;   // 默认支撑线：MA20
    private Double C_SSF_偏离率;


    public Boolean 高位爆量上影大阴;   // 高位-爆量/上影/大阴


    // ---------------------------------------------------


    private Boolean MA20多;
    private Boolean MA20空;
    private Boolean SSF多;
    private Boolean SSF空;

    private Boolean 上MA20;
    private Boolean 下MA20;
    private Boolean 上SSF;
    private Boolean 下SSF;


    private Boolean N60日新高;
    private Boolean N100日新高;
    private Boolean 历史新高;


    private Boolean 百日新高;


    private Boolean 月多;
    private Boolean 均线预萌出;
    private Boolean 均线萌出;
    private Boolean 大均线多头;
    private Boolean 均线大多头;
    private Boolean 均线极多头;


    private Boolean RPS红;
    private Boolean RPS一线红;
    private Boolean RPS双线红;
    private Boolean RPS三线红;


}