package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewRelaStockDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 股票-自定义板块 关联 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-11
 */
public interface IBaseBlockNewRelaStockService extends IService<BaseBlockNewRelaStockDO> {

    int delByBlockNewId(Long blockNewId);

    int deleteAll();


    List<BaseBlockNewDO> listByStockCode(String stockCode, Integer type);


    List<BaseStockDO> listStockByBlockNewCodeList(List<String> blockNewCodeList);

    List<BaseBlockDO> listBlockByBlockNewCodeList(List<String> blockNewCodeList);
}
