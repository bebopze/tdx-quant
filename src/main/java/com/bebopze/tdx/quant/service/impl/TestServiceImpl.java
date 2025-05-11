package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.TestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author: bebopze
 * @date: 2025/5/11
 */
@Slf4j
@Service
public class TestServiceImpl implements TestService {


    @Autowired
    private IBaseStockService iBaseStockService;


    @Override
    public void testSql() {

        iBaseStockService.test();


        System.out.println();
    }
}
