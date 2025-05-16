package com.bebopze.tdx.quant.strategy.sell;

import com.bebopze.tdx.quant.client.EastMoneyKlineAPI;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.indicator.Fun1;

import java.math.BigDecimal;
import java.util.List;


/**
 * 卖出策略 -> 破位          等级：强制
 *
 * @author: bebopze
 * @date: 2025/5/13
 */
public class DownMASellStrategy {


    public static void main(String[] args) {


        String stockCode = "300059";

        // 实时行情
        SHSZQuoteSnapshotResp resp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
        SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequote = resp.getRealtimequote();


        StockKlineHisResp stockKlineHisResp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);

        double C = realtimequote.getCurrentPrice().doubleValue();

        // 历史行情
        List<KlineDTO> klineDTOList = ConvertStock.str2DTO(stockKlineHisResp.getKlines(), 500);


        double[] C_arr = ConvertStock.fieldValArr(klineDTOList, "close");
        Object[] date_arr = ConvertStock.objFieldValArr(klineDTOList, "date");


        // -------------------------------------------------------------------------------------------------------------


        // 1、下MA50

        Fun1 fun1 = new Fun1(stockCode);

        boolean 下MA50 = fun1.下MA(50);


        // MA50


        // 2、MA空(20)
        boolean MA20_空 = fun1.MA空(20);


        // 3、RPS三线 < 85


        boolean sell = 下MA50 || MA20_空;


        if (sell) {

            // 卖出
            SubmitTradeV2Req req = new SubmitTradeV2Req();
            req.setStockCode(stockCode);
            req.setPrice(new BigDecimal("123.45"));
            req.setAmount(100);

            req.setTradeTypeEnum(TradeTypeEnum.SELL);

            Integer wtbh = EastMoneyTradeAPI.submitTradeV2(req);
            System.out.println("wtbh : " + wtbh);
        }
    }


}
