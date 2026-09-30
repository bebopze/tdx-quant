package com.bebopze.tdx.quant.common.constant.tq;

import lombok.AllArgsConstructor;


/**
 * price_type 类型                            - https://help.tdx.com.cn/quant/docs/markdown/Dict.html#price-type类型
 *
 * @author: bebopze
 * @date: 2026/10/1
 */
@AllArgsConstructor
public enum PriceTypeEnum {


    // 名称             类型      数值       说明
    //
    // PRICE_MY        int       0       自填价
    // PRICE_SJ        int       1       市价
    // PRICE_ZTJ       int       2       涨停价/笼子上限
    // PRICE_DTJ       int       3       跌停价/笼子下限


    PRICE_MY(0, "自填价"),
    PRICE_SJ(1, "市价"),
    PRICE_ZTJ(2, "涨停价/笼子上限"),
    PRICE_DTJ(3, "跌停价/笼子下限");


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 类型
     */
    public final int type;

    /**
     * 描述
     */
    public final String desc;

}