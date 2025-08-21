package com.bebopze.tdx.quant.common.domain.kline;

import lombok.Data;


/**
 * 全A行情   -   分页 列表查询
 *
 * @author: bebopze
 * @date: 2025/8/3
 */
@Data
public class StockKlineListResp {


    //  {
    //       "f1": 2,
    //       "f2": 1228,
    //       "f3": 41,
    //       "f4": 5,
    //       "f5": 1012187,         101218700
    //       "f6": 1240239179.62,
    //       "f7": 147,
    //       "f8": 52,
    //       "f9": 423,
    //       "f10": 75,
    //       "f12": "000001",
    //       "f13": 0,
    //       "f14": "平安银行",
    //       "f15": 1233,
    //       "f16": 1215,
    //       "f17": 1224,
    //       "f18": 1223,
    //       "f23": 56,
    //       "f152": 2
    //  }


    // {
    //     "f1": 3,
    //     "f2": 1470,         1.470
    //     "f3": 426,          4.26%
    //     "f4": 60,           0.060
    //     "f5": 68600006,     68600006
    //     "f6": 9887139213.0,
    //     "f12": "513120",
    //     "f13": 1,
    //     "f14": "港股创新药ETF",
    //     "f15": 1471,         1.471
    //     "f16": 1414,
    //     "f17": 1414,
    //     "f18": 1410,         1.410
    //     "f152": 2
    // }


    // ------------------------------------------------------------


    // 价格精度  ->  小数位（个股：2 / ETF：3）
    // 股票价格 精度     ->     A股-2位小数；ETF-3位小数；
    private int f1;


    // 涨跌幅精度  ->  小数位（个股：2 / ETF：2）
    private String f152;


    // 2位精度 -> /100
    // 3位精度 -> /1000
    int priceDivisor;

    public int getPriceDivisor() {
        return f1 == 3 ? 1_000 : 1_00;
    }


    // ------------------------------------------------------------


    // 最新价（1228   ->   12.28）
    private double f2;

    public double getF2() {
        return f2 / getPriceDivisor();
    }


    // 涨跌幅（41   ->   0.41%）
    private double f3;

    public double getF3() {
        return f3 / 100.0;
    }


    // 涨跌额（5   ->   0.05）
    private double f4;

    public double getF4() {
        return f4 / getPriceDivisor();
    }


    // 个股 -> 成交量（1012187(手)   ->   1012187 x 100）
    // ETF -> 成交量（68600006(股)  ->   68600006）
    private long f5;

    public long getF5() {
        // 3-ETF
        return f1 == 3 ? f5 : f5 * 100;
    }


    // 成交额（1240239179.62）
    private double f6;


    // 振幅（147   ->   1.47%）
    private double f7;

    public double getF7() {
        return f7 / 100.0;
    }

    // 换手率（52   ->   0.52%）
    private double f8;

    public double getF8() {
        return f8 / 100.0;
    }


    // 市盈率(动态)（423   ->   4.23）
    private double f9;

    public double getF9() {
        return f9 / 100.0;
    }


    // 量比（75   ->   0.75）
    private double f10;

    public double getF10() {
        return f10 / 100.0;
    }


    // 股票代码     000001
    private String f12;


    // 交易所：0-深证；1-上证；（暂无 2-北交所；）
    private String f13;


    // 股票名称（平安银行）
    private String f14;


    // 最高价（1233   ->   12.33）
    private double f15;

    public double getF15() {
        return f15 / getPriceDivisor();
    }


    // 最低价（1215   ->   12.15）
    private double f16;

    public double getF16() {
        return f16 / getPriceDivisor();
    }


    // 开盘价（1224   ->   12.24）
    private double f17;

    public double getF17() {
        return f17 / getPriceDivisor();
    }


    // 昨收盘价（1223   ->   12.23）
    private double f18;

    public double getF18() {
        return f18 / getPriceDivisor();
    }


    // 市净率（56   ->   0.56）
    private double f23;

    public double getF23() {
        return f23 / 100.0;
    }


//    // 2（无意义   ->   所有个股 全为2）
//    private String f152;


    // -----------------------------------------------------------------------------------------------------------------


    public String getF14() {
        // f14: "三 力 士",
        return f14.replaceAll(" ", "");
    }


}