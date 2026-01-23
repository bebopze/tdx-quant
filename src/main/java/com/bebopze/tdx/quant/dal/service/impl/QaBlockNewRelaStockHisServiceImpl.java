package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.dal.entity.QaBlockNewRelaStockHisDO;
import com.bebopze.tdx.quant.dal.mapper.QaBlockNewRelaStockHisMapper;
import com.bebopze.tdx.quant.dal.service.IQaBlockNewRelaStockHisService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;


/**
 * <p>
 * 量化分析 - 每日 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-07-14
 */
@Slf4j
@Service
public class QaBlockNewRelaStockHisServiceImpl extends ServiceImpl<QaBlockNewRelaStockHisMapper, QaBlockNewRelaStockHisDO> implements IQaBlockNewRelaStockHisService {


    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteAll(Integer blockNewId, LocalDate date) {
        return baseMapper.deleteAll(blockNewId, date);
    }

    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteByDateSet(Integer blockNewId, Set<LocalDate> dateSet) {
        log.info("deleteByDateSet     >>>     size : {}", ListUtil.size(dateSet));


        int batchSize = 500;
        List<LocalDate> list = Lists.newArrayList(dateSet);
        if (list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<LocalDate> sub = list.subList(i, end);

            count += baseMapper.deleteByBlockNewIdAndDateSet(blockNewId, sub);
        }


        return count;
    }


    @TotalTime
    // @Cacheable(value = "listByBlockNewIdDateAndLimit", key = "#blockNewId + '_' + #date + '_' + #limit", sync = true)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 3000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class}              // 排除业务异常
    )
    @Override
    public List<QaBlockNewRelaStockHisDO> listByBlockNewIdDateAndLimit(Integer blockNewId, LocalDate date, int limit) {
        return baseMapper.listByBlockNewIdDateAndLimit(blockNewId, date, limit);
    }


    @Override
    public QaBlockNewRelaStockHisDO last() {
        // 默认倒序
        List<QaBlockNewRelaStockHisDO> listAll = listByBlockNewIdDateAndLimit(1, LocalDate.of(9999, 1, 1), 1);
        return ListUtil.first(listAll);
    }


    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchInsert(List<QaBlockNewRelaStockHisDO> list) {
        log.info("batchInsert     >>>     size : {}", ListUtil.size(list));


        int batchSize = 1000;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<QaBlockNewRelaStockHisDO> sub = list.subList(i, end);

            count += baseMapper.batchInsert(sub);
        }


        return count;
    }


}