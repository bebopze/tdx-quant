package com.bebopze.tdx.quant.common.domain.dto.trade;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 账户  ->  风控预警
 *
 * 面向 A 股融资融券账户的“当前快照”风控输出。
 * 只做确定性、可解释的规则判断，不引入黑盒评分：
 * 1）担保比例（200% / 150% / 130%）
 * 2）总仓位 / 融资上限
 * 3）单票集中度
 * 4）可用资金 / 保证金缓冲
 * 5）持仓亏损
 * 6）数据完整性
 *
 * @author: bebopze
 * @date: 2026/10/20
 */
@Data
@Schema(name = "AccountWarnDTO", description = "股票融资账户 - 风控预警")
public class AccountWarnDTO {


    @Schema(description = "数据是否完整；false 时禁止据此加仓")
    private Boolean dataComplete;

    @Schema(description = "风险等级：SAFE-正常；WATCH-关注；WARN-预警；CRITICAL-严重")
    private String riskLevel;

    @Schema(description = "风险等级中文")
    private String riskLevelLabel;

    @Schema(description = "风险评分：0~100，越高风险越大")
    private Integer riskScore;

    @Schema(description = "风险摘要")
    private String riskSummary;

    @Schema(description = "计算时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime asOf;

    @Schema(description = "数据来源")
    private String source;

    @Schema(description = "风控模型版本")
    private String modelVersion;

    @Schema(description = "数据质量问题")
    private List<String> dataIssues;

    @Schema(description = "命中的风险项（按严重程度可排序）")
    private List<RiskWarning> warnings;

    @Schema(description = "建议动作（去重）")
    private List<String> actions;

    @Schema(description = "账户快照")
    private AccountSnapshot account;

    @Schema(description = "担保比例指标")
    private GuaranteeMetrics guarantee;

    @Schema(description = "杠杆 / 仓位指标")
    private LeverageMetrics leverage;

    @Schema(description = "集中度指标")
    private ConcentrationMetrics concentration;

    @Schema(description = "流动性指标")
    private LiquidityMetrics liquidity;

    @Schema(description = "持仓集中度 Top N")
    private List<PositionRisk> topPositions;

    @Schema(description = "本次使用的风控阈值")
    private RiskConfig config;


    /**
     * 单条风险
     */
    @Data
    public static class RiskWarning {

        @Schema(description = "风险编码")
        private String code;

        @Schema(description = "风险等级：WATCH / WARN / CRITICAL")
        private String level;

        @Schema(description = "指标名称")
        private String metric;

        @Schema(description = "当前值")
        private BigDecimal value;

        @Schema(description = "阈值")
        private BigDecimal threshold;

        @Schema(description = "单位（% / 元）")
        private String unit;

        @Schema(description = "风险说明")
        private String message;

        @Schema(description = "建议动作")
        private String action;
    }


    /**
     * 账户快照
     */
    @Data
    public static class AccountSnapshot {

        @Schema(description = "总资产（元）")
        private BigDecimal totalAsset;

        @Schema(description = "净资产（元）")
        private BigDecimal netAsset;

        @Schema(description = "总负债（元）")
        private BigDecimal totalLiability;

        @Schema(description = "总市值（元）")
        private BigDecimal totalMarketValue;

        @Schema(description = "总仓位（总市值 / 净资产，小数）")
        private BigDecimal totalPositionRatio;

        @Schema(description = "总仓位（%）")
        private BigDecimal totalPositionRatioPct;

        @Schema(description = "负债率（总负债 / 净资产，小数）")
        private BigDecimal liabilityRatio;

        @Schema(description = "负债率（%）")
        private BigDecimal liabilityRatioPct;

        @Schema(description = "持仓盈亏（元）")
        private BigDecimal profit;

        @Schema(description = "持仓盈亏 / 净资产（%）")
        private BigDecimal profitRatioPct;

        @Schema(description = "当日盈亏（元）")
        private BigDecimal currentProfit;

        @Schema(description = "持仓证券数量")
        private Integer positionCount;
    }


    /**
     * 担保比例
     */
    @Data
    public static class GuaranteeMetrics {

        @Schema(description = "实时担保比例（小数，1.3833 => 138.33%）")
        private BigDecimal realRate;

        @Schema(description = "维持担保比例（小数）")
        private BigDecimal maintainRate;

        @Schema(description = "关注线（2.00）")
        private BigDecimal watchLine;

        @Schema(description = "预警线（1.50）")
        private BigDecimal warningLine;

        @Schema(description = "平仓线（1.30）")
        private BigDecimal closeLine;

        @Schema(description = "所处区间：SAFE / WATCH / WARN / CRITICAL / NO_LIABILITY / UNKNOWN")
        private String zone;

        @Schema(description = "是否适用（无融资负债时不适用）")
        private Boolean applicable;

        @Schema(description = "距预警线（百分点）")
        private BigDecimal distanceToWarningPct;

        @Schema(description = "距平仓线（百分点）")
        private BigDecimal distanceToClosePct;
    }


    /**
     * 杠杆 / 仓位
     */
    @Data
    public static class LeverageMetrics {

        @Schema(description = "总仓位（总市值 / 净资产，小数）")
        private BigDecimal positionRatio;

        @Schema(description = "总仓位（%）")
        private BigDecimal positionRatioPct;

        @Schema(description = "最大允许总仓位（小数）")
        private BigDecimal maxPositionRatio;

        @Schema(description = "最大允许总仓位（%）")
        private BigDecimal maxPositionPct;

        @Schema(description = "仓位利用率（总仓位 / 最大仓位，%）")
        private BigDecimal utilizationPct;

        @Schema(description = "剩余可买仓位（占净资产，%）")
        private BigDecimal availableBuyPct;
    }


    /**
     * 集中度
     */
    @Data
    public static class ConcentrationMetrics {

        @Schema(description = "持仓证券数量")
        private Integer positionCount;

        @Schema(description = "第一大持仓代码")
        private String top1Code;

        @Schema(description = "第一大持仓名称")
        private String top1Name;

        @Schema(description = "第一大持仓市值（元）")
        private BigDecimal top1MarketValue;

        @Schema(description = "第一大持仓占持仓总市值（%）")
        private BigDecimal top1WeightPct;

        @Schema(description = "前五大持仓占持仓总市值（%）")
        private BigDecimal top5WeightPct;

        @Schema(description = "单票仓位上限（%）")
        private BigDecimal singleStockLimitPct;

        @Schema(description = "当前最大单票仓位（%）")
        private BigDecimal maxSingleStockRatioPct;
    }


    /**
     * 流动性
     */
    @Data
    public static class LiquidityMetrics {

        @Schema(description = "可用资金-担保（元）")
        private BigDecimal availableCash;

        @Schema(description = "可用保证金-可融（元）")
        private BigDecimal availableMargin;

        @Schema(description = "当前可买总资金（元）")
        private BigDecimal availableBuyCap;

        @Schema(description = "可买资金 / 净资产（%）")
        private BigDecimal availableBuyPct;

        @Schema(description = "剩余授信额度（元）")
        private BigDecimal remainingCredit;

        @Schema(description = "融资负债（元）")
        private BigDecimal financingDebt;

        @Schema(description = "融券负债（元）")
        private BigDecimal securitiesDebt;

        @Schema(description = "可买资金 / 总负债（%）")
        private BigDecimal marginCoveragePct;
    }


    /**
     * 单票风险
     */
    @Data
    public static class PositionRisk {

        @Schema(description = "证券代码")
        private String code;

        @Schema(description = "证券名称")
        private String name;

        @Schema(description = "持仓市值（元）")
        private BigDecimal marketValue;

        @Schema(description = "占持仓总市值（%）")
        private BigDecimal weightPct;

        @Schema(description = "单票仓位（占净资产，%）")
        private BigDecimal positionRatioPct;

        @Schema(description = "持仓盈亏（元）")
        private BigDecimal profit;

        @Schema(description = "持仓盈亏比例（%）")
        private BigDecimal profitRatioPct;

        @Schema(description = "风险等级：SAFE / WATCH / WARN / CRITICAL")
        private String riskLevel;

        @Schema(description = "风险说明")
        private String riskMessage;
    }


    /**
     * 当前使用的风控阈值
     */
    @Data
    public static class RiskConfig {

        @Schema(description = "是否融资账户")
        private Boolean rzAccount;

        @Schema(description = "配置的最大融资比例（小数）")
        private BigDecimal maxRzPct;

        @Schema(description = "生效的账户总仓位上限（%）")
        private BigDecimal positionLimitPct;

        @Schema(description = "单票仓位上限（%）")
        private BigDecimal singleStockLimitPct;

        @Schema(description = "担保比例关注线（小数）")
        private BigDecimal guaranteeWatchLine;

        @Schema(description = "担保比例预警线（小数）")
        private BigDecimal guaranteeWarningLine;

        @Schema(description = "担保比例平仓线（小数）")
        private BigDecimal guaranteeCloseLine;

        @Schema(description = "模型版本")
        private String modelVersion;
    }


}