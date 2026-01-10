package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.domain.dto.kline.DataInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.PropsUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.service.DataService;
import jakarta.annotation.PostConstruct;
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

    @Autowired
    private IConfAccountService confAccountService;


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
        eastmoneyRefreshSession(validatekey, cookie, true);
    }

    private void eastmoneyRefreshSession(String validatekey, String cookie, boolean save2DB) {
        PropsUtil.refreshEastmoneySession(validatekey, cookie);
        EastMoneyTradeAPI.refreshEastmoneySession();


        // 写入DB
        if (save2DB) {
            ConfAccountDO entity = new ConfAccountDO();
            entity.setId(1L);
            entity.setValidatekey(validatekey);
            entity.setCookie(cookie);
            confAccountService.updateById(entity);
        }
    }


    @PostConstruct
    public void init__eastmoneySession() {
        String activeProfile = PropsUtil.getProperty("spring.profiles.active");

        try {
            QueryCreditNewPosResp posResp = EastMoneyTradeAPI.queryCreditNewPosV2();
            log.info("application-{}.yml     >>>     session 有效", activeProfile);

        } catch (BizException e) {
            log.warn("application-{}.yml     >>>     session 无效 - [{}]   ->   尝试从DB（conf_account）读取", activeProfile, e.getMsg());

            ConfAccountDO entity = confAccountService.getById(1L);
            eastmoneyRefreshSession(entity.getValidatekey(), entity.getCookie(), false);
        }
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
