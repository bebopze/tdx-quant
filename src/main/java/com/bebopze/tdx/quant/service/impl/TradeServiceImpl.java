package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.client.EastMoneyHttpClient;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.TradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.trade.req.RevokeOrdersReq;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosV2Resp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.service.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * BS
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
@Slf4j
@Service
public class TradeServiceImpl implements TradeService {


    @Override
    public QueryCreditNewPosV2Resp queryCreditNewPosV2() {
        QueryCreditNewPosV2Resp resp = EastMoneyHttpClient.queryCreditNewPosV2();
        return resp;
    }

    @Override
    public SHSZQuoteSnapshotResp SHSZQuoteSnapshot(String stockCode) {
        SHSZQuoteSnapshotResp dto = EastMoneyHttpClient.SHSZQuoteSnapshot(stockCode);
        return dto;
    }


    @Override
    public Integer bs(TradeBSParam param) {

        SubmitTradeV2Req req = convert2Req(param);


        Integer wtdh = EastMoneyHttpClient.submitTradeV2(req);
        return wtdh;
    }

    @Override
    public void revokeOrders(TradeRevokeOrdersParam param) {

        RevokeOrdersReq req = convert2Req(param);

        EastMoneyHttpClient.revokeOrders(req);
    }


    /**
     * 下单 -> B/S
     *
     * @param param
     * @return
     */
    private SubmitTradeV2Req convert2Req(TradeBSParam param) {

        SubmitTradeV2Req req = new SubmitTradeV2Req();
        req.setStockCode(param.getStockCode());
        req.setStockName(param.getStockName());
        req.setPrice(param.getPrice());
        req.setAmount(param.getAmount());


        // B/S
        TradeTypeEnum tradeTypeEnum = TradeTypeEnum.getByTradeType(param.getTradeType());
        req.setTradeType(tradeTypeEnum.getEastMoneyTradeType());
        req.setXyjylx(tradeTypeEnum.getXyjylx());


        // 市场（HA-沪A / SA-深A / B-北交所）
        String market = StockMarketEnum.getEastMoneyMarketByStockCode(param.getStockCode());
        req.setMarket(market == null ? StockMarketEnum.SH.getEastMoneyMarket() : market);


        req.setTradeTypeEnum(tradeTypeEnum);

        return req;
    }


    /**
     * 撤单
     *
     * @param param
     * @return
     */
    private RevokeOrdersReq convert2Req(TradeRevokeOrdersParam param) {

        RevokeOrdersReq req = new RevokeOrdersReq();
        // 委托日期
        String date = StringUtils.isEmpty(param.getDate()) ? LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) : param.getDate();

        String revokes = date + "_" + param.getWtdh();
        req.setRevokes(revokes);


        return req;
    }

}
