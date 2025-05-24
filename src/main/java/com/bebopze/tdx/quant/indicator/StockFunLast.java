package com.bebopze.tdx.quant.indicator;

import com.bebopze.tdx.quant.client.EastMoneyKlineAPI;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.common.constant.KlineTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.kline.StockKlineHisResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.List;

import static com.bebopze.tdx.quant.common.tdxfun.TdxExtFun.SSF;
import static com.bebopze.tdx.quant.common.tdxfun.TdxFun.*;


/**
 * 基础指标   -   非序列化（仅返回  最后一个交易日）
 *
 * @author: bebopze
 * @date: 2025/5/16
 */
@Slf4j
@Data
public class StockFunLast {

    private String stockCode;
    private String stockName;


    // 我的持仓
    private QueryCreditNewPosResp queryCreditNewPosResp;


    // 实时行情  -  买5/卖5
    private SHSZQuoteSnapshotResp shszQuoteSnapshotResp;


    // 历史行情
    private List<KlineDTO> klineDTOList;
    // 实时行情
    // private KlineDTO lastKlineDTO;


    private double C;

    private Object[] date_arr;

    private double[] close_arr;


    private double[] ssf_arr;


    // -----------------------------------------------------------------------------------------------------------------


    public StockFunLast(String stockCode) {
        initData(stockCode, 500);
    }

    public StockFunLast(String stockCode, int limit) {
        initData(stockCode, limit);
    }


    /**
     * 加载 行情数据
     *
     * @param stockCode 股票code
     * @param limit     N日
     */
    public void initData(String stockCode, Integer limit) {
        Assert.isTrue(limit > 0, "limit必须大于0");


        // --------------------------- HTTP 获取   个股行情 data

        // 实时行情 - API
        SHSZQuoteSnapshotResp shszQuoteSnapshotResp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
        SHSZQuoteSnapshotResp.RealtimequoteDTO realtimequote = shszQuoteSnapshotResp.getRealtimequote();


        // 历史行情 - API
        StockKlineHisResp stockKlineHisResp = EastMoneyKlineAPI.stockKlineHis(stockCode, KlineTypeEnum.DAY);


        // 我的持仓
        QueryCreditNewPosResp queryCreditNewPosResp = EastMoneyTradeAPI.queryCreditNewPosV2();


        // -------------------------------------------------------------------------------------------------------------

        // --------------------------- resp -> DTO


        // 收盘价 - 实时
        double C = realtimequote.getCurrentPrice().doubleValue();


        // 历史行情
        List<KlineDTO> klineDTOList = ConvertStockKline.strList2DTOList(stockKlineHisResp.getKlines(), limit);


        Object[] date_arr = ConvertStockKline.objFieldValArr(klineDTOList, "date");
        double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");


        // --------------------------- init data

        this.stockCode = stockCode;
        this.stockName = shszQuoteSnapshotResp.getName();

        this.queryCreditNewPosResp = queryCreditNewPosResp;

        this.shszQuoteSnapshotResp = shszQuoteSnapshotResp;
        this.klineDTOList = klineDTOList;

        this.C = C;

        this.date_arr = date_arr;
        this.close_arr = close_arr;


        this.ssf_arr = SSF(close_arr);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                指标
    // -----------------------------------------------------------------------------------------------------------------


    // -------------------------------------------- MA


    public boolean 上MA(int N) {
        // MA20
        double[] MA20_arr = MA(close_arr, N);
        // last
        double MA20 = MA20_arr[MA20_arr.length - 1];

        return C >= MA20;
    }

    public boolean 下MA(int N) {
        // MA20
        double[] MA20_arr = MA(close_arr, N);
        // last
        double MA20 = MA20_arr[MA20_arr.length - 1];

        return C < MA20;
    }


    public boolean MA向上(int N) {
        // MA20
        double[] MA20_arr = MA(close_arr, N);


        // last 1
        double MA20 = MA20_arr[MA20_arr.length - 1];
        // last 2
        double MA20_pre = MA20_arr[MA20_arr.length - 2];

        return MA20 >= MA20_pre;
    }


    public boolean MA向下(int N) {
        // MA20
        double[] MA20_arr = MA(close_arr, N);


        // last 1
        double MA20 = MA20_arr[MA20_arr.length - 1];
        // last 2
        double MA20_pre = MA20_arr[MA20_arr.length - 2];

        return MA20 < MA20_pre;
    }


    public boolean MA多(int N) {
        boolean 上MA = 上MA(N);
        boolean MA向上 = MA向上(N);

        return 上MA && MA向上;
    }


    public boolean MA空(int N) {
        boolean 下MA = 下MA(N);
        boolean MA向下 = MA向下(N);

        return 下MA && MA向下;
    }


    // -------------------------------------------- SSF


    public boolean 上SSF() {
        // last
        double SSF = ssf_arr[ssf_arr.length - 1];

        return C >= SSF;
    }

    public boolean 下SSF() {
        // last
        double SSF = ssf_arr[ssf_arr.length - 1];

        return C < SSF;
    }


    public boolean SSF向上() {
        // last 1
        double SSF = ssf_arr[ssf_arr.length - 1];
        // last 2
        double SSF_pre = ssf_arr[ssf_arr.length - 2];

        return SSF >= SSF_pre;
    }

    public boolean SSF向下() {
        // last 1
        double SSF = ssf_arr[ssf_arr.length - 1];
        // last 2
        double SSF_pre = ssf_arr[ssf_arr.length - 2];

        return SSF < SSF_pre;
    }


    public boolean SSF多() {
        boolean 上SSF = 上SSF();
        boolean SSF向上 = SSF向上();

        return 上SSF && SSF向上;
    }


    public boolean SSF空() {
        boolean 下SSF = 下SSF();
        boolean SSF向下 = SSF向下();

        return 下SSF && SSF向下;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {

        String stockCode = "300059";


        StockFunLast fun = new StockFunLast(stockCode, 100);


        // 1、下MA50
        boolean 下MA50 = fun.下MA(50);


        // 2、MA空(20)
        boolean MA20_空 = fun.MA空(20);


        // 3、RPS三线 < 85


        boolean sell = 下MA50 || MA20_空;


        double[][] macd = MACD(fun.close_arr);
        Object[] dateArr = fun.date_arr;


        int len = macd[0].length;
        for (int i = len - 10; i < len; i++) {

            Object date = dateArr[i];

            double DIF = macd[0][i];
            double DEA = macd[1][i];
            double MACD = macd[2][i];

            System.out.println(date + " " + DIF + " " + DEA + " " + MACD);
        }
    }

}
