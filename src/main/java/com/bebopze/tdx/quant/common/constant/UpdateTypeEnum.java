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


    HIS(1, "历史行情", 250 * 100),

    N_DAY(2, "近N日行情", 250 * 2),


    // ------------------------------------------------- 全量更新   =>   OOM优化  ->  分段执行


    ALL_RANGE(100, "全量更新（分段执行）", -1),


    ;


    public final Integer type;

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