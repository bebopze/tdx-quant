package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockRelaStockDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


/**
 * <p>
 * 股票-板块 关联 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
public interface IBaseBlockRelaStockService extends IService<BaseBlockRelaStockDO> {

    int deleteByBlockId(Long blockId);

    int deleteByStockId(Long stockId);

    int deleteAll();


    List<BaseBlockDO> listBlockByStockCode(String stockCode);
}
