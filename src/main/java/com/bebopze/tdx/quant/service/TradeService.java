package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.RevokeOrderResultDTO;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.TradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.GetOrdersDataResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;

import java.util.List;


/**
 * BS接口
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
public interface TradeService {


    /**
     * 我的持仓
     *
     * @return
     */
    QueryCreditNewPosResp queryCreditNewPosV2();


    /**
     * 实时行情
     *
     * @param stockCode
     * @return
     */
    SHSZQuoteSnapshotResp SHSZQuoteSnapshot(String stockCode);

    /**
     * 下单 - B/S
     *
     * @param param
     * @return
     */
    Integer bs(TradeBSParam param);


    /**
     * 当日委托单 列表
     *
     * @return
     */
    List<GetOrdersDataResp> getOrdersData();

    /**
     * 批量撤单
     *
     * @param paramList
     * @return
     */
    List<RevokeOrderResultDTO> revokeOrders(List<TradeRevokeOrdersParam> paramList);
}
