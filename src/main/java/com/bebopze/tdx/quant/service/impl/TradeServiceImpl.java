package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.client.EastMoneyHttpClient;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosV2Resp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.service.TradeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


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

        SubmitTradeV2Req reqDTO = convert2Req(param);


        Integer wtdh = EastMoneyHttpClient.submitTradeV2(reqDTO);
        return wtdh;
    }


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

}
