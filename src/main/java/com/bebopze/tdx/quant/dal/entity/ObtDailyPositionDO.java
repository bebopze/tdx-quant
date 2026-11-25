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
 * 期权BS回测-每日持仓
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
@Getter
@Setter
@ToString
@TableName("obt_daily_position")
@Schema(name = "ObtDailyPositionDO", description = "期权BS回测-每日持仓")
public class ObtDailyPositionDO implements Serializable {

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
     * 标的code
     */
    @Schema(description = "标的code")
    @TableField("underlying_code")
    private String underlyingCode;

    /**
     * 标的name
     */
    @Schema(description = "标的name")
    @TableField("underlying_name")
    private String underlyingName;

    /**
     * 合约code
     */
    @Schema(description = "合约code")
    @TableField("option_code")
    private String optionCode;

    /**
     * 合约name
     */
    @Schema(description = "合约name")
    @TableField("option_name")
    private String optionName;

    /**
     * C/P
     */
    @Schema(description = "C/P")
    @TableField("call_put_type")
    private String callPutType;

    /**
     * 行权价
     */
    @Schema(description = "行权价")
    @TableField("strike_price")
    private BigDecimal strikePrice;

    /**
     * 合约月份
     */
    @Schema(description = "合约月份")
    @TableField("contract_month")
    private Integer contractMonth;

    /**
     * 到期日
     */
    @Schema(description = "到期日")
    @TableField("expiry_date")
    private LocalDate expiryDate;

    /**
     * 合约乘数
     */
    @Schema(description = "合约乘数")
    @TableField("multiplier")
    private Integer multiplier;

    /**
     * 数量
     */
    @Schema(description = "数量")
    @TableField("qty")
    private Integer qty;

    /**
     * 成本价
     */
    @Schema(description = "成本价")
    @TableField("avg_cost")
    private BigDecimal avgCost;

    /**
     * 收盘价
     */
    @Schema(description = "收盘价")
    @TableField("close_price")
    private BigDecimal closePrice;

    /**
     * 成本金额
     */
    @Schema(description = "成本金额")
    @TableField("cost_amount")
    private BigDecimal costAmount;

    /**
     * 市值
     */
    @Schema(description = "市值")
    @TableField("market_value")
    private BigDecimal marketValue;

    /**
     * 浮动盈亏
     */
    @Schema(description = "浮动盈亏")
    @TableField("unrealized_pnl")
    private BigDecimal unrealizedPnl;

    /**
     * 收益率（%）
     */
    @Schema(description = "收益率（%）")
    @TableField("pnl_pct")
    private BigDecimal pnlPct;

    /**
     * 权重（%）
     */
    @Schema(description = "权重（%）")
    @TableField("weight_pct")
    private BigDecimal weightPct;

    /**
     * 持仓天数
     */
    @Schema(description = "持仓天数")
    @TableField("hold_days")
    private Integer holdDays;

    /**
     * 建仓来源
     */
    @Schema(description = "建仓来源")
    @TableField("open_reason")
    private String openReason;

    /**
     * 虚实值
     */
    @Schema(description = "虚实值")
    @TableField("moneyness")
    private String moneyness;

    /**
     * 是否有缺失行情
     */
    @Schema(description = "是否有缺失行情")
    @TableField("stale_flag")
    private Integer staleFlag;

}
