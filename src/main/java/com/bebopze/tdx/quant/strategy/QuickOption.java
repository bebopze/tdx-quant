package com.bebopze.tdx.quant.strategy;

import com.alibaba.fastjson.JSON;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.indicator.Fun1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Objects;


/**
 * 快捷操作
 *
 * @author: bebopze
 * @date: 2025/5/17
 */
@Slf4j
public class QuickOption {


    /**
     * 一键清仓     -     全部持仓
     *
     * @param fun1
     */
    public static void 一键清仓(Fun1 fun1) {

    }

    public static void 一键建仓(Fun1 fun1) {

    }


    public static void 一键卖出(String code) {
        一键卖出(new Fun1(code));
    }

    /**
     * 一键清仓     -     指定个股
     *
     * @param fun
     */
    public static void 一键卖出(Fun1 fun) {

        String stockCode = fun.getStockCode();
        String stockName = fun.getStockName();

        BigDecimal buy5 = fun.getShszQuoteSnapshotResp().getFivequote().getBuy5();


        // ---------- check


        // 持仓个股
        CcStockInfo ccStockInfo = fun.getQueryCreditNewPosResp().getStocks().stream()
                .filter(e -> Objects.equals(e.getStkcode(), stockCode))
                .findAny().orElse(null);
        Assert.notNull(ccStockInfo, String.format("当前个股 [%s-%s] 无持仓", stockCode, stockName));

        // 可卖数量
        Integer stkavl = ccStockInfo.getStkavl();
        Assert.isTrue(stkavl > 0, String.format("当前个股 [%s-%s] 可用数量不足：[%s]     >>>     ccStockInfo : %s",
                                                stockCode, stockName, stkavl, JSON.toJSONString(ccStockInfo)));


        // ---------- sell
        SubmitTradeV2Req req = new SubmitTradeV2Req();
        req.setStockCode(stockCode);
        req.setStockName(stockName);
        req.setPrice(buy5);
        req.setAmount(stkavl);

        req.setTradeTypeEnum(TradeTypeEnum.SELL);
        req.setMarket(ccStockInfo.getMarket());


        Integer wtbh = EastMoneyTradeAPI.submitTradeV2(req);
        log.info("[一键卖出] suc     >>>     委托编号 : {}", wtbh);
    }


    // 一键买入
    public static void 一键买入() {


    }


    // 等比买入 - 加仓
    public static void 等比买入() {


    }


    // 等比卖出 - 减仓
    public static void 等比卖出() {


    }

}
