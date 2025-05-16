package com.bebopze.tdx.quant.common.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 指标结果
 *
 * @author: bebopze
 * @date: 2025/5/16
 */
@Data
public class FunIdxDTO implements Serializable {


    private boolean 上MA;

    private boolean 下MA;


    private boolean MA向上;

    private boolean MA向下;


    private boolean MA多;

    private boolean MA空;
}
