package com.bebopze.tdx.quant.common.domain.trade.req;

import lombok.Data;

import java.io.Serializable;


/**
 * 撤单
 *
 * @author: bebopze
 * @date: 2025/4/29
 */
@Data
public class RevokeOrdersReq implements Serializable {


    // revokes: 20250428_2374


    /**
     * 委托日期_委托编号
     */
    private String revokes;
}