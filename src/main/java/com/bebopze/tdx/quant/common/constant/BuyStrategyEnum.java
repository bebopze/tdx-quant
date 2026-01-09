package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * B策略
 *
 * @author: bebopze
 * @date: 2025/8/24
 */
@Getter
@AllArgsConstructor
public enum BuyStrategyEnum {


    // 个股 B策略
    涨停_SSF多_月多(1, "涨停_SSF多_月多"),


//    MA20空(2, "MA20空"),
//
//    SSF空(3, "SSF空"),
//
//    跌停_MA5空_MA10空(4, "跌停_MA5空_MA10空"),   // 天齐锂业（2025-11-21）
//
//    高位爆量上影大阴(5, "高位爆量上影大阴"),
//
//    H_SSF_偏离率(6, "H_SSF_偏离率"),
//    C_SSF_偏离率(7, "C_SSF_偏离率"),
//
//    C_短期MA_偏离率(8, "C_短期MA_偏离率"),
//    C_中期MA_偏离率(9, "C_中期MA_偏离率"),
//    C_长期MA_偏离率(10, "C_长期MA_偏离率"),
//
//
//    强势卖出(11, "强势卖出（中期涨幅>100% + C_SSF_偏离率>25%）"),
//
//
//    // ----------------------------------------------------- 追妖股（打板）
//
//    下MA5(51, "下MA5"),
//    下MA10(52, "下MA10"),
//    下SSF(53, "下SSF"),
//
//    跌停(54, "跌停"),   // 涨停（打板 追妖股）
//
//
//    // ----------------------------------------------------- 板块 S策略
//
//
//    // 板块 S策略
//    S71(71, "【主线板块】走弱"),
//    S72(72, "板块S"),
//
//
//    // ----------------------------------------------------- 大盘 S策略
//
//
//    // 大盘 S策略
//    S91(91, "大盘仓位限制->等比减仓"),


    ;


    private final Integer type;

    private final String desc;


    public static BuyStrategyEnum getByType(Integer type) {
        for (BuyStrategyEnum value : BuyStrategyEnum.values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        return null;
    }


    public static String getDescByType(Integer type) {
        BuyStrategyEnum topBlockStrategyEnum = getByType(type);
        return topBlockStrategyEnum == null ? null : topBlockStrategyEnum.desc;
    }


}