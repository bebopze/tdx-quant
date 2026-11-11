package com.bebopze.tdx.quant.common.domain.trade.option.req;

import lombok.Data;

import java.io.Serializable;


/**
 * ETF期权 - 批量 撤单
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Data
public class OptionRevokeOrdersReq implements Serializable {


    // wtbhs       9000019830-05,9000020920-15


    /**
     * 委托编号-交易所,委托编号-交易所
     */
    private String wtbhs;


}