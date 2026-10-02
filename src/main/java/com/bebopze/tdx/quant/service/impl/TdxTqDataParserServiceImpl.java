package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.tdxfun.BlockKlineFun;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.service.ExtDataService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.TdxDataParserService;
import com.bebopze.tdx.quant.task.TdxTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;


/**
 * TQ数据 导入
 *
 * @author: bebopze
 * @date: 2026/10/1
 */
@Slf4j
@Service
public class TdxTqDataParserServiceImpl implements TdxDataParserService {


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewService baseBlockNewService;

    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    @Autowired
    private MarketService marketService;

    @Autowired
    private ExtDataService extDataService;

    @Autowired
    private InitDataService initDataService;


    @Autowired
    private TdxTask tdxTask;

    @Autowired
    private BlockKlineFun blockKlineFun;


    @Override
    public void importAll() {

    }

    @Override
    public void importAll__blockRelaStock() {

    }

    @Override
    public void importTdxBlockCfg() {

    }

    @Override
    public void importBlockReport() {

    }

    @Override
    public void importBlockNewReport() {

    }

    @Override
    public void importETF() {

    }

    @Override
    public void importHkStock() {

    }

    @Override
    public void importUsStock() {

    }

    @Override
    public void refreshKlineAll(UpdateTypeEnum updateTypeEnum) {

    }

    @Override
    public void fillBlockKline(String blockCode) {

    }

    @Override
    public void fillBlockKlineAll() {

    }

    @Override
    public void fillStockKline(String stockCode, Integer apiType, UpdateTypeEnum updateTypeEnum) {

    }

    @Override
    public void fillStockKlineAll(UpdateTypeEnum updateTypeEnum) {

    }

    @Override
    public Map<String, Set<String>> marketRelaStockCodePrefixList(int type, int N) {
        return Map.of();
    }

    @Override
    public void calcAndFillBlockKlineAll() {

    }


}