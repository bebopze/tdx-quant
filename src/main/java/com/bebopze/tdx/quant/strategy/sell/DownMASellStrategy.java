package com.bebopze.tdx.quant.strategy.sell;

import com.bebopze.tdx.quant.client.EastMoneyKlineHttpClient;
import com.bebopze.tdx.quant.client.EastMoneyTradeHttpClient;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import lombok.var;

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
        SHSZQuoteSnapshotResp resp = EastMoneyTradeHttpClient.SHSZQuoteSnapshot(stockCode);
        SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequote = resp.getRealtimequote();


        StockKlineHisResp stockKlineHisResp = EastMoneyKlineHttpClient.stockKlineHis(stockCode, KlineTypeEnum.DAY);

        double C = realtimequote.getCurrentPrice().doubleValue();

        // 历史行情
        List<KlineDTO> klineDTOList = ConvertStock.str2DTO(stockKlineHisResp.getKlines(), 5000);


        double[] closeArr = ConvertStock.fieldValArr(klineDTOList, "close");
        Object[] dateArr = ConvertStock.objFieldValArr(klineDTOList, "date");


        // MA函数   ->   验证通过
        double[] ma_arr = TdxFun.MA(closeArr, 50);

        for (int i = 0; i < ma_arr.length; i++) {
            System.out.println(dateArr[i] + "     " + ma_arr[i]);
        }


        // double MA50 = MA(C, 50);
        // 1、RPS三线 < 85


        // 2、下MA50


        // 3、MA空(20)


    }


}
