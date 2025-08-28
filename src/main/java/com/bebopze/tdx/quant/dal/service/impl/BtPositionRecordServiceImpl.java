package com.bebopze.tdx.quant.dal.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.bebopze.tdx.quant.dal.mapper.BtPositionRecordMapper;
import com.bebopze.tdx.quant.dal.service.IBtPositionRecordService;
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
 * 回测-每日持仓记录 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Slf4j
@Service
public class BtPositionRecordServiceImpl extends ServiceImpl<BtPositionRecordMapper, BtPositionRecordDO> implements IBtPositionRecordService {


    @Override
    public List<BtPositionRecordDO> listByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate) {
        return baseMapper.listByTaskIdAndTradeDate(taskId, tradeDate);
    }

    @Override
    public List<BtPositionRecordDO> listByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.listByTaskIdAndTradeDateRange(taskId, startDate, endDate);
    }

    @Override
    public List<BtPositionRecordDO> listByTaskId(Long taskId) {
        return listByTaskIdAndTradeDateRange(taskId, null, null);
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
    public boolean retryBatchSave(List<BtPositionRecordDO> entityList) {

        try {
            return this.saveBatch(entityList);

        } catch (Exception ex) {

            log.error("positionRecord saveBatch - err     >>>     taskId : {} , tradeDate : {} , size : {} , entityList : {} , errMsg : {}",
                      entityList.get(0).getTaskId(), entityList.get(0).getTradeDate(), entityList.size(), JSON.toJSONString(entityList), ex.getMessage(), ex);

            throw ex;
        }

    }

}