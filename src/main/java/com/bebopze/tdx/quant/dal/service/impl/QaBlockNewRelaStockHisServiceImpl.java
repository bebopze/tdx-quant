package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.QaBlockNewRelaStockHisDO;
import com.bebopze.tdx.quant.dal.mapper.QaBlockNewRelaStockHisMapper;
import com.bebopze.tdx.quant.dal.service.IQaBlockNewRelaStockHisService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 量化分析 - 每日 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-07-14
 */
@Service
public class QaBlockNewRelaStockHisServiceImpl extends ServiceImpl<QaBlockNewRelaStockHisMapper, QaBlockNewRelaStockHisDO> implements IQaBlockNewRelaStockHisService {


    @Override
    public int deleteAll(Integer blockNewId, LocalDate date) {
        return baseMapper.deleteAll(blockNewId, date);
    }


    // @Cacheable(value = "listByBlockNewIdDateAndLimit", key = "#blockNewId + '_' + #date + '_' + #limit", sync = true)
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            exclude = {IllegalArgumentException.class, IllegalStateException.class}              // 排除业务异常
    )
    @Override
    public List<QaBlockNewRelaStockHisDO> listByBlockNewIdDateAndLimit(Integer blockNewId, LocalDate date, int limit) {
        return baseMapper.listByBlockNewIdDateAndLimit(blockNewId, date, limit);
    }


    @Override
    public QaBlockNewRelaStockHisDO last() {
        return listByBlockNewIdDateAndLimit(1, LocalDate.of(9999, 1, 1), 1).get(0);
    }


}
