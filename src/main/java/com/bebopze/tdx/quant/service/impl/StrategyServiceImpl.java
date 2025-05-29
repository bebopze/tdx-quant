package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.indicator.StockFunLast;
import com.bebopze.tdx.quant.service.StrategyService;
import com.bebopze.tdx.quant.strategy.buy.BuyStockStrategy;
import com.bebopze.tdx.quant.strategy.sell.DownMASellStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author: bebopze
 * @date: 2025/5/20
 */
@Slf4j
@Service
public class StrategyServiceImpl implements StrategyService {


    @Autowired
    private BuyStockStrategy buyStrategy;

    @Autowired
    private DownMASellStrategy sellStrategy;


    @Override
    public void buyStockRule(String stockCode) {

        buyStrategy.buyStockRule();
    }

    @Override
    public void holdingStockRule(String stockCode) {

        sellStrategy.holdingStockRule(stockCode);

    }


    @Override
    public void breakSell() {


        // 持仓
        QueryCreditNewPosResp queryCreditNewPosResp = EastMoneyTradeAPI.queryCreditNewPosV2();

        List<CcStockInfo> stocks = queryCreditNewPosResp.getStocks();


        stocks.forEach(stock -> {

            //
            String stockCode = stock.getStkcode();

            StockFunLast fun = new StockFunLast(stockCode);


        });
    }

}
