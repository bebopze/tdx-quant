package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;

/**
 * <p>
 * 量化分析 - 大盘中期顶底 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-07-21
 */
public interface IQaMarketMidCycleService extends IService<QaMarketMidCycleDO> {

    int deleteAll();

    QaMarketMidCycleDO getByDate(LocalDate date);

    QaMarketMidCycleDO last();
}