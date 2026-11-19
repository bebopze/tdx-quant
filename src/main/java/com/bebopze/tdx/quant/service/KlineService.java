package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;


/**
 * kline 行情数据 拉取
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
public interface KlineService {


    /**
     * 行情（kline_his）  ->   板块 + 个股
     *
     * @param updateTypeEnum 1-全量更新；2-增量更新（实时行情）；
     */
    void refreshKlineAll(UpdateTypeEnum updateTypeEnum);

    void refreshKlineAll__lastDay();


    void fillBlockKline(String blockCode, UpdateTypeEnum updateTypeEnum);

    void fillBlockKlineAll(UpdateTypeEnum updateTypeEnum);

    void fillBlockKlineAll__lastDay();


    void fillStockKline(String stockCode, UpdateTypeEnum updateTypeEnum);

    void fillStockKlineAll(UpdateTypeEnum updateTypeEnum);

    void fillStockKlineAll__lastDay();
}