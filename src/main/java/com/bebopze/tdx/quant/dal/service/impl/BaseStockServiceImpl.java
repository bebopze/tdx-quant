package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.JsonFileWriterAndReader;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockMapper;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * <p>
 * 股票-实时行情 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Slf4j
@Service
public class BaseStockServiceImpl extends ServiceImpl<BaseStockMapper, BaseStockDO> implements IBaseStockService {


    @Autowired
    private BaseBlockMapper baseBlockMapper;


    /**
     * 添加手动注入的方法（因为 baseMapper 是 protected）
     *
     * @param mapper
     */
    public void injectMapper(BaseStockMapper mapper) {
        this.baseMapper = mapper;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public Long getIdByCode(String code) {
        return baseMapper.getIdByCode(code);
    }


    @Override
    public BaseStockDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }

    @Override
    public BaseStockDO getSimpleByCode(String code) {
        return baseMapper.getSimpleByCode(code);
    }


    @Override
    public Map<String, Set<String>> market_stockCodePrefixList_map(int type, int N) {
        Map<String, Set<String>> market_stockCodePrefixList_map = Maps.newHashMap();


        // 1-股票；2-ETF；
        if (type == 1 || type == 2) {
            List<BaseStockDO> baseStockDOList = listAllSimple();
            baseStockDOList.stream()
                           .filter(e -> Objects.equals(type, e.getType()))
                           .forEach(e -> {

                               String tdxMarketType = e.getTdxMarketType().toString();
                               String code_prefix = e.getCode().substring(0, N);

                               market_stockCodePrefixList_map.computeIfAbsent(tdxMarketType, k -> Sets.newHashSet()).add(code_prefix);
                           });
        }

        // 3-板块
        else if (type == 3) {
            List<BaseBlockDO> baseBlockDOList = baseBlockMapper.listAllSimple();
            baseBlockDOList.forEach(e -> {

                String tdxBlockType = e.getTypeDesc();
                String code_prefix = e.getCode().substring(0, N);

                market_stockCodePrefixList_map.computeIfAbsent(tdxBlockType, k -> Sets.newHashSet()).add(code_prefix);
            });
        }


        return market_stockCodePrefixList_map;
    }

    @Override
    public List<BaseStockDO> listByCodeList(Collection<String> codeList) {
        if (CollectionUtils.isEmpty(codeList)) {
            return Collections.emptyList();
        }

        return baseMapper.listByCodeList(codeList);
    }


    @Override
    public List<BaseStockDO> listSimpleByCodeList(Collection<String> codeList) {
        if (CollectionUtils.isEmpty(codeList)) {
            return Collections.emptyList();
        }

        return baseMapper.listSimpleByCodeList(codeList);
    }


    @Override
    public Map<String, Long> codeIdMap(Collection<String> codeList) {

        List<BaseStockDO> entityList = listSimpleByCodeList(codeList);


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseStockDO::getCode,  // 键：从对象中提取 code 字段
                                                          BaseStockDO::getId,    // 值：从对象中提取 id 字段

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
    }


    @Override
    public List<BaseStockDO> listAllKline(Integer type) {
        return listAllKline(type, false);
    }


    @TotalTime
    @Override
    public List<BaseStockDO> listAllKline(Integer type, boolean refresh) {
        return listAllKline_0(type, refresh)
                .stream()
                .filter(e -> type == null || Objects.equals(type, e.getType())).
                collect(Collectors.toList());
    }

    private List<BaseStockDO> listAllKline_0(Integer type, boolean refresh) {
        log.info("listAllKline     >>>     type : {} , refresh : {}", type, refresh);


        // listAllFromDiskCache >>> totalTime :6.9 s
        return listAllFromDiskCache(refresh);


        // listByCursor     >>>     totalTime : 52.4s
        // return listByCursor();


        // listAllKline     >>>     totalTime : 52.7s
        // return baseMapper.listAllKline();


        // 不可用（OFFSET  =>  全表顺序读 + 丢弃）
        // return listAllPageQuery();
    }


    @Override
    public List<BaseStockDO> listAllETFKline() {
        return baseMapper.listAllETFKline();
    }


    @Override
    public List<BaseStockDO> listAllSimple() {
        return baseMapper.listAllSimple();
    }


    @Override
    public Map<String, Long> codeIdMap() {

        List<BaseStockDO> entityList = listAllSimple();


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseStockDO::getCode,
                                                          BaseStockDO::getId,

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
    }

    @Override
    public Map<Long, String> idCodeMap() {
        List<BaseStockDO> entityList = listAllSimple();


        Map<Long, String> id_code_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseStockDO::getId,
                                                          BaseStockDO::getCode,

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return id_code_map;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * disk cache   =>   本地 file读取     ->     不走DB 网路IO
     *
     * @return
     */
    private List<BaseStockDO> listAllFromDiskCache(boolean refresh) {
        long start = System.currentTimeMillis();


        // read Cache
        List<BaseStockDO> list = JsonFileWriterAndReader.readLargeListFromFile___stock_listAllKline();

        if (CollectionUtils.isEmpty(list) || list.size() < 5500 || refresh) {
            list = listByCursor();


            // write Cache
//  TODO   先注释掉，等后面再开启           JsonFileWriterAndReader.writeLargeListToFile___stock_listAllKline(list);
        }


        //  listAllFromDiskCache     >>>     totalTime : 6.9s
        log.info("listAllFromDiskCache     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start));


        // 过滤ETF
        // return list.stream().filter(e -> e.getType().equals(StockTypeEnum.A_STOCK.type)).collect(Collectors.toList());
        return list;
    }


    private List<BaseStockDO> listByCursor() {
        long start = System.currentTimeMillis();


        Long lastId = 0L;
        int pageSize = 50;


        List<BaseStockDO> list = Lists.newArrayList();


        while (true) {
            long start_1 = System.currentTimeMillis();


            List<BaseStockDO> pageList = baseMapper.listByCursor(lastId, pageSize);
            if (pageList.isEmpty()) {
                break;
            }

            list.addAll(pageList);
            lastId = pageList.get(pageList.size() - 1).getId();


            log.info("listByCursor - page=[{}]     >>>     pageTime : {} , totalTime : {}",
                     list.size() / pageSize, DateTimeUtil.formatNow2Hms(start_1), DateTimeUtil.formatNow2Hms(start));
        }


        log.info("listByCursor     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start));
        return list;
    }


    /**
     * 不可用（OFFSET  =>  全表顺序读 + 丢弃）
     *
     * @return
     */
    private List<BaseStockDO> listAllPageQuery() {
        long start = System.currentTimeMillis();


        int pageNum = 1;
        int pageSize = 500;


        List<BaseStockDO> list = Lists.newArrayList();


        boolean hasNext = true;
        while (hasNext) {


            // OFFSET  =>  全表顺序读 + 丢弃
            PageHelper.startPage(pageNum++, pageSize);
            List<BaseStockDO> pageList = baseMapper.listAllKline();

            list.addAll(pageList);


            PageInfo<BaseStockDO> page = new PageInfo<>(pageList);
            hasNext = page.isHasNextPage();
        }


        log.info("listAllPageQuery     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start));
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
    public boolean updateById(BaseStockDO entity) {
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
    @Override
    public int batchInsert(List<BaseStockDO> list) {
        log.info("batchInsert     >>>     size : {}", ListUtil.size(list));


        int batchSize = 1000;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<BaseStockDO> sub = list.subList(i, end);

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
    public int batchInsertOrUpdate(List<BaseStockDO> list) {
        log.info("batchInsertOrUpdate     >>>     size : {}", ListUtil.size(list));


        int batchSize = 1000;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<BaseStockDO> sub = list.subList(i, end);

            count += baseMapper.batchInsertOrUpdate(sub);
        }

        return count;
    }


}