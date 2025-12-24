package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 回测-任务
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Getter
@Setter
@ToString
@TableName("bt_task")
@Schema(name = "BtTaskDO", description = "回测-任务")
public class BtTaskDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务批次号（用于 中断恢复）
     */
    @TableField(value = "batch_no")
    @Schema(description = "任务批次号（用于 中断恢复）")
    private Integer batchNo;

    /**
     * 任务状态（用于每日 更新至最新交易日）：1-进行中（新开任务）；2-已完成（已更新至 最新交易日）；3-待更新至 最新交易日（之前已完成过）；
     */
    @TableField(value = "status")
    @Schema(description = "任务状态（用于每日 更新至最新交易日）：1-进行中（新开任务）；2-已完成（已更新至 最新交易日）；3-待更新至 最新交易日（之前已完成过）；")
    private Integer status;

    /**
     * 是否提前淘汰：0-否；1-是；
     */
    @TableField(value = "fail_fast_flag")
    @Schema(description = "是否提前淘汰：0-否；1-是；")
    private Integer failFastFlag;

    /**
     * B策略
     */
    @TableField("buy_strategy")
    @Schema(description = "B策略")
    private String buyStrategy;

    /**
     * S策略
     */
    @Schema(description = "S策略")
    @TableField("sell_strategy")
    private String sellStrategy;

    /**
     * 主线策略
     */
    @Schema(description = "主线策略")
    @TableField("top_block_strategy")
    private String topBlockStrategy;

    /**
     * 回测-起始日期
     */
    @TableField("start_date")
    @Schema(description = "回测-起始日期")
    private LocalDate startDate;

    /**
     * 回测-结束日期
     */
    @TableField("end_date")
    @Schema(description = "回测-结束日期")
    private LocalDate endDate;

    /**
     * 初始资金
     */
    @Schema(description = "初始资金")
    @TableField("initial_capital")
    private BigDecimal initialCapital;

    /**
     * 结束资金
     */
    @TableField("final_capital")
    @Schema(description = "结束资金")
    private BigDecimal finalCapital;

    /**
     * 交易总笔数
     */
    @TableField("total_trades")
    @Schema(description = "交易总笔数")
    private Integer totalTrades;

    /**
     * 交易总金额
     */
    @TableField("total_trade_amount")
    @Schema(description = "交易总金额")
    private BigDecimal totalTradeAmount;

    /**
     * 初始净值
     */
    @Schema(description = "初始净值")
    @TableField("initial_nav")
    private BigDecimal initialNav;

    /**
     * 结束净值
     */
    @TableField("final_nav")
    @Schema(description = "结束净值")
    private BigDecimal finalNav;

    /**
     * 总天数
     */
    @TableField("total_days")
    @Schema(description = "总天数")
    private Integer totalDays;

    /**
     * 总收益率（%）
     */
    @Schema(description = "总收益率（%）")
    @TableField("total_return_pct")
    private BigDecimal totalReturnPct;

    /**
     * 年化收益率（%）
     */
    @Schema(description = "年化收益率（%）")
    @TableField("annual_return_pct")
    private BigDecimal annualReturnPct;

    /**
     * 胜率（%）
     */
    @TableField("win_pct")
    @Schema(description = "胜率（%）")
    private BigDecimal winPct;

    /**
     * 盈亏比
     */
    @Schema(description = "盈亏比")
    @TableField("profit_factor")
    private BigDecimal profitFactor;

    /**
     * 最大回撤（%）
     */
    @Schema(description = "最大回撤（%）")
    @TableField("max_drawdown_pct")
    private BigDecimal maxDrawdownPct;

    /**
     * 盈利天数 占比  =  盈利天数 / 总天数
     */
    @TableField("profit_days_pct")
    @Schema(description = "盈利天数-占比")
    private BigDecimal profitDaysPct;

    /**
     * 平局天数 占比  =  平局天数 / 总天数
     */
    @TableField("draw_days_pct")
    @Schema(description = "平局天数-占比")
    private BigDecimal drawDaysPct;

    /**
     * 亏损天数 占比  =  亏损天数 / 总天数
     */
    @TableField("loss_days_pct")
    @Schema(description = "亏损天数-占比")
    private BigDecimal lossDaysPct;

    /**
     * 持仓天数 占比  =  持仓天数 / 总天数
     */
    @TableField("hold_pos_days_pct")
    @Schema(description = "持仓天数-占比")
    private BigDecimal holdPosDaysPct;

    /**
     * 空仓天数 占比  =  空仓天数 / 总天数
     */
    @TableField("clear_pos_days_pct")
    @Schema(description = "空仓天数-占比")
    private BigDecimal clearPosDaysPct;

    /**
     * 持仓平均仓位（%） =  仓位之和 / 持仓天数
     *
     * 仅统计 有持仓时 的平均仓位
     */
    @TableField("avg_pos_when_hold_pct")
    @Schema(description = "持仓平均仓位（%）")
    private BigDecimal avgPosWhenHoldPct;

    /**
     * 总平均仓位（%） =  仓位之和 / 总天数               // 平均资金利用率（%）
     *
     * 统计 全部天数 的平均仓位
     */
    @TableField("avg_pos_pct")
    @Schema(description = "总平均仓位（%）")
    private BigDecimal avgPosPct;

    /**
     * 持仓日均持股数量  =  持仓总持股数量 / 持仓天数
     *
     * 仅统计 有持仓时 的平均持股数量
     */
    @TableField("avg_pos_when_hold_count")
    @Schema(description = "持仓日均持股数量")
    private BigDecimal avgPosWhenHoldCount;

    /**
     * 总日均持股数量  =  持仓总持股数量 / 总天数
     *
     * 统计 全部天数 的平均持股数量
     */
    @TableField("avg_pos_count")
    @Schema(description = "总日均持股数量")
    private BigDecimal avgPosCount;

    /**
     * 夏普比率
     */
    @TableField("sharpe_ratio")
    @Schema(description = "夏普比率")
    private BigDecimal sharpeRatio;

    /**
     * 胜率-JSON详情
     */
    @Schema(description = "胜率-JSON详情")
    @TableField("trade_stat_result")
    @JsonIgnore
    private transient String tradeStatResult;

    /**
     * 最大回撤-JSON详情
     */
    @Schema(description = "最大回撤-JSON详情")
    @TableField("drawdown_result")
    @JsonIgnore
    private String drawdownResult;

    /**
     * 扩展字段-JSON
     */
    @Schema(description = "扩展字段-JSON")
    @TableField("ext_data")
    // @JsonIgnore
    @JsonRawValue
    private String extData;

    /**
     * 创建时间
     */
    @TableField("gmt_create")
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField("gmt_modify")
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtModify;


    // -----------------------------------------------------------------------------------------------------------------


//    public JSONObject getDrawdownResultDesc() {
//        if (drawdownResult == null) {
//            return new JSONObject();
//        }
//
//        return JSON.parseObject(drawdownResult);
//    }
//
//    public JSONObject getTradeStatResultDesc() {
//        if (tradeStatResult == null) {
//            return new JSONObject();
//        }
//
//        return JSON.parseObject(tradeStatResult);
//    }


    @JsonRawValue
    public String getExtData() {
        return null == extData ? "" : extData;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TableField(exist = false)
    private LocalDate startDate2;

    @TableField(exist = false)
    private LocalDate endDate2;


}