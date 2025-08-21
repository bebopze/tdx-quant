package com.bebopze.tdx.quant.service;


/**
 * 扩展数据（个股/板块 -> 指标计算）
 *
 * @author: bebopze
 * @date: 2025/5/24
 */
public interface ExtDataService {


    /**
     * 扩展数据（自定义 指标）  ->   板块 + 个股
     */
    void refreshExtDataAll(Integer N);


    /**
     * 扩展数据（自定义 指标） - 个股
     */
    void calcStockExtData(Integer N);


    /**
     * 扩展数据（自定义 指标） - 板块
     */
    void calcBlockExtData(Integer N);
}
