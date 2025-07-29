package com.bebopze.tdx.quant.common.util;

import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;


/**
 * A股   ->   B/S规则          =>          数量（1手）、价格（精度）、...
 *
 * @author: bebopze
 * @date: 2025/7/29
 */
public class StockUtil {


    /**
     * A股  ->  买入数量 限制       =>       最小单位：1手 = 100股     ->     N x 100股
     *
     * @param val
     * @return
     */
    public static int quantity(int val) {
        // 560 -> 500
        int qty = val - (val % 100);
        // 最小100股
        return Math.max(qty, 100);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * A股   ->   价格精度
     *
     * -            个股   ->   价格 2位小数           1.23        // 东方财富 交易接口     精度错误 -> 下单失败
     * -            ETF   ->   价格 3位小数           1.234
     *
     * @param stockCode
     * @return
     */
    public static int priceScale(String stockCode) {

        // 个股类型
        StockTypeEnum stockTypeEnum = StockTypeEnum.getByStockCode(stockCode);


        int scale = 2;

        // ETF
        if (Objects.equals(stockTypeEnum, StockTypeEnum.ETF)) {
            scale = 3;
        }

        return scale;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 东方财富   -   证券类型 - 代码（ 0-股票 / E-ETF / R-创业板 / W-科创板 / J-北交所 / ... ）
     *
     * @param stockCode
     * @return
     */
    public static String stktype_ex(String stockCode) {

        // 个股类型
        StockTypeEnum stockTypeEnum = StockTypeEnum.getByStockCode(stockCode);

        // ETF
        if (Objects.equals(stockTypeEnum, StockTypeEnum.ETF)) {
            return "E";
        }


        // 股票
        return "0";
    }

    public static String stktype_ex(String stockCode, String stockName) {
        if (StringUtils.isBlank(stockCode)) {
            return stktype_ex(stockCode);
        }

        if (stockName.contains("ETF")) {
            return "E";
        }

        return stktype_ex(stockCode);
    }


    // -----------------------------------------------------------------------------------------------------------------


}
