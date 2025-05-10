package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.TradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosV2Resp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;


/**
 * BS接口
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
public interface TradeService {

    QueryCreditNewPosV2Resp queryCreditNewPosV2();

    SHSZQuoteSnapshotResp SHSZQuoteSnapshot(String stockCode);

    Integer bs(TradeBSParam param);

    void revokeOrders(TradeRevokeOrdersParam param);
}
