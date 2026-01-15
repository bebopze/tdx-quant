package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.dal.entity.QaTopBlockDO;
import com.bebopze.tdx.quant.dal.mapper.QaTopBlockMapper;
import com.bebopze.tdx.quant.dal.service.IQaTopBlockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 量化分析 - LV3主线板块（板块-月多2） 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-09-24
 */
@Service
public class QaTopBlockServiceImpl extends ServiceImpl<QaTopBlockMapper, QaTopBlockDO> implements IQaTopBlockService {


    @Override
    public Map<LocalDate, Long> dateIdMap() {

        List<QaTopBlockDO> entityList = baseMapper.listAllSimple();

        return entityList.stream().collect(
                Collectors.toMap(
                        QaTopBlockDO::getDate,
                        QaTopBlockDO::getId
                ));
    }


    @Override
    public QaTopBlockDO getByDate(LocalDate date) {
        return baseMapper.getByDate(date);
    }

    @TotalTime
    @Override
    public List<QaTopBlockDO> listByDate(LocalDate startDate, LocalDate endDate) {
        return baseMapper.listByDate(startDate, endDate);
    }


    @Override
    public List<QaTopBlockDO> lastN(LocalDate date, int N) {
        return baseMapper.lastN(date, N);
    }

    @Override
    public List<QaTopBlockDO> beforeAfterN(LocalDate date, int N) {
        date = date.plusDays(N / 5 * 7);
        return lastN(date, N);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class,
                    SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    @Override
    public int batchInsert(List<QaTopBlockDO> list) {

        int batchSize = 500;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<QaTopBlockDO> sub = list.subList(i, end);

            count += baseMapper.batchInsert(sub);
        }


        return count;
    }

    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class,
                    SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    @Override
    public int batchUpdate(List<QaTopBlockDO> list) {

        int batchSize = 500;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<QaTopBlockDO> sub = list.subList(i, end);

            count += baseMapper.batchUpdate(sub);
        }


        return count;
    }


    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class,
                    SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    @Override
    public int batchInsertOrUpdate(List<QaTopBlockDO> list) {
        int batchSize = 500;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<QaTopBlockDO> sub = list.subList(i, end);

            count += baseMapper.batchInsertOrUpdate(sub);
        }

        return count;
    }


}