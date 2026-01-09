package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockRelaStockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockRelaStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return blockCodeList.isEmpty() ? Collections.emptyList() : baseMapper.listByBlockCodeList(blockCodeList);
    }

    @Override
    public List<BaseBlockRelaStockDO> listByStockCodeList(Collection<String> stockCodeList) {
        return stockCodeList.isEmpty() ? Collections.emptyList() : baseMapper.listByStockCodeList(stockCodeList);
    }


    @Override
    public List<BaseBlockDO> listBlockByStockCode(String stockCode) {
        return stockCode.isEmpty() ? Collections.emptyList() : baseMapper.listBlockByStockCode(stockCode);
    }

    @Override
    public List<BaseBlockDO> listBlockByStockCodeList(Collection<String> stockCodeList) {
        return stockCodeList.isEmpty() ? Collections.emptyList() : baseMapper.listBlockByStockCodeList(stockCodeList);
    }

    @Override
    public List<BaseStockDO> listStockByBlockCode(String blockCode) {
        return listStockByBlockCodeList(Lists.newArrayList(blockCode));
    }

    @Override
    public List<BaseStockDO> listStockByBlockCodeList(Collection<String> blockCodeList) {
        return blockCodeList.isEmpty() ? Collections.emptyList() : baseMapper.listStockByBlockCodeList(blockCodeList);
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


    @TotalTime
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchInsert(List<BaseBlockRelaStockDO> list) {

        int batchSize = 5000;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<BaseBlockRelaStockDO> sub = list.subList(i, end);

            // 批量插入 优化后：5W条插入     =>     耗时：8min（saveBatch  [Mybatis-plus]）  ->   耗时：1.5s（batchInsert  [手写SQL]）
            count += baseMapper.batchInsert(sub);
        }


        return count;
    }

}