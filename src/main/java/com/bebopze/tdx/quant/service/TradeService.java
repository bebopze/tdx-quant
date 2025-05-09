package com.bebopze.tdx.quant.service;

import com.alibaba.fastjson.JSONObject;


/**
 * BS接口
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
public interface TradeService {

    JSONObject queryCreditNewPosV2(String validatekey);

    JSONObject wdcc();
}
