package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.req.RevokeOrdersReqDTO;
import com.bebopze.tdx.quant.common.domain.req.SubmitTradeV2ReqDTO;
import com.bebopze.tdx.quant.util.HttpUtil;
import com.bebopze.tdx.quant.util.PropsUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;


/**
 * 东方财富 - API封装               static
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@Slf4j
public class EastMoneyHttpClient {


    private static final String SID = PropsUtil.getSid();

    private static final String COOKIE = PropsUtil.getCookie();


//    /**
//     * 登录
//     * <p>
//     * https://jywg.eastmoneysec.com/Login/Authentication?validatekey=
//     *
//     * @param validatekey
//     * @return
//     */
//    @PostMapping("/Login/Authentication")
//    JSONObject login(@RequestParam String validatekey,
//                     @RequestBody LoginReqDTO reqDTO);


    public static void main(String[] args) {


        SubmitTradeV2ReqDTO reqDTO = new SubmitTradeV2ReqDTO();
        reqDTO.setStockCode("588050");
        reqDTO.setStockName("科创ETF");
        reqDTO.setPrice("2.055");
        reqDTO.setAmount("100");

        Integer wtbh1 = submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);
        reqDTO.setPrice("2.066");
        Integer wtbh2 = submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);
        reqDTO.setPrice("2.077");
        Integer wtbh3 = submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);


        //
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        RevokeOrdersReqDTO revokeOrdersReqDTO = new RevokeOrdersReqDTO();
        revokeOrdersReqDTO.setRevokes(today + "_" + wtbh1);
        revokeOrders("", TradeTypeEnum.DANBAO_SELL, revokeOrdersReqDTO);
        revokeOrdersReqDTO.setRevokes(today + "_" + wtbh2);
        revokeOrders("", TradeTypeEnum.DANBAO_SELL, revokeOrdersReqDTO);
        revokeOrdersReqDTO.setRevokes(today + "_" + wtbh3);
        revokeOrders("", TradeTypeEnum.DANBAO_SELL, revokeOrdersReqDTO);
    }


    /**
     * 我的持仓
     * <p>
     * 信用账户
     * - v1
     * https://jywg.eastmoneysec.com/MarginSearch/queryCreditNewPosV1?validatekey=b41148f0-825d-4b4a-a2ad-474fb46729d3
     * - v2
     * https://jywg.eastmoneysec.com/MarginSearch/queryCreditNewPosV2?validatekey=b41148f0-825d-4b4a-a2ad-474fb46729d3
     *
     * @param
     * @return
     */
    public static void queryCreditNewPosV2() {


        // https://jywg.18.cn/MarginSearch/queryCreditNewPosV2?validatekey=38ba1303-3726-458b-a2be-93cca7ca5c9e


        String url = "https://jywg.18.cn/MarginSearch/queryCreditNewPosV2?validatekey=" + SID;


        Map<String, String> headers = Maps.newHashMap();
        headers.put("Cookie", COOKIE);


        // String result = HttpUtil.doPost(url, null, headers);
        String result = HttpUtil.doGet(url, headers);


        // TODO parse result


        System.out.println(result);
    }


    /**
     * 融资/担保 买入（卖出） - 信用账户
     * <p>
     * https://jywg.18.cn/MarginTrade/SubmitTradeV2?validatekey=4a7a091e-1994-44a5-965a-8a91ce615ebf
     *
     * @param tradeTypeEnum
     * @param reqDTO
     * @return
     */
    public static Integer submitTradeV2(String validatekey,
                                        TradeTypeEnum tradeTypeEnum,
                                        SubmitTradeV2ReqDTO reqDTO) {


        // https://jywg.18.cn/MarginTrade/SubmitTradeV2?validatekey=bc7439e3-301b-44b7-bc94-9ad210699699
        String url = "https://jywg.18.cn/MarginTrade/SubmitTradeV2?validatekey=" + SID;


        Map<String, String> headers = Maps.newHashMap();
        headers.put("Cookie", COOKIE);


        // TradeTypeEnum.DANBAO_BUY;

        // stockCode: 588050
        // stockName: 科创ETF
        // price: 2.055
        // amount: 100
        // tradeType: S
        // xyjylx: 7
        // market: HA
        // SubmitTradeV2ReqDTO reqDTO = new SubmitTradeV2ReqDTO();
        // reqDTO.setStockCode("588050");
        // reqDTO.setStockName("科创ETF");
        // reqDTO.setPrice("2.055");
        // reqDTO.setAmount("100");

        // B：买入 / S：卖出
        reqDTO.setTradeType(tradeTypeEnum.getTradeType());
        // 担保买入-6; 卖出-7; 融资买入-a;   [融券卖出-A];
        reqDTO.setXyjylx(tradeTypeEnum.getXyjylx());


        // TODO   市场（HA-沪A / SA-深A / B-北交所）
        // String market = EastMoneyMarketEnum.getMarket(1);
        String market = StockMarketEnum.getEastMoneyMarketByStockCode(reqDTO.getStockCode());
        reqDTO.setMarket(market == null ? StockMarketEnum.SH.getEastMoneyMarket() : market);

        JSONObject formData = JSON.parseObject(JSON.toJSONString(reqDTO));


        try {
            String result = HttpUtil.doPost(url, formData, headers);

            JSONObject resultJson = JSON.parseObject(result);
            if (MapUtils.isNotEmpty(resultJson) && Objects.equals(resultJson.getInteger("Status"), 0)) {
                for (Object e : resultJson.getJSONArray("Data")) {// 委托编号
                    Integer wtbh = ((JSONObject) e).getInteger("Wtbh");

                    // TODO ...


                    log.info("信用账户-{} suc     >>>     url : {} , formData : {} , headers : {} , 委托编号 : {}", tradeTypeEnum.getDesc(), url, formData, headers, wtbh);
                    return wtbh;
                }

            } else {

                log.error("信用账户-{} fail     >>>     url : {} , formData : {} , headers : {} , errMsg : {} , result : {}", tradeTypeEnum.getDesc(), url, formData, headers, resultJson.getString("Message"), result);
            }


        } catch (Exception e) {

            log.error("信用账户-{} fail     >>>     url : {} , formData : {} , headers : {} , errMsg : {}", tradeTypeEnum.getDesc(), url, formData, headers, e.getMessage(), e);
        }


        return null;
    }


    /**
     * 撤单
     * <p>
     * https://jywg.18.cn/MarginTrade/RevokeOrders?validatekey=bc7439e3-301b-44b7-bc94-9ad210699699
     *
     * @param validatekey
     * @param reqDTO
     * @return
     */
    public static void revokeOrders(String validatekey,
                                    TradeTypeEnum tradeTypeEnum,
                                    RevokeOrdersReqDTO reqDTO) {


        // https://jywg.18.cn/MarginTrade/RevokeOrders?validatekey=bc7439e3-301b-44b7-bc94-9ad210699699
        String url = "https://jywg.18.cn/MarginTrade/RevokeOrders?validatekey=" + SID;


        Map<String, String> headers = Maps.newHashMap();
        headers.put("Cookie", COOKIE);


        JSONObject formData = JSON.parseObject(JSON.toJSONString(reqDTO));


        try {
            String result = HttpUtil.doPost(url, formData, headers);


            // 20250506: 您的撤单委托已提交，可至当日委托查看撤单结果
            // 20250504: 撤单失败，该笔委托已经全部成交或全部撤单
            if (result.contains("您的撤单委托已提交，可至当日委托查看撤单结果")) {
                log.info("信用账户-{}   撤单 suc     >>>     url : {} , formData : {} , headers : {} , 委托编号 : {}", tradeTypeEnum.getDesc(), url, formData, headers, reqDTO.getRevokes());
                return;
            } else if (result.contains("撤单失败，该笔委托已经全部成交或全部撤单")) {
                log.error("信用账户-{}   撤单 fail     >>>     url : {} , formData : {} , headers : {} , result : {}", tradeTypeEnum.getDesc(), url, formData, headers, result);
                return;
            }


            JSONObject resultJson = JSON.parseObject(result);
            if (MapUtils.isNotEmpty(resultJson) && Objects.equals(resultJson.getInteger("Status"), 0)) {
                resultJson.getJSONArray("Data").forEach(e -> {
                    // 委托编号
                    Integer wtbh = ((JSONObject) e).getInteger("Wtbh");

                    // TODO ...


                    log.info("信用账户-{}   撤单 suc     >>>     url : {} , formData : {} , headers : {} , 委托编号 : {}", tradeTypeEnum.getDesc(), url, formData, headers, wtbh);
                });

            } else {

                log.error("信用账户-{}   撤单 fail     >>>     url : {} , formData : {} , headers : {} , errMsg : {} , result : {}", tradeTypeEnum.getDesc(), url, formData, headers, resultJson.getString("Message"), result);
            }


        } catch (Exception e) {

            log.error("信用账户-{}   撤单 fail     >>>     url : {} , formData : {} , headers : {} , errMsg : {}", tradeTypeEnum.getDesc(), url, formData, headers, e.getMessage(), e);
        }
    }

}
