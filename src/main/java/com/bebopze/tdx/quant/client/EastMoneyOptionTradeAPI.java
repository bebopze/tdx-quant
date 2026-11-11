package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.config.aspect.StockTradingRateLimiter;
import com.bebopze.tdx.quant.common.constant.option.OptionPriceTypeEnum;
import com.bebopze.tdx.quant.common.constant.option.OptionTradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.trade.option.req.OptionGetHisOrdersReq;
import com.bebopze.tdx.quant.common.domain.trade.option.req.OptionGetTodayOrdersReq;
import com.bebopze.tdx.quant.common.domain.trade.option.req.OptionRevokeOrdersReq;
import com.bebopze.tdx.quant.common.domain.trade.option.req.OptionSubmitTradeReq;
import com.bebopze.tdx.quant.common.domain.trade.option.resp.*;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.HttpUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bebopze.tdx.quant.client.EastMoneyTradeAPI.SID;
import static com.bebopze.tdx.quant.client.EastMoneyTradeAPI.headers;


/**
 * 东方财富 - ETF期权 交易   API封装               static
 *
 * @author: bebopze
 * @date: 2026/11/7
 */
@Slf4j
public class EastMoneyOptionTradeAPI {


    static {
        bootMark("T3_CLASS_INIT_BEGIN");
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 当前账户 实时总资金
     */
    private static double TOTAL_CAP = 1_0000;

//    /**
//     * 单笔买入 金额限制：50万
//     */
//    private static final double PER_BUY_AMOUNT_LIMIT = 50_0000;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * ETF期权   -   账户资金（汇总）
     * -
     * -
     * - https://jywg.18.cn/StockOptionTrade/GetAccountFunds?validatekey=f68791ef-a002-4be6-878e-a974e491c8d0
     *
     * @param
     * @return
     */
    public static OptionGetAccountFundsResp getAccountFunds() {


        String url = "https://jywg.18.cn/StockOptionTrade/GetAccountFunds?validatekey=" + SID;


        String result = HttpUtil.doPost(url, null, headers);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        Assert.isTrue(resultJson.getInteger("Status") == 0, resultJson.getString("Message"));


        log.info("/StockOptionTrade/GetAccountFunds   suc     >>>     result : {}", result);


        JSONObject data = resultJson.getJSONArray("Data").getJSONObject(0);


        // ----------------------------- 资金持仓 汇总
        OptionGetAccountFundsResp resp = data.toJavaObject(OptionGetAccountFundsResp.class);


        // 实时总资金
        TOTAL_CAP = resp.getZczz();


        return resp;
    }


    /**
     * ETF期权   -   期权持仓 列表
     * -
     * -
     * - https://jywg.18.cn/StockOptionTrade/GetGGQQPosition?validatekey=f68791ef-a002-4be6-878e-a974e491c8d0
     *
     * @param
     * @return
     */
    public static List<OptionGetGGQQPositionResp> getGGQQPosition() {


        String url = "https://jywg.18.cn/StockOptionTrade/GetGGQQPosition?validatekey=" + SID;


        // 分页参数（qqhs：页面大小、dwc：当前页码）
        OptionGetTodayOrdersReq req = new OptionGetTodayOrdersReq(500, 1);


        // --------------


        List<OptionGetGGQQPositionResp> dataList = Lists.newArrayList();


        boolean haxNext = true;
        while (haxNext) {


            JSONObject formData = JSON.parseObject(JSON.toJSONString(req));
            String result = HttpUtil.doPost(url, formData, headers);


            JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
            Assert.isTrue(resultJson.getInteger("Status") == 0, resultJson.getString("Message"));


            // ----------------------------- 当日委托 列表
            List<OptionGetGGQQPositionResp> pageDataList = resultJson.getJSONArray("Data").toJavaList(OptionGetGGQQPositionResp.class);
            dataList.addAll(pageDataList);


            // 下一页
            haxNext = pageDataList.size() >= req.getQqhs();
            if (haxNext) {
                req.setDwc(req.getDwc() + 1);
            }
        }


        return dataList;
    }


    /**
     * ETF期权   -   客户结单-资金明细
     * -
     * -
     * - https://jywg.18.cn/StockOptionTrade/GetLastSettlementSheet?validatekey=f68791ef-a002-4be6-878e-a974e491c8d0
     *
     * @param
     * @return
     */
    public static OptionGetLastSettlementSheetResp getLastSettlementSheet() {


        String url = "https://jywg.18.cn/StockOptionTrade/GetLastSettlementSheet?validatekey=" + SID;


        // 不区分   Request Method


        // String result = HttpUtil.doGet(url, headers);
        String result = HttpUtil.doPost(url, null, headers);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        Assert.isTrue(resultJson.getInteger("Status") == 0, resultJson.getString("Message"));


        JSONObject data = resultJson.getJSONArray("Data").getJSONObject(0);


        // ----------------------------- 资金持仓 汇总
        OptionGetLastSettlementSheetResp resp = data.toJavaObject(OptionGetLastSettlementSheetResp.class);


        // ----------------------------- 持股详情 列表
        // List<CcStockInfo> stockDTOList = resp.getStocks();


        // 实时总资金
//        TOTAL_CAP = resp.getMax_TotalCap();


        return resp;
    }


    /**
     * ETF期权   -   实时行情：买5 / 卖5
     * -
     * - https://emhsmarketwgmix.eastmoneysec.com/api/SHSZQuoteSnapshot?id=10012265&DC_APP_KEY=dcquotes-service-tweb&DC_TIMESTAMP=1793990910101&DC_SIGN=2883AA88F6D1
     *
     * @param stockOptionCode ETF期权 合约编码（如：10012265）
     * @return
     */
    public static SHSZQuoteSnapshotResp SHSZQuoteSnapshot(String stockOptionCode) {
        // 通用
        return EastMoneyTradeAPI.SHSZQuoteSnapshot(stockOptionCode);
    }


    /**
     * ETF期权   -   历史委托 列表
     * -
     * - https://jywg.18.cn/StockOptionTrade/GetHisOrders?validatekey=fc5b25d1-8169-4986-b657-444822a25204
     *
     * @return
     */
    public static List<OptionGetOrderResp> getHisOrders(OptionGetHisOrdersReq req) {


        String url = "https://jywg.18.cn/StockOptionTrade/GetHisOrders?validatekey=" + SID;


        List<OptionGetOrderResp> dataList = Lists.newArrayList();


        boolean haxNext = true;
        while (haxNext) {


            JSONObject formData = JSON.parseObject(JSON.toJSONString(req));
            String result = HttpUtil.doPost(url, formData, headers);


            JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
            Assert.isTrue(resultJson.getInteger("Status") == 0, resultJson.getString("Message"));


            // ----------------------------- 历史委托 列表
            List<OptionGetOrderResp> pageDataList = resultJson.getJSONArray("Data").toJavaList(OptionGetOrderResp.class);
            dataList.addAll(pageDataList);


            // 下一页
            haxNext = pageDataList.size() >= req.getQqhs();
            if (haxNext) {
                req.setPage(req.getPage() + 1);
            }
        }


        return dataList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * ETF期权   -   可交易合约 列表
     * -
     *
     * - 页面（买入开仓）：https://jywg.18.cn/StockOptionTrade/BuyOpen
     * -
     * -
     * - API（合约列表 search API）：https://emhsmarketwgmix.eastmoneysec.com/api/SHSZOption/searchPage?pageNum=1&pageSize=10&targetCode=&callOrPut=C&DC_APP_KEY=dcquotes-service-tweb&DC_TIMESTAMP=1793988691802&DC_SIGN=16053ABC1A4EBF1
     *
     * @param
     * @return
     */
    public static JSONArray SHSZOptionSearchPage(String targetCode) {


        String url = "https://emhsmarketwgmix.eastmoneysec.com/api/SHSZOption/searchPage?" +

                "pageNum=" + 1 +
                "&pageSize=" + 1000 +


                // 标的证券（588000-科创50ETF；159915-创业板ETF；510500-中证500ETF；）
                "&targetCode=" + targetCode +

                // 合约类型：  C-认购；P-认沽；
                "&callOrPut=" + "C" +


                // 合约编码（关键词模糊搜索：科创50购12月1350）
                "&id=" + "" +


                "&DC_APP_KEY=dcquotes-service-tweb" +
                // 时间戳
                "&DC_TIMESTAMP=" + System.currentTimeMillis() +
                // 签名
                "&DC_SIGN=";


        String result = HttpUtil.doGet(url);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getInteger("code") == 0) {
            log.info("/SHSZOption/searchPage   suc     >>>     result : {}", result);
        }


        JSONArray data = resultJson.getJSONArray("data");


        return data;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 开仓/平仓
     * -
     * - https://jywg.18.cn/StockOptionTrade/SubmitTrade?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
     *
     * @param req
     * @return 委托编号
     */
    // @BSLimiter
    public static Integer submitTrade(OptionSubmitTradeReq req) {


        // 防误触   ->   全局校验：买入金额 < 总资金 * 20%   、  单笔买入 金额限制
        // buyCheck(req);


        // 量化限流
        limiter(req.getHybh());


        // -------------------------------------------------------------------------------------------------------------


        String url = "https://jywg.18.cn/StockOptionTrade/SubmitTrade?validatekey=" + SID;


        JSONObject formData = JSON.parseObject(JSON.toJSONString(req));


        try {
            String result = HttpUtil.doPost(url, formData, headers);

            JSONObject resultJson = JSON.parseObject(result);
            if (MapUtils.isNotEmpty(resultJson) && Objects.equals(resultJson.getInteger("Status"), 0)) {
                for (Object e : resultJson.getJSONArray("Data")) {

                    // 委托编号
                    Integer wtbh = ((JSONObject) e).getInteger("Wtbh");


                    // TODO ...


                    log.info("ETF期权-[{}]   suc     >>>     url : {} , formData : {} , 委托编号 : {}",
                             req.getOptionTradeTypeEnum().getDesc(), url, formData, wtbh);


                    return wtbh;
                }

            } else {
                String errMsg = resultJson.getString("Message");
                throw new BizException(errMsg);
            }


        } catch (Exception e) {
            log.error("信用账户-[{}]   fail     >>>     url : {} , formData : {} , exMsg : {}",
                      req.getOptionTradeTypeEnum().getDesc(), url, formData, e.getMessage(), e);

            throw new BizException("下单异常：" + e.getMessage());
        }


        return null;
    }


//    /**
//     * 全局校验：买入金额 < 总资金 * 20%   、  单笔买入 金额限制
//     *
//     * @param req
//     */
//    private static void buyCheck(SubmitTradeV2Req req) {
//
//        TradeTypeEnum tradeTypeEnum = req.getTradeTypeEnum();
//
//        if (tradeTypeEnum == TradeTypeEnum.RZ_BUY || tradeTypeEnum == TradeTypeEnum.ZY_BUY) {
//            double buyAmount = req.getPrice() * req.getAmount();
//
//
//            // 买入金额 < 总资金 * 20%
//            Assert.isTrue(buyAmount < TOTAL_CAP * 0.2,
//                          String.format("个股[%s]买入仓位占比[%s]超限制[20%%]", req.getStockCode(), NumUtil.of(buyAmount / TOTAL_CAP)));
//
//
//            // 单笔买入 金额限制
//            Assert.isTrue(buyAmount < PER_BUY_AMOUNT_LIMIT,
//                          String.format("个股[%s]单笔买入金额[%s]超上限[%s]", req.getStockCode(), buyAmount, PER_BUY_AMOUNT_LIMIT));
//        }
//    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * ETF期权   -   当日委托单 列表
     * -
     * - https://jywg.18.cn/StockOptionTrade/GetTodayOrders?validatekey=fc5b25d1-8169-4986-b657-444822a25204
     *
     * @return
     */
    public static List<OptionGetOrderResp> getTodayOrders() {


        String url = "https://jywg.18.cn/StockOptionTrade/GetTodayOrders?validatekey=" + SID;


        List<OptionGetOrderResp> dataList = Lists.newArrayList();


        // 分页参数（qqhs：页面大小、dwc：当前页码）
        OptionGetTodayOrdersReq req = new OptionGetTodayOrdersReq(500, 1);


        boolean haxNext = true;
        while (haxNext) {


            JSONObject formData = JSON.parseObject(JSON.toJSONString(req));
            String result = HttpUtil.doPost(url, formData, headers);


            JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
            if (resultJson.getInteger("Status") == 0) {
                log.info("/StockOptionTrade/GetTodayOrders   suc     >>>     result : {}", result);
            }


            // ----------------------------- 当日委托 列表
            List<OptionGetOrderResp> pageDataList = resultJson.getJSONArray("Data").toJavaList(OptionGetOrderResp.class);
            dataList.addAll(pageDataList);


            // 下一页
            haxNext = pageDataList.size() >= req.getQqhs();
            if (haxNext) {
                req.setDwc(req.getDwc() + 1);
            }
        }


        return dataList;
    }


    /**
     * 批量撤单
     *
     *
     * - 页面 ： https://jywg.18.cn/StockOptionTrade/Revoke
     *
     * - https://jywg.18.cn/StockOptionTrade/RevokeOrders?validatekey=fa48b9d8-d7c3-4c26-b9f9-957a44586333
     *
     * @param req
     * @param wtbh_stockCode_Map
     * @return
     */
    // @BSLimiter
    public static List<OptionRevokeOrderResultDTO> revokeOrders(OptionRevokeOrdersReq req,
                                                                Map<String, String> wtbh_stockCode_Map) {


        // 量化限流
        Arrays.stream(req.getWtbhs().split(",")).forEach(e -> {
            // 委托编号-交易所
            String stockCode = wtbh_stockCode_Map.getOrDefault(e, "-1");
            limiter(stockCode);
        });


        // -------------------------------------------------------------------------------------------------------------


        String url = "https://jywg.18.cn/StockOptionTrade/RevokeOrders?validatekey=" + SID;


        // wtbhs       9000019830-05,9000020920-15
        JSONObject formData = JSON.parseObject(JSON.toJSONString(req));


        String result = HttpUtil.doPost(url, formData, headers);


        JSONObject jsonResult = JSON.parseObject(result);
        if (jsonResult.getInteger("Status") == 0) {
            log.info("/StockOptionTrade/RevokeOrders   suc     >>>     req : {} , result : {}", JSON.toJSONString(req), result);
        }


        // result_Data_arr   ->   平铺
        List<OptionRevokeOrderResultDTO> dtoList = OptionRevokeOrderResultDTO.convert(jsonResult.getJSONArray("Data"));
        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    private static final StockTradingRateLimiter limiter = new StockTradingRateLimiter();


    /**
     * 量化限流
     *
     * @param stockCode
     */
    @SneakyThrows
    public static void limiter(String stockCode) {
        long start = System.currentTimeMillis();


        // 获取当前速率信息
        StockTradingRateLimiter.RateInfo rateInfo = limiter.getRateInfo(stockCode);
        log.info("[" + stockCode + "] " + rateInfo.toString());


        // 限流 -> 等待许可后执行交易
        limiter.tryAcquireWithTimeout(stockCode, 60 * 1000);
        log.info("[" + stockCode + "] 执行交易耗时：" + DateTimeUtil.formatNow2Hms(start));
    }


    // -----------------------------------------------------------------------------------------------------------------


    static {
        bootMark("T3_STATIC_FIELDS_DONE");
    }


    private static void bootMark(String name) {

        System.err.printf(
                "[BOOT-MARK] %-25s | %s | nano=%d%n",
                name,
                java.time.Instant.now(),
                System.nanoTime()
        );

    }

    public static void main(String[] args) {

//
//        bootMark("T4_MAIN_ENTER");
//
//
////        System.err.println(
////                "[ORIGINAL] MAIN_ENTER = " + Instant.now()
////        );
//
//
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        // 账户资金
//        OptionGetAccountFundsResp accountFunds = getAccountFunds();
//        // System.out.println(accountFunds);
//
//
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        // 持仓列表
//        List<OptionGetGGQQPositionResp> ggqqPosition = getGGQQPosition();
//
//        System.out.println(ggqqPosition.size() + "     " /*+ ggqqPosition*/);
//
//
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        // 实时行情（买5/卖5）
//        SHSZQuoteSnapshotResp shszQuoteSnapshotResp = SHSZQuoteSnapshot("10012265");
//        // System.out.println(shszQuoteSnapshotResp);
//
//
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        // 合约搜索（支持 keyword 模糊搜索）
//        JSONArray optionList = SHSZOptionSearchPage("588000");
//
//
//        System.out.println(optionList.size());
//        // System.out.println(optionList);
//
//
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        // 期权BS
//        OptionSubmitTradeReq req = new OptionSubmitTradeReq();
//        req.setHybh("10011565");
//        req.setAmount(1);
//        req.setPrice(new BigDecimal("0.0001"));
//
//
//        req.setOptionTradeTypeEnum(OptionTradeTypeEnum.BUY_OPEN);
//        req.setOptionPriceTypeEnum(OptionPriceTypeEnum.LMT);
//        req.setEtfCode("588000");
//
//
//        Integer wtdh = submitTrade(req);
//        System.out.println(wtdh);


        // -------------------------------------------------------------------------------------------------------------


        // 历史委托 列表
        OptionGetHisOrdersReq req_1 = new OptionGetHisOrdersReq();
        req_1.setSt(LocalDate.of(2026, 11, 1));
        req_1.setEt(LocalDate.of(2026, 11, 6));
        req_1.setQqhs(10);

        List<OptionGetOrderResp> resp_1 = getHisOrders(req_1);


        System.out.println(resp_1.size());
//         System.out.println(resp_1);


//        // 当日委托单 列表
//        List<OptionGetOrderResp> ordersDataRespList = getTodayOrders();
//        System.out.println(JSON.toJSONString(ordersDataRespList));
//
//
//        // 撤单
//        OptionRevokeOrdersReq revoke_req = new OptionRevokeOrdersReq();
//        revoke_req.setWtbhs("9000020920-15,9000020912-15");
//        List<OptionRevokeOrderResultDTO> revoke_resp = revokeOrders(revoke_req, Maps.newHashMap());
//
//        System.out.println(JSON.toJSONString(revoke_resp));
    }


    // -----------------------------------------------------------------------------------------------------------------


}