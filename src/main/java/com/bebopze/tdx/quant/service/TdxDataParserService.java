package com.bebopze.tdx.quant.service;

import java.util.Map;
import java.util.Set;


/**
 * tdx  个股/ETF/板块/大盘  解析导入（关联关系 + 行情数据 + ext_data计算）                // 数据 初始化  ->  数据 更新
 *
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


    void importOptionUnderlying();


    void importOption();


    // -----------------------------------------------------------------------------------------------------------------


    Map<String, Set<String>> marketRelaStockCodePrefixList(int type, int N);


    // -----------------------------------------------------------------------------------------------------------------


    @Deprecated
    void calcAndFillBlockKlineAll();


}