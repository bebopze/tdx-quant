package com.bebopze.tdx.quant.common.domain.dto.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;


/**
 * 每日主线板块  涨停数/百日新高数/...  统计（TOP榜）
 *
 * @author: bebopze
 * @date: 2025/12/20
 */
@Data
@AllArgsConstructor
public class TopStatDTO {


    // 日期
    public LocalDate date;

    // 板块code
    public String blockCode;
    // 板块name
    public String blockName;

    // 涨停数
    private int ztCount;
    // 百日新高
    private int n100HighCount;
}