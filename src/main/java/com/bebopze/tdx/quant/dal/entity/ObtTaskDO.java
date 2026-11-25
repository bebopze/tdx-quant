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
 * 期权BS回测-任务
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
@Getter
@Setter
@ToString
@TableName("obt_task")
@Schema(name = "ObtTaskDO", description = "期权BS回测-任务")
public class ObtTaskDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务号
     */
    @Schema(description = "任务号")
    @TableField("task_no")
    private String taskNo;

    /**
     * 任务名称
     */
    @Schema(description = "任务名称")
    @TableField("task_name")
    private String taskName;

    /**
     * 状态：0排队 1运行 2成功 3失败 4取消
     */
    @Schema(description = "状态：0排队 1运行 2成功 3失败 4取消")
    @TableField("status")
    private Integer status;

    /**
     * 进度 0-100
     */
    @Schema(description = "进度 0-100")
    @TableField("progress")
    private Integer progress;

    /**
     * 回测起始日
     */
    @Schema(description = "回测起始日")
    @TableField("start_date")
    private LocalDate startDate;

    /**
     * 回测结束日
     */
    @Schema(description = "回测结束日")
    @TableField("end_date")
    private LocalDate endDate;

    /**
     * 实际首个建仓日
     */
    @Schema(description = "实际首个建仓日")
    @TableField("actual_start_date")
    private LocalDate actualStartDate;

    /**
     * 初始本金
     */
    @Schema(description = "初始本金")
    @TableField("initial_capital")
    private BigDecimal initialCapital;

    /**
     * 标的code（逗号分隔）
     */
    @Schema(description = "标的code（逗号分隔）")
    @TableField("underlying_codes")
    private String underlyingCodes;

    /**
     * 信号模式
     */
    @Schema(description = "信号模式")
    @TableField("signal_mode")
    private String signalMode;

    /**
     * 月份模式
     */
    @Schema(description = "月份模式")
    @TableField("month_mode")
    private String monthMode;

    /**
     * 行权价模式
     */
    @Schema(description = "行权价模式")
    @TableField("strike_mode")
    private String strikeMode;

    /**
     * 参数JSON
     */
    @Schema(description = "参数JSON")
    @TableField("params_json")
    private String paramsJson;

    /**
     * 引擎版本
     */
    @Schema(description = "引擎版本")
    @TableField("engine_version")
    private String engineVersion;

    /**
     * 数据版本
     */
    @Schema(description = "数据版本")
    @TableField("data_version")
    private String dataVersion;

    /**
     * 期末权益
     */
    @Schema(description = "期末权益")
    @TableField("final_equity")
    private BigDecimal finalEquity;

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
     * 最大回撤（%）
     */
    @Schema(description = "最大回撤（%）")
    @TableField("max_drawdown_pct")
    private BigDecimal maxDrawdownPct;

    /**
     * 最大回撤开始日
     */
    @Schema(description = "最大回撤开始日")
    @TableField("max_drawdown_start")
    private LocalDate maxDrawdownStart;

    /**
     * 最大回撤结束日
     */
    @Schema(description = "最大回撤结束日")
    @TableField("max_drawdown_end")
    private LocalDate maxDrawdownEnd;

    /**
     * 最大回撤天数
     */
    @Schema(description = "最大回撤天数")
    @TableField("max_drawdown_days")
    private Integer maxDrawdownDays;

    /**
     * 夏普比率
     */
    @Schema(description = "夏普比率")
    @TableField("sharpe_ratio")
    private BigDecimal sharpeRatio;

    /**
     * 索提诺比率
     */
    @Schema(description = "索提诺比率")
    @TableField("sortino_ratio")
    private BigDecimal sortinoRatio;

    /**
     * 卡玛比率
     */
    @Schema(description = "卡玛比率")
    @TableField("calmar_ratio")
    private BigDecimal calmarRatio;

    /**
     * 年化波动率（%）
     */
    @Schema(description = "年化波动率（%）")
    @TableField("volatility_pct")
    private BigDecimal volatilityPct;

    /**
     * 胜率（%）
     */
    @Schema(description = "胜率（%）")
    @TableField("win_rate_pct")
    private BigDecimal winRatePct;

    /**
     * 盈亏比
     */
    @Schema(description = "盈亏比")
    @TableField("profit_factor")
    private BigDecimal profitFactor;

    /**
     * 交易笔数
     */
    @Schema(description = "交易笔数")
    @TableField("total_trades")
    private Integer totalTrades;

    /**
     * 买入笔数
     */
    @Schema(description = "买入笔数")
    @TableField("buy_trades")
    private Integer buyTrades;

    /**
     * 卖出笔数
     */
    @Schema(description = "卖出笔数")
    @TableField("sell_trades")
    private Integer sellTrades;

    /**
     * 交易日数
     */
    @Schema(description = "交易日数")
    @TableField("total_days")
    private Integer totalDays;

    /**
     * 平均仓位（%）
     */
    @Schema(description = "平均仓位（%）")
    @TableField("avg_position_pct")
    private BigDecimal avgPositionPct;

    /**
     * 交易成本
     */
    @Schema(description = "交易成本")
    @TableField("total_fee")
    private BigDecimal totalFee;

    /**
     * 信号次数
     */
    @Schema(description = "信号次数")
    @TableField("signal_count")
    private Integer signalCount;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    @TableField("started_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    @TableField("finished_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedAt;

    /**
     * 耗时（ms）
     */
    @Schema(description = "耗时（ms）")
    @TableField("duration_ms")
    private Long durationMs;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    @TableField("creator")
    private String creator;

    /**
     * 收藏：0否 1是
     */
    @Schema(description = "收藏：0否 1是")
    @TableField("favorite_flag")
    private Integer favoriteFlag;

    /**
     * 逻辑删除：0否 1是
     */
    @Schema(description = "逻辑删除：0否 1是")
    @TableField("deleted_flag")
    private Integer deletedFlag;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField("gmt_create")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField("gmt_modify")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtModify;

}
