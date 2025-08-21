package com.bebopze.tdx.quant.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


/**
 * Number
 *
 * @author: bebopze
 * @date: 2025/5/23
 */
public class NumUtil {


    public static double[] decimal2Double(List<BigDecimal> decimalList) {
        return decimalList.stream().mapToDouble(NumUtil::decimal2Double).toArray();
    }


    public static double decimal2Double(BigDecimal val) {
        return decimal2Double(val, 3);
    }

    public static double decimal2Double(BigDecimal val, int scale) {
        return val.setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }


    public static BigDecimal double2Decimal(Double val) {
        return double2Decimal(val, 3);
    }

    public static BigDecimal double2Decimal(Double val, int scale) {
        return val == null || Double.isNaN(val) ? null : new BigDecimal(val).setScale(scale, RoundingMode.HALF_UP);
    }


    public static BigDecimal num2Decimal(Number val) {
        return num2Decimal(val, 3);
    }

    public static BigDecimal num2Decimal(Number val, int scale) {
        return double2Decimal(of(val), scale);
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


    // -----------------------------------------------------------------------------------------------------------------


    public static String str(Number val) {
        BigDecimal decimal = num2Decimal(val);
        return decimal == null ? null : decimal.stripTrailingZeros().toPlainString();
    }


}
