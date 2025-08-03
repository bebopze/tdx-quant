package com.bebopze.tdx.quant.service;

import java.util.List;
import java.util.Map;


/**
 * @author: bebopze
 * @date: 2025/5/7
 */
public interface TdxDataParserService {


    void importAll();


    void importTdxBlockCfg();


    void importBlockReport();

    void importBlockNewReport();


    void importETF();


    // ----------------------------------------------------------------


    /**
     * 行情（kline_his）  ->   板块 + 个股
     *
     * @param updateType 1-全量更新；2-增量更新；
     */
    void refreshKlineAll(int updateType);


    void fillBlockKline(String blockCode);

    void fillBlockKlineAll();


    void fillStockKline(String stockCode, Integer apiType, int updateType);

    void fillStockKlineAll(int updateType);


    // ----------------------------------------------------------------


    Map<String, List<String>> marketRelaStockCodePrefixList(int N);
}