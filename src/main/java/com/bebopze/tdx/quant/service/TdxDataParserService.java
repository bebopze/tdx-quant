package com.bebopze.tdx.quant.service;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;
import java.util.Map;


/**
 * @author: bebopze
 * @date: 2025/5/7
 */
public interface TdxDataParserService {


    void importAll();

    void refreshKlineAll();


    void importTdxBlockCfg();


    void importBlockReport();

    void importBlockNewReport();


    void fillBlockKline(String blockCode);

    void fillBlockKlineAll();


    void fillStockKline(String stockCode);

    void fillStockKlineAll(String beginStockCode);


    void xgcz();


    Map<String, List<String>> marketRelaStockCodePrefixList();

    JSONObject check();
}