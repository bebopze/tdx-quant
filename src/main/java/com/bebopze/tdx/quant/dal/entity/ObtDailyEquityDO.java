package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <p>
 * 期权BS回测-每日净值
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
@Getter
@Setter
@ToString
@TableName("obt_daily_equity")
@Schema(name = "ObtDailyEquityDO", description = "期权BS回测-每日净值")
public class ObtDailyEquityDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @Schema(description = "任务ID")
    @TableField("task_id")
    private Long taskId;

    /**
     * 交易日
     */
    @Schema(description = "交易日")
    @TableField("trade_date")
    private LocalDate tradeDate;

    /**
     * 单位净值
     */
    @Schema(description = "单位净值")
    @TableField("nav")
    private BigDecimal nav;

    /**
     * 总权益
     */
    @Schema(description = "总权益")
    @TableField("equity")
    private BigDecimal equity;

    /**
     * 现金
     */
    @Schema(description = "现金")
    @TableField("cash")
    private BigDecimal cash;

    /**
     * 持仓市值
     */
    @Schema(description = "持仓市值")
    @TableField("market_value")
    private BigDecimal marketValue;

    /**
     * 仓位（%）
     */
    @Schema(description = "仓位（%）")
    @TableField("position_pct")
    private BigDecimal positionPct;

    /**
     * 当日收益（%）
     */
    @Schema(description = "当日收益（%）")
    @TableField("daily_return_pct")
    private BigDecimal dailyReturnPct;

    /**
     * 累计收益（%）
     */
    @Schema(description = "累计收益（%）")
    @TableField("cum_return_pct")
    private BigDecimal cumReturnPct;

    /**
     * 回撤（%）
     */
    @Schema(description = "回撤（%）")
    @TableField("drawdown_pct")
    private BigDecimal drawdownPct;

    /**
     * 基准收盘
     */
    @Schema(description = "基准收盘")
    @TableField("benchmark_close")
    private BigDecimal benchmarkClose;

    /**
     * 基准净值
     */
    @Schema(description = "基准净值")
    @TableField("benchmark_nav")
    private BigDecimal benchmarkNav;

    /**
     * 基准收益（%）
     */
    @Schema(description = "基准收益（%）")
    @TableField("benchmark_return_pct")
    private BigDecimal benchmarkReturnPct;

    /**
     * 信号类型：1底 3顶
     */
    @Schema(description = "信号类型：1底 3顶")
    @TableField("signal_type")
    private Integer signalType;

    /**
     * 信号状态
     */
    @Schema(description = "信号状态")
    @TableField("signal_status")
    private String signalStatus;

    /**
     * 持仓数
     */
    @Schema(description = "持仓数")
    @TableField("position_count")
    private Integer positionCount;

    /**
     * 费用
     */
    @Schema(description = "费用")
    @TableField("fee")
    private BigDecimal fee;

    /**
     * 是否有缺失行情
     */
    @Schema(description = "是否有缺失行情")
    @TableField("stale_flag")
    private Integer staleFlag;

    /**
     * 当日买入金额（查询聚合，非表字段）
     */
    @Schema(description = "当日买入金额")
    @TableField(exist = false)
    private BigDecimal buyAmount;

    /**
     * 当日卖出金额（查询聚合，非表字段）
     */
    @Schema(description = "当日卖出金额")
    @TableField(exist = false)
    private BigDecimal sellAmount;

}
