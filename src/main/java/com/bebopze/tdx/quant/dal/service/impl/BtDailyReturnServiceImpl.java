package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.bebopze.tdx.quant.dal.mapper.BtDailyReturnMapper;
import com.bebopze.tdx.quant.dal.service.IBtDailyReturnService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 回测-每日收益率 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Service
public class BtDailyReturnServiceImpl extends ServiceImpl<BtDailyReturnMapper, BtDailyReturnDO> implements IBtDailyReturnService {


    @Override
    public BtDailyReturnDO getByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate) {
        return baseMapper.getByTaskIdAndTradeDate(taskId, tradeDate);
    }

    @Override
    public List<BtDailyReturnDO> listByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.listByTaskIdAndTradeDateRange(taskId, startDate, endDate);
    }

}