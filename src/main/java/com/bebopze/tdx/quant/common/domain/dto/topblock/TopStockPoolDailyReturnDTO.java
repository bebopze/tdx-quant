package com.bebopze.tdx.quant.common.domain.dto.topblock;

import lombok.Data;

import java.time.LocalDate;


/**
 * 主线个股 列表   ->   每日收益率
 *
 * @author: bebopze
 * @date: 2025/10/21
 */
@Data
public class TopStockPoolDailyReturnDTO {


    private LocalDate date;


    /**
     * 当日收益率（%）
     */
    private double daily_return;


    /**
     * 净值（初始值1.0000）
     */
    private double nav;


    /**
     * 总资金（初始值 100W ）
     */
    private double capital;
}