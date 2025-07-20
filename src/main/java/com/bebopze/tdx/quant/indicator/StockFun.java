package com.bebopze.tdx.quant.indicator;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.EastMoneyKlineAPI;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.config.FastJson2Config;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.constant.StockLimitEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.bebopze.tdx.quant.common.tdxfun.TdxExtFun.*;
import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.COUNT;
import static com.bebopze.tdx.quant.common.util.BoolUtil.*;


/**
 * 基础指标   -   序列化（返回 数组）
 *
 * @author: bebopze
 * @date: 2025/5/16
 */
@Slf4j
@Data
@NoArgsConstructor
public class StockFun {

    String code;
    String name;


    // 实时行情  -  买5/卖5
    SHSZQuoteSnapshotResp shszQuoteSnapshotResp;

    // 实时行情
    // private KlineDTO lastKlineDTO;


    // ------------------------------------

    // 历史行情
    List<KlineDTO> klineDTOList;
    // 扩展数据（预计算 指标）
    List<ExtDataDTO> extDataDTOList;


    KlineArrDTO klineArrDTO;
    ExtDataArrDTO extDataArrDTO;


    // ------------------------------------


    double C;


    Map<LocalDate, Integer> dateIndexMap;


    LocalDate[] date;
    double[] open;
    double[] high;
    double[] low;
    double[] close;
    long[] vol;
    double[] amo;


    double[] ssf;


    double[] rps10;
    double[] rps20;
    double[] rps50;
    double[] rps120;
    double[] rps250;


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                            个股 - 行情数据 init
    // -----------------------------------------------------------------------------------------------------------------


    public StockFun(String code, BaseStockDO stockDO) {


        String stockName = stockDO.getName();


        // 历史行情
        klineDTOList = stockDO.getKlineDTOList();
        // 扩展数据（预计算 指标）
        extDataDTOList = stockDO.getExtDataDTOList();


        // last
        KlineDTO klineDTO = klineDTOList.get(klineDTOList.size() - 1);


        // 收盘价 - 实时
        C = klineDTO.getClose();


        // -----------------------------------------------


        klineArrDTO = ConvertStock.dtoList2Arr(klineDTOList);
        extDataArrDTO = ConvertStock.dtoList2Arr2(extDataDTOList);


        // -----------------------------------------------

        date = klineArrDTO.date;

        open = klineArrDTO.open;
        high = klineArrDTO.high;
        low = klineArrDTO.low;
        close = klineArrDTO.close;

        vol = klineArrDTO.vol;
        amo = klineArrDTO.amo;


        // -----------------------------------------------


        rps10 = extDataArrDTO.rps10;
        rps20 = extDataArrDTO.rps20;
        rps50 = extDataArrDTO.rps50;
        rps120 = extDataArrDTO.rps120;
        rps250 = extDataArrDTO.rps250;


        // -----------------------------------------------


        dateIndexMap = Maps.newHashMap();
        for (int i = 0; i < date.length; i++) {
            dateIndexMap.put(date[i], i);
        }


        // --------------------------- init data


        this.code = code;
        this.name = stockName;


        this.shszQuoteSnapshotResp = null;


//        this.C = C;


//        this.dateIndexMap = dateIndexMap;
//
//
//        this.date_arr = date_arr;
//        this.open_arr = open_arr;
//        this.high_arr = high_arr;
//        this.low_arr = low_arr;
//        this.close_arr = close_arr;
//        this.vol_arr = vol_arr;
//        this.amo_arr = amo_arr;


//        this.rps10_arr = rps10_arr;
//        this.rps20_arr = rps20_arr;
//        this.rps50_arr = rps50_arr;
//        this.rps120_arr = rps120_arr;
//        this.rps250_arr = rps250_arr;


//         this.ssf_arr = SSF(close_arr);

        ssf = extDataArrDTO.SSF;
    }


    // -------------------------------------------------------------------------------------------------------------


    public StockFun(String code) {

        // 个股
        initData(code, null);
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
        List<KlineDTO> klineDTOList = ConvertStockKline.klines2DTOList(stockKlineHisResp.getKlines(), limit);


        LocalDate[] date_arr = ConvertStockKline.dateFieldValArr(klineDTOList, "date");
        double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");
        double[] high_arr = ConvertStockKline.fieldValArr(klineDTOList, "high");


        // TODO   RPS（预计算） -> DB获取


//        double[] rps50_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 50);
//        double[] rps120_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 120);
//        double[] rps250_arr = STOCK_RPS_CACHE.get(stockCode + "-" + 250);


        // --------------------------- init data


        this.code = stockCode;
        this.name = stockName;


        this.shszQuoteSnapshotResp = shszQuoteSnapshotResp;
        this.klineDTOList = klineDTOList;

        this.C = C;

        this.date = date_arr;
        this.close = close_arr;
        this.high = high_arr;


        this.ssf = SSF();


        this.rps50 = rps50;
        this.rps120 = rps120;
        this.rps250 = rps250;


    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  基础指标
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    public boolean[] 上MA(int N) {
        return TdxExtFun.上MA(close, N);
    }

    public boolean[] 下MA(int N) {
        return TdxExtFun.下MA(close, N);
    }


    public boolean[] MA向上(int N) {
        return TdxExtFun.MA向上(close, N);
    }


    public boolean[] MA向下(int N) {
        return TdxExtFun.MA向下(close, N);
    }


    public boolean[] MA多(int N) {
        return TdxExtFun.MA多(close, N);
    }


    public boolean[] MA空(int N) {
        return TdxExtFun.MA空(close, N);
    }


    // -------------------------------------------- SSF


    public double[] SSF() {
        ssf = TdxExtFun.SSF(close);
        return ssf;
    }


    public boolean[] 上SSF() {
        return TdxExtFun.上SSF(close, ssf);
    }

    public boolean[] 下SSF() {
        return TdxExtFun.下SSF(close, ssf);
    }


    public boolean[] SSF向上() {
        return TdxExtFun.SSF向上(close, ssf);
    }

    public boolean[] SSF向下() {
        return TdxExtFun.SSF向下(close, ssf);
    }


    public boolean[] SSF多() {
        return TdxExtFun.SSF多(close, ssf);
    }


    public boolean[] SSF空() {
        return TdxExtFun.SSF空(close, ssf);
    }


    // -------------------------------------------- 中期涨幅


    public double[] 中期涨幅N(int N) {
        return TdxExtFun.中期涨幅N(high, low, close, N);
    }

    // 高位-爆量/上影/大阴
    public boolean[] 高位爆量上影大阴() {
        return TdxExtFun.高位爆量上影大阴(high, low, close, amo, is20CM(), date);
    }


    public boolean is20CM() {
        return StockLimitEnum.is20CM(code, name);
    }


    // -------------------------------------------- C/SSF 偏离率


    public double C_SSF_偏离率(int idx) {
        double[] result = TdxExtFun.C_SSF_偏离率(new double[]{close[idx]}, new double[]{ssf[idx]});
        return result[0];
    }

    public double[] C_SSF_偏离率() {
        return TdxExtFun.C_SSF_偏离率(close, ssf);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  高级指标
    // -----------------------------------------------------------------------------------------------------------------


    public boolean[] N日新高(int N) {

        boolean[] N日新高_H_arr = TdxExtFun.N日新高(high, N);
        boolean[] N日新高_C_arr = TdxExtFun.N日新高(close, N);


        // H新高 || C新高
        return con_or(N日新高_H_arr, N日新高_C_arr);
    }


    public boolean[] 均线预萌出() {
        return TdxExtFun.均线预萌出(close);
    }


    public boolean[] 均线萌出() {
        return TdxExtFun.均线萌出(close);
    }


    public boolean[] 大均线多头() {
        return TdxExtFun.大均线多头(close);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  复杂指标
    // -----------------------------------------------------------------------------------------------------------------


    public boolean[] 月多() {
        return TdxExtFun.月多(date, open, high, low, close);
    }


    public double[] RPS三线和() {
        return TdxExtFun.RPS三线和(rps10, rps20, rps50, rps120, rps250);
    }

    public boolean[] RPS三线和2(double RPS) {
        return TdxExtFun.RPS三线和2(rps10, rps20, rps50, rps120, rps250, RPS);
    }


    public boolean[] RPS三线红(int RPS) {
        return TdxExtFun.RPS三线红(rps50, rps120, rps250, RPS);
    }


    public boolean[] RPS红(int RPS) {
        // RPS一线红(95) || RPS双线红(90) || RPS三线红(85);
        return TdxExtFun.RPS红(rps50, rps120, rps250, Math.max(RPS + 10, 100), Math.max(RPS + 5, 100), RPS);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  选股公式
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 创N日新高   ->    N日新高   +   形态(均线)  +  强度(RPS)   过滤
     *
     * @param N
     * @return
     */
    public boolean[] 创N日新高(int N) {


        // CON_1 :=  COUNT(N日新高(N),  5);
        // CON_2 :=  SSF多     AND     N日涨幅(3) > -10;
        // CON_3 :=  MA多(5) + MA多(10) + MA多(20) + MA多(50)  >=  3;

        // CON_4 :=  RPS一线红(95) || RPS双线红(90) || RPS三线红(85);
        // CON_5 :=  周多   ||   大均线多头;
        //
        // CON_1 AND CON_2 AND CON_3     AND     (CON_4 || CON_5);


        // --------------------------------------------------------------- N日新高[近5日] / SSF多 / N日涨幅[非-妖顶]


        boolean[] con_1 = int2Bool(COUNT(N日新高(N), 5));


        boolean[] con_2 = SSF多();


        boolean[] con_3 = new boolean[close.length];
        double[] N日涨幅 = changePct(close, 3);
        for (int i = 0; i < N日涨幅.length; i++) {
            con_3[i] = N日涨幅[i] >= -10;
        }


        boolean[] con_4 = con_sumCompare(3, MA多(5), MA多(10), MA多(20), MA多(50));


        boolean[] con_A = con_merge(con_1, con_2, con_3, con_4);


        // --------------------------------------------------------------- RPS / 均线形态


        boolean[] con_5 = TdxExtFun.RPS红(rps50, rps120, rps250, 95, 90, 85);
        boolean[] con_6 = TdxExtFun.大均线多头(close);
        boolean[] con_7 = TdxExtFun.均线预萌出(close);

        boolean[] con_B = con_or(con_5, con_6, con_7);


        return con_merge(con_A, con_B);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  统计指标（百日新高/...）
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 百日新高（开盘啦APP）     ->     近5日内创百日新高，并且未大幅回落
     *
     * @param N
     * @return
     */
    public boolean[] 百日新高(int N) {


        // CON_1 :=  COUNT(N日新高(N),  5);
        // CON_2 :=  SSF多     AND     N日涨幅(3) > -10;
        // CON_3 :=  MA多(5) + MA多(10) + MA多(20) + MA多(50)  >=  3;

        // CON_4 :=  RPS一线红(95) || RPS双线红(90) || RPS三线红(85);
        // CON_5 :=  周多   ||   大均线多头;
        //
        // CON_1 AND CON_2 AND CON_3     AND     (CON_4 || CON_5);


        // --------------------------------------------------------------- N日新高[近5日] / SSF多 / N日涨幅[非-妖顶]


        // 1、近5日内 创百日新高
        boolean[] con_1 = int2Bool(COUNT(N日新高(N), 5));


        // 2、未 大幅回落

        // MA10 支撑线
        boolean[] con_2 = MA多(10);

        // 3日涨跌幅 > -10%
        boolean[] con_3 = new boolean[close.length];
        double[] N日涨幅 = changePct(close, 3);
        for (int i = 0; i < N日涨幅.length; i++) {
            con_3[i] = N日涨幅[i] >= -10;
        }


        return con_merge(con_1, con_2, con_3);
    }


    // -----------------------------------------------------------------------------------------------------------------


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


        double[] closeArr = fun.close;
        // double[] ssf = SSF(closeArr);


        LocalDate[] date_arr = fun.date;
        double[] ssf_arr = fun.ssf;
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
