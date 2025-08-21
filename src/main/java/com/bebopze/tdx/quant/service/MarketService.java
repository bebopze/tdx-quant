package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;

import java.time.LocalDate;


/**
 * 大盘量化
 *
 * @author: bebopze
 * @date: 2025/7/21
 */
public interface MarketService {

    void importMarketMidCycle();

    QaMarketMidCycleDO marketInfo(LocalDate date);
}