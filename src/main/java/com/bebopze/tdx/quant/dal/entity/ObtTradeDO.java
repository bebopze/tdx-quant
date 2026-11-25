package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * <p>
 * 期权BS回测-交易明细
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
@Getter
@Setter
@ToString
@TableName("obt_trade")
@Schema(name = "ObtTradeDO", description = "期权BS回测-交易明细")
public class ObtTradeDO implements Serializable {

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
     * 交易时间
     */
    @Schema(description = "交易时间")
    @TableField("trade_time")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime tradeTime;

    /**
     * 当日序号
     */
    @Schema(description = "当日序号")
    @TableField("seq")
    private Integer seq;

    /**
     * 1买 2卖
     */
    @Schema(description = "1买 2卖")
    @TableField("trade_type")
    private Integer tradeType;

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
     * 成交价
     */
    @Schema(description = "成交价")
    @TableField("price")
    private BigDecimal price;

    /**
     * 数量
     */
    @Schema(description = "数量")
    @TableField("qty")
    private Integer qty;

    /**
     * 合约乘数
     */
    @Schema(description = "合约乘数")
    @TableField("multiplier")
    private Integer multiplier;

    /**
     * 成交额
     */
    @Schema(description = "成交额")
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 费用
     */
    @Schema(description = "费用")
    @TableField("fee")
    private BigDecimal fee;

    /**
     * 触发规则
     */
    @Schema(description = "触发规则")
    @TableField("reason_code")
    private String reasonCode;

    /**
     * 触发描述
     */
    @Schema(description = "触发描述")
    @TableField("reason_desc")
    private String reasonDesc;

    /**
     * 触发值
     */
    @Schema(description = "触发值")
    @TableField("trigger_value")
    private BigDecimal triggerValue;

    /**
     * 成交后现金
     */
    @Schema(description = "成交后现金")
    @TableField("cash_after")
    private BigDecimal cashAfter;

    /**
     * 成交后持仓
     */
    @Schema(description = "成交后持仓")
    @TableField("qty_after")
    private Integer qtyAfter;

    /**
     * 周期ID
     */
    @Schema(description = "周期ID")
    @TableField("cycle_id")
    private Integer cycleId;

    /**
     * 信号日
     */
    @Schema(description = "信号日")
    @TableField("signal_date")
    private LocalDate signalDate;

    /**
     * 是否顺延
     */
    @Schema(description = "是否顺延")
    @TableField("deferred_flag")
    private Integer deferredFlag;

}
