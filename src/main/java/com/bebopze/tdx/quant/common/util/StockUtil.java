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
     * 行情数据个数 >= 250（MA250  ->  最少最近 250日 的行情数据）
     *
     * @param N
     * @return
     */
    public static Integer N(Integer N) {
        return N == null ? Integer.MAX_VALUE : Math.max(N, 300);
    }


    /**
     * 增量更新：    是 -> 增量更新   /   否 -> 全量更新
     *
     * @param N
     * @return
     */
    public static boolean incrUpdate(Integer N) {
        return N < Integer.MAX_VALUE;
    }

    /**
     * 截取 行情数据个数 N  <  行情总数       =>       增量更新
     *
     * @param N        截取last 行情个数  N
     * @param totalNum 行情总数
     * @return
     */
    public static boolean incrUpdate(Integer N, int totalNum) {
        // 截取 行情数据个数 N  <  行情总数     =>       增量更新
        return incrUpdate(N) && N < totalNum;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * A股  ->  买入数量 限制       =>       普通股票     最小单位：1手 = 100股     ->     N x 100股
     * -                                   科创板688   最小单位：2手 = 200股     ->     N x 200股
     *
     * @param val
     * @param stockCode
     * @return
     */
    public static int quantity(int val, String stockCode) {
        // 688/689   ->   科创板
        boolean kcb = stockCode.startsWith("68");
        // return quantity(val, kcb);

        int qty = quantity(val, kcb);

        System.out.println("stockCode=" + stockCode + "   val=" + val + "   qty=" + qty);
        return qty;
    }


    /**
     * A股  ->  买入数量 限制       =>       普通股票     最小单位：1手 = 100股     ->     N x 100股
     * -                                   科创板688   最小单位：2手 = 200股     ->     N x 200股
     *
     * @param val
     * @param kcb 是否 科创板
     * @return
     */
    public static int quantity(int val, boolean kcb) {

        // 最小 起买股数
        // 688/689   ->   科创板（200股 起买）
        int minQty = kcb ? 200 : 100;


        // 大于min  ->  原值val
        val = val >= minQty ? val :
                // 低于min     =>     大于 min x 70%  ->  给 默认值min
                val > minQty * 0.7 ? minQty : 0;


        return quantity(val);
    }

    /**
     * 取整百
     *
     * @param val
     * @return
     */
    private static int quantity(int val) {
        // 560 -> 500
        return val - (val % 100);
    }


    /**
     * 实际 可用数量   <=   可用数量
     *
     * @param qty    理论数量（未取 整百 -> 手）
     * @param avlQty 可用数量
     * @return
     */
    @Deprecated
    public static int quantity(int qty, int avlQty) {
        return Math.min(quantity2(qty), avlQty);
    }


    /**
     * A股  ->  买入数量 限制       =>       最小单位：1手 = 100股     ->     N x 100股   // TODO   科创板688   最小2手   ->   N x 200股
     *
     * @param val 理论数量（未取 整百 -> 手）
     * @return
     */
    @Deprecated
    private static int quantity2(int val) {
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
        if (StringUtils.isBlank(stockName)) {
            return stktype_ex(stockCode);
        }

        if (stockName.contains("ETF")) {
            return "E";
        }

        return stktype_ex(stockCode);
    }


    // -----------------------------------------------------------------------------------------------------------------


}
