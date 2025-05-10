package com.bebopze.tdx.quant.common.domain.req;

import lombok.Data;


/**
 * 撤单
 *
 * @author: bebopze
 * @date: 2025/4/29
 */
@Data
public class RevokeOrdersReq {


    // revokes: 20250428_2374


    /**
     * 委托日期_委托单号
     */
    private String revokes;
}