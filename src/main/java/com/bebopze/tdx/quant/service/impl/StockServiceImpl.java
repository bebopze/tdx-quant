package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: bebopze
 * @date: 2025/5/18
 */
@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private IBaseStockService baseStockService;


    @Override
    public BaseStockDO info(String stockCode) {
        return baseStockService.getByCode(stockCode);
    }
}
