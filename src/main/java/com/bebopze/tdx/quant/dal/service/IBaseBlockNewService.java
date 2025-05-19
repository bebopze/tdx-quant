package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * tdx - 自定义板块 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-11
 */
public interface IBaseBlockNewService extends IService<BaseBlockNewDO> {

    BaseBlockNewDO getByCode(String code);

    Long getIdByCode(String code);


    Map<String, Long> codeIdMap();

    Map<String, Long> codeIdMap(Collection<String> blockCodeList);


    List<BaseBlockNewDO> listSimpleByCodeList(Collection<String> blockCodeList);
}
