package com.bebopze.tdx.quant.dal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <p>
 * 股票-实时行情 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseStockServiceImpl extends ServiceImpl<BaseStockMapper, BaseStockDO> implements IBaseStockService {


    @Override
    public Long getIdByCode(String code) {
        return baseMapper.getIdByCode(code);
    }


    @Override
    public BaseStockDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }


    @Override
    public Map<String, List<String>> market_stockCodePrefixList_map() {
        Map<String, List<String>> market_stockCodePrefixList_map = Maps.newHashMap();


        // selectMaps 返回 List<Map<String,Object>>，然后映射成 DTO
        List<Map<String, Object>> rows = baseMapper.selectMaps(

                new QueryWrapper<BaseStockDO>()
                        // 指定 distinct 和 要查询的列
                        .select("DISTINCT   LEFT(code,3) AS code_prefix", "tdx_market_type")
                        .orderByAsc("tdx_market_type")
        );


        rows.forEach(row -> {

            String market_type = String.valueOf(row.get("tdx_market_type"));
            String code_prefix = (String) row.get("code_prefix");


            List<String> stockCodePrefixList = market_stockCodePrefixList_map.get(market_type);
            if (CollectionUtils.isEmpty(stockCodePrefixList)) {
                market_stockCodePrefixList_map.put(market_type, Lists.newArrayList(code_prefix));
            } else {
                stockCodePrefixList.add(code_prefix);
            }
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

}
