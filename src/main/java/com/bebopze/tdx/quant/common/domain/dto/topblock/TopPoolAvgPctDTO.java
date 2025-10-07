package com.bebopze.tdx.quant.common.domain.dto.topblock;

import lombok.Data;


/**
 * 主线板块/个股 池   ->   指数 涨跌幅（汇总 计算平均值）
 *
 * @author: bebopze
 * @date: 2025/10/7
 */
@Data
public class TopPoolAvgPctDTO {


    // 上榜涨幅（首次 上榜日期  收盘价   ->   today）
    private double start2Today_changePct;
    // 上榜涨幅（首次 上榜日期  收盘价   ->   endTopDate）
    private double start2End_changePct;
    private double start2Max_changePct;


    // 上榜涨幅（        今日  收盘价   ->   nextDay）
    private double today2Next_changePct;
    // 上榜涨幅（        今日  收盘价   ->   endTopDate）
    private double today2End_changePct;
    private double today2Max_changePct;


    private double start2Next_changePct;
    private double start2Next3_changePct;
    private double start2Next5_changePct;
    private double start2Next10_changePct;
    private double start2Next15_changePct;
    private double start2Next20_changePct;
}