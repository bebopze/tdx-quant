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


    public static BigDecimal double2Decimal(double v) {
        return new BigDecimal(v).setScale(3, RoundingMode.HALF_UP);
    }


    /**
     * 判断 value 是否在 [min, max] 区间内（包含边界）
     *
     * @param value
     * @param min
     * @param max
     * @return
     */
    public static boolean between(double value, double min, double max) {
        return value >= min && value <= max;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static double of(Number val) {
        return of(val, 3);
    }

    public static double of(Number val, int newScale) {
        if (null == val || (val instanceof Double && (Double.isNaN((Double) val) || Double.isInfinite((Double) val)))) {
            return Double.NaN;
        }
        return new BigDecimal(String.valueOf(val)).setScale(newScale, RoundingMode.HALF_UP).doubleValue();
    }


    public static double NaN_0(double v) {
        return Double.isNaN(v) ? 0 : v;
    }

}