package com.bebopze.tdx.quant.common.domain.dto.analysis;

import lombok.Data;

import java.time.LocalDate;


/**
 * 主线个股 列表   ->   每日收益率
 *
 * @author: bebopze
 * @date: 2025/10/21
 */
@Data
public class TopPoolDailyReturnDTO {


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


    // -------------------------------------------------


    // 昨日持仓数量  =  oldPosCount  +  oldSellCount
    private int preCount;
    // 今日持仓数量  =  oldPosCount  +  newBuyCount
    private int todayCount;

    // 今日  不变数量
    private int oldPosCount;
    // 今日S 淘汰数量
    private int oldSellCount;
    // 今日B 买入数量
    private int newBuyCount;

    /**
     * 日均 调仓换股比例
     */
    private double posReplaceRatio;
}