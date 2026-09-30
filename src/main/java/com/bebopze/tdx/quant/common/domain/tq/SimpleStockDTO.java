package com.bebopze.tdx.quant.common.domain.tq;

import lombok.Data;


/**
 * TQ 成分股 基础信息（code + name）
 *
 * @author: bebopze
 * @date: 2026/10/01
 */
@Data
public class SimpleStockDTO {


    /**
     * 代码
     */
    private String Code;

    /**
     * 名称
     */
    private String Name;
}