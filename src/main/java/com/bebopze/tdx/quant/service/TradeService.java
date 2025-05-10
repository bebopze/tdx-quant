package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosV2Resp;


/**
 * BS接口
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
public interface TradeService {

    QueryCreditNewPosV2Resp queryCreditNewPosV2();

    Integer bs(TradeBSParam param);
}
