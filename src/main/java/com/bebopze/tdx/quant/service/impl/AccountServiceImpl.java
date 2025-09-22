package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.dal.entity.CfgAccountDO;
import com.bebopze.tdx.quant.dal.service.ICfgAccountService;
import com.bebopze.tdx.quant.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;


/**
 * 配置-持仓账户
 *
 * @author: bebopze
 * @date: 2025/9/23
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {


    @Autowired
    private ICfgAccountService cfgAccountService;


    @Override
    public Long create(CfgAccountDO entity) {
        cfgAccountService.save(entity);
        return entity.getId();
    }

    @Override
    public void delete(Long id) {
        cfgAccountService.removeById(id);
    }

    @Override
    public void update(CfgAccountDO entity) {
        Assert.isTrue(entity != null && entity.getId() != null, "id不能为空");

        cfgAccountService.updateById(entity);
    }

    @Override
    public CfgAccountDO info(Long id) {
        return cfgAccountService.getById(id);
    }

}