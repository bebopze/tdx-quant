package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.base.BaseStockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.StockBlockInfoDTO;


/**
 * @author: bebopze
 * @date: 2025/5/18
 */
public interface StockService {

    BaseStockDTO info(String stockCode);

    StockBlockInfoDTO blockInfo(String stockCode);
}