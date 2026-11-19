package com.bebopze.tdx.quant.common.constant;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


/**
 * 通达信 - 板块类型
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@AllArgsConstructor
public enum BlockTypeEnum {


    HY_PT("hy_pt", 2, "普通（细分）行业"),

    HY_YJ("hy_yj", 12, "研究行业"),


    DQ("dq", 3, "地区板块"),

    GN("gn", 4, "概念板块"),

    FG("fg", 5, "风格板块"),

    ZS("zs", 6, "指数板块"),


    ZDY("zdy", 7, "自定义板块");


    /**
     * RPS板块  ->  2-细分板块  +  4-概念板块
     */
    static final List<BlockTypeEnum> RPS_BLOCK_TYPE = Lists.newArrayList(HY_PT, GN);


    @Getter
    public String code;

    @Getter
    public Integer type;

    @Getter
    public String desc;


    public static String getDescByType(Integer type) {
        for (BlockTypeEnum value : BlockTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value.desc;
            }
        }
        return null;
    }


    /**
     * 是否 RPS板块
     *
     * @param type
     * @return
     */
    public static boolean isRpsBlock(Integer type) {
        return RPS_BLOCK_TYPE.contains(getDescByType(type));
    }

}