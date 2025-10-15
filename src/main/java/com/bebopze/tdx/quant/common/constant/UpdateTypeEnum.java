package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 数据更新类型：1-全量更新；2-增量更新；
 *
 * @author: bebopze
 * @date: 2025/10/15
 */
@Getter
@AllArgsConstructor
public enum UpdateTypeEnum {


    ALL(1, "全量更新"),

    INCR(2, "增量更新"),


    ;


    public final Integer type;

    public final String desc;


    public static UpdateTypeEnum getByType(Integer type) {
        for (UpdateTypeEnum value : UpdateTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }


    public static String getDescByType(Integer type) {
        UpdateTypeEnum topTypeEnum = getByType(type);
        return topTypeEnum == null ? null : topTypeEnum.desc;
    }

}