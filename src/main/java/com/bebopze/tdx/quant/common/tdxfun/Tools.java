package com.bebopze.tdx.quant.common.tdxfun;

import static com.bebopze.tdx.quant.parser.check.TdxFunCheck.bool2Int;


/**
 * Tools
 *
 * @author: bebopze
 * @date: 2025/6/10
 */
public class Tools {


    public static boolean RPS一线红(double rps50, double rps120, double rps250, double N) {
        return rps50 >= N || rps120 >= N || rps250 >= N;
    }

    public static boolean RPS双线红(double rps50, double rps120, double rps250, double N) {
        return bool2Int(rps50 >= N) + bool2Int(rps120 >= N) + bool2Int(rps250 >= N) >= 2;
    }

    public static boolean RPS三线红(double rps50, double rps120, double rps250, double N) {
        return rps50 >= N && rps120 >= N && rps250 >= N;
    }

}