package com.bebopze.tdx.quant.service;

import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.common.domain.resp.QueryCreditNewPosV2Resp;


/**
 * BS接口
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
public interface TradeService {

    QueryCreditNewPosV2Resp queryCreditNewPosV2();

    JSONObject wdcc();

}
