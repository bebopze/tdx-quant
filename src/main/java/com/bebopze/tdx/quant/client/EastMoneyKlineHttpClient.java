package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineDayResp;
import com.bebopze.tdx.quant.common.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


/**
 * 东方财富 - 行情   API封装               static
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@Slf4j
public class EastMoneyKlineHttpClient {


    public static void main(String[] args) {

        String stockCode = "300059";


        StockKlineDayResp resp_day = stockKlineHisDay(stockCode);
        StockKlineDayResp resp_week = stockKlineHisWeek(stockCode);


        // System.out.println(JSON.toJSONString(resp_day));
        System.out.println(JSON.toJSONString(resp_week));
    }


    /**
     * 个股/板块 - 历史行情       日K
     * -
     * - https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&beg=0&end=20500101&rtntype=6&secid=0.300059&klt=101&fqt=1
     * -
     * -
     * - 页面（行情中心）     https://quote.eastmoney.com/sz300059.html#fullScreenChart
     *
     * @param
     * @return
     */
    public static StockKlineDayResp stockKlineHisDay(String stockCode) {


        // secid=90.BK1090     - 板块
        // String secid_bk = String.format("90.%s", stockCode);


        // secid=0.300059      - 个股
        String secid = String.format("0.%s", stockCode);


        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get?" +
                "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13" +
                "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61" +
                "&beg=0" +
                "&end=20500101" +
                "&rtntype=6" +
                "&klt=101" +
                "&fqt=1" +
                "&secid=" + secid;


        String result = HttpUtil.doGet(url, null);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getInteger("rc") == 0) {
            log.info("/api/qt/stock/kline/get   日K   suc     >>>     result : {}", result);
        }


        StockKlineDayResp resp = JSON.toJavaObject(resultJson.getJSONObject("data"), StockKlineDayResp.class);


        // ----------------------------- 历史行情
        List<String> klines = resp.getKlines();


        return resp;
    }


    /**
     * 个股/板块 - 历史行情       周K
     * -
     * -
     * - 旧版
     * - https://push2.eastmoney.com/api/qt/stock/cqcx/get?id=SZ300059
     * -
     * -
     * -
     * - 新版     行情中心
     * - https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=0.430017&fields1=f1%2Cf2%2Cf3%2Cf4%2Cf5%2Cf6&fields2=f51%2Cf52%2Cf53%2Cf54%2Cf55%2Cf56%2Cf57%2Cf58%2Cf59%2Cf60%2Cf61&klt=102&fqt=1&end=20250514&lmt=10000
     * -
     * -
     * - 页面（行情中心）     https://quote.eastmoney.com/sz300059.html#fullScreenChart
     *
     * @param
     * @return
     */
    public static StockKlineDayResp stockKlineHisWeek(String stockCode) {

        // SZ300059
        String id = StockMarketEnum.getMarketSymbol(stockCode) + stockCode;

        // String url = "https://push2.eastmoney.com/api/qt/stock/cqcx/get?id=" + stockCode;


        int limit = 10000;

        // 0.300059
        String secid = String.format("0.%s", stockCode);


        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get?" +
                "fields1=f1,f2,f3,f4,f5,f6" +
                "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61" +
                "&klt=102" +
                "&fqt=1" +
                "&end=20500101" +
                "&lmt=" + limit +
                "&secid=" + secid;


        String result = HttpUtil.doGet(url, null);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getInteger("rc") == 0) {
            log.info("/api/qt/stock/kline/get   周K   suc     >>>     result : {}", result);
        }


        StockKlineDayResp resp = JSON.toJavaObject(resultJson.getJSONObject("data"), StockKlineDayResp.class);


        // ----------------------------- 历史行情
        List<String> klines = resp.getKlines();


        return resp;
    }


}
