package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;

import java.util.Map;
import java.util.Set;


/**
 * @author: bebopze
 * @date: 2025/5/7
 */
public interface TdxDataParserService {


    void importAll();


    void importAll__blockRelaStock();


    void importTdxBlockCfg();


    /**
     * 系统板块   txt报表导入                         （行业/概念）板块 - 个股     关联关系
     */
    void importBlockReport();

    /**
     * 自定义板块 txt报表导入                               自定义板块 - 个股     关联关系
     */
    void importBlockNewReport();


    void importETF();


    void importHkStock();

    void importUsStock();


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 行情（kline_his）  ->   板块 + 个股
     *
     * @param updateTypeEnum 1-全量更新；2-增量更新（实时行情）；
     */
    void refreshKlineAll(UpdateTypeEnum updateTypeEnum);


    void fillBlockKline(String blockCode);

    void fillBlockKlineAll();


    void fillStockKline(String stockCode, Integer apiType, UpdateTypeEnum updateTypeEnum);

    void fillStockKlineAll(UpdateTypeEnum updateTypeEnum);


    // -----------------------------------------------------------------------------------------------------------------


    Map<String, Set<String>> marketRelaStockCodePrefixList(int type, int N);


    // -----------------------------------------------------------------------------------------------------------------


    void calcAndFillBlockKlineAll();
}