package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 主线列表 类型
 *
 * @author: bebopze
 * @date: 2025/10/10
 */
@Getter
@AllArgsConstructor
public enum TopTypeEnum {


    AUTO(1, "机选"),

    MANUAL(2, "人选"),


    ;


    public final Integer type;

    public final String desc;


    public static TopTypeEnum getByType(Integer type) {
        for (TopTypeEnum value : TopTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }


    public static String getDescByType(Integer type) {
        TopTypeEnum topTypeEnum = getByType(type);
        return topTypeEnum == null ? null : topTypeEnum.desc;
    }

}