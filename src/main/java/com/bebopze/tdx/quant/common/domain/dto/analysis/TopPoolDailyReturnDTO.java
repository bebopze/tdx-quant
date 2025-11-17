package com.bebopze.tdx.quant.common.domain.dto.analysis;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


/**
 * 主线个股 列表   ->   每日收益率
 *
 * @author: bebopze
 * @date: 2025/10/21
 */
@Data
@NoArgsConstructor
public class TopPoolDailyReturnDTO {


    private LocalDate date;


    /**
     * 普通账户 当日收益率（%）
     */
    private double dailyReturn;
    /**
     * 融资账户 当日收益率（%）    =     普通账户 当日收益率（%）* 2（普通账户 涨/跌 10%  ->  融资账户 涨/跌 20%）
     */
    private double marginDailyReturn;


    /**
     * 普通账户 净值（初始值1.0000）
     */
    private double nav;
    /**
     * 融资账户 净值（初始值1.0000）
     */
    private double marginNav;


    /**
     * 普通账户 总资金（初始值 100W ）
     */
    private double capital;
    /**
     * 融资账户 总资金（初始值 100W ）
     */
    private double marginCapital;


    // -------------------------------------------------


    // 昨日 持仓数量  =  oldPosCount  +  oldSellCount
    private int prevCount;
    // 今日 持仓数量  =  oldPosCount  +  newBuyCount
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





//    // -------------------------------------------------
//
//    // 今日 涨停数量
//    private int todayZtCount;
//    // 次日 开盘价涨跌幅（%）
//    private double todayZt_nextDayOpenPct;
//    // 次日 开盘价涨跌幅（%）
//    private double todayZt_nextDayLowPct;
//    private double todayZt_nextDayAvgPct;


}