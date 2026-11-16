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


    N_DAY(1, "nd", "近N日行情", 250 * 2),


    HIS(2, "his", "历史行情", 250 * 100),


    // ------------------------------------------------- 全量更新   =>   OOM优化  ->  分段执行


//    N_DAY(1, "nd", "近N日更新", "拉取近N日K线（短期）-> 用于[近N日更新]"),
//
//    HIS(2, "his", "历史更新", "拉取历史K线（长期）-> 用于[历史更新]"),
//
//    ALL(3, "all", "暂不支持[全部更新（近N日更新 + 历史更新）]", "拉取全部K线（his + nd）-> 用于[回测]");


    ALL_RANGE(3, "all（nd + his）", "全量更新（分段执行）", -1),


    ;


    /**
     * 更新类型
     */
    public final Integer type;

    /**
     * 字段flag
     */
    private final String klineField;

    /**
     * 更新说明
     */
    public final String desc;

    /**
     * 更新数据天数  ->  近多少天的数据（交易日）
     */
    public final int days;


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