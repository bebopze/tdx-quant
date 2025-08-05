package com.bebopze.tdx.quant.dal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.JsonFileWriterAndReader;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
    public Map<String, List<String>> market_stockCodePrefixList_map(int N) {
        Map<String, List<String>> market_stockCodePrefixList_map = Maps.newHashMap();


        // selectMaps 返回 List<Map<String,Object>>，然后映射成 DTO
        List<Map<String, Object>> rows = baseMapper.selectMaps(

                new QueryWrapper<BaseStockDO>()
                        // 指定 distinct 和 要查询的列
                        //.select("DISTINCT   LEFT(code,3) AS code_prefix", "tdx_market_type")
                        .select(String.format("DISTINCT   LEFT(code, %s) AS code_prefix", N), "tdx_market_type")
                        .orderByAsc("tdx_market_type")
        );


        rows.forEach(row -> {

            String market_type = String.valueOf(row.get("tdx_market_type"));
            String code_prefix = (String) row.get("code_prefix");


            market_stockCodePrefixList_map.computeIfAbsent(market_type, k -> Lists.newArrayList()).add(code_prefix);
        });


        return market_stockCodePrefixList_map;
    }


    @Override
    public List<BaseStockDO> listSimpleByCodeList(Collection<String> stockCodeList) {
        if (CollectionUtils.isEmpty(stockCodeList)) {
            return Collections.emptyList();
        }

        return baseMapper.listSimpleByCodeList(stockCodeList);
    }


    @Override
    public Map<String, Long> codeIdMap(Collection<String> stockCodeList) {

        List<BaseStockDO> entityList = listSimpleByCodeList(stockCodeList);


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseStockDO::getCode,  // 键：从对象中提取 code 字段
                                                          BaseStockDO::getId,    // 值：从对象中提取 id 字段

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
    }


    @Override
    // @Cacheable("stock_listAllKline")
    public List<BaseStockDO> listAllKline() {
        return listAllKline(false);
    }

    @Override
    // @Cacheable(value = "stock_listAllKline", unless = "#refresh == true")
    public List<BaseStockDO> listAllKline(boolean refresh) {


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
    public List<BaseStockDO> listAllSimple() {
        return baseMapper.listAllSimple();
    }


    @Override
    public Map<String, Long> codeIdMap() {

        List<BaseStockDO> entityList = baseMapper.listAllSimple();


        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseStockDO::getCode,
                                                          BaseStockDO::getId,

                                                          (existingKey, newKey) -> existingKey  // 合并函数：处理重复键（保留旧值）
                                                  ));

        return code_id_map;
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
        List<BaseStockDO> list = JsonFileWriterAndReader.readStringFromFile___stock_listAllKline();

        if (CollectionUtils.isEmpty(list) || refresh) {
            // list = baseMapper.listAllKline();
            list = listByCursor();


            // write Cache
            JsonFileWriterAndReader.writeStringToFile___stock_listAllKline(list);
        }


        //  listAllFromDiskCache     >>>     totalTime : 6.9s
        log.info("listAllFromDiskCache     >>>     totalTime : {}", DateTimeUtil.format2Hms(System.currentTimeMillis() - start));
        return list;
    }


    private List<BaseStockDO> listByCursor() {
        long start = System.currentTimeMillis();


        Long lastId = 0L;
        int pageSize = 50;


        List<BaseStockDO> list = Lists.newArrayList();


        while (true) {

            List<BaseStockDO> pageList = baseMapper.listByCursor(lastId, pageSize);
            if (pageList.isEmpty()) {
                break;
            }

            list.addAll(pageList);
            lastId = pageList.get(pageList.size() - 1).getId();
        }


        log.info("listByCursor     >>>     totalTime : {}", DateTimeUtil.format2Hms(System.currentTimeMillis() - start));
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


        log.info("listAllPageQuery     >>>     totalTime : {}", DateTimeUtil.format2Hms(System.currentTimeMillis() - start));
        return list;
    }


}