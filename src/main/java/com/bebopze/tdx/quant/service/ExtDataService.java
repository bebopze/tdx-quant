package com.bebopze.tdx.quant.service;


/**
 * @author: bebopze
 * @date: 2025/5/24
 */
public interface ExtDataService {


    /**
     * 扩展数据（自定义 指标）  ->   板块 + 个股
     */
    void refreshExtDataAll();


    /**
     * 扩展数据（自定义 指标） - 个股
     */
    void calcStockExtData();


    /**
     * 扩展数据（自定义 指标） - 板块
     */
    void calcBlockExtData();
}
