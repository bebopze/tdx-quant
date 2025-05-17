package com.bebopze.tdx.quant.strategy.sell;

import com.alibaba.fastjson.JSON;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.indicator.Fun1;
import com.bebopze.tdx.quant.strategy.QuickOption;
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


        if (sell) {
            QuickOption.一键卖出(fun1);
        }
    }


}
