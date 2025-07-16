package com.bebopze.tdx.quant.common.tdxfun;

import static com.bebopze.tdx.quant.common.util.BoolUtil.bool2Int;


/**
 * Tools
 *
 * @author: bebopze
 * @date: 2025/6/10
 */
public class Tools {


    // -----------------------------------------------------------------------------------------------------------------


//    public static boolean 创N日新高(double rps50, double rps120, double rps250, double N) {
//
//
//        StockFun fun = new StockFun();
//
//
//        fun.中期涨幅N();
//
//
//        // CON_1 :=  COUNT(N日新高(N),  5);
//        //
//        // CON_2 :=  SSF多     AND     N日涨幅(3) > -10;
//        //
//        // CON_3 :=  MA多(5) + MA多(10) + MA多(20) + MA多(50)  >=  3;
//        //
//        //
//        //
//        // CON_4 :=  RPS一线红(95) || RPS双线红(90) || RPS三线红(85);
//        //
//        // CON_5 :=  周多   ||   大均线多头;
//        //
//        //
//        // CON_1 AND CON_2 AND CON_3     AND     (CON_4 || CON_5);
//
//
//        boolean CON_1 = COUNT(N日新高(60), 5);
//
//
//        return rps50 >= N || rps120 >= N || rps250 >= N;
//    }


    // -----------------------------------------------------------------------------------------------------------------


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