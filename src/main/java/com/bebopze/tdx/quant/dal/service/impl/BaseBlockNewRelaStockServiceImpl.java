package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.constant.BlockNewTypeEnum;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewRelaStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockNewRelaStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockNewRelaStockService;
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
public class BaseBlockNewRelaStockServiceImpl extends ServiceImpl<BaseBlockNewRelaStockMapper, BaseBlockNewRelaStockDO> implements IBaseBlockNewRelaStockService {


    @Override
    public int delByBlockNewId(Long blockNewId) {
        return baseMapper.delByBlockNewId(blockNewId);
    }

    @Override
    public int deleteAll() {
        return baseMapper.deleteAll();
    }

    @Override
    public List<BaseBlockNewDO> listByStockCode(String stockCode, Integer type) {
        return baseMapper.listByStockCode(stockCode, type);
    }

    @Override
    public List<BaseStockDO> listStockByBlockNewCodeList(List<String> blockNewCodeList) {
        return baseMapper.listStockByBlockNewCodeList(blockNewCodeList, BlockNewTypeEnum.STOCK.getType());
    }

    @Override
    public List<BaseBlockDO> listBlockByBlockNewCodeList(List<String> blockNewCodeList) {
        return baseMapper.listBlockByBlockNewCodeList(blockNewCodeList, BlockNewTypeEnum.BLOCK.getType());
    }

}