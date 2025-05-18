package com.bebopze.tdx.quant.dal.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockRelaBlockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockRelaBlockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseStockRelaBlockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;


/**
 * <p>
 * 股票-板块 关联 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class BaseStockRelaBlockServiceImpl extends ServiceImpl<BaseStockRelaBlockMapper, BaseStockRelaBlockDO> implements IBaseStockRelaBlockService {


    @Override
    public int deleteByBlockId(Long blockId) {

        return baseMapper.deleteByBlockId(blockId);


//        int count = baseMapper.delete(new LambdaQueryWrapper<BaseStockRelaBlockDO>()
//                                              .eq(BaseStockRelaBlockDO::getBlockId, blockId));
//
//        return count;
    }


    @Override
    public int deleteByStockId(Long stockId) {


        return baseMapper.deleteByStockId(stockId);


//        int count = baseMapper.delete(new LambdaQueryWrapper<BaseStockRelaBlockDO>()
//                                              .eq(BaseStockRelaBlockDO::getStockId, stockId));
//
//        return count;
    }

    @Override
    public int deleteAll() {
        return baseMapper.deleteAll();
    }


    @Override
    public List<BaseBlockDO> listBlockByStockCode(String stockCode) {
        return baseMapper.listBlockByStockCode(stockCode);
    }
}
