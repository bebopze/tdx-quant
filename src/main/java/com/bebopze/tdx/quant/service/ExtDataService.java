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
     *
     * @param N null 或者 <=0   ->   全量更新
     *          >0             ->   增量更新（最近 N 日行情数据）
     */
    void refreshExtDataAll(Integer N);


    /**
     * 扩展数据（自定义 指标） - 个股
     *
     * @param N         null 或者 <=0   ->   全量更新
     *                  >0             ->   增量更新（最近 N 日行情数据）
     * @param stockType 股票类型：1-A股；2-ETF；
     *                  （StockTypeEnum.type）
     */
    void calcStockExtData(Integer N, Integer stockType);


    /**
     * 扩展数据（自定义 指标） - 板块
     *
     * @param N null 或者 <=0   ->   全量更新
     *          >0             ->   增量更新（最近 N 日行情数据）
     */
    void calcBlockExtData(Integer N);

}