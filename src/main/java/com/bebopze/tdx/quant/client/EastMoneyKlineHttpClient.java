package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
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
public class EastMoneyKlineHttpClient {


    public static void main(String[] args) {

        String stockCode = "300059";


        StockKlineHisResp stockKlineHisResp = stockKlineHis(KlineTypeEnum.DAY, stockCode);

        System.out.println(stockKlineHisResp);
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
     *
     * @param
     * @return
     */
    public static StockKlineHisResp stockKlineHis(KlineTypeEnum klineTypeEnum,
                                                  String stockCode) {


        String url = stockKlineHisUrl(klineTypeEnum.getType(), stockCode);


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
     * 个股行情 - url拼接
     *
     * @param klt       K线 - 类型
     * @param stockCode 证券代码
     * @return
     */
    private static String stockKlineHisUrl(Integer klt,
                                           String stockCode) {


        // secid=90.BK1090     - 板块
        // String secid_bk = String.format("90.%s", stockCode);


        // secid=0.300059      - 个股
        String secid = String.format("0.%s", stockCode);


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