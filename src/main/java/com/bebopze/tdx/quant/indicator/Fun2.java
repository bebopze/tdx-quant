package com.bebopze.tdx.quant.indicator;

import com.bebopze.tdx.quant.client.EastMoneyKlineAPI;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.bebopze.tdx.quant.common.tdxfun.TdxExtFun.SSF;
import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.MA;


/**
 * 基础指标   -   序列化（返回 数组）
 *
 * @author: bebopze
 * @date: 2025/5/16
 */
@Slf4j
public class Fun2 {

    private String stockCode;


    // 实时行情  -  买5/卖5
    private SHSZQuoteSnapshotResp shszQuoteSnapshotResp;


    // 历史行情
    private List<KlineDTO> klineDTOList;
    // 实时行情
    // private KlineDTO lastKlineDTO;


    private double C;

    private Object[] date_arr;

    private double[] close_arr;

    private double[] ssf_arr = SSF(close_arr);


    // -----------------------------------------------------------------------------------------------------------------


    public Fun2(String stockCode) {
        initData(stockCode, null);
    }


    /**
     * 加载 行情数据
     *
     * @param stockCode 股票code
     * @param limit     N日
     */
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


        // 收盘价 - 实时
        double C = realtimequote.getCurrentPrice().doubleValue();


        // 历史行情
        List<KlineDTO> klineDTOList = ConvertStock.str2DTO(stockKlineHisResp.getKlines(), limit);


        Object[] date_arr = ConvertStock.objFieldValArr(klineDTOList, "date");
        double[] close_arr = ConvertStock.fieldValArr(klineDTOList, "close");


        // --------------------------- init data

        this.stockCode = stockCode;

        this.shszQuoteSnapshotResp = shszQuoteSnapshotResp;
        this.klineDTOList = klineDTOList;

        this.C = C;

        this.date_arr = date_arr;
        this.close_arr = close_arr;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                指标
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
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        boolean[] 上MA = 上MA(N);
        boolean[] MA向上 = MA向上(N);


        for (int i = 0; i < len; i++) {
            arr[i] = 上MA[i] && MA向上[i];
        }

        return arr;
    }


    public boolean[] MA空(int N) {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        boolean[] 下MA = 下MA(N);
        boolean[] MA向下 = MA向下(N);


        for (int i = 0; i < len; i++) {
            arr[i] = 下MA[i] && MA向下[i];
        }

        return arr;
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
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        boolean[] 上SSF = 上SSF();
        boolean[] SSF向上 = SSF向上();


        for (int i = 0; i < len; i++) {
            arr[i] = 上SSF[i] && SSF向上[i];
        }

        return arr;
    }


    public boolean[] SSF空() {
        int len = close_arr.length;
        boolean[] arr = new boolean[len];


        boolean[] 下SSF = 下SSF();
        boolean[] SSF向下 = SSF向下();


        for (int i = 0; i < len; i++) {
            arr[i] = 下SSF[i] && SSF向下[i];
        }

        return arr;
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


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {

        String stockCode = "300059";


        Fun2 fun = new Fun2(stockCode);


        // 1、下MA50
        boolean[] 下MA50 = fun.下MA(50);


        // 2、MA空(20)
        boolean[] MA20_空 = fun.MA空(20);


        boolean[] 下MA100 = fun.MA空(100);


        boolean[] con = con_merge(下MA50, MA20_空, 下MA100);


        // 3、RPS三线 < 85


    }


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

}