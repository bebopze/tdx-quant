package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockRelaStockDO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;

import java.util.Collection;
import java.util.List;
import java.util.Set;


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


    List<BaseBlockRelaStockDO> listByBlockCodeList(Collection<String> blockCodeList);

    List<BaseBlockRelaStockDO> listByStockCodeList(Collection<String> stockCodeList);


    List<BaseBlockDO> listBlockByStockCode(String stockCode);

    List<BaseBlockDO> listBlockByStockCodeList(Collection<String> stockCodeList);

    List<BaseStockDO> listStockByBlockCodeList(Collection<String> blockCodeList);


    /**
     * 板块-个股   =>   lv3级【end_level=1】   ->     3级-行业（普通/研究） + 概念板块
     *
     * @return
     */
    List<BaseBlockRelaStockDO> listAll();


    List<BaseStockDO> listETFByBlockCodes(Set<String> topBlockCodeSet);
}