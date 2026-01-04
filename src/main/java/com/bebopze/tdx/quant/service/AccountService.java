package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.dal.entity.ConfAccountDO;


/**
 * @author: bebopze
 * @date: 2025/9/23
 */
public interface AccountService {

    Long create(ConfAccountDO entity);

    void delete(Long id);

    void update(ConfAccountDO entity);

    ConfAccountDO info(Long id);
}