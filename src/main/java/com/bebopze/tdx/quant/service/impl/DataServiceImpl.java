package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.domain.dto.kline.DataInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.PropsUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaBlockNewRelaStockHisDO;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.dal.service.IQaBlockNewRelaStockHisService;
import com.bebopze.tdx.quant.dal.service.IQaMarketMidCycleService;
import com.bebopze.tdx.quant.service.DataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.bebopze.tdx.quant.common.constant.TdxConst.INDEX_BLOCK;


/**
 * Data
 *
 * @author: bebopze
 * @date: 2025/8/15
 */
@Slf4j
@Service
public class DataServiceImpl implements DataService {


    @Autowired
    private IBaseStockService stockService;

    @Autowired
    private IBaseBlockService blockService;

    @Autowired
    private IQaBlockNewRelaStockHisService qaBlockNewRelaStockHisService;

    @Autowired
    private IQaMarketMidCycleService qaMarketMidCycleService;


    @Override
    public DataInfoDTO dataInfo() {

        DataInfoDTO info = new DataInfoDTO();


        // stock
        stockDataInfo(info);


        // block
        blockDataInfo(info);


        // topBlock
        topBlockDataInfo(info);


        // market
        marketDataInfo(info);


        return info;
    }


    @Override
    public void eastmoneyRefreshSession(String validatekey, String cookie) {
        PropsUtil.refreshEastmoneySession(validatekey, cookie);
        EastMoneyTradeAPI.refreshEastmoneySession();
    }


    private void stockDataInfo(DataInfoDTO info) {

        BaseStockDO stockDO = stockService.getByCode("300059");

        KlineDTO klineDTO = ListUtil.last(stockDO.getKlineDTOList());
        ExtDataDTO extDataDTO = ListUtil.last(stockDO.getExtDataDTOList());


        info.setStock_tradeDate(stockDO.getTradeDate());
        info.setStock_updateTime(stockDO.getGmtModify());
        info.setStock_klineDTO(klineDTO);
        info.setStock_extDataDTO(extDataDTO);
    }

    private void blockDataInfo(DataInfoDTO info) {

        BaseBlockDO blockDO = blockService.getByCode(INDEX_BLOCK);

        KlineDTO klineDTO = ListUtil.last(blockDO.getKlineDTOList());
        ExtDataDTO extDataDTO = ListUtil.last(blockDO.getExtDataDTOList());


        info.setBlock_tradeDate(blockDO.getTradeDate());
        info.setBlock_updateTime(blockDO.getGmtModify());
        info.setBlock_klineDTO(klineDTO);
        info.setBlock_extDataDTO(extDataDTO);
    }

    private void topBlockDataInfo(DataInfoDTO info) {

        QaBlockNewRelaStockHisDO lastEntity = qaBlockNewRelaStockHisService.last();


        info.setTopBlock_tradeDate(lastEntity.getDate());
        info.setTopBlock_updateTime(lastEntity.getGmtModify());
    }


    private void marketDataInfo(DataInfoDTO info) {

        QaMarketMidCycleDO lastEntity = qaMarketMidCycleService.last();


        info.setMarket_tradeDate(lastEntity.getDate());
        info.setMarket_updateTime(lastEntity.getGmtModify());
    }


}
