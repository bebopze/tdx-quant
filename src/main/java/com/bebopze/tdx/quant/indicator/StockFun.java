package com.bebopze.tdx.quant.indicator;

import com.bebopze.tdx.quant.common.constant.StockLimitEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.domain.dto.fun.MidAdjustResult;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;

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


    // ------------------------------------

    // K线数据
    List<KlineDTO> klineDTOList;
    // 扩展数据（预计算 指标）
    List<ExtDataDTO> extDataDTOList;


    KlineArrDTO klineArrDTO;
    ExtDataArrDTO extDataArrDTO;


    // ------------------------------------


    Map<LocalDate, Integer> dateIndexMap;
    int maxIdx;


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


    public StockFun(BaseStockDO stockDO) {
        this(stockDO == null ? null : stockDO.getCode(), stockDO);
    }


    public StockFun(String code, BaseStockDO stockDO) {
        Assert.notNull(stockDO, String.format("stockDO:[%s] is null  ->  请检查 dataCache 是否为null", code));
        long start = System.currentTimeMillis();


        String stockName = stockDO.getName();


        // K线数据
        klineDTOList = stockDO.getKlineDTOList();
        // 扩展数据（预计算 指标）
        extDataDTOList = stockDO.getExtDataDTOList();


        // -----------------------------------------------


        klineArrDTO = ConvertStock.kline__dtoList2Arr(klineDTOList);
        extDataArrDTO = ConvertStock.extData__dtoList2Arr(extDataDTOList);


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

        maxIdx = Math.max(0, date.length - 1);


        // --------------------------- init data


        this.code = code;
        this.name = stockName;


        this.shszQuoteSnapshotResp = null;


        ssf = extDataArrDTO.SSF;


        // -------------------------------------------------------------------------------------------------------------
        log.info("StockFun - init     >>>     [{}-{}] , time : {}", code, name, DateTimeUtil.formatNow2Hms(start));
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


    public double[] MA(int N) {
        return TdxFun.MA(close, N);
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

    public double[] SAR() {
        return TdxFun.TDX_SAR(high, low);
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
        return TdxExtFun.SSF多(close, ArrayUtils.isEmpty(ssf) ? SSF() : ssf);
    }


    public boolean[] SSF空() {
        return TdxExtFun.SSF空(close, ssf);
    }


    // -------------------------------------------- 中期涨幅


    public double[] 中期涨幅N(int N) {
        return TdxExtFun.中期涨幅N(high, low, close, N);
    }

    public double[] N日涨幅(int N) {
        return TdxExtFun.changePct(close, N);
    }

    public MidAdjustResult 中期调整() {
        return TdxExtFun.中期调整(high, low, close);
    }


    // 高位-爆量/上影/大阴
    public boolean[] 上影大阴() {
        return TdxExtFun.上影大阴(high, low, close, is20CM());
    }

    // 高位-爆量/上影/大阴
    public boolean[] 高位爆量上影大阴() {
        return TdxExtFun.高位爆量上影大阴(high, low, close, amo, is20CM(), date);
    }


    public boolean[] 涨停() {
        return TdxExtFun.涨停(close, chgPctLimit());
    }

    public boolean[] 跌停() {
        return TdxExtFun.跌停(close, chgPctLimit());
    }


    public boolean is20CM() {
        return StockLimitEnum.is20CM(code, name);
    }

    public Integer chgPctLimit() {
        return StockLimitEnum.getChgPctLimit(code, name);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  趋势指标
    // -----------------------------------------------------------------------------------------------------------------


    // -------------------------------------------- C/MA  偏离率（趋势）
    // -------------------------------------------- C/SSF 偏离率（趋势）


    public double C_SSF_偏离率(int idx) {
        double[] result = TdxExtFun.C_SSF_偏离率(new double[]{close[idx]}, new double[]{ssf[idx]});
        return result[0];
    }

    public double[] C_SSF_偏离率() {
        return TdxExtFun.C_SSF_偏离率(close, ssf);
    }

    public double[] H_SSF_偏离率() {
        return TdxExtFun.C_SSF_偏离率(high, ssf);
    }

    public double[] C_MA_偏离率(int N) {
        return TdxExtFun.C_MA_偏离率(close, N);
    }

    public double[] H_MA_偏离率(int N) {
        return TdxExtFun.H_MA_偏离率(high, close, N);
    }


    public int[] 短期趋势支撑线() {
        return TdxExtFun.短期趋势支撑线(close);
    }

    public int[] 中期趋势支撑线(int[] 短期趋势支撑线) {
        return TdxExtFun.中期趋势支撑线(close, 短期趋势支撑线);
    }

    public int[] 长期趋势支撑线(int[] 中期趋势支撑线) {
        return TdxExtFun.中期趋势支撑线(close, 中期趋势支撑线);
    }


    public void 趋势股() {
        // TODO   趋势股
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

    public boolean[] 历史新高() {

        boolean[] 历史新高_H_arr = TdxExtFun.历史新高(high);
        boolean[] 历史新高_C_arr = TdxExtFun.历史新高(close);


        // H新高 || C新高
        return con_or(历史新高_H_arr, 历史新高_C_arr);
    }


    public boolean[] 均线预萌出() {
        return TdxExtFun.均线预萌出(close);
    }


    public boolean[] 均线萌出() {
        return TdxExtFun.均线萌出(close);
    }


    public boolean[] 小均线多头() {
        return TdxExtFun.小均线多头(close);
    }

    public boolean[] 大均线多头() {
        return TdxExtFun.大均线多头(close);
    }

    public boolean[] 均线大多头() {
        return TdxExtFun.均线大多头(close);
    }

    public boolean[] 均线极多头() {
        return TdxExtFun.均线极多头(close);
    }


    public int[] klineType() {
        return TdxExtFun.klineType(close);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  复杂指标
    // -----------------------------------------------------------------------------------------------------------------


    public boolean[] 月多() {
        return TdxExtFun.月多(date, open, high, low, close);
    }

    public boolean[] 口袋支点(double[] MA10,
                              double[] MA20,
                              double[] MA50,
                              double[] MA100,
                              double[] MA120,
                              double[] MA200,
                              double[] MA250,
                              double[] SAR,
                              MidAdjustResult 中期调整,
                              boolean[] RPS红,
                              boolean[] 均线预萌出,
                              boolean[] N60日新高,
                              double[] 中期涨幅N20,
                              boolean[] 上影大阴) {


        return TdxExtFun.口袋支点(open, high, low, close, amo, MA10, MA20, MA50, MA100, MA120, MA200, MA250, SAR, 中期调整, RPS红, 均线预萌出, N60日新高, 中期涨幅N20, 上影大阴);
    }


    public boolean[] XZZB() {
        return TdxExtFun.XZZB(high, low, close);
    }

    public boolean[] BSQJ() {
        return TdxExtFun.BSQJ(close);
    }


    public double[] RPS三线和() {
        return TdxExtFun.RPS三线和(rps10, rps20, rps50, rps120, rps250);
    }

    public boolean[] RPS三线和2(double RPS) {
        return TdxExtFun.RPS三线和2(rps10, rps20, rps50, rps120, rps250, RPS);
    }

    public double[] RPS五线和() {
        return TdxExtFun.RPS五线和(rps10, rps20, rps50, rps120, rps250);
    }


    public boolean[] RPS一线红(double RPS) {
        return TdxExtFun.RPS一线红(rps50, rps120, rps250, RPS);
    }

    public boolean[] RPS双线红(double RPS) {
        return TdxExtFun.RPS双线红(rps50, rps120, rps250, RPS);
    }

    public boolean[] RPS三线红(double RPS) {
        return TdxExtFun.RPS三线红(rps10, rps20, rps50, rps120, rps250, RPS);
    }

    public boolean[] 首次三线红(double RPS) {
        return TdxExtFun.首次三线红(rps10, rps20, rps50, rps120, rps250, RPS);
    }


    public boolean[] RPS99() {
        return TdxExtFun.RPS一线红(rps50, rps120, rps250, 99);
    }


    public boolean[] RPS红(double RPS) {
        // RPS一线红(95) || RPS双线红(90) || RPS三线红(85);
        return TdxExtFun.RPS红(rps10, rps20, rps50, rps120, rps250, RPS);
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
        double[] N3日涨幅 = N日涨幅(3);
        for (int i = 0; i < N3日涨幅.length; i++) {
            con_3[i] = N3日涨幅[i] >= -10;
        }


        boolean[] con_4 = con_sumCompare(3, MA多(5), MA多(10), MA多(20), MA多(50));


        boolean[] con_A = con_and(con_1, con_2, con_3, con_4);


        // --------------------------------------------------------------- RPS / 均线形态


        boolean[] con_5 = TdxExtFun.RPS红(rps10, rps20, rps50, rps120, rps250, 85);
        boolean[] con_6 = TdxExtFun.大均线多头(close);
        boolean[] con_7 = TdxExtFun.均线预萌出(close);

        boolean[] con_B = con_or(con_5, con_6, con_7);


        return TdxExtFun.con_and(con_A, con_B);
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


        // -------------------------------------------------------------------------------------------------------------


        // 3日 最大跌幅
        Integer chgPctLimit = chgPctLimit();
        double N3_df_pctLimit = chgPctLimit == 5 ? -7 : chgPctLimit == 10 ? -10 : chgPctLimit == 20 ? -12 : chgPctLimit == 30 ? -15 : -10;


        // --------------------------------------------------------------- N日新高[近5日] / SSF多 / N日涨幅[非-妖顶]


        // 1、近5日内 创百日新高
        boolean[] con_1 = int2Bool(COUNT(N日新高(N), 7)); // 近7日


        // 2、未 大幅回落

        // MA5 支撑线
        boolean[] con_2_1 = MA多(5);
        // MA形态（小均线多头）
        boolean[] con_2_2 = con_sumCompare(3, MA多(5), MA多(10), MA多(20), MA多(50));
        boolean[] con_2 = con_or(con_2_1, con_2_2);

        // SSF多
        boolean[] con_3 = SSF多();

        // 禁止跌停（排除：高位妖股）
        boolean[] con_4 = negated(跌停());   // 跌停 -> 取反

        // 窄幅波动（3日涨跌幅 > -10%）
        boolean[] con_5 = new boolean[close.length];
        double[] N3日涨幅 = N日涨幅(3);
        for (int i = 0; i < N3日涨幅.length; i++) {
            con_5[i] = N3日涨幅[i] >= N3_df_pctLimit;
        }


        return con_and(con_1, con_2, con_3, con_4, con_5);
    }


    public boolean[] 二阶段() {

        return null;
    }

    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 最后一天 数据
     *
     * @param arr
     * @return
     */
    public boolean last(boolean[] arr) {
        return arr[arr.length - 1];
    }


}