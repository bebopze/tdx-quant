package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineListSinaResp;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.HttpUtil;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.client.EastMoneyKlineAPI.lastTradeDate;


/**
 * 新浪财经 - 行情   API封装               static
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@Slf4j
public class SinaKlineAPI {


    /**
     * 东方财富   ->   批量拉取  全A（ETF） 实时行情          // Tips：专供 盘中刷新行情Task 使用（每500ms 拉取 100只股票）
     *
     * @return
     */
    public static List<StockSnapshotKlineDTO> pullAllStockETFSnapshotKline() {

        List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS = pullAllStockSnapshotKline();
        List<StockSnapshotKlineDTO> ETFSnapshotKlineDTOS = pullAllETFSnapshotKline();


        stockSnapshotKlineDTOS.addAll(ETFSnapshotKlineDTOS);


        Assert.notEmpty(stockSnapshotKlineDTOS, String.format("pullAllStockETFSnapshotKline - err     >>>     新浪财经 -> 批量拉取 全A（ETF）实时行情 异常 , size : %s",
                                                              stockSnapshotKlineDTOS.size()));


        return stockSnapshotKlineDTOS;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 东方财富  ->  批量拉取 全A 实时行情               // Tips：专供 盘中刷新行情Task 使用（每500ms 拉取 100只股票）
     *
     * @return
     */
    public static List<StockSnapshotKlineDTO> pullAllStockSnapshotKline() {


        int pageNum = 1;
        int pageSize = 100;


        LocalDate date = lastTradeDate();


        List<StockSnapshotKlineDTO> dtoList = Lists.newArrayList();


        // ------------------------------------------------------------------
        long start = System.currentTimeMillis();


        while (true) {
            long page_start = System.currentTimeMillis();


            String url = allStockKlineUrl(pageNum++, pageSize);

            String result = HttpUtil.doGet(url, null);


            JSONArray jsonArray = JSON.parseArray(result);
            if (CollectionUtils.isNotEmpty(jsonArray)) {
                log.info("/quotes_service/api/json_v2.php/Market_Center.getHQNodeData   suc     >>>     pageNum : {} , pageSize : {} , pageTime : {} , totalTime : {} , result : {}",
                         pageNum - 1, pageSize, DateTimeUtil.formatNow2Hms(page_start), DateTimeUtil.formatNow2Hms(start), result);
            }


            List<StockKlineListSinaResp> dataList = jsonArray.stream()
                                                             .map(e -> {

                                                                 JSONObject json = (JSONObject) e;

//                                                                 // 包含大量 退市/停牌 个股（东方财富 A股 -> 5735）
//                                                                 if (isErrStock(json)) {
//                                                                     log.warn("个股行情 异常 -> 退市/停牌/未上市/盘前     >>>     {} {}", json.getString("f12"), json.getString("f14"));
//                                                                     return null;
//                                                                 }

                                                                 return json.toJavaObject(StockKlineListSinaResp.class);

                                                             }).filter(Objects::nonNull).collect(Collectors.toList());


            // ------------------------------------------------------


            // dataList  ->  dtoList
            List<StockSnapshotKlineDTO> pageDTOList = convert2StockSnapshotDTOList(dataList, date);
            dtoList.addAll(pageDTOList);


            // ------------------------------------------------------ total: 5738     ->     58页


            if (jsonArray.size() < pageSize || dtoList.size() > 7500 || pageNum > 75) {
                break;
            }


            // 间隔 500ms ~ 1s
            SleepUtils.randomSleep(1000, 3000);
        }


        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 东方财富 - 自选股（行情中心）

    // https://quote.eastmoney.com/zixuan/?from=quotecenter


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 东方财富  ->  批量拉取 全部ETF 实时行情
     *
     *
     * - 东方财富网 > 行情中心 > 基金市场 > ETF基金行情 > 全部ETF       https://quote.eastmoney.com/center/gridlist.html#fund_etf
     *
     * @return
     */
    @Deprecated
    public static List<StockSnapshotKlineDTO> pullAllETFSnapshotKline() {


        int pageNum = 1;
        int pageSize = 100;


        LocalDate date = lastTradeDate();


        List<StockSnapshotKlineDTO> dtoList = Lists.newArrayList();


        // ------------------------------------------------------------------


        while (true) {

            String url = allETFKlineUrl(pageNum++, pageSize);

            String result = HttpUtil.doGet(url, null);


            JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
            if (resultJson.getInteger("rc") == 0) {
                log.info("/api/qt/clist/get   suc     >>>     pageNum : {} , pageSize : {} , result : {}", pageNum - 1, pageSize, result);
            }


            JSONArray jsonArray = resultJson.getJSONObject("data").getJSONArray("diff");

            List<StockKlineListSinaResp> dataList = jsonArray.stream()
                                                             .map(e -> {

                                                                 JSONObject json = (JSONObject) e;

//                                                                 // 包含大量 退市/停牌 个股（ETF）（东方财富 A股 -> 5735）
//                                                                 if (isErrStock(json)) {
//                                                                     return null;
//                                                                 }

                                                                 return json.toJavaObject(StockKlineListSinaResp.class);

                                                             }).filter(Objects::nonNull).collect(Collectors.toList());


            // ------------------------------------------------------


            // dataList  ->  dtoList
            List<StockSnapshotKlineDTO> pageDTOList = convert2StockSnapshotDTOList(dataList, date);
            dtoList.addAll(pageDTOList);


            // ------------------------------------------------------ total: 1213     ->     13页


            if (jsonArray.size() < pageSize || dtoList.size() > 2000 || pageNum > 20) {
                break;
            }


            // 间隔 500ms ~ 1s
            SleepUtils.randomSleep(500, 1000);
        }


        return dtoList;
    }


    /**
     * dataList  ->  dtoList
     *
     * @param dataList
     * @param date
     * @return
     */
    private static List<StockSnapshotKlineDTO> convert2StockSnapshotDTOList(List<StockKlineListSinaResp> dataList,
                                                                            LocalDate date) {


        return dataList.parallelStream().map(e -> {

            StockSnapshotKlineDTO dto = new StockSnapshotKlineDTO();

            dto.setStockCode(e.getCode());
            dto.setStockName(e.getName());

            dto.setPrevClose(e.getSettlement());


            // -------------------------------


            dto.setDate(date);

            dto.setOpen(e.getOpen());
            dto.setHigh(e.getHigh());
            dto.setLow(e.getLow());
            dto.setClose(e.getTrade());


            dto.setVol(e.getVolume());
            dto.setAmo(e.getAmount());

            dto.setRange_pct(e.getRangePct());
            dto.setChange_pct(e.getChangepercent());
            dto.setChange_price(e.getPricechange());
            dto.setTurnover_pct(e.getTurnoverratio());


            return dto;
        }).collect(Collectors.toList());

    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 新浪财经  ->  批量拉取 全A 实时行情
     *
     *
     *
     * - https://quote.eastmoney.com/center/gridlist.html                东方财富网 > 行情中心 > 沪深京个股 > 沪深京A股
     *
     *
     *
     * - https://vip.stock.finance.sina.com.cn/mkt/#stock_hs_up          新浪财经>行情中心首页>A股>排行>A股涨幅
     *
     * - https://stock.caixin.com/stock-list.html?mar=all                财新>行情中心>股票>沪深A股
     *
     * - https://quote.hexun.com/          和讯>行情中心>沪深股市>A股市场>全部分类>沪深板块
     *
     *
     *
     * - https://www.iwencai.com/unifiedwap/result?w=%E5%85%A8A%EF%BC%9BO%E3%80%81H%E3%80%81L%E3%80%81C%E3%80%81VOL%E3%80%81AMO%E3%80%81%E6%8C%AF%E5%B9%85%E3%80%81%E6%B6%A8%E8%B7%8C%E5%B9%85%E3%80%81%E6%B6%A8%E8%B7%8C%E9%A2%9D%E3%80%81%E6%8D%A2%E6%89%8B%E7%8E%87%EF%BC%9B&querytype=stock
     * -                    同花顺>问财 > 全A；O、H、L、C、VOL、AMO、振幅、涨跌幅、涨跌额、换手率；行业；概念；
     *
     * * - https://wenda.tdx.com.cn/site/wenda_pc/stock_index.html?message=%E5%85%A8A%EF%BC%9BO%E3%80%81H%E3%80%81L%E3%80%81C%E3%80%81VOL%E3%80%81AMO%E3%80%81%E6%8C%AF%E5%B9%85%E3%80%81%E6%B6%A8%E8%B7%8C%E5%B9%85%E3%80%81%E6%B6%A8%E8%B7%8C%E9%A2%9D%E3%80%81%E6%8D%A2%E6%89%8B%E7%8E%87%EF%BC%9B%E8%A1%8C%E4%B8%9A%EF%BC%9B&queryType=AG
     * -                    通达信>问小达 > 全A；O、H、L、C、VOL、AMO、振幅、涨跌幅、涨跌额、换手率；行业；
     *
     *
     *
     * - https://xueqiu.com/hq             雪球>行情中心>沪深股市>沪深一览
     *
     * - https://q.10jqka.com.cn/          同花顺>个股行情
     *
     * - https://stockapp.finance.qq.com/mstats/#mod=list&id=hs_hsj&module=hs&type=hsj&sort=10&page=1&max=20          腾讯证券>首页>沪深股市>沪深京
     *
     * - https://www.futunn.com/hk/quote/cn/stock-list/all-cn-stocks/top-market-cap          富途牛牛>沪深市场
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    @SneakyThrows
    private static String allStockKlineUrl(int pageNum, int pageSize) {


        // https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page=2&num=200&sort=symbol&asc=0&node=hs_a&symbol=&_s_r_a=page


        String url = "https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?" +

                "&_s_r_a=page" +
                "&symbol=" +
                "&node=hs_a" +

                // 排序
                "&sort=symbol" +      // symbol - 股票code
                "&asc=0" +            // 0-正序（小->大）

                // 分页
                "&page=" + pageNum +
                "&num=" + pageSize;          // pageSize  ->  最大值100（前端页面：80）


        return url;
    }


    /**
     * 新浪财经 > 基金数据中心首页 > 基金行情 > ETF基金
     *
     * -                                        https://vip.stock.finance.sina.com.cn/fund_center/index.html#jjhqetf
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    @SneakyThrows
    private static String allETFKlineUrl(int pageNum, int pageSize) {


        // https://vip.stock.finance.sina.com.cn/quotes_service/api/jsonp.php/IO.XSRV2.CallbackList['1KnUEEthTLUr9FAR']/Market_Center.getHQNodeDataSimple?page=1&num=80&sort=symbol&asc=0&node=etf_hq_fund&%5Bobject%20HTMLDivElement%5D=n0r9


        // page=1&num=80&sort=symbol&asc=0&node=etf_hq_fund&%5Bobject%20HTMLDivElement%5D=n0r9


        String url = "https://vip.stock.finance.sina.com.cn/quotes_service/api/jsonp.php/IO.XSRV2.CallbackList['1KnUEEthTLUr9FAR']/Market_Center.getHQNodeDataSimple?" +

                "&node=etf_hq_fund" +

                // 排序
                "&sort=symbol" +      // f12 - 股票code
                "&asc=0" +            // 0-正序（小->大）


                // 分页
                "&page=" + pageNum +
                "&num=" + pageSize;        // pageSize  ->  最大值100


        return url;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {

        String stockCode = "300059";


        long start = System.currentTimeMillis();


        List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS = pullAllStockSnapshotKline();
        // List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS = pullAllETFSnapshotKline();
        // List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS = allStockETFSnapshotKline();


        System.out.println(stockSnapshotKlineDTOS.size());


        System.out.println("耗时：" + DateTimeUtil.formatNow2Hms(start));
    }


}