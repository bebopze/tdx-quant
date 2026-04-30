package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.bebopze.tdx.quant.dal.mapper.BtPositionRecordMapper;
import com.bebopze.tdx.quant.dal.service.IBtPositionRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
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


    @TotalTime
    @Override
    public List<BtPositionRecordDO> listByTaskIdAndTradeDateAndPosType(Long taskId,
                                                                       LocalDate tradeDate,
                                                                       Integer positionType) {

        return baseMapper.listByTaskIdAndTradeDateAndPosType(taskId, tradeDate, positionType);
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
                    MysqlDataTruncation.class, SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    @Override
    public boolean retryBatchSave(List<BtPositionRecordDO> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }

        return this.saveBatch(entityList);
    }


    // -----------------------------------------------------------------------------------------------------------------


}