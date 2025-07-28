package com.bebopze.tdx.quant.common.util;

import com.bebopze.tdx.quant.common.constant.StockTypeEnum;

import java.util.Objects;


/**
 * 股票类型：1-A股；2-ETF；
 *
 * @author: bebopze
 * @date: 2025/7/28
 */
public class StockTypeUtil {


    /**
     * 股票类型：1-A股；2-ETF；
     *
     * @param stockCode
     * @return
     */
    public static Integer stockType(String stockCode) {
        return StockTypeEnum.getTypeByStockCode(stockCode);
    }


    /**
     * 股票价格 精度     ->     A股-2位小数；ETF-3位小数；
     *
     * @param stockCode
     * @return
     */
    public static int stockPriceScale(String stockCode) {

        Integer stockType = stockType(stockCode);


        // A股-2位小数；
        int scale = 2;

        // ETF-3位小数；
        if (Objects.equals(stockType, StockTypeEnum.ETF.type)) {
            scale = 3;
        }

        return scale;
    }


}
