package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.dal.mapper.QaMarketMidCycleMapper;
import com.bebopze.tdx.quant.dal.service.IQaMarketMidCycleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * <p>
 * 量化分析 - 大盘中期顶底 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-07-21
 */
@Service
public class QaMarketMidCycleServiceImpl extends ServiceImpl<QaMarketMidCycleMapper, QaMarketMidCycleDO> implements IQaMarketMidCycleService {

    @Override
    public int deleteAll() {
        return baseMapper.deleteAll();
    }

    @Override
    public QaMarketMidCycleDO getByDate(LocalDate date) {
        return baseMapper.getByDate(date);
    }

    @Override
    public QaMarketMidCycleDO last() {
        return baseMapper.last();
    }
}
