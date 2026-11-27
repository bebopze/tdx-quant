package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.dal.entity.BaseOptionUnderlyingDO;
import com.bebopze.tdx.quant.dal.mapper.BaseOptionUnderlyingMapper;
import com.bebopze.tdx.quant.dal.service.IBaseOptionUnderlyingService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation;
import lombok.extern.slf4j.Slf4j;
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
 * 期权标的-实时行情 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2026-11-28
 */
@Slf4j
@Service
public class BaseOptionUnderlyingServiceImpl extends ServiceImpl<BaseOptionUnderlyingMapper, BaseOptionUnderlyingDO> implements IBaseOptionUnderlyingService {


    @Override
    public Long getIdByCode(String code) {
        return baseMapper.getIdByCode(code);
    }


    @Override
    public BaseOptionUnderlyingDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }


    @Override
    public List<BaseOptionUnderlyingDO> listSimpleByKeyword(String keyword) {
        return baseMapper.listSimpleByKeyword(keyword);
    }


    @Override
    public Map<String, Long> codeIdMap() {

        List<BaseOptionUnderlyingDO> entityList = baseMapper.listAllSimple();


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseOptionUnderlyingDO::getCode,
                                                          BaseOptionUnderlyingDO::getId,

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
    }


    @Override
    public Map<String, Long> codeIdMapNotExpired() {


        List<BaseOptionUnderlyingDO> entityList = baseMapper.listAllSimple();


        LocalDate date = LocalDate.now();
        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseOptionUnderlyingDO::getCode,
                                                          BaseOptionUnderlyingDO::getId,

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @DBLimiter(12)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class,
                    MysqlDataTruncation.class, SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    @Override
    public boolean updateById(BaseOptionUnderlyingDO entity) {
        return super.updateById(entity);
    }


    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class,
                    MysqlDataTruncation.class, SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    public int batchInsert(List<BaseOptionUnderlyingDO> list) {
        log.info("batchInsert     >>>     size : {}", ListUtil.size(list));


        int batchSize = 1000;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<BaseOptionUnderlyingDO> sub = list.subList(i, end);

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
                    MysqlDataTruncation.class, SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    public int batchInsertOrUpdate(List<BaseOptionUnderlyingDO> list) {
        log.info("batchInsertOrUpdate     >>>     size : {}", ListUtil.size(list));

        return baseMapper.batchInsertOrUpdate(list);
    }


    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    public int saveOrUpdateBatch(List<BaseOptionUnderlyingDO> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }


        List<BaseOptionUnderlyingDO> insert_list = list.stream().filter(e -> e.getId() == null).toList();
        List<BaseOptionUnderlyingDO> update_list = list.stream().filter(e -> e.getId() != null).toList();


        long s1 = System.currentTimeMillis();
        batchInsert(insert_list);
        log.info("insert_list - batchInsert     >>>     insert_size : {} , totalTime : {}", insert_list.size(), DateTimeUtil.formatNow2Hms(s1));


        long s2 = System.currentTimeMillis();
        baseMapper.updateById(update_list);
        log.info("update_list - updateById     >>>     update_size : {} , totalTime : {}", update_list.size(), DateTimeUtil.formatNow2Hms(s2));


        return list.size();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public int delAllKlineExtData() {

        int count = baseMapper.delAllKlineExtData();
        log.info("delAllKlineExtData - block     >>>     count : {}", count);

        return count;
    }


}
