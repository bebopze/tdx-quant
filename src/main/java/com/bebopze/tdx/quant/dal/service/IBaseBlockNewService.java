package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
