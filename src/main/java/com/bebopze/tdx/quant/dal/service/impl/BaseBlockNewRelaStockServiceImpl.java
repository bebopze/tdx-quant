package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.BlockNewTypeEnum;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.mapper.BaseBlockNewRelaStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseBlockNewRelaStockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
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


    @TotalTime
    @DBLimiter(1)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int batchInsert(List<BaseBlockNewRelaStockDO> list) {
        log.info("batchInsert     >>>     size : {}", ListUtil.size(list));


        int batchSize = 5000;
        if (list == null || list.isEmpty()) {
            return 0;
        }


        int count = 0;
        int size = list.size();

        for (int i = 0; i < size; i += batchSize) {
            int end = Math.min(i + batchSize, size);
            List<BaseBlockNewRelaStockDO> sub = list.subList(i, end);

            count += baseMapper.batchInsert(sub);
        }


        return count;
    }


}