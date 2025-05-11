package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockNewMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockNewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


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
    public BaseBlockNewDO getByCode(String code) {
        return baseMapper.getByCode(code);
    }

}
