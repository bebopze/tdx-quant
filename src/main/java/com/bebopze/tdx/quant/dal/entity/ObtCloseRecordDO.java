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
 * 期权BS回测-平仓记录
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
@Getter
@Setter
@ToString
@TableName("obt_close_record")
@Schema(name = "ObtCloseRecordDO", description = "期权BS回测-平仓记录")
public class ObtCloseRecordDO implements Serializable {

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
     * 周期ID
     */
    @Schema(description = "周期ID")
    @TableField("cycle_id")
    private Integer cycleId;

    /**
     * 标的code
     */
    @Schema(description = "标的code")
    @TableField("underlying_code")
    private String underlyingCode;

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
     * 开仓日
     */
    @Schema(description = "开仓日")
    @TableField("open_date")
    private LocalDate openDate;

    /**
     * 平仓日
     */
    @Schema(description = "平仓日")
    @TableField("close_date")
    private LocalDate closeDate;

    /**
     * 平仓数量
     */
    @Schema(description = "平仓数量")
    @TableField("qty")
    private Integer qty;

    /**
     * 均价成本
     */
    @Schema(description = "均价成本")
    @TableField("avg_cost")
    private BigDecimal avgCost;

    /**
     * 均价卖出
     */
    @Schema(description = "均价卖出")
    @TableField("avg_sell_price")
    private BigDecimal avgSellPrice;

    /**
     * 已实现盈亏
     */
    @Schema(description = "已实现盈亏")
    @TableField("realized_pnl")
    private BigDecimal realizedPnl;

    /**
     * 收益率（%）
     */
    @Schema(description = "收益率（%）")
    @TableField("pnl_pct")
    private BigDecimal pnlPct;

    /**
     * 持有天数
     */
    @Schema(description = "持有天数")
    @TableField("hold_days")
    private Integer holdDays;

    /**
     * 最大浮盈（%）
     */
    @Schema(description = "最大浮盈（%）")
    @TableField("max_pnl_pct")
    private BigDecimal maxPnlPct;

    /**
     * 最大浮亏（%）
     */
    @Schema(description = "最大浮亏（%）")
    @TableField("min_pnl_pct")
    private BigDecimal minPnlPct;

    /**
     * 平仓原因
     */
    @Schema(description = "平仓原因")
    @TableField("close_reason")
    private String closeReason;

    /**
     * 平仓原因描述
     */
    @Schema(description = "平仓原因描述")
    @TableField("close_reason_desc")
    private String closeReasonDesc;

}
