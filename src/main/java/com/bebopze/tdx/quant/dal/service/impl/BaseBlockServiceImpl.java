package com.bebopze.tdx.quant.dal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // return baseMapper.getByCode(code);


        BaseBlockDO entity = baseMapper.selectOne(new LambdaQueryWrapper<BaseBlockDO>()
                                                          .eq(BaseBlockDO::getCode, code));
        return entity;
    }


    @Override
    public Long getIdByCode(String code) {
        return baseMapper.getIdByCode(code);


//        BaseBlockDO entity = baseMapper.selectOne(new LambdaQueryWrapper<BaseBlockDO>()
//                                                          .select(BaseBlockDO::getId)
//                                                          .eq(BaseBlockDO::getCode, code)
//                                                          .last("LIMIT 1"));
//
//        return entity == null ? null : entity.getId();
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

}