package com.bebopze.tdx.quant.common.domain.dto.topblock;

import com.bebopze.tdx.quant.common.util.MapUtil;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;


/**
 * 主线板块/个股 池   ->   指数 涨跌幅（汇总 计算平均值）
 *
 * @author: bebopze
 * @date: 2025/10/7
 */
@Data
@NoArgsConstructor
public class TopPoolAvgPctDTO {


    // 交易日
    private transient LocalDate date;

    private int total;


    // -----------------------------------------------------------------------------------------------------------------


    // 昨日收盘价
    private double prev_close;
    // 今日收盘价
    private double today_close;
    // 今日涨幅（%）
    private double today_changePct;


    // 上榜涨幅（        今日  收盘价   ->   nextDay）
    private double today2Next_changePct;
    // 上榜涨幅（        今日  收盘价   ->   endTopDate）
    private double today2End_changePct;
    private double today2Max_changePct;


    // 上榜涨幅（首次 上榜日期  收盘价   ->   today）
    private double start2Today_changePct;
    // 上榜涨幅（首次 上榜日期  收盘价   ->   endTopDate）
    private double start2End_changePct;
    private double start2Max_changePct;


    private double start2Next_changePct;
    private double start2Next3_changePct;
    private double start2Next5_changePct;
    private double start2Next10_changePct;
    private double start2Next15_changePct;
    private double start2Next20_changePct;


    // -----------------------------------------------------------------------------------------------------------------


    private Map<String, Integer> buy_countMap = Maps.newLinkedHashMap();
    private Map<String, Integer> max_countMap = Maps.newLinkedHashMap();
    private Map<String, Integer> sell_countMap = Maps.newLinkedHashMap();


    public Map<String, Integer> getBuy_countMap() {
        return MapUtil.reverseSortByValue(buy_countMap);
    }

    public Map<String, Integer> getMax_countMap() {
        return MapUtil.reverseSortByValue(max_countMap);
    }

    public Map<String, Integer> getSell_countMap() {
        return MapUtil.reverseSortByValue(sell_countMap);
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 涨停个股 数量
    private int zt_count;

    // 次日   ->   O/H/L/C 涨跌幅（%）
    private double today2Next_openPct;
    private double today2Next_highPct;
    private double today2Next_lowPct;


    // 成交额
    private double amo;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 类型：1-机选；2-人选（TOP50）；3-历史新高；4-极多头；5-RPS三线红；6-10亿；7-首次三线红；8-口袋支点；9-T0；10-涨停（打板）；
     */
    private int type = 1;


    // -----------------------------------------------------------------------------------------------------------------


    public TopPoolAvgPctDTO(int type) {
        this.type = type;
    }

}