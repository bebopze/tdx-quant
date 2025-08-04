package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.domain.dto.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineListResp;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineTrendResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.EventStreamUtil;
import com.bebopze.tdx.quant.common.util.HttpUtil;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


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


        // List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS = allStockSnapshotKline();
        // System.out.println(stockSnapshotKlineDTOS);


        // StockKlineHisResp stockKlineHisResp = stockKlineHis(KlineTypeEnum.DAY, stockCode);
        // System.out.println(stockKlineHisResp);


        // StockKlineTrendResp stockKlineTrends = stockKlineTrends(stockCode);
        // System.out.println(stockKlineTrends);
    }


    // -----------------------------------------------------------------------------------------------------------------


    // https://push2.eastmoney.com/api/qt/clist/get?np=1&fltt=1&invt=2&cb=jQuery37104509600548884083_1754172795547&fs=m%3A0%2Bt%3A6%2Cm%3A0%2Bt%3A80%2Cm%3A1%2Bt%3A2%2Cm%3A1%2Bt%3A23%2Cm%3A0%2Bt%3A81%2Bs%3A2048&fields=f12%2Cf13%2Cf14%2Cf1%2Cf2%2Cf4%2Cf3%2Cf152%2Cf5%2Cf6%2Cf7%2Cf15%2Cf18%2Cf16%2Cf17%2Cf10%2Cf8%2Cf9%2Cf23&fid=f6&pn=1&pz=20&po=1&dect=1&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=%7C0%7C0%7C0%7Cweb&_=1754172795561


    // https://push2.eastmoney.com/api/qt/clist/get?np=1&fltt=1&invt=2&cb=jQuery37104509600548884083_1754172795547&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048&fields=f12,f13,f14,f1,f2,f4,f3,f152,f5,f6,f7,f15,f18,f16,f17,f10,f8,f9,f23&fid=f6&pn=1&pz=20&po=1&dect=1&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=|0|0|0|web&_=1754172795561


    // https://push2.eastmoney.com/api/qt/clist/get?

    // np=1             ->  不变
    // fltt=1           ->  不变
    // invt=2           ->  不变


    // fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048                                 ->  不变
    // fields=f12,f13,f14,f1,f2,f4,f3,f152,f5,f6,f7,f15,f18,f16,f17,f10,f8,f9,f23           ->  不变


    // fid=f12     ->     排序字段（不变）                  f12 - 股票code
    // po=0        ->     0-正序；1-倒序；（不变）


    // pn=1      ->   页数
    // pz=100    ->   最大100（不变）


    // dect=1               ->  不变
    // wbp2u=|0|0|0|web     ->  不变

    // _=1754172795561     ->     ms时间戳（ now() ）


    // cb=jQuery37104509600548884083_1754172795547     - DEL
    // ut=fa5fd1943c7b386f172d6893dbfba10b             - DEL


    /**
     * 东方财富  ->  批量拉取 全A 实时行情
     *
     * @return
     */
    public static List<StockSnapshotKlineDTO> allStockSnapshotKline() {


        int pageNum = 1;
        int pageSize = 100;


        LocalDate date = lastTradeDate();


        List<StockSnapshotKlineDTO> dtoList = Lists.newArrayList();


        // ------------------------------------------------------------------


        while (true) {

            String url = allStockKlineUrl(pageNum++, pageSize);

            String result = HttpUtil.doGet(url, null);


            JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
            if (resultJson.getInteger("rc") == 0) {
                log.info("/api/qt/clist/get   suc     >>>     pageNum : {} , pageSize : {} , result : {}", pageNum - 1, pageSize, result);
            }


            JSONArray jsonArray = resultJson.getJSONObject("data").getJSONArray("diff");

            List<StockKlineListResp> dataList = jsonArray.stream()
                                                         .map(e -> {

                                                             JSONObject json = (JSONObject) e;

                                                             // 包含大量 退市/停牌 个股（东方财富 A股 -> 5735）
                                                             if (isErrStock(json)) {
                                                                 return null;
                                                             }

                                                             return json.toJavaObject(StockKlineListResp.class);

                                                         }).filter(Objects::nonNull).collect(Collectors.toList());


            // ------------------------------------------------------


            // dataList  ->  dtoList
            List<StockSnapshotKlineDTO> pageDTOList = convert2StockSnapshotDTOList(dataList, date);
            dtoList.addAll(pageDTOList);


            // ------------------------------------------------------


            if (jsonArray.size() < pageSize || dtoList.size() > 7000 || pageNum > 70) {
                break;
            }


            // 间隔0.5s
            SleepUtils.winSleep(500);
        }


        return dtoList;
    }


    /**
     * A股   最近一个 交易日
     *
     * @return
     */
    private static LocalDate lastTradeDate() {
        SHSZQuoteSnapshotResp resp = EastMoneyTradeAPI.SHSZQuoteSnapshot("300059");
        return resp.getRealtimequote().getDate();
    }


    /**
     * 过滤   ->   退市/停牌 个股
     *
     * @param json
     * @return
     */
    private static boolean isErrStock(JSONObject json) {
        // 任意 v : "-"     ->     false（退市/停牌）
        return json.values().stream().anyMatch("-"::equals);
    }


    /**
     * dataList  ->  dtoList
     *
     * @param dataList
     * @param date
     * @return
     */
    private static List<StockSnapshotKlineDTO> convert2StockSnapshotDTOList(List<StockKlineListResp> dataList,
                                                                            LocalDate date) {


        return dataList.parallelStream().map(e -> {

            StockSnapshotKlineDTO dto = new StockSnapshotKlineDTO();

            dto.setStockCode(e.getF12());
            dto.setStockName(e.getF14());


            dto.setDate(date);

            dto.setOpen(e.getF17());
            dto.setHigh(e.getF15());
            dto.setLow(e.getF16());
            dto.setClose(e.getF2());


            dto.setVol(e.getF5());
            dto.setAmo(e.getF6());

            dto.setRange_pct(e.getF7());
            dto.setChange_pct(e.getF3());
            dto.setChange_price(e.getF4());
            dto.setTurnover_pct(e.getF8());


            return dto;
        }).collect(Collectors.toList());

    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 个股/板块 - 全量 历史行情
     *
     * @param stockCode
     * @param klineTypeEnum
     * @return
     */
    public static StockKlineHisResp stockKlineHis(String stockCode, KlineTypeEnum klineTypeEnum) {
        return stockKlineHis(stockCode, klineTypeEnum, null);
    }


    /**
     * 个股/板块 - 全量 历史行情
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
     * @param limit         行情数据天数
     * @return
     */
    public static StockKlineHisResp stockKlineHis(String stockCode, KlineTypeEnum klineTypeEnum, Integer limit) {


        String url = stockKlineHisUrl(stockCode, klineTypeEnum.getEastMoneyType(), limit);


        String result = HttpUtil.doGet(url, null);


        JSONObject resultJson = JSON.parseObject(result, JSONObject.class);
        if (resultJson.getInteger("rc") == 0) {
            log.info("/api/qt/stock/kline/get   suc     >>>     klineType : {} , stockCode : {} , result : {}",
                     klineTypeEnum.getDesc(), stockCode, result);
        }


        StockKlineHisResp resp = resultJson.getJSONObject("data").toJavaObject(StockKlineHisResp.class);


        // ----------------------------- 历史行情
        List<String> klines = resp.getKlines();


        return resp;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 最后一日  实时行情数据
     *
     * @param stockCode
     * @return
     */
    public static List<String> stockKlineLastN(String stockCode) {

        SHSZQuoteSnapshotResp resp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);

        SHSZQuoteSnapshotResp.RealtimequoteDTO e = resp.getRealtimequote();


        // 2025-05-13,21.06,21.97,20.89,21.45,8455131,18181107751.03,5.18,2.98,0.62,6.33
        // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率

        // 历史行情-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）


        List<Object> kline = Lists.newArrayList(String.valueOf(e.getDate()), e.getOpen(), e.getHigh(), e.getLow(), e.getCurrentPrice(), e.getVolume(), e.getAmount(),
                                                e.getRangePct(), e.getZdf(), e.getZd(), e.getTurnover());


        String klineStr = kline.stream().map(obj -> obj != null ? obj.toString() : "").collect(Collectors.joining(","));
        return Lists.newArrayList(klineStr);
    }


    // -----------------------------------------------------------------------------------------------------------------


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


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 东方财富  ->  批量拉取 全A 实时行情
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


        // https://push2.eastmoney.com/api/qt/clist/get?

        // np=1             ->  不变
        // fltt=1           ->  不变
        // invt=2           ->  不变


        // fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048                                 ->  不变
        // fields=f12,f13,f14,f1,f2,f4,f3,f152,f5,f6,f7,f15,f18,f16,f17,f10,f8,f9,f23           ->  不变


        // fid=f12     ->     排序字段（不变）                  f12 - 股票code
        // po=0        ->     0-正序；1-倒序；（不变）


        // pn=1      ->   页数
        // pz=100    ->   最大100（不变）


        // dect=1               ->  不变
        // wbp2u=|0|0|0|web     ->  不变

        // _=1754172795561     ->     ms时间戳（ now() ）


        // cb=jQuery37104509600548884083_1754172795547     - DEL
        // ut=fa5fd1943c7b386f172d6893dbfba10b             - DEL


        String url = "https://push2.eastmoney.com/api/qt/clist/get?" +
                "fields=f12,f13,f14,f1,f2,f4,f3,f152,f5,f6,f7,f15,f18,f16,f17,f10,f8,f9,f23" +
                // "&fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048" +
                "&fs=" + URLEncoder.encode("m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23,m:0+t:81+s:2048", "UTF-8") +

                "&np=1" +
                "&fltt=1" +
                "&invt=2" +

                "&dect=1" +
                // "&wbp2u=|0|0|0|web" +
                "&wbp2u=" + URLEncoder.encode("|0|0|0|web", "UTF-8") +


                // 排序
                "&fid=f12" +         // f12 - 股票code
                "&po=0" +            // 0-正序（小->大）

                // 分页
                "&pn=" + pageNum +
                "&pz=" + pageSize +       // pageSize  ->  最大值100

                // 时间戳（ms）
                "&_=" + System.currentTimeMillis();


        return url;
    }


    /**
     * 个股行情 - url拼接
     *
     * @param stockCode 证券代码
     * @param klt       K线 - 类型
     * @param limit
     * @return
     */
    private static String stockKlineHisUrl(String stockCode, Integer klt, Integer limit) {


        // secid=90.BK1090     - 板块
        // String secid_bk = String.format("90.%s", stockCode);


        // 0-深圳；1-上海；2-北京；
        Integer tdxMarketType = StockMarketEnum.getTdxMarketType(stockCode);
        // 深圳+北京 -> 0（缺省值 -> ETF）
        Integer marketType = Objects.equals(tdxMarketType, 1) ? 1 : 0;


        // secid=0.300059      - 个股
        String secid = String.format("%s.%s", marketType, stockCode);


        // 截止日期
        String end = DateTimeUtil.format_yyyyMMdd(LocalDate.now());
        // 日K   ->   limit为空（不限制）
        String limitStr = limit != null ? String.valueOf(limit) :
                Objects.equals(klt, KlineTypeEnum.DAY.getEastMoneyType()) ? "" : "10000";


        String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get?" +
                "fields1=f1,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f12,f13" +
                "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61" +
                "&beg=0" +
                "&rtntype=6" +

                "&fqt=1" +

                "&klt=" + klt +

                "&end=" + end +
                "&lmt=" + limitStr +
                "&secid=" + secid;


        return url;
    }

}