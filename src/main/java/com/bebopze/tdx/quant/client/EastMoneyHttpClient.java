package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosV2Resp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosV2StockResp;
import com.bebopze.tdx.quant.common.domain.trade.req.RevokeOrdersReq;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.util.HttpUtil;
import com.bebopze.tdx.quant.util.PropsUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.math.BigDecimal;
import java.util.List;
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


    private static final Map<String, String> headers = Maps.newHashMap();

    static {
        headers.put("Cookie", COOKIE);
    }


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


        QueryCreditNewPosV2Resp queryCreditNewPosV2Resp = queryCreditNewPosV2();
        System.out.println(JSON.toJSONString(queryCreditNewPosV2Resp));


        // stockCode: 000063
        // stockName: 中兴通讯
        // price: 123.45
        // amount: 100
        // tradeType: S
        // xyjylx: 7
        // market: SA
        SubmitTradeV2Req req = new SubmitTradeV2Req();
        req.setStockCode("000063");
        req.setStockName("中兴通讯");
        req.setPrice(new BigDecimal("123.45"));
        req.setAmount(100);


        req.setStockCode("001696");
        req.setStockName("宗申动力");
        req.setPrice(new BigDecimal("123.45"));
        req.setAmount(100);


//        req.setStockCode("588050");
//        req.setStockName("科创ETF");
//        req.setPrice(new BigDecimal("3.055"));
//        req.setAmount(100);


        req.setTradeTypeEnum(TradeTypeEnum.SELL);
        req.setTradeType(req.getTradeTypeEnum().getEastMoneyTradeType());
        req.setXyjylx(req.getTradeTypeEnum().getXyjylx());

        String market = StockMarketEnum.getEastMoneyMarketByStockCode(req.getStockCode());
        req.setMarket(market == null ? StockMarketEnum.SH.getEastMoneyMarket() : market);

        Integer wtbh1 = submitTradeV2(req);

        System.out.println();
//        reqDTO.setPrice("2.066");
//        Integer wtbh2 = submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);
//        reqDTO.setPrice("2.077");
//        Integer wtbh3 = submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);
//
//
//        //
//        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
//
//        RevokeOrdersReqDTO revokeOrdersReqDTO = new RevokeOrdersReqDTO();
//        revokeOrdersReqDTO.setRevokes(today + "_" + wtbh1);
//        revokeOrders("", TradeTypeEnum.DANBAO_SELL, revokeOrdersReqDTO);
//        revokeOrdersReqDTO.setRevokes(today + "_" + wtbh2);
//        revokeOrders("", TradeTypeEnum.DANBAO_SELL, revokeOrdersReqDTO);
//        revokeOrdersReqDTO.setRevokes(today + "_" + wtbh3);
//        revokeOrders("", TradeTypeEnum.DANBAO_SELL, revokeOrdersReqDTO);
    }


    /**
     * 我的持仓
     * <p>
     * 信用账户
     * - v1
     * https://jywg.18.cn/MarginSearch/queryCreditNewPosV1?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
     * - v2
     * https://jywg.18.cn/MarginSearch/queryCreditNewPosV2?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
     *
     * @param
     * @return
     */
    public static QueryCreditNewPosV2Resp queryCreditNewPosV2() {


        String url = "https://jywg.18.cn/MarginSearch/queryCreditNewPosV2?validatekey=" + SID;


        // 不区分   Request Method


        // String result = HttpUtil.doGet(url, headers);
        String result = HttpUtil.doPost(url, null, headers);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getInteger("Status") == 0) {
            log.info("/MarginSearch/queryCreditNewPosV2   suc     >>>     result : {}", result);
        }


        JSONObject data = resultJson.getJSONArray("Data").getJSONObject(0);


        // ----------------------------- 资金持仓 汇总
        QueryCreditNewPosV2Resp resp = JSON.toJavaObject(data, QueryCreditNewPosV2Resp.class);


        // ----------------------------- 持股详情 列表
        List<QueryCreditNewPosV2StockResp> stockDTOList = resp.getStocks();


        return resp;
    }


    /**
     * 融资/担保 买入（卖出） - 信用账户
     * <p>
     * https://jywg.18.cn/MarginTrade/SubmitTradeV2?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
     *
     * @param req
     * @return
     */
    public static Integer submitTradeV2(SubmitTradeV2Req req) {


        // https://jywg.18.cn/MarginTrade/SubmitTradeV2?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
        String url = "https://jywg.18.cn/MarginTrade/SubmitTradeV2?validatekey=" + SID;


        JSONObject formData = JSON.parseObject(JSON.toJSONString(req));


        try {
            String result = HttpUtil.doPost(url, formData, headers);

            JSONObject resultJson = JSON.parseObject(result);
            if (MapUtils.isNotEmpty(resultJson) && Objects.equals(resultJson.getInteger("Status"), 0)) {
                for (Object e : resultJson.getJSONArray("Data")) {

                    // 委托编号
                    Integer wtbh = ((JSONObject) e).getInteger("Wtbh");


                    // TODO ...


                    log.info("信用账户-[{}]   suc     >>>     url : {} , formData : {} , headers : {} , 委托编号 : {}",
                             req.getTradeTypeEnum().getDesc(), url, formData, headers, wtbh);


                    return wtbh;
                }

            } else {
                String errMsg = resultJson.getString("Message");
                log.error("信用账户-[{}]   fail     >>>     url : {} , formData : {} , headers : {} , errMsg : {} , result : {}",
                          req.getTradeTypeEnum().getDesc(), url, formData, headers, errMsg, result);

                throw new RuntimeException(errMsg);
            }


        } catch (Exception e) {
            log.error("信用账户-[{}]   fail     >>>     url : {} , formData : {} , headers : {} , errMsg : {}",
                      req.getTradeTypeEnum().getDesc(), url, formData, headers, e.getMessage(), e);

            throw new RuntimeException("下单异常");
        }


        return null;
    }


    /**
     * 撤单
     * <p>
     * https://jywg.18.cn/MarginTrade/RevokeOrders?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
     *
     * @param validatekey
     * @param reqDTO
     * @return
     */
    public static void revokeOrders(String validatekey,
                                    TradeTypeEnum tradeTypeEnum,
                                    RevokeOrdersReq reqDTO) {


        // https://jywg.18.cn/MarginTrade/RevokeOrders?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
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