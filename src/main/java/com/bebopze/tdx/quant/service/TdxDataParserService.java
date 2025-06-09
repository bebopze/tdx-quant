package com.bebopze.tdx.quant.service;

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


    void fillStockKline(String stockCode, Integer apiType);

    void fillStockKlineAll();


    Map<String, List<String>> marketRelaStockCodePrefixList();
}