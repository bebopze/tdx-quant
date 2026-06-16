package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * 东方财富 委托单   -   委托状态（未报/已报/已撤/部成/部撤/已成/废单）
 *
 * @author: bebopze
 * @date: 2026/6/16
 */
@Slf4j
@AllArgsConstructor
public enum BSOrderStatusEnum {


    未报("未报"),

    已报("已报"),


    已撤("已撤"),


    部成("部成"),


    部撤("部撤"),

    已成("已成"),


    废单("废单");


    @Getter
    public String wtzt;


    /**
     * 已成交   ->   已撤/已成/废单/部撤
     *
     * @param wtzt
     * @return
     */
    public static boolean filled(String wtzt) {
        return 已撤.wtzt.equals(wtzt) || 已成.wtzt.equals(wtzt) || 废单.wtzt.equals(wtzt) || 部撤.wtzt.equals(wtzt);
    }

    /**
     * 未成交   ->   未报/已报/部成
     *
     * @param wtzt
     * @return
     */
    public static boolean unfilled(String wtzt) {
        return !filled(wtzt);
    }


    public static boolean unfilled_2(String wtzt) {
        // 已成交   ->   已撤/已成/废单/部撤
        // 未成交   ->   未报/已报/部成
        return 未报.wtzt.equals(wtzt) || 已报.wtzt.equals(wtzt) || 部成.wtzt.equals(wtzt);
    }


}