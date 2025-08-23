package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 主线策略
 *
 * @author: bebopze
 * @date: 2025/8/17
 */
@Getter
@AllArgsConstructor
public enum TopBlockStrategyEnum {


    LV1(1, "LV1"),


    LV2(2, "LV2（百日新高-占比Top1）"),
    // 板块-月多 + RPS红 + SSF多
    LV3(3, "LV3（板块-月多2）"),

    LV2_LV3(4, "LV2 <- 升级/降级 ->LV3"),


    ;


    private final Integer type;

    private final String desc;


    public static TopBlockStrategyEnum getByType(Integer type) {
        for (TopBlockStrategyEnum value : TopBlockStrategyEnum.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }


    public static String getDescByType(Integer type) {
        TopBlockStrategyEnum topBlockStrategyEnum = getByType(type);
        return topBlockStrategyEnum == null ? null : topBlockStrategyEnum.desc;
    }


}
