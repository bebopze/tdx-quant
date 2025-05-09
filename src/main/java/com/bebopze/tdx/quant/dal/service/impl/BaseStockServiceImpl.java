package com.bebopze.tdx.quant.dal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * <p>
 * 股票-实时行情 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Service
public class BaseStockServiceImpl extends ServiceImpl<BaseStockMapper, BaseStockDO> implements IBaseStockService {


    @Override
    public void test() {
        BaseStockDO baseStockDO = baseMapper.selectById(1L);
        System.out.println(baseStockDO);
    }

    @Override
    public BaseStockDO getByCode(String code) {

        // return baseMapper.selectByCode(code);


        BaseStockDO entity = baseMapper.selectOne(new LambdaQueryWrapper<BaseStockDO>()
                                                          .eq(BaseStockDO::getCode, code));
        return entity;
    }

    @Override
    public Long getIdByCode(String code) {


        // return baseMapper.getIdByCode(code);


        BaseStockDO entity = baseMapper.selectOne(new LambdaQueryWrapper<BaseStockDO>()
                                                          .select(BaseStockDO::getId)
                                                          .eq(BaseStockDO::getCode, code)
                                                          .last("LIMIT 1"));

        return entity == null ? null : entity.getId();
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

}