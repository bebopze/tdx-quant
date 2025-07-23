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
import java.time.LocalDateTime;

/**
 * <p>
 * 回测-每日收益率
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Getter
@Setter
@ToString
@TableName("bt_daily_return")
@Schema(name = "BtDailyReturnDO", description = "回测-每日收益率")
public class BtDailyReturnDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 回测任务ID
     */
    @TableField("task_id")
    @Schema(description = "回测任务ID")
    private Long taskId;

    /**
     * 交易日期
     */
    @TableField("trade_date")
    @Schema(description = "交易日期")
    private LocalDate tradeDate;

    /**
     * 当日收益率
     */
    @TableField("daily_return")
    @Schema(description = "当日收益率")
    private BigDecimal dailyReturn;

    /**
     * 当日盈亏额
     */
    @TableField("profit_loss_amount")
    @Schema(description = "当日盈亏额")
    private BigDecimal profitLossAmount;

    /**
     * 净值（初始为1.0000）
     */
    @TableField("nav")
    @Schema(description = "净值（初始为1.0000）")
    private BigDecimal nav;

    /**
     * 总资金 = 持仓市值 + 可用资金
     */
    @TableField("capital")
    @Schema(description = "总资金")
    private BigDecimal capital;

    /**
     * 持仓市值
     */
    @TableField("market_value")
    @Schema(description = "持仓市值")
    private BigDecimal marketValue;

    /**
     * 仓位占比（%）
     */
    @Schema(description = "仓位占比（%）")
    @TableField("position_pct")
    private BigDecimal positionPct;

    /**
     * 仓位上限占比（%）
     */
    @Schema(description = "仓位上限占比（%）")
    @TableField("position_limit_pct")
    private BigDecimal positionLimitPct;

    /**
     * 可用资金
     */
    @TableField("avl_capital")
    @Schema(description = "可用资金")
    private BigDecimal avlCapital;

    /**
     * 买入金额
     */
    @TableField("buy_capital")
    @Schema(description = "买入金额")
    private BigDecimal buyCapital;

    /**
     * 卖出金额
     */
    @TableField("sell_capital")
    @Schema(description = "卖出金额")
    private BigDecimal sellCapital;

    /**
     * 基准收益（沪深300、标普500、行业指数、...）
     */
    @TableField("benchmark_return")
    @Schema(description = "基准收益（可选）")
    private BigDecimal benchmarkReturn;

    /**
     * 创建时间
     */
    @TableField("gmt_create")
    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField("gmt_modify")
    @Schema(description = "更新时间")
    private LocalDateTime gmtModify;
}
