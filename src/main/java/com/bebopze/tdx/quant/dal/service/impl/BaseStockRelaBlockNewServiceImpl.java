package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockRelaBlockNewDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockRelaBlockNewMapper;
import com.bebopze.tdx.quant.dal.service.IBaseStockRelaBlockNewService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 股票-自定义板块 关联 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseStockRelaBlockNewServiceImpl extends ServiceImpl<BaseStockRelaBlockNewMapper, BaseStockRelaBlockNewDO> implements IBaseStockRelaBlockNewService {


    @Override
    public int delByBlockNewId(Long blockNewId) {
        return baseMapper.delByBlockNewId(blockNewId);
    }

    @Override
    public int deleteAll() {
        return baseMapper.deleteAll();
    }

    @Override
    public List<BaseBlockNewDO> listBlockByStockCode(String stockCode) {
        return baseMapper.listBlockByStockCode(stockCode);
    }

}
