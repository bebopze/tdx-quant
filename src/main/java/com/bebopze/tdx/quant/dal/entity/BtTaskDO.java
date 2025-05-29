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
     * 总天数
     */
    @TableField("total_day")
    @Schema(description = "总天数")
    private Integer totalDay;

    /**
     * 总收益率
     */
    @Schema(description = "总收益率")
    @TableField("total_return_pct")
    private BigDecimal totalReturnPct;

    /**
     * 年化收益率
     */
    @Schema(description = "年化收益率")
    @TableField("annual_return_pct")
    private BigDecimal annualReturnPct;

    /**
     * 夏普比率
     */
    @TableField("sharpe_ratio")
    @Schema(description = "夏普比率")
    private BigDecimal sharpeRatio;

    /**
     * 最大回撤
     */
    @Schema(description = "最大回撤")
    @TableField("max_drawdown_pct")
    private BigDecimal maxDrawdownPct;

    /**
     * 胜率
     */
    @TableField("win_rate")
    @Schema(description = "胜率")
    private BigDecimal winRate;

    /**
     * 盈亏比
     */
    @Schema(description = "盈亏比")
    @TableField("profit_factor")
    private BigDecimal profitFactor;

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
