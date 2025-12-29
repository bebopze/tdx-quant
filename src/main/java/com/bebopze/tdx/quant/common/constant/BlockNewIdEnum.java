package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 自定义板块ID - 1-百日新高；2-涨幅榜；3-均线极多头；4-均线大多头；11-RPS红（一线95/双线90/三线85）；12-大均线多头；13-二阶段；21-板块AMO-TOP1；
 *
 * @author: bebopze
 * @date: 2025/7/20
 */
@AllArgsConstructor
public enum BlockNewIdEnum {


    // 1-百日新高；2-涨幅榜；3-均线极多头；4-均线大多头；
    // 11-RPS红（一线95/双线90/三线85）；12-大均线多头；13-二阶段；
    // 21-板块AMO-TOP1；


    百日新高(1, "百日新高"),
    涨幅榜(2, "涨幅榜"),
    均线极多头(3, "均线极多头"),
    均线大多头(4, "均线大多头"),


    RPS红(11, "RPS红（一线95/双线90/三线85）"),
    大均线多头(12, "大均线多头"),
    二阶段(13, "二阶段"),


    涨停(21, "涨停"),
    跌停(22, "跌停"),


    板块AMO_TOP1(31, "板块AMO-TOP1"),
    板块_月多2(32, "板块-月多2"),


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