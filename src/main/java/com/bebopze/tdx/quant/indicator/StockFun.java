package com.bebopze.tdx.quant.indicator;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.EastMoneyKlineAPI;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.config.FastJson2Config;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.google.common.collect.Maps;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.bebopze.tdx.quant.common.tdxfun.TdxExtFun.SSF;
import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.MA;


/**
 * 基础指标   -   序列化（返回 数组）
 *
 * @author: bebopze
 * @date: 2025/5/16
 */
@Slf4j
@NoArgsConstructor
public class StockFun {

    String stockCode;
    String stockName;


    // 实时行情  -  买5/卖5
    SHSZQuoteSnapshotResp shszQuoteSnapshotResp;


    // 历史行情
    List<KlineDTO> klineDTOList;
    // 实时行情
    // private KlineDTO lastKlineDTO;


    double C;

    Object[] date_arr;

    double[] close_arr;
    double[] high_arr;


    double[] ssf_arr;// = SSF(close_arr);


    double[] rps50_arr;
    double[] rps120_arr;
    double[] rps250_arr;


    // -----------------------------------------------------------------------------------------------------------------
    //                                            个股 - 行情数据 init
    // -----------------------------------------------------------------------------------------------------------------


    public StockFun(String stockCode, BaseStockDO baseStockDO) {


        // limit = limit == null ? 500 : limit;


        // --------------------------- HTTP 获取   个股行情 data

        // 实时行情 - API
        // SHSZQuoteSnapshotResp shszQuoteSnapshotResp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
        // SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequote = shszQuoteSnapshotResp.getRealtimequote();


        // 历史行情 - API
        // StockKlineHisResp stockKlineHisResp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);


        // -------------------------------------------------------------------------------------------------------------

        // --------------------------- resp -> DTO


        // String stockName = shszQuoteSnapshotResp.getName();
        String stockName = baseStockDO.getName();


        // 历史行情
        List<KlineDTO> klineDTOList = baseStockDO.getKLineHis();


        // last
        KlineDTO klineDTO = klineDTOList.get(klineDTOList.size() - 1);


        // 收盘价 - 实时
        double C = klineDTO.getClose().doubleValue();


        // 历史行情
        // List<KlineDTO> klineDTOList = ConvertStockKline.strList2DTOList(stockKlineHisResp.getKlines(), limit);


        Object[] date_arr = ConvertStockKline.objFieldValArr(klineDTOList, "date");
        double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");
        double[] high_arr = ConvertStockKline.fieldValArr(klineDTOList, "high");


        // TODO   RPS（预计算） -> DB获取


//        double[] rps50_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 50);
//        double[] rps120_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 120);
//        double[] rps250_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 250);


        // --------------------------- init data


        this.stockCode = stockCode;
        this.stockName = stockName;


        this.shszQuoteSnapshotResp = null;
        this.klineDTOList = klineDTOList;

        this.C = C;

        this.date_arr = date_arr;
        this.close_arr = close_arr;
        this.high_arr = high_arr;


        this.ssf_arr = SSF(close_arr);


        this.rps50_arr = rps50_arr;
        this.rps120_arr = rps120_arr;
        this.rps250_arr = rps250_arr;
    }


    public StockFun(String stockCode) {

        // 个股
        initData(stockCode, null);
    }


    /**
     * 加载   个股-行情数据
     *
     * @param stockCode 股票code
     * @param limit     N日
     */
    @SneakyThrows
    public void initData(String stockCode, Integer limit) {

        limit = limit == null ? 500 : limit;


        // --------------------------- HTTP 获取   个股行情 data

        // 实时行情 - API
        SHSZQuoteSnapshotResp shszQuoteSnapshotResp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
        SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequote = shszQuoteSnapshotResp.getRealtimequote();


        // 历史行情 - API
        StockKlineHisResp stockKlineHisResp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);


        // -------------------------------------------------------------------------------------------------------------

        // --------------------------- resp -> DTO


        String stockName = shszQuoteSnapshotResp.getName();


        // 收盘价 - 实时
        double C = realtimequote.getCurrentPrice().doubleValue();


        // 历史行情
        List<KlineDTO> klineDTOList = ConvertStockKline.strList2DTOList(stockKlineHisResp.getKlines(), limit);


        Object[] date_arr = ConvertStockKline.objFieldValArr(klineDTOList, "date");
        double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");
        double[] high_arr = ConvertStockKline.fieldValArr(klineDTOList, "high");


        // TODO   RPS（预计算） -> DB获取


//        double[] rps50_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 50);
//        double[] rps120_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 120);
//        double[] rps250_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 250);


        // --------------------------- init data


        this.stockCode = stockCode;
        this.stockName = stockName;


        this.shszQuoteSnapshotResp = shszQuoteSnapshotResp;
        this.klineDTOList = klineDTOList;

        this.C = C;

        this.date_arr = date_arr;
        this.close_arr = close_arr;
        this.high_arr = high_arr;


        this.ssf_arr = SSF(close_arr);


        this.rps50_arr = rps50_arr;
        this.rps120_arr = rps120_arr;
        this.rps250_arr = rps250_arr;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  基础指标
    // -----------------------------------------------------------------------------------------------------------------


    public boolean[] 上MA(int N) {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        // MA20
        double[] MA20_arr = MA(close_arr, N);


        for (int i = 0; i < len; i++) {
            double MA20 = MA20_arr[i];
            double C = close_arr[i];

            arr[i] = C >= MA20;
        }

        return arr;
    }

    public boolean[] 下MA(int N) {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        // MA20
        double[] MA20_arr = MA(close_arr, N);


        for (int i = 0; i < len; i++) {
            double MA20 = MA20_arr[i];
            double C = close_arr[i];

            arr[i] = C < MA20;
        }

        return arr;
    }


    public boolean[] MA向上(int N) {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        // MA20
        double[] MA20_arr = MA(close_arr, N);


        for (int i = 0; i < len; i++) {

            if (i == 0) {
                arr[i] = false;

            } else {
                double MA20 = MA20_arr[i];
                double MA20_pre = MA20_arr[i - 1];

                arr[i] = MA20 >= MA20_pre;
            }
        }

        return arr;
    }


    public boolean[] MA向下(int N) {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        // MA20
        double[] MA20_arr = MA(close_arr, N);


        for (int i = 0; i < len; i++) {


            if (i == 0) {
                arr[i] = false;

            } else {
                double MA20 = MA20_arr[i];
                double MA20_pre = MA20_arr[i - 1];

                arr[i] = MA20 < MA20_pre;
            }
        }

        return arr;
    }


    public boolean[] MA多(int N) {
        return con_merge(上MA(N), MA向上(N));
    }


    public boolean[] MA空(int N) {
        return con_merge(下MA(N), MA向下(N));
    }


    // -------------------------------------------- SSF


    public boolean[] 上SSF() {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        for (int i = 0; i < len; i++) {
            double SSF = ssf_arr[i];
            double C = close_arr[i];

            arr[i] = C >= SSF;
        }

        return arr;
    }

    public boolean[] 下SSF() {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        for (int i = 0; i < len; i++) {
            double SSF = ssf_arr[i];
            double C = close_arr[i];

            arr[i] = C < SSF;
        }

        return arr;
    }


    public boolean[] SSF向上() {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        for (int i = 0; i < len; i++) {

            if (i == 0) {
                arr[i] = false;

            } else {
                double SSF = ssf_arr[i];
                double SSF_pre = ssf_arr[i - 1];

                arr[i] = SSF >= SSF_pre;
            }
        }

        return arr;
    }

    public boolean[] SSF向下() {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        for (int i = 0; i < len; i++) {

            if (i == 0) {
                arr[i] = false;

            } else {
                double SSF = ssf_arr[i];
                double SSF_pre = ssf_arr[i - 1];

                arr[i] = SSF < SSF_pre;
            }
        }

        return arr;
    }


    public boolean[] SSF多() {
        return con_merge(上SSF(), SSF向上());
    }


    public boolean[] SSF空() {
        return con_merge(下SSF(), SSF向下());
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  高级指标
    // -----------------------------------------------------------------------------------------------------------------


    public boolean[] N日新高(int N) {

        boolean[] N日新高_H_arr = TdxExtFun.N日新高(high_arr, N);
        boolean[] N日新高_C_arr = TdxExtFun.N日新高(close_arr, N);


        // H新高 || C新高
        return con_or(N日新高_H_arr, N日新高_C_arr);
    }


    public boolean[] 大均线多头() {
        return TdxExtFun.大均线多头(close_arr);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  复杂指标
    // -----------------------------------------------------------------------------------------------------------------


    public boolean[] 月多() {
        boolean[] 月多_arr = TdxExtFun.月多(close_arr);
        return 月多_arr;
    }


    public boolean[] RPS三线红(int RPS) {
        return null;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 结果合并   -   AND
     *
     * @param arr_list
     * @return
     */
    public static boolean[] con_merge(boolean[]... arr_list) {

        int len = arr_list[0].length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {
            boolean acc = true;
            for (boolean[] arr : arr_list) {
                acc &= arr[i];
                if (!acc) break;
            }
            result[i] = acc;
        }

        return result;
    }


    /**
     * 结果合并   -   OR
     *
     * @param arr_list
     * @return
     */
    public static boolean[] con_or(boolean[]... arr_list) {

        int len = arr_list[0].length;
        boolean[] result = new boolean[len];


        for (int i = 0; i < len; i++) {
            boolean acc = false;
            for (boolean[] arr : arr_list) {
                acc = acc || arr[i];
                if (acc) break;
            }
            result[i] = acc;
        }

        return result;
    }


    /**
     * 最后一天 数据
     *
     * @param arr
     * @return
     */
    public boolean last(boolean[] arr) {
        return arr[arr.length - 1];
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        FastJson2Config fastJson2Config = new FastJson2Config();


        String stockCode = "300059";


        StockFun fun = new StockFun(stockCode);


        // 1、下MA50
        boolean[] 下MA50 = fun.下MA(50);


        // 2、MA空(20)
        boolean[] MA20_空 = fun.MA空(20);


        boolean[] 下MA100 = fun.MA空(100);


        double[] closeArr = fun.close_arr;
        // double[] ssf = SSF(closeArr);


        Object[] date_arr = fun.date_arr;
        double[] ssf_arr = fun.ssf_arr;
        boolean[] booleans = fun.SSF多();

        boolean[] con = con_merge(下MA50, MA20_空, 下MA100);


        // 3、RPS三线 < 85


        Map<String, BigDecimal> date_ssf_map = Maps.newTreeMap();

        for (int i = 0; i < date_arr.length; i++) {
            date_ssf_map.put(date_arr[i].toString(), NumUtil.double2Decimal(ssf_arr[i]));
        }

        System.out.println(JSON.toJSONString(date_ssf_map));
    }


}
