package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.RevokeOrderResultDTO;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.TradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.trade.req.RevokeOrdersReq;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.GetOrdersDataResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.service.TradeService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


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
    public QueryCreditNewPosResp queryCreditNewPosV2() {
        QueryCreditNewPosResp resp = EastMoneyTradeAPI.queryCreditNewPosV2();
        return resp;
    }


    @Override
    public SHSZQuoteSnapshotResp SHSZQuoteSnapshot(String stockCode) {
        SHSZQuoteSnapshotResp dto = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
        return dto;
    }


    @Override
    public Integer bs(TradeBSParam param) {

        SubmitTradeV2Req req = convert2Req(param);


        // 委托编号
        Integer wtdh = EastMoneyTradeAPI.submitTradeV2(req);
        return wtdh;
    }


    @Override
    public List<GetOrdersDataResp> getOrdersData() {

        List<GetOrdersDataResp> respList = EastMoneyTradeAPI.getOrdersData();
        return respList;
    }


    @Override
    public List<RevokeOrderResultDTO> revokeOrders(List<TradeRevokeOrdersParam> paramList) {

        RevokeOrdersReq req = convert2Req(paramList);

        List<RevokeOrderResultDTO> dtoList = EastMoneyTradeAPI.revokeOrders(req);
        return dtoList;
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
     * @param paramList
     * @return
     */
    private RevokeOrdersReq convert2Req(List<TradeRevokeOrdersParam> paramList) {
        List<String> revokeList = Lists.newArrayList();


        for (TradeRevokeOrdersParam param : paramList) {

            // 委托日期
            String date = StringUtils.isEmpty(param.getDate()) ? LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) : param.getDate();

            // 委托日期_委托编号
            String revoke = date + "_" + param.getWtdh();

            revokeList.add(revoke);
        }


        RevokeOrdersReq req = new RevokeOrdersReq();
        req.setRevokes(String.join(",", revokeList));

        return req;
    }


}
