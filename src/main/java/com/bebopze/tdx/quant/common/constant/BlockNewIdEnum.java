package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 自定义板块ID - 1-百日新高；2-涨幅榜；3-RPS红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；
 *
 * @author: bebopze
 * @date: 2025/7/20
 */
@AllArgsConstructor
public enum BlockNewIdEnum {


    // 1-百日新高；2-涨幅榜；3-RPS红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；6-均线大多头；
    // 11-板块AMO-TOP1


    百日新高(1, "百日新高"),

    涨幅榜(2, "涨幅榜"),


    RPS红(3, "RPS红（一线95/双线90/三线85）"),
    二阶段(4, "二阶段"),
    大均线多头(5, "大均线多头"),
    均线大多头(6, "均线大多头"),


    板块AMO_TOP1(11, "板块AMO-TOP1；"),


    ;


    @Getter
    private Integer blockNewId;

    @Getter
    private String desc;


    public static String getDescByBlockNewId(Integer blockNewId) {
        for (BlockNewIdEnum value : BlockNewIdEnum.values()) {
            if (value.blockNewId.equals(blockNewId)) {
                return value.desc;
            }
        }
        return null;
    }


}