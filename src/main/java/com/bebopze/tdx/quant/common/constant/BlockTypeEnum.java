package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 通达信 - 板块类型
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@AllArgsConstructor
public enum BlockTypeEnum {


    HY_PT("hy_pt", 2, "普通（细分）行业板块"),

    HY_YJ("hy_yj", 12, "研究行业板块"),


    DQ("dq", 3, "地区板块"),

    GN("gn", 4, "概念板块"),

    FG("fg", 5, "风格板块"),

    ZS("zs", 6, "指数板块"),


    ZDY("zdy", 7, "自定义板块");


    @Getter
    private String code;

    @Getter
    private Integer type;

    @Getter
    private String desc;


    public static String getDescByType(Integer type) {
        for (BlockTypeEnum value : BlockTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value.desc;
            }
        }
        return null;
    }

}
