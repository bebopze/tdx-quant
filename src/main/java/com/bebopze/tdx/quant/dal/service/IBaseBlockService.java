package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 板块/指数-实时行情（以 tdx 为准） 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
public interface IBaseBlockService extends IService<BaseBlockDO> {


    Long getIdByCode(String code);

    BaseBlockDO getByCode(String code);

    BaseBlockDO getSimpleById(Long id);


    Map<String, Long> codeIdMap();

    Map<String, Long> codeIdMap(Collection<String> blockCodeList);


    List<BaseBlockDO> listSimpleByCodeList(Collection<String> blockCodeList);


    /**
     * 细分行业 + 概念板块
     *
     * @return
     */
    List<BaseBlockDO> listAllRpsKline();

    List<BaseBlockDO> listAllKline();

    List<BaseBlockDO> listAllSimple();
}
