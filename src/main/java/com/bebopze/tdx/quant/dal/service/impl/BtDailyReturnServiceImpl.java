package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.bebopze.tdx.quant.dal.mapper.BtDailyReturnMapper;
import com.bebopze.tdx.quant.dal.service.IBtDailyReturnService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
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
@Slf4j
@Service
public class BtDailyReturnServiceImpl extends ServiceImpl<BtDailyReturnMapper, BtDailyReturnDO> implements IBtDailyReturnService {


    @TotalTime
    @Override
    public BtDailyReturnDO getByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate) {
        return baseMapper.getByTaskIdAndTradeDate(taskId, tradeDate);
    }

    @Override
    public BtDailyReturnDO lastByTaskId(Long taskId) {
        return baseMapper.lastByTaskId(taskId);
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

    @Override
    public int deleteByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.deleteByTaskIdAndTradeDateRange(taskId, startDate, endDate);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @DBLimiter(6)
    @Transactional(rollbackFor = Exception.class)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class,
                    SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    @Override
    public boolean retrySave(BtDailyReturnDO entity) {
        return this.save(entity);
    }


    // -----------------------------------------------------------------------------------------------------------------


}