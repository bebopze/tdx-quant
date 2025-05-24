package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockRelaStockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockRelaStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
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
public class BaseBlockRelaStockServiceImpl extends ServiceImpl<BaseBlockRelaStockMapper, BaseBlockRelaStockDO> implements IBaseBlockRelaStockService {


    @Override
    public int deleteByBlockId(Long blockId) {

        return baseMapper.deleteByBlockId(blockId);
    }


    @Override
    public int deleteByStockId(Long stockId) {
        return baseMapper.deleteByStockId(stockId);
    }

    @Override
    public int deleteAll() {
        return baseMapper.deleteAll();
    }


    @Override
    public List<BaseBlockDO> listBlockByStockCode(String stockCode) {
        return baseMapper.listBlockByStockCode(stockCode);
    }

    @Override
    public List<BaseBlockDO> listBlockByStockCodeList(List<String> stockCodeList) {
        return baseMapper.listBlockByStockCodeList(stockCodeList);
    }

    @Override
    public List<BaseStockDO> listStockByBlockCodeList(List<String> blockCodeList) {
        return baseMapper.listStockByBlockCodeList(blockCodeList);
    }

}