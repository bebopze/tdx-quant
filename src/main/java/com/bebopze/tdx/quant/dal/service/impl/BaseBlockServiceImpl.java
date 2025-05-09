package com.bebopze.tdx.quant.dal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


/**
 * <p>
 * 板块/指数-实时行情（以 tdx 为准） 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Service
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


        // return baseMapper.getIdByCode(code);


        BaseBlockDO entity = baseMapper.selectOne(new LambdaQueryWrapper<BaseBlockDO>()
                                                          .select(BaseBlockDO::getId)
                                                          .eq(BaseBlockDO::getCode, code)
                                                          .last("LIMIT 1"));

        return entity == null ? null : entity.getId();
    }

}