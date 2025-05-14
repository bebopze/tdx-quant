package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 股票-实时行情 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
public interface IBaseStockService extends IService<BaseStockDO> {

    void test();

    BaseStockDO getByCode(String code);

    Long getIdByCode(String code);

    Map<String, List<String>> market_stockCodePrefixList_map();


    List<BaseStockDO> listSimpleByCodeList(Collection<String> stockCodeList);

    Map<String, Long> codeIdMap(Collection<String> stockCodeList);


    Map<String, Long> codeIdMap();
}
