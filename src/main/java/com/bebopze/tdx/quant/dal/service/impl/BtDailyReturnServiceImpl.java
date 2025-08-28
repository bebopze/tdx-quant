package com.bebopze.tdx.quant.dal.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.bebopze.tdx.quant.dal.mapper.BtDailyReturnMapper;
import com.bebopze.tdx.quant.dal.service.IBtDailyReturnService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional(rollbackFor = Exception.class)
    @Retryable(
            value = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 90000),   // 最大90秒延迟
            exclude = {IllegalArgumentException.class, IllegalStateException.class}              // 排除业务异常
    )
    @Override
    public boolean retrySave(BtDailyReturnDO entity) {

        try {
            return this.save(entity);

        } catch (Exception ex) {

            log.error("dailyReturn save - err     >>>     taskId : {} , tradeDate : {} , entity : {} , errMsg : {}",
                      entity.getTaskId(), entity.getTradeDate(), JSON.toJSONString(entity), ex.getMessage(), ex);

            throw ex;
        }

    }


}