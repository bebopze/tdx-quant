package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.dal.entity.ConfAccountDO;
import com.bebopze.tdx.quant.dal.service.IConfAccountService;
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
    private IConfAccountService confAccountService;


    @Override
    public Long create(ConfAccountDO entity) {
        confAccountService.save(entity);
        return entity.getId();
    }

    @Override
    public void delete(Long id) {
        confAccountService.removeById(id);
    }

    @Override
    public void update(ConfAccountDO entity) {
        Assert.isTrue(entity != null && entity.getId() != null, "id不能为空");

        confAccountService.updateById(entity);
    }

    @Override
    public ConfAccountDO info(Long id) {
        return confAccountService.getById(id);
    }

}