package com.bebopze.tdx.quant.common.constant.tq;

import lombok.AllArgsConstructor;


/**
 * dividend_type 复权类型                   - https://help.tdx.com.cn/quant/docs/markdown/Dict.html#dividend_type复权类型
 *
 * @author: bebopze
 * @date: 2026/10/01
 */
@AllArgsConstructor
public enum DividendTypeEnum {


    // 名称      类型     数值      说明
    // type     str     none     不复权
    // type     str     front    前复权
    // type     str     back     后复权


    NONE("none", "不复权"),
    FRONT("front", "前复权"),
    BACK("back", "后复权");


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 周期
     */
    public final String type;
    /**
     * 描述
     */
    public final String desc;

}