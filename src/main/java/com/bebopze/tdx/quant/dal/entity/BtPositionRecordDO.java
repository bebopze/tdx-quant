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
import java.time.LocalDateTime;

/**
 * <p>
 * 回测-每日持仓记录
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Getter
@Setter
@ToString
@TableName("bt_position_record")
@Schema(name = "BtPositionRecordDO", description = "回测-每日持仓记录")
public class BtPositionRecordDO implements Serializable {

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
     * 交易日
     */
    @TableField("trade_date")
    @Schema(description = "交易日")
    private LocalDate tradeDate;

    /**
     * 股票ID
     */
    @TableField("stock_id")
    @Schema(description = "股票ID")
    private Long stockId;

    /**
     * 股票代码
     */
    @TableField("stock_code")
    @Schema(description = "股票代码")
    private String stockCode;

    /**
     * 股票名称
     */
    @TableField("stock_name")
    @Schema(description = "股票名称")
    private String stockName;

    /**
     * 加权平均成本价
     */
    @TableField("avg_cost_price")
    @Schema(description = "加权平均成本价")
    private BigDecimal avgCostPrice;

    /**
     * 当前交易日 - 收盘价
     */
    @TableField("close_price")
    @Schema(description = "当前交易日 - 收盘价")
    private BigDecimal closePrice;

    /**
     * 持仓数量
     */
    @TableField("quantity")
    @Schema(description = "持仓数量")
    private Integer quantity;

    /**
     * 可用数量
     */
    @Schema(description = "可用数量")
    @TableField("avl_quantity")
    private Integer avlQuantity;

    /**
     * 市值
     */
    @Schema(description = "市值")
    @TableField("market_value")
    private BigDecimal marketValue;

    /**
     * 仓位占比（%）
     */
    @Schema(description = "仓位占比（%）")
    @TableField("position_pct")
    private BigDecimal positionPct;

    /**
     * 浮动盈亏
     */
    @Schema(description = "浮动盈亏")
    @TableField("unrealized_pnl")
    private BigDecimal unrealizedPnl;

    /**
     * 盈亏率
     */
    @Schema(description = "盈亏率")
    @TableField("unrealized_pnl_pct")
    private BigDecimal unrealizedPnlPct;

    /**
     * 首次-买入日期
     */
    @TableField("buy_date")
    @Schema(description = "首次-买入日期")
    private LocalDate buyDate;

    /**
     * 持仓天数
     */
    @TableField("holding_days")
    @Schema(description = "持仓天数")
    private Integer holdingDays;

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
}
