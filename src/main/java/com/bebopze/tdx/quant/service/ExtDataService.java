package com.bebopze.tdx.quant.service;


import com.bebopze.tdx.quant.common.constant.KlineDataEnum;

/**
 * 扩展数据（个股/板块 -> 指标计算）
 *
 * @author: bebopze
 * @date: 2025/5/24
 */
public interface ExtDataService {


    /**
     * 扩展数据（自定义 指标）  ->   板块 + 个股
     *
     * @param klineDataEnum 更新类型：近N日/全量更新
     */
    void refreshExtDataAll(KlineDataEnum klineDataEnum);


    /**
     * 扩展数据（自定义 指标） - 个股
     *
     * @param klineDataEnum 更新类型：近N日/全量更新
     * @param stockType     股票类型：1-A股；2-ETF；11-港股；12-美股；
     *                      （StockTypeEnum.type）
     */
    void calcStockExtData(KlineDataEnum klineDataEnum, Integer stockType);


    /**
     * 扩展数据（自定义 指标） - 板块
     *
     * @param klineDataEnum 更新类型：近N日/全量更新
     */
    void calcBlockExtData(KlineDataEnum klineDataEnum);

}