package com.bebopze.tdx.quant;

import com.bebopze.tdx.quant.client.EastMoneyKlineHttpClient;
import com.bebopze.tdx.quant.client.EastMoneyTradeHttpClient;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;

import java.util.List;


/**
 * TdxFun     -     Test
 *
 * @author: bebopze
 * @date: 2025/5/16
 */
public class TdxFunTest {


    public static void main(String[] args) {


        String stockCode = "300059";


        // 实时行情 - API
        SHSZQuoteSnapshotResp resp = EastMoneyTradeHttpClient.SHSZQuoteSnapshot(stockCode);
        SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequote = resp.getRealtimequote();


        // 历史行情 - API
        StockKlineHisResp stockKlineHisResp = EastMoneyKlineHttpClient.stockKlineHis(stockCode, KlineTypeEnum.DAY);


        // -------------------------------------------------------------------------------------------------------------


        // 收盘价 - 实时
        double C = realtimequote.getCurrentPrice().doubleValue();


        // 历史行情
        List<KlineDTO> klineDTOList = ConvertStock.str2DTO(stockKlineHisResp.getKlines(), 5000);


        Object[] dateArr = ConvertStock.objFieldValArr(klineDTOList, "date");

        double[] closeArr = ConvertStock.fieldValArr(klineDTOList, "close");


        // --------------------------------- MA


        // MA函数   ->   验证通过
        double[] ma_arr = TdxFun.MA(closeArr, 50);

        for (int i = 0; i < ma_arr.length; i++) {
            System.out.println(dateArr[i] + "     " + ma_arr[i]);
        }


        // --------------------------------- XX


        // --------------------------------- XX


    }


}