package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * <p>
 * 期权-实时行情 服务类
 * </p>
 *
 * @author bebopze
 * @since 2026-11-16
 */
public interface IBaseOptionService extends IService<BaseOptionDO> {


    BaseOptionDO getByCode(String optionCode);

}