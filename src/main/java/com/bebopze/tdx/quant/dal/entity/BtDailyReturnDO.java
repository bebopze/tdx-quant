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
     * 净值（初始为1.0000）
     */
    @TableField("nav")
    @Schema(description = "净值（初始为1.0000）")
    private BigDecimal nav;

    /**
     * 期末资金
     */
    @TableField("capital")
    @Schema(description = "期末资金")
    private BigDecimal capital;

    /**
     * 基准收益（可选）
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
