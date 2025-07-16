package com.bebopze.tdx.quant.common.util;

/**
 * boolean
 *
 * @author: bebopze
 * @date: 2025/7/13
 */
public class BoolUtil {


    /**
     * bool -> int
     *
     * @param bool
     * @return
     */
    public static Integer bool2Int(boolean bool) {
        return bool ? 1 : 0;
    }


    /**
     * int -> bool
     *
     * @param val
     * @return
     */
    public static boolean int2Bool(int val) {
        return val >= 1;
    }


    /**
     * int -> bool
     *
     * @param vals 序列
     * @return
     */
    public static boolean[] int2Bool(int[] vals) {
        int len = vals.length;

        boolean[] result = new boolean[len];
        for (int i = 0; i < len; i++) {
            result[i] = int2Bool(vals[i]);
        }

        return result;
    }

}
