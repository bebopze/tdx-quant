package com.bebopze.tdx.quant.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @author: bebopze
 * @date: 2025/5/23
 */
public class NumUtil {


    public static double[] decimal2Double(List<BigDecimal> decimalList) {
        return decimalList.stream().mapToDouble(NumUtil::decimal2Double).toArray();
    }


    public static double decimal2Double(BigDecimal val) {
        return decimal2Double(val, 2);
    }

    public static double decimal2Double(BigDecimal val, int scale) {
        return val.setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

}