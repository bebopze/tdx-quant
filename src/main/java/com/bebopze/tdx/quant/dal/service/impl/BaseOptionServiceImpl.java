package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.bebopze.tdx.quant.dal.mapper.BaseOptionMapper;
import com.bebopze.tdx.quant.dal.service.IBaseOptionService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <p>
 * 期权-实时行情 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2026-11-16
 */
@Slf4j
@Service
public class BaseOptionServiceImpl extends ServiceImpl<BaseOptionMapper, BaseOptionDO> implements IBaseOptionService {


    @Override
    public Long getIdByCode(String code) {
        return baseMapper.getIdByCode(code);
    }


    @Override
    public BaseOptionDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }


    @Override
    public List<BaseOptionDO> listSimpleByKeyword(String keyword) {
        return baseMapper.listSimpleByKeyword(keyword);
    }


    @Override
    public Map<String, Long> codeIdMap() {

        List<BaseOptionDO> entityList = baseMapper.listAllSimple();


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseOptionDO::getCode,
                                                          BaseOptionDO::getId,

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
    public boolean updateById(BaseOptionDO entity) {
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
    public int batchInsert(List<BaseOptionDO> list) {
        log.info("batchInsert     >>>     size : {}", ListUtil.size(list));


        int batchSize = 1000;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<BaseOptionDO> sub = list.subList(i, end);

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
    public int batchInsertOrUpdate(List<BaseOptionDO> list) {
        log.info("batchInsertOrUpdate     >>>     size : {}", ListUtil.size(list));

        return saveOrUpdateBatch(list);
    }


    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    public int saveOrUpdateBatch(List<BaseOptionDO> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }


        List<BaseOptionDO> insert_list = list.stream().filter(e -> e.getId() == null).toList();
        List<BaseOptionDO> update_list = list.stream().filter(e -> e.getId() != null).toList();


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