package com.bebopze.tdx.quant.common.domain.dto.topblock;

import lombok.Data;

import java.time.LocalDate;


/**
 * 上榜涨幅
 *
 * @author: bebopze
 * @date: 2025/9/28
 */
@Data
public class TopChangePctDTO {


    // 当前日期（指定 基准日期）
    // private LocalDate today;


    // 首次 上榜日期（以 today 为基准日期，往前倒推          直至   连续 7日 未上榜   ->   取idx=7，即这 7个日期 之后的 第1个日期）
    //                                               若不存在 连续 7日 未上榜   ->   降级为   连续3日 未上榜）
    //                                               若不存在 连续 3日 未上榜   ->   降级为   连续1日 未上榜）
    public LocalDate firstTopDate;

    // 跌出 榜单日期（以 today 为基准日期，往后倒推          直至   连续10日 未上榜   ->   取idx=0，即这10个日期中的 第1个日期）
    //                                               若不存在 连续10日 未上榜   ->   降级为   连续5日 未上榜）
    //                                               若不存在 连续 5日 未上榜   ->   降级为   连续3日 未上榜）
    //                                               若不存在 连续 3日 未上榜   ->   降级为   连续1日 未上榜）
    public LocalDate endTopDate;


    // 上榜涨幅（首次 上榜日期  收盘价   ->   today）
    public double first2Today_changePct;

    // 上榜涨幅（首次 上榜日期  收盘价   ->   endTopDate）
    public double first2End_changePct;

    // 上榜涨幅（        今日  收盘价   ->   endTopDate）
    public double today2End_changePct;
}