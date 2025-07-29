package com.bebopze.tdx.quant.common.util;


/**
 * A股   ->   B/S规则
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


}