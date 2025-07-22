package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;

import java.time.LocalDate;

/**
 * @author: bebopze
 * @date: 2025/7/21
 */
public interface IndexService {

    void importMarketMidCycle();

    QaMarketMidCycleDO marketInfo(LocalDate date);
}