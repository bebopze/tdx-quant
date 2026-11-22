package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.base.BaseStockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.StockBlockInfoDTO;


/**
 * @author: bebopze
 * @date: 2025/5/18
 */
public interface StockService {


    /**
     * 股票行情（kline + ext_data） ->  支持 股票/板块/期权
     *
     * @param stockCode
     * @param updateType
     * @return
     */
    BaseStockDTO info(String stockCode, Integer updateType);


    /**
     * 个股-板块
     *
     * @param stockCode
     * @return
     */
    StockBlockInfoDTO blockInfo(String stockCode);

}