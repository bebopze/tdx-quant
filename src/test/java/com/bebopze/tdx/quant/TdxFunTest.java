package com.bebopze.tdx.quant;

import com.bebopze.tdx.quant.client.EastMoneyKlineAPI;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
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
        SHSZQuoteSnapshotResp resp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
        SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequote = resp.getRealtimequote();


        // 历史行情 - API
        StockKlineHisResp stockKlineHisResp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);


        // -------------------------------------------------------------------------------------------------------------


        // 收盘价 - 实时
        double C = realtimequote.getCurrentPrice().doubleValue();


        // 历史行情
        List<KlineDTO> klineDTOList = ConvertStockKline.klines2DTOList(stockKlineHisResp.getKlines(), 5000);


        Object[] dateArr = ConvertStockKline.objFieldValArr(klineDTOList, "date");

        double[] closeArr = ConvertStockKline.fieldValArr(klineDTOList, "close");


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