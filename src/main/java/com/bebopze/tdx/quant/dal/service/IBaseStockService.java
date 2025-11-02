package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * <p>
 * 股票-实时行情 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
public interface IBaseStockService extends IService<BaseStockDO> {

    Long getIdByCode(String code);

    BaseStockDO getByCode(String code);

    BaseStockDO getSimpleByCode(String code);


    Map<String, Set<String>> market_stockCodePrefixList_map(int type, int N);


    List<BaseStockDO> listByCodeList(Collection<String> codeList);

    List<BaseStockDO> listSimpleByCodeList(Collection<String> codeList);

    Map<String, Long> codeIdMap(Collection<String> codeList);


    List<BaseStockDO> listAllKline();

    List<BaseStockDO> listAllKline(boolean refresh);

    List<BaseStockDO> listAllETFKline();


    List<BaseStockDO> listAllSimple();

    Map<String, Long> codeIdMap();


    // -----------------------------------------------------------------------------------------------------------------


    boolean updateById(BaseStockDO entity);
}