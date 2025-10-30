package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockRelaStockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockRelaStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * <p>
 * 股票-板块 关联 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Service
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
    public List<BaseBlockRelaStockDO> listByBlockCodeList(Collection<String> blockCodeList) {
        return baseMapper.listByBlockCodeList(blockCodeList);
    }

    @Override
    public List<BaseBlockRelaStockDO> listByStockCodeList(Collection<String> stockCodeList) {
        return baseMapper.listByStockCodeList(stockCodeList);
    }


    @Override
    public List<BaseBlockDO> listBlockByStockCode(String stockCode) {
        return baseMapper.listBlockByStockCode(stockCode);
    }

    @Override
    public List<BaseBlockDO> listBlockByStockCodeList(Collection<String> stockCodeList) {
        return baseMapper.listBlockByStockCodeList(stockCodeList);
    }

    @Override
    public List<BaseStockDO> listStockByBlockCodeList(Collection<String> blockCodeList) {
        return baseMapper.listStockByBlockCodeList(blockCodeList);
    }

    @Override
    public List<BaseBlockRelaStockDO> listAll() {
        return baseMapper.listAll();
    }

    @Override
    public List<BaseStockDO> listETFByBlockCodes(Set<String> topBlockCodeSet) {
        // TODO
        return Collections.emptyList();
    }

}