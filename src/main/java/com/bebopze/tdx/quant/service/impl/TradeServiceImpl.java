package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.client.EastMoneyHttpClient;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.req.SubmitTradeV2ReqDTO;
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
    public JSONObject queryCreditNewPosV2(String validatekey) {

//        validatekey = this.validatekey;
//
//
//        JSONObject result = eastMoneySecClient.queryCreditNewPosV2(validatekey);
//        log.info("eastMoneySecClient#queryCreditNewPosV2     >>>     validatekey : {} , result : {}", validatekey, JSON.toJSONString(result));
//

        return null;
    }

    @Override
    public JSONObject wdcc() {


        EastMoneyHttpClient.queryCreditNewPosV2();


        SubmitTradeV2ReqDTO reqDTO = new SubmitTradeV2ReqDTO();
        reqDTO.setStockCode("588050");
        reqDTO.setStockName("科创ETF");
        reqDTO.setPrice("2.055");
        reqDTO.setAmount("100");


        // 委托单号
        Integer wtbh = EastMoneyHttpClient.submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);


        return null;
    }


    public static void main(String[] args) {

//        JSONObject jsonObject = queryCreditNewPosV2("");
//
//        System.out.println();
    }

}
