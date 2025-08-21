package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 自定义板块 - 关联类型
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@Getter
@AllArgsConstructor
public enum BlockNewTypeEnum {


    STOCK(1, "个股"),

    BLOCK(2, "板块"),


    MARKET(3, "指数"),


    ;


    private final Integer type;

    private final String desc;


    public static String getDescByType(Integer type) {
        for (BlockNewTypeEnum value : BlockNewTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value.desc;
            }
        }
        return null;
    }


}
