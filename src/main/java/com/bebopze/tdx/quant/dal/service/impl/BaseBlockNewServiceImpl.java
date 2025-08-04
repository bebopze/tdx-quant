package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockNewMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockNewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <p>
 * tdx - 自定义板块 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-11
 */
@Service
public class BaseBlockNewServiceImpl extends ServiceImpl<BaseBlockNewMapper, BaseBlockNewDO> implements IBaseBlockNewService {


    @Override
    public Long getIdByCode(String code) {
        return baseMapper.getIdByCode(code);
    }

    @Override
    public BaseBlockNewDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }


    @Override
    public Map<String, Long> codeIdMap() {

        List<BaseBlockNewDO> entityList = baseMapper.listAllSimple();

        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseBlockNewDO::getCode,
                                                          BaseBlockNewDO::getId,

                                                          (existingKey, newKey) -> existingKey
                                                  ));

        return code_id_map;
    }


    @Override
    public Map<String, Long> codeIdMap(Collection<String> blockCodeList) {

        List<BaseBlockNewDO> entityList = listSimpleByCodeList(blockCodeList);

        Map<String, Long> code_id_map = entityList.stream()
                                                  .collect(Collectors.toMap(
                                                          BaseBlockNewDO::getCode,
                                                          BaseBlockNewDO::getId,

                                                          (existingKey, newKey) -> existingKey
                                                  ));

        return code_id_map;
    }


    @Override
    public List<BaseBlockNewDO> listSimpleByCodeList(Collection<String> blockCodeList) {
        return baseMapper.listSimpleByCodeList(blockCodeList);
    }


}