package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 主线列表 BS策略类型
 *
 * @author: bebopze
 * @date: 2025/10/10
 */
@Getter
@AllArgsConstructor
public enum BSStrategyTypeEnum {


//    C_SSF_偏离率(1, "C_SSF_偏离率"),
//
//
//    C_MA5_偏离率(2, "C_MA5_偏离率"),
//    C_MA10_偏离率(3, "C_MA10_偏离率"),
//    C_MA15_偏离率(4, "C_MA15_偏离率"),
//    C_MA20_偏离率(5, "C_MA20_偏离率"),
//    C_MA25_偏离率(6, "C_MA25_偏离率"),
//    C_MA30_偏离率(7, "C_MA30_偏离率"),
//    C_MA40_偏离率(8, "C_MA40_偏离率"),
//    C_MA50_偏离率(9, "C_MA50_偏离率"),
//    C_MA60_偏离率(10, "C_MA60_偏离率"),
//    C_MA100_偏离率(11, "C_MA100_偏离率"),
//    C_MA120_偏离率(12, "C_MA120_偏离率"),
//    C_MA150_偏离率(13, "C_MA150_偏离率"),
//    C_MA200_偏离率(14, "C_MA200_偏离率"),
//    C_MA250_偏离率(15, "C_MA250_偏离率"),


    BS_MA20多(1, "BS_MA20多"),
    BS_上MA20(2, "BS_上MA20"),
    BS_SSF多(3, "BS_SSF多"),
    BS_上SSF(4, "BS_上SSF"),
    BS_上SAR(5, "BS_上SAR（操盘BS）"),


    BS_XZZB(11, "BS_XZZB"),
    BS_BS区间(12, "BS_BS区间"),


    BS_MA_偏离率(21, "BS_MA_偏离率"),
    BS_MA_偏离率2(22, "BS_MA_偏离率2"),


    ;


    public final Integer type;

    public final String desc;


    public static BSStrategyTypeEnum getByType(Integer type) {
        for (BSStrategyTypeEnum value : BSStrategyTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }


    public static String getDescByType(Integer type) {
        BSStrategyTypeEnum topTypeEnum = getByType(type);
        return topTypeEnum == null ? null : topTypeEnum.desc;
    }


}