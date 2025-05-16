package com.bebopze.tdx.quant.strategy.sell;

import com.alibaba.fastjson.JSON;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.indicator.Fun1;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Objects;


/**
 * 卖出策略 -> 破位          等级：强制
 *
 * @author: bebopze
 * @date: 2025/5/13
 */
public class DownMASellStrategy {


    public static void main(String[] args) {


        // String stockCode = "300059";

        // 纳指ETF
        String stockCode = "159941";


        Fun1 fun1 = new Fun1(stockCode);


        // 1、下MA50


        boolean 下MA50 = fun1.下MA(50);


        // 2、MA空(20)
        boolean MA20_空 = fun1.MA空(20);


        // 3、RPS三线 < 85
        // boolean RPS三线红_NOT = true;


        boolean sell = 下MA50 || MA20_空 /*|| RPS三线红_NOT*/;


        String stockName = fun1.getStockName();


        // 买5价（ 最低价  ->  一键卖出 ）
        BigDecimal buy5 = fun1.getShszQuoteSnapshotResp().getFivequote().getBuy5();

        // 持仓
        CcStockInfo ccStockInfo = fun1.getQueryCreditNewPosResp().getStocks().stream()
                .filter(e -> Objects.equals(e.getStkcode(), stockCode))
                .findAny().orElse(null);
        Assert.notNull(ccStockInfo, String.format("当前个股 [%s-%s] 无持仓", stockCode, stockName));
        // 可卖数量
        Integer stkavl = ccStockInfo.getStkavl();
        Assert.isTrue(stkavl > 0, String.format("当前个股 [%s-%s] 可用数量不足：[%s]     >>>     ccStockInfo : %s",
                                                stockCode, stockName, stkavl, JSON.toJSONString(ccStockInfo)));


        if (sell) {

            // 卖出
            SubmitTradeV2Req req = new SubmitTradeV2Req();
            req.setStockCode(stockCode);
            req.setStockName(stockName);
            req.setPrice(buy5);
            req.setAmount(stkavl);

//            BigDecimal price = new BigDecimal(Math.max(buy5.doubleValue(), fun1.getShszQuoteSnapshotResp().getTopprice().doubleValue())).setScale(3, BigDecimal.ROUND_HALF_UP);
//            req.setPrice(price);
//            req.setAmount(100);

            req.setTradeTypeEnum(TradeTypeEnum.SELL);
            req.setMarket(ccStockInfo.getMarket());


            Integer wtbh = EastMoneyTradeAPI.submitTradeV2(req);
            System.out.println("wtbh : " + wtbh);
        }
    }


}
