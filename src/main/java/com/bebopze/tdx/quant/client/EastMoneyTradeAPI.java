package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.domain.dto.RevokeOrderResultDTO;
import com.bebopze.tdx.quant.common.domain.trade.req.RevokeOrdersReq;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.GetOrdersDataResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosV2StockResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.util.HttpUtil;
import com.bebopze.tdx.quant.common.util.PropsUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * 东方财富 - 交易   API封装               static
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@Slf4j
public class EastMoneyTradeAPI {


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


//        // --------------------------------- 我的持仓   -   信用账户
//
//        QueryCreditNewPosV2Resp queryCreditNewPosV2Resp = queryCreditNewPosV2();
//        System.out.println(JSON.toJSONString(queryCreditNewPosV2Resp));
//
//
//        // stockCode: 000063
//        // stockName: 中兴通讯
//        // price: 123.45
//        // amount: 100
//        // tradeType: S
//        // xyjylx: 7
//        // market: SA
//        SubmitTradeV2Req req = new SubmitTradeV2Req();
//        req.setStockCode("000063");
//        req.setStockName("中兴通讯");
//        req.setPrice(new BigDecimal("123.45"));
//        req.setAmount(100);
//
//
//        req.setStockCode("001696");
//        req.setStockName("宗申动力");
//        req.setPrice(new BigDecimal("123.45"));
//        req.setAmount(100);
//
//
////        req.setStockCode("588050");
////        req.setStockName("科创ETF");
////        req.setPrice(new BigDecimal("3.055"));
////        req.setAmount(100);
//
//
//        req.setTradeTypeEnum(TradeTypeEnum.SELL);
//        req.setTradeType(req.getTradeTypeEnum().getEastMoneyTradeType());
//        req.setXyjylx(req.getTradeTypeEnum().getXyjylx());
//
//        String market = StockMarketEnum.getEastMoneyMarketByStockCode(req.getStockCode());
//        req.setMarket(market == null ? StockMarketEnum.SH.getEastMoneyMarket() : market);
//
//        Integer wtbh1 = submitTradeV2(req);
//
//        System.out.println();
//
//
////        reqDTO.setPrice("2.066");
////        Integer wtbh2 = submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);
////        reqDTO.setPrice("2.077");
////        Integer wtbh3 = submitTradeV2(null, TradeTypeEnum.DANBAO_SELL, reqDTO);
//

        // --------------------------------- 当日委托单 列表
        List<GetOrdersDataResp> ordersDataRespList = getOrdersData();
        System.out.println(JSON.toJSONString(ordersDataRespList));


        // --------------------------------- 撤单


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


        // --------------------------------- 买5/卖5


        SHSZQuoteSnapshotResp resp = SHSZQuoteSnapshot("300059");
        System.out.println(JSON.toJSONString(resp));
    }


    /**
     * 我的持仓   -   信用账户
     * -
     * -
     * - v1
     * - https://jywg.18.cn/MarginSearch/queryCreditNewPosV1?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
     * -
     * - v2
     * - https://jywg.18.cn/MarginSearch/queryCreditNewPosV2?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
     *
     * @param
     * @return
     */
    public static QueryCreditNewPosResp queryCreditNewPosV2() {


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
        QueryCreditNewPosResp resp = JSON.toJavaObject(data, QueryCreditNewPosResp.class);


        // ----------------------------- 持股详情 列表
        List<QueryCreditNewPosV2StockResp> stockDTOList = resp.getStocks();


        return resp;
    }


    /**
     * 实时行情：买5 / 卖5
     * -
     * - https://emhsmarketwgmix.eastmoneysec.com/api/SHSZQuoteSnapshot?id=300059
     *
     * @param stockCode
     * @return
     */
    public static SHSZQuoteSnapshotResp SHSZQuoteSnapshot(String stockCode) {


        String url = "https://emhsmarketwgmix.eastmoneysec.com/api/SHSZQuoteSnapshot?id=" + stockCode;


        String result = HttpUtil.doGet(url, null);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getString("code").equals(stockCode)) {
            log.info("/api/SHSZQuoteSnapshot   suc     >>>     stockCode : {} , result : {}", stockCode, result);
        }


        // ----------------------------- 个股信息
        SHSZQuoteSnapshotResp resp = JSON.toJavaObject(resultJson, SHSZQuoteSnapshotResp.class);


        // ----------------------------- 买5 / 卖5
        SHSZQuoteSnapshotResp.FivequoteDTO fivequoteDTO = resp.getFivequote();


        // ----------------------------- 实时报价
        SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequoteDTO = resp.getRealtimequote();


        return resp;
    }


    /**
     * 融资/担保 买入（卖出） - 信用账户
     * -
     * - https://jywg.18.cn/MarginTrade/SubmitTradeV2?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
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


                    log.info("信用账户-[{}]   suc     >>>     url : {} , formData : {} , 委托编号 : {}",
                             req.getTradeTypeEnum().getDesc(), url, formData, wtbh);


                    return wtbh;
                }

            } else {
                String errMsg = resultJson.getString("Message");
                // log.error("信用账户-[{}]   fail     >>>     url : {} , formData : {} , errMsg : {} , result : {}",
                //          req.getTradeTypeEnum().getDesc(), url, formData, errMsg, result);

                throw new BizException(errMsg);
            }


        } catch (Exception e) {
            log.error("信用账户-[{}]   fail     >>>     url : {} , formData : {} , exMsg : {}",
                      req.getTradeTypeEnum().getDesc(), url, formData, e.getMessage(), e);

            throw new BizException("下单异常：" + e.getMessage());
        }


        return null;
    }


    /**
     * 当日委托单 列表
     * -
     * - https://jywg.18.cn/MarginSearch/GetOrdersData
     *
     * @return
     */
    public static List<GetOrdersDataResp> getOrdersData() {


        String url = "https://jywg.18.cn/MarginSearch/GetOrdersData?validatekey=" + SID;


        // 不区分   Request Method


        String result = HttpUtil.doGet(url, headers);
        // String result = HttpUtil.doPost(url, null, headers);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getInteger("Status") == 0) {
            log.info("/MarginSearch/GetOrdersData   suc     >>>     result : {}", result);
        }


        List<GetOrdersDataResp> respList = JSON.parseArray(resultJson.getString("Data"), GetOrdersDataResp.class);
        return respList;
    }


    /**
     * 批量撤单
     * -
     * - https://jywg.18.cn/MarginTrade/RevokeOrders?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
     *
     * @param req
     * @return
     */
    public static List<RevokeOrderResultDTO> revokeOrders(RevokeOrdersReq req) {


        // https://jywg.18.cn/MarginTrade/RevokeOrders?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
        String url = "https://jywg.18.cn/MarginTrade/RevokeOrders?validatekey=" + SID;


        JSONObject formData = JSON.parseObject(JSON.toJSONString(req));


        String result = HttpUtil.doPost(url, formData, headers);


        // 20250511: 您的撤单委托已提交，可至当日委托查看撤单结果   20250511: 撤单失败，该笔委托已经全部成交或全部撤单   20250511: 委托序号与委托人不符


        // 20250506: 您的撤单委托已提交，可至当日委托查看撤单结果
        // 20250504: 撤单失败，该笔委托已经全部成交或全部撤单
        // 20250511: 委托序号与委托人不符


        String[] revokeArr = req.getRevokes().split(",");
        String[] resultArr = result.split("   ");


        List<RevokeOrderResultDTO> dtoList = Lists.newArrayList();
        for (int i = 0; i < resultArr.length; i++) {

            String revoke = revokeArr[i];
            String revoke_r = resultArr[i];


            RevokeOrderResultDTO dto = new RevokeOrderResultDTO();
            dto.setRevoke(revoke);
            dto.setResultDesc(revoke_r);


            boolean suc;
            if (revoke_r.contains("您的撤单委托已提交，可至当日委托查看撤单结果")) {
                suc = true;
            } else if (revoke_r.contains("撤单失败，该笔委托已经全部成交或全部撤单")) {
                suc = false;
            } else if (revoke_r.contains("委托序号与委托人不符")) {
                suc = false;
            } else {
                suc = false;
            }
            dto.setSuc(suc);


            dtoList.add(dto);
        }


        return dtoList;
    }

}