package com.bebopze.tdx.quant.common.domain.trade.req;

import lombok.Data;

import java.io.Serializable;


/**
 * 批量 撤单
 *
 * @author: bebopze
 * @date: 2025/4/29
 */
@Data
public class RevokeOrdersReq implements Serializable {


    // revokes: 20250428_2374

    // revokes: 20250512_5519,20250512_5518,20250512_5517


    /**
     * 委托日期_委托编号,委托日期_委托编号
     */
    private String revokes;
}