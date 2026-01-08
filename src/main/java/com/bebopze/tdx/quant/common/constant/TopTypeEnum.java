package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 主线列表 策略类型：1-机选；2-精选（TOP50）；3-历史新高；4-极多头；5-RPS三线红；6-10亿；7-首次三线红；8-口袋支点；9-T0；10-涨停（打板）；
 *
 * @author: bebopze
 * @date: 2025/10/10
 */
@Getter
@AllArgsConstructor
public enum TopTypeEnum {


    AUTO(1, "机选"),

    MANUAL(2, "精选（TOP50）"),


    历史新高(3, "历史新高"),

    极多头(4, "均线极多头"),

    RPS三线红(5, "RPS三线红"),

    十亿(6, "10亿"),

    首次三线红(7, "首次三线红"),

    口袋支点(8, "口袋支点"),

    T0(9, "T0"),


    // 涨停 + SSF多 + 月多 + IN主线
    涨停_SSF多_月多(10, "打板（涨停+SSF多+月多+IN主线）- 次1日（开盘价[open] -> 直接买入）"),

    涨停_次2日(11, "涨停（打板）- 次2日"),

    ;


//    HISTORY_NEW_TOP(3, "历史新高"),
//
//    EXTREME_MULTI_HEAD(4, "极多头"),
//
//    RPS_THREE_LINE_RED(5, "RPS三线红"),
//
//    TEN_BILLION(6, "10亿"),
//
//    FIRST_THREE_LINE_RED(7, "首次三线红"),
//
//    POCKET_BRANCH_POINT(8, "口袋支点"),
//
//    T0(9, "T0"),
//
//    ZT_DABAN(10, "涨停（打板）"),


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