package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.JsonFileWriterAndReader;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <p>
 * 板块/指数-实时行情（以 tdx 为准） 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Slf4j
@Service
public class BaseBlockServiceImpl extends ServiceImpl<BaseBlockMapper, BaseBlockDO> implements IBaseBlockService {


    @Override
    public Long getIdByCode(String code) {
        return baseMapper.getIdByCode(code);
    }

    @Override
    public BaseBlockDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }

    @Override
    public BaseBlockDO getSimpleByCode(String code) {
        return baseMapper.getSimpleByCode(code);
    }

    @Override
    public BaseBlockDO getSimpleById(Long id) {
        return baseMapper.getSimpleById(id);
    }


    @Override
    public Map<String, Long> codeIdMap() {

        List<BaseBlockDO> entityList = baseMapper.listAllSimple();


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseBlockDO::getCode,
                                                          BaseBlockDO::getId,

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
    }


    @Override
    public Map<String, Long> codeIdMap(Collection<String> blockCodeList) {

        List<BaseBlockDO> entityList = listSimpleByCodeList(blockCodeList);


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseBlockDO::getCode,  // 键：从对象中提取 code 字段
                                                          BaseBlockDO::getId,    // 值：从对象中提取 id 字段

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
    }

    @Override
    public Map<Long, String> idCodeMap() {
        List<BaseBlockDO> entityList = baseMapper.listAllSimple();


        Map<Long, String> id_code_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseBlockDO::getId,
                                                          BaseBlockDO::getCode,

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return id_code_map;
    }


    @Override
    public List<BaseBlockDO> listSimpleByCodeList(Collection<String> blockCodeList) {
        if (CollectionUtils.isEmpty(blockCodeList)) {
            return Collections.emptyList();
        }

        return baseMapper.listSimpleByCodeList(blockCodeList);
    }


    @Override
    public List<BaseBlockDO> listAllSimple() {
        return baseMapper.listAllSimple();
    }

    @Override
    public List<BaseBlockDO> listAllKline() {
        return listAllKline(false);
    }

    @TotalTime
    @Override
    public List<BaseBlockDO> listAllKline(boolean refresh) {
        log.info("listAllKline     >>>     refresh : {}", refresh);


        // listAllFromDiskCache >>> totalTime :6.9 s
        return listAllFromDiskCache(refresh);


        // return baseMapper.listAllKline();
    }


    @Override
    public List<BaseBlockDO> listAllRpsKline() {
        return baseMapper.listAllRpsKline();
    }


    // -----------------------------------------------------------------------------------------------------------------


    private List<BaseBlockDO> listAllFromDiskCache(boolean refresh) {
        long start = System.currentTimeMillis();


        // read Cache
        List<BaseBlockDO> list = JsonFileWriterAndReader.readLargeListFromFile___block_listAllKline();

        if (CollectionUtils.isEmpty(list) || list.size() < 850 || refresh) {
            list = baseMapper.listAllKline();


            // write Cache
//  TODO   先注释掉，等后面再开启           JsonFileWriterAndReader.writeLargeListToFile___block_listAllKline(list);
        }


        //  listAllFromDiskCache     >>>     totalTime : 6.9s
        log.info("listAllFromDiskCache     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start));
        return list;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @DBLimiter(12)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class,
                    SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    @Override
    public boolean updateById(BaseBlockDO entity) {
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
                    SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    public int batchInsert(List<BaseBlockDO> list) {
        log.info("batchInsert     >>>     size : {}", ListUtil.size(list));


        int batchSize = 1000;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<BaseBlockDO> sub = list.subList(i, end);

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
    public int batchInsertOrUpdate(List<BaseBlockDO> list) {
        log.info("batchInsertOrUpdate     >>>     size : {}", ListUtil.size(list));

        return saveOrUpdateBatch(list);


        // -------------------------------------------------------------------------------------------------------------
        // batchInsertOrUpdate “BUG”（INSERT INTO xxx   ON DUPLICATE KEY UPDATE）：自增ID 双倍跳跃  ->  每执行1次，自增ID x2
        // -------------------------------------------------------------------------------------------------------------


//        int batchSize = 1000;
//        if (list == null || list.isEmpty()) {
//            return 0;
//        }
//
//
//        int count = 0;
//        int size = list.size();
//
//        for (int i = 0; i < size; i += batchSize) {
//            int end = Math.min(i + batchSize, size);
//            List<BaseBlockDO> sub = list.subList(i, end);
//
//            count += baseMapper.batchInsertOrUpdate(sub);
//        }
//
//        return count;
    }


    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    public int saveOrUpdateBatch(List<BaseBlockDO> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }


        List<BaseBlockDO> insert_list = list.stream().filter(e -> e.getId() == null).toList();
        List<BaseBlockDO> update_list = list.stream().filter(e -> e.getId() != null).toList();


        long s1 = System.currentTimeMillis();
        batchInsert(insert_list);
        log.info("insert_list - batchInsert     >>>     insert_size : {} , totalTime : {}", insert_list.size(), DateTimeUtil.formatNow2Hms(s1));


        long s2 = System.currentTimeMillis();
        baseMapper.updateById(update_list);
        log.info("update_list - updateById     >>>     update_size : {} , totalTime : {}", update_list.size(), DateTimeUtil.formatNow2Hms(s2));


        return list.size();
    }


}