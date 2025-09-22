package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.dal.entity.CfgAccountDO;


/**
 * @author: bebopze
 * @date: 2025/9/23
 */
public interface AccountService {

    Long create(CfgAccountDO entity);

    void delete(Long id);

    void update(CfgAccountDO entity);

    CfgAccountDO info(Long id);
}