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
 * 期权BS回测-信号事件
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
@Getter
@Setter
@ToString
@TableName("obt_signal_event")
@Schema(name = "ObtSignalEventDO", description = "期权BS回测-信号事件")
public class ObtSignalEventDO implements Serializable {

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
     * 信号日
     */
    @Schema(description = "信号日")
    @TableField("signal_date")
    private LocalDate signalDate;

    /**
     * 执行日
     */
    @Schema(description = "执行日")
    @TableField("exec_date")
    private LocalDate execDate;

    /**
     * 前状态
     */
    @Schema(description = "前状态")
    @TableField("prev_status")
    private Integer prevStatus;

    /**
     * 1底 3顶
     */
    @Schema(description = "1底 3顶")
    @TableField("signal_type")
    private Integer signalType;

    /**
     * 周期ID
     */
    @Schema(description = "周期ID")
    @TableField("cycle_id")
    private Integer cycleId;

    /**
     * 参考标的
     */
    @Schema(description = "参考标的")
    @TableField("ref_underlying")
    private String refUnderlying;

    /**
     * 参考收盘
     */
    @Schema(description = "参考收盘")
    @TableField("ref_close")
    private BigDecimal refClose;

    /**
     * 底_DAY
     */
    @Schema(description = "底_DAY")
    @TableField("market_low_day")
    private Integer marketLowDay;

    /**
     * 顶_DAY
     */
    @Schema(description = "顶_DAY")
    @TableField("market_high_day")
    private Integer marketHighDay;

    /**
     * MA50占比（%）
     */
    @Schema(description = "MA50占比（%）")
    @TableField("ma50_pct")
    private BigDecimal ma50Pct;

    /**
     * 仓位占比（%）
     */
    @Schema(description = "仓位占比（%）")
    @TableField("position_pct")
    private BigDecimal positionPct;

    /**
     * 是否处理
     */
    @Schema(description = "是否处理")
    @TableField("processed_flag")
    private Integer processedFlag;

}
