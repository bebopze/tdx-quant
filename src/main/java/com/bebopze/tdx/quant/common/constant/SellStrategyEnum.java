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

    MA20空(2, "MA20空"),

    SSF空(3, "SSF空"),

    跌停_MA5空_MA10空(4, "跌停_MA5空_MA10空"),   // 天齐锂业（2025-11-21）

    高位爆量上影大阴(5, "高位爆量上影大阴"),

    C_SSF_偏离率(6, "C_SSF_偏离率"),

    C_短期MA_偏离率(7, "C_短期MA_偏离率"),
    C_中期MA_偏离率(8, "C_中期MA_偏离率"),
    C_长期MA_偏离率(9, "C_长期MA_偏离率"),


    强势卖出(10, "强势卖出（中期涨幅>100% + C_SSF_偏离率>25%）"),


    // 板块 S策略
    S21(21, "【主线板块】走弱"),
    S22(22, "板块S"),


    // 大盘 S策略
    S31(31, "大盘仓位限制->等比减仓"),


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