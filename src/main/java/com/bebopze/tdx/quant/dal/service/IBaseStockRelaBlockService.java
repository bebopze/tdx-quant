package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseStockRelaBlockDO;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * <p>
 * 股票-板块 关联 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
public interface IBaseStockRelaBlockService extends IService<BaseStockRelaBlockDO> {

    int deleteByBlockId(Long blockId);

    int deleteByStockId(Long stockId);
}
