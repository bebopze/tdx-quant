package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * S策略
 *
 * @author: bebopze
 * @date: 2025/8/24
 */
@Getter
@AllArgsConstructor
public enum SellStrategyEnum {


    // 个股 S策略
    月空_MA20空(1, "月空_MA20空"),
    SSF空(2, "SSF空"),
    高位爆量上影大阴(3, "高位爆量上影大阴"),
    C_SSF_偏离率(4, "C_SSF_偏离率"),

    强势卖出(5, "强势卖出（中期涨幅>100% + C_SSF_偏离率>25%）"),


    // 板块 S策略
    S11(11, "【主线板块】走弱"),
    S12(12, "板块S"),


    // 大盘 S策略
    S21(21, "大盘仓位限制->等比减仓"),


    ;


    private final Integer type;

    private final String desc;


    public static SellStrategyEnum getByType(Integer type) {
        for (SellStrategyEnum value : SellStrategyEnum.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }


    public static String getDescByType(Integer type) {
        SellStrategyEnum topBlockStrategyEnum = getByType(type);
        return topBlockStrategyEnum == null ? null : topBlockStrategyEnum.desc;
    }


}
