package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
 * 板块/指数-实时行情（以 tdx 为准） 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseBlockServiceImpl extends ServiceImpl<BaseBlockMapper, BaseBlockDO> implements IBaseBlockService {


    @Override
    public BaseBlockDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }


    @Override
    public Long getIdByCode(String code) {
        return baseMapper.getIdByCode(code);
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
    public List<BaseBlockDO> listSimpleByCodeList(Collection<String> blockCodeList) {
        if (CollectionUtils.isEmpty(blockCodeList)) {
            return Collections.emptyList();
        }

        return baseMapper.listSimpleByCodeList(blockCodeList);
    }

    @Override
    public List<BaseBlockDO> listAllKline() {
        return baseMapper.listAllKline();
    }

}