package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.bebopze.tdx.quant.dal.mapper.BaseOptionMapper;
import com.bebopze.tdx.quant.dal.service.IBaseOptionService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

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
    public BaseOptionDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }


}