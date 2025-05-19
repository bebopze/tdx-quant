package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.indicator.Fun1;
import com.bebopze.tdx.quant.service.StrategyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author: bebopze
 * @date: 2025/5/20
 */
@Slf4j
@Service
public class StrategyServiceImpl implements StrategyService {


    @Override
    public void breakSell() {


        // 持仓
        QueryCreditNewPosResp queryCreditNewPosResp = EastMoneyTradeAPI.queryCreditNewPosV2();

        List<CcStockInfo> stocks = queryCreditNewPosResp.getStocks();


        stocks.forEach(stock -> {

            //
            String stockCode = stock.getStkcode();

            Fun1 fun = new Fun1(stockCode);


        });
    }

}
