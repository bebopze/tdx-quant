package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.Map;

/**
 * <p>
 * 期权-实时行情 服务类
 * </p>
 *
 * @author bebopze
 * @since 2026-11-16
 */
public interface IBaseOptionService extends IService<BaseOptionDO> {


    Long getIdByCode(String code);

    BaseOptionDO getByCode(String code);


    Map<String, Long> codeIdMap();


    int delAllKlineExtData();
}