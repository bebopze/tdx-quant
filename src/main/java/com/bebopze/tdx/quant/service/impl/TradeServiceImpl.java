package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.client.EastMoneyHttpClient;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.resp.QueryCreditNewPosV2Resp;
import com.bebopze.tdx.quant.service.TradeService;
import com.bebopze.tdx.quant.util.PropsUtil;
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

    // @Value("eastmoney.validatekey")
    private static String validatekey = PropsUtil.getSid();


    @Override
    public QueryCreditNewPosV2Resp queryCreditNewPosV2() {
        QueryCreditNewPosV2Resp resp = EastMoneyHttpClient.queryCreditNewPosV2();
        return resp;
    }


    @Override
    public JSONObject wdcc() {


        EastMoneyHttpClient.queryCreditNewPosV2();


        SubmitTradeV2Req reqDTO = new SubmitTradeV2Req();
        reqDTO.setStockCode("588050");
        reqDTO.setStockName("科创ETF");
        reqDTO.setPrice("2.055");
        reqDTO.setAmount("100");


        // 委托单号
        Integer wtbh = EastMoneyHttpClient.submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);


        return null;
    }


//    @Override
//    public JSONObject buy() {
//
//
//        EastMoneyHttpClient.queryCreditNewPosV2();
//
//
//        SubmitTradeV2ReqDTO reqDTO = new SubmitTradeV2ReqDTO();
//        reqDTO.setStockCode("588050");
//        reqDTO.setStockName("科创ETF");
//        reqDTO.setPrice("2.055");
//        reqDTO.setAmount("100");
//
//
//        // 委托单号
//        Integer wtbh = EastMoneyHttpClient.submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);
//
//
//        return null;
//    }

}
