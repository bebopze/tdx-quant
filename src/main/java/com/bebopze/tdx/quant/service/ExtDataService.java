package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;


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
     * @param updateTypeEnum 更新类型：近N日/历史更新
     */
    void refreshExtDataAll(UpdateTypeEnum updateTypeEnum);


    /**
     * 扩展数据（自定义 指标） - 个股
     *
     * @param updateTypeEnum 更新类型：近N日/历史更新
     * @param stockType      股票类型：1-A股；2-ETF；11-港股；12-美股；
     *                       （StockTypeEnum.type）
     */
    void calcStockExtData(UpdateTypeEnum updateTypeEnum, Integer stockType);


    /**
     * 扩展数据（自定义 指标） - 板块
     *
     * @param updateTypeEnum 更新类型：近N日/历史更新
     */
    void calcBlockExtData(UpdateTypeEnum updateTypeEnum);

}