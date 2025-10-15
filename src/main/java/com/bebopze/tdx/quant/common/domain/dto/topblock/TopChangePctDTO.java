package com.bebopze.tdx.quant.common.domain.dto.topblock;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


/**
 * 上榜日期、涨幅
 *
 * @author: bebopze
 * @date: 2025/9/28
 */
@Data
@NoArgsConstructor
public class TopChangePctDTO {


    // 当前日期（指定 基准日期）
    // private LocalDate today;


    /**
     * 主线 板块/个股code
     */
    private String code;


    // 首次 上榜日期（以 today 为基准日期，往前倒推          SSF空/MA20空 -> 至今   区间   首次上榜）
    @JSONField(format = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate topStartDate;


    // 跌出 榜单日期（以 today 为基准日期，往后倒推          今日 往后   ->   首次 下SSF/下MA20）
    @JSONField(format = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate topEndDate;


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


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 备选优化方案（替代 -> xxx_man 字段）
     *
     * 类型：1-机选；2-人选；
     */
    private int type = 1;


    /**
     * 人选列表 -> 是否删除：0-否；1-是；
     */
    private int isDel = 0;


    // -----------------------------------------------------------------------------------------------------------------


    public TopChangePctDTO(String code) {
        this.code = code;
    }

}