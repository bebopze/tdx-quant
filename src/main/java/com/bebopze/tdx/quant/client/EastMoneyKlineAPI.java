package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineTrendResp;
import com.bebopze.tdx.quant.common.util.EventStreamUtil;
import com.bebopze.tdx.quant.common.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;


/**
 * 东方财富 - 行情   API封装               static
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@Slf4j
public class EastMoneyKlineAPI {


    public static void main(String[] args) {

        String stockCode = "300059";


        // StockKlineHisResp stockKlineHisResp = stockKlineHis(KlineTypeEnum.DAY, stockCode);
        // System.out.println(stockKlineHisResp);

        StockKlineTrendResp stockKlineTrends = stockKlineTrends(stockCode);
        System.out.println(stockKlineTrends);
    }


    /**
     * 个股/板块 - 历史行情
     * -
     * - https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61&beg=0&end=20500101&rtntype=6&secid=0.300059&klt=101&fqt=1
     * -
     * -
     * - 页面（行情中心 - 新版）     https://quote.eastmoney.com/concept/sz300059.html
     * -
     * - 页面（行情中心 - 旧版）     https://quote.eastmoney.com/sz300059.html#fullScreenChart
     * -
     * -
     *
     * @param stockCode
     * @param klineTypeEnum
     * @return
     */
    public static StockKlineHisResp stockKlineHis(String stockCode, KlineTypeEnum klineTypeEnum) {


        String url = stockKlineHisUrl(stockCode, klineTypeEnum.getType());


        String result = HttpUtil.doGet(url, null);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getInteger("rc") == 0) {
            log.info("/api/qt/stock/kline/get   suc     >>>     klineType : {} , stockCode : {} , result : {}",
                     klineTypeEnum.getDesc(), stockCode, result);
        }


        StockKlineHisResp resp = JSON.toJavaObject(resultJson.getJSONObject("data"), StockKlineHisResp.class);


        // ----------------------------- 历史行情
        List<String> klines = resp.getKlines();


        return resp;
    }


    /**
     * 个股/板块 - 分时
     * -
     * - https://31.push2.eastmoney.com/api/qt/stock/trends2/sse?fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f14,f17&fields2=f51,f52,f53,f54,f55,f56,f57,f58&mpi=1000&ut=fa5fd1943c7b386f172d6893dbfba10b&secid=0.300059&ndays=1&iscr=0&iscca=0&wbp2u=1849325530509956|0|1|0|web
     * -
     * -
     * - 页面（行情中心 - 新版）     https://quote.eastmoney.com/concept/sz300059.html
     * -
     * - 页面（行情中心 - 旧版）     https://quote.eastmoney.com/sz300059.html#fullScreenChart
     * -
     * -
     *
     * @param stockCode
     * @param ndays     分时 - 天数
     * @return
     */
    public static StockKlineTrendResp stockKlineTrends(String stockCode, int ndays) {


        // 0.300059
        String secid = String.format("0.%s", stockCode);

        ndays = Math.max(ndays, 1);


        String url = "https://31.push2.eastmoney.com/api/qt/stock/trends2/sse?" +
                "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13,f14,f17" +
                "&fields2=f51,f52,f53,f54,f55,f56,f57,f58" +
                "&mpi=1000" +
                // "&ut=fa5fd1943c7b386f172d6893dbfba10b" +
                "&secid=" + secid +
                "&ndays=" + ndays +
                "&iscr=0" +
                "&iscca=0"
                // "&wbp2u=1849325530509956|0|1|0|we"
                ;


        String result = EventStreamUtil.fetchOnce(url);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getInteger("rc") == 0) {
            log.info(
                    "/api/qt/stock/trends2/sse   suc     >>>     klineType : {} , ndays : {} , stockCode : {} , result : {}",
                    "分时", ndays, stockCode, result);
        }


        StockKlineTrendResp resp = JSON.toJavaObject(resultJson.getJSONObject("data"), StockKlineTrendResp.class);


        // ----------------------------- 分时行情
        List<String> trends = resp.getTrends();


        return resp;
    }

    public static StockKlineTrendResp stockKlineTrends(String stockCode) {
        return stockKlineTrends(stockCode, 1);
    }


    /**
     * 个股行情 - url拼接
     *
     * @param stockCode 证券代码
     * @param klt       K线 - 类型
     * @return
     */
    private static String stockKlineHisUrl(String stockCode, Integer klt) {


        // secid=90.BK1090     - 板块
        // String secid_bk = String.format("90.%s", stockCode);


        // 0-深圳；1-上海；2-北京；
        Integer tdxMarketType = StockMarketEnum.getTdxMarketType(stockCode);
        // 深圳+北京 -> 0（缺省值 -> ETF）
        Integer marketType = Objects.equals(tdxMarketType, 1) ? 1 : 0;


        // secid=0.300059      - 个股
        String secid = String.format("%s.%s", marketType, stockCode);


        // 截止日期
        String end = "20500101";
        // 日K   ->   limit为空（不限制）
        String limit = Objects.equals(klt, KlineTypeEnum.DAY.getType()) ? "" : "10000";


        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get?" +
                "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13" +
                "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61" +
                "&beg=0" +
                "&rtntype=6" +

                "&fqt=1" +

                "&klt=" + klt +

                "&end=" + end +
                "&lmt=" + limit +
                "&secid=" + secid;


        return url;
    }

}