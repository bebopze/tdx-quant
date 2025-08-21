package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.bebopze.tdx.quant.dal.mapper.BtDailyReturnMapper;
import com.bebopze.tdx.quant.dal.service.IBtDailyReturnService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bebopze.tdx.quant.dal.service.IBtTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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


    @Lazy
    @Autowired
    private IBtTaskService taskService;


    @Override
    public BtDailyReturnDO getByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate) {
        return baseMapper.getByTaskIdAndTradeDate(taskId, tradeDate);
    }

    @Override
    public List<BtDailyReturnDO> listByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.listByTaskIdAndTradeDateRange(taskId, startDate, endDate);
    }

    @Override
    public List<BtDailyReturnDO> listByTaskId(Long taskId) {
        return listByTaskIdAndTradeDateRange(taskId, null, null);
    }


    @Deprecated
    @Override
    public List<Long> listTaskIdByBatchNoAndTotalDaysAndLeDailyReturn(Integer batchNo,
                                                                      int totalDays,
                                                                      Double dailyReturn) {

        return baseMapper.listTaskIdByBatchNoAndTotalDaysAndLeDailyReturn(batchNo, totalDays, dailyReturn);
    }

    @Override
    public int deleteByTaskIds(List<Long> taskIdList) {
        return baseMapper.deleteByTaskIds(taskIdList);
    }


}