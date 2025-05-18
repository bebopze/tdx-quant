package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.StockBlockInfoDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;

/**
 * @author: bebopze
 * @date: 2025/5/18
 */
public interface StockService {

    BaseStockDO info(String stockCode);

    StockBlockInfoDTO blockInfo(String stockCode);
}
