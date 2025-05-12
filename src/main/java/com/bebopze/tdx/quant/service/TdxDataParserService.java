package com.bebopze.tdx.quant.service;

import com.alibaba.fastjson.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * @author: bebopze
 * @date: 2025/5/7
 */
public interface TdxDataParserService {


    void tdxData();

    void exportBlock();

    void exportBlockNew();


    void blockNew();

    void xgcz();


    Map<String, List<String>> marketRelaStockCodePrefixList();

    JSONObject check();

}
