package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;


/**
 * 雪球 - 行情   API封装               static                           // 垃圾雪球 -> API 强制登录！！！
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@Slf4j
public class XueqiuKlineAPI {


    public static void main(String[] args) {

        String stockCode = "300059";


        JSONObject stockKlineHisResp = stockKlineHis(stockCode, KlineTypeEnum.DAY);
        System.out.println(stockKlineHisResp);

        // StockKlineTrendResp stockKlineTrends = stockKlineTrends(stockCode);
        // System.out.println(stockKlineTrends);
    }


    /**
     * 个股/板块 - 历史行情
     *
     *
     *
     * - https://stock.xueqiu.com/v5/stock/chart/kline.json?symbol=SZ300059&begin=1747360616861&period=month&type=before&count=-1080&indicator=kline,pe,pb,ps,pcf,market_capital,agt,ggt,balance
     * -
     *
     * @param stockCode
     * @param klineTypeEnum
     * @return
     */
    public static JSONObject stockKlineHis(String stockCode, KlineTypeEnum klineTypeEnum) {


        String url = stockKlineHisUrl(stockCode, klineTypeEnum);


        String result = HttpUtil.doGet(url, null);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getInteger("error_code") == 0) {
            log.info("/v5/stock/chart/kline   suc     >>>     klineType : {} , stockCode : {} , result : {}", klineTypeEnum.getDesc(), stockCode, result);
        }


        // StockKlineHisResp resp = JSON.toJavaObject(resultJson.getJSONObject("data"), StockKlineHisResp.class);


        // ----------------------------- 历史行情
        // List<String> klines = resp.getKlines();


        return resultJson;
    }


    /**
     * 个股行情 - url拼接
     *
     * @param stockCode     证券代码
     * @param klineTypeEnum
     * @return
     */
    private static String stockKlineHisUrl(String stockCode, KlineTypeEnum klineTypeEnum) {


        // SZ/SH/BJ
        String market = StockMarketEnum.getXueqiuMarket(stockCode);
        // SZ300059
        String symbol = market + stockCode;


        // 时间戳（ms）
        long begin = System.currentTimeMillis();


        String url = "https://stock.xueqiu.com/v5/stock/chart/kline.json?indicator=kline,pe,pb,ps,pcf,market_capital,agt,ggt,balance"

                + "&period=" + klineTypeEnum.getXueqiuType()

                // 往前 5000个 交易日
                + "&type=" + "before"
                + "&count=" + (-5000)

                + "&begin=" + begin

                + "&symbol=" + symbol;


        return url;
    }

}