package com.bebopze.tdx.quant.common.domain.dto.topblock;

import com.alibaba.fastjson2.annotation.JSONField;
import com.bebopze.tdx.quant.common.constant.TopTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;


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
    private String name;


    // 首次 上榜日期（以 today 为基准日期，往前倒推          SSF空/MA20空 -> 至今   区间   首次上榜）
    @JSONField(format = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate topStartDate;


    // 跌出 榜单日期（以 today 为基准日期，往后倒推          今日 往后   ->   首次 下SSF/下MA20）
    @JSONField(format = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate topEndDate;


    private double prev_close;
    private double today_close;
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


    // B/Max/S信号
    private Set<String> buySignalSet = Sets.newHashSet();
    private Set<String> maxSignalSet = Sets.newHashSet();
    private Set<String> sellSignalSet = Sets.newHashSet();


    private ExtDataDTO buySignalExtDataDTO;
    private ExtDataDTO maxSignalExtDataDTO;
    private ExtDataDTO sellSignalExtDataDTO;


    // -----------------------------------------------------------------------------------------------------------------

    // 人选 -> 策略（TOP50）


//    private ExtDataDTO topSignalExtDataDTO = buySignalExtDataDTO;


    // 是否涨停
    private boolean ztFlag;
    // 是否跌停
    private boolean dtFlag;


    // 次日   ->   开盘价/最高价/最低价/收盘价 涨跌幅（%）    （当日 涨停后  ->  次日   是否能买进、平均可买进 挂单价格%、平均涨幅、打板成功率）
    private double today2Next_openPct;
    private double today2Next_highPct;
    private double today2Next_lowPct;                         // 次1日 涨跌幅   基准日期/价格：今日收盘价

    // 次2日   ->   开盘价/最高价/最低价/收盘价 涨跌幅（%）         // 次2日 涨跌幅   基准日期/价格：今日收盘价
    private double today2N2_openPct;
    private double today2N2_highPct;
    private double today2N2_lowPct;
    private double today2N2_closePct;


    // 次日   ->   开盘价/最高价/最低价/收盘价
    private double today2Next_open;
    private double today2Next_high;
    private double today2Next_low;
    private double today2Next_close;

    // 次2日   ->   开盘价/最高价/最低价/收盘价
    private double today2N2_open;
    private double today2N2_high;
    private double today2N2_low;
    private double today2N2_close;


    // -----------------------------------------------------------------------------------------------------------------


//    // 昨日是否持仓
//    private boolean prev_posFlag = false;
//    // 今日是否持仓
//    private boolean posFlag = false;
//    // 1-买入；2-持仓中；3-空仓；
//    private int posStatus = 3;
//    // 首次买入日期
//    private LocalDate firstBuyDate;
//    // 卖出日期
//    private LocalDate sellDate;


    // -----------------------------------------------------------------------------------------------------------------


    // 成交额
    private double amo;


    private double RPS三线和;
    private double RPS五线和;


    private double 中期涨幅;
    private double N3日涨幅;
    private double N5日涨幅;
    private double N10日涨幅;
    private double N20日涨幅;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 策略类型：1-机选；2-人选；3-历史新高；4-极多头；5-RPS三线红；6-10亿；7-首次三线红；8-口袋支点；9-T0；10-涨停（打板）；
     *
     * @see TopTypeEnum
     */
    private Set<Integer> topTypeSet = Sets.newHashSet(1);


    // -----------------------------------------------------------------------------------------------------------------


    public TopChangePctDTO(String code, String name) {
        this.code = code;
        this.name = name;
    }


}