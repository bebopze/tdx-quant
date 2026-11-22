package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseOptionMapper;
import com.bebopze.tdx.quant.dal.service.IBaseOptionService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

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


    @Override
    public int delAllKlineExtData() {
        return baseMapper.delAllKlineExtData();
    }


}