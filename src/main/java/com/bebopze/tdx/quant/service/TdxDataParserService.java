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


    /**
     * 增量更新 最近 N 个交易日（N=1 走 TQ 实时快照；N>1 走 TQ 批量日线）。
     *
     * @param days 最近 N 个交易日（1~250）
     */
    void fillStockKlineLastNDays(int days);


    /**
     * 超快实时链路：仅更新 base_stock 的实时行情字段（开/高/低/收/量/额/涨跌），
     * 不读取、不重写全历史 kline_his；用于 15s 级盘中刷新。
     */
    void fillStockRealtimeSnapshot();


    // -----------------------------------------------------------------------------------------------------------------


    Map<String, Set<String>> marketRelaStockCodePrefixList(int type, int N);


    // -----------------------------------------------------------------------------------------------------------------


    void calcAndFillBlockKlineAll();
}