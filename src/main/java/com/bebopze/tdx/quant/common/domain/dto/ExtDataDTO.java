package com.bebopze.tdx.quant.common.domain.dto;

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


    // ---------------------------------------------------


    private Boolean SSF多;


    private Boolean N日新高;
    private Boolean 均线预萌出;
    private Boolean 均线萌出;
    private Boolean 大均线多头;


    private Boolean 月多;
    private Boolean RPS三线红;


}
