package com.bebopze.tdx.quant.common.domain.dto.analysis;

import com.bebopze.tdx.quant.common.tdxfun.PerformanceMetrics;
import lombok.Data;

import java.time.LocalDate;


/**
 * 收益率 汇总统计
 *
 * @author: bebopze
 * @date: 2025/10/21
 */
@Data
public class TopPoolSumReturnDTO {


    /**
     * 回测-起始日期
     */
    private LocalDate startDate;
    /**
     * 回测-结束日期
     */
    private LocalDate endDate;


    /**
     * 交易总金额
     */
    private double totalTradeAmount;


    /**
     * 初始资金
     */
    private double initialCapital = 100_0000.0;
    /**
     * 结束资金
     */
    private double finalCapital;


    /**
     * 初始净值
     */
    private double initialNav = 1.0000;
    /**
     * 结束净值
     */
    private double finalNav;


    /**
     * 总收益率（%）
     */
    private double totalReturnPct;
    /**
     * 年化收益率（%）
     */
    private double annualReturnPct;


    // ------------------------------------------------------ 笔级 ------------------------------------------------------


    /**
     * 交易总笔数（如无交易明细，则简化为：交易总天数）
     */
    private int totalTrades;

    /**
     * 盈利总笔数（单笔收益>0）
     */
    private int winTrades;
    /**
     * 胜率 = 盈利总笔数 / 交易总笔数
     */
    private double winTradesPct;

    /**
     * 亏损总笔数（单笔收益<0）
     */
    private int lossTrades;
    /**
     * 败率 = 亏损总笔数 / 交易总笔数
     */
    private double lossTradesPct;


    /**
     * 笔均盈利（%） =  所有盈利笔总收益率 / 盈利总笔数
     *
     * 盈利交易 平均收益率（%）
     */
    private double avgWinTradesPct;
    /**
     * 笔均亏损（%）（已取绝对值） =  所有亏损笔总亏损率 / 亏损总笔数
     *
     * 亏损交易 平均亏损率（%）
     */
    private double avgLossTradesPct;
    /**
     * 盈亏比（笔级） =  笔均盈利 / 笔均亏损
     */
    private double tradeLevelProfitFactor;


    // ------------------------------------------------------ 日级 ------------------------------------------------------


    /**
     * 总天数
     */
    private Integer totalDays;

    /**
     * 盈利天数（当日收益>0）
     */
    private int winDays;
    /**
     * 盈利天数占比（%） =  盈利天数 / 总天数
     */
    private double winDaysPct;

    /**
     * 亏损天数（当日收益<0）
     */
    private int lossDays;
    /**
     * 亏损天数占比（%） = 亏损天数 / 总天数
     */
    private double lossDaysPct;


    /**
     * 日均盈利（%） =  所有盈利日总收益率 / 盈利天数
     *
     * 盈利日 平均收益率（%）
     */
    private double avgWinDailyPct;
    /**
     * 日均亏损（%） =  所有亏损日总亏损率 / 亏损天数
     *
     * 亏损日 平均亏损率（%）（已取绝对值）
     */
    private double avgLossDailyPct;

    /**
     * 盈亏比（日级） =  日均盈利 / 日均亏损
     */
    private double dailyLevelProfitFactor;


    // ------------------------------------------------------ 期望 ------------------------------------------------------


    // 想看 “每单位风险能换多少收益” → 继续用 Profit Factor（<1 就 <1，接受现实）
    // 想看 “综合到底赚不赚钱” → 直接算 期望收益 或 夏普/卡尔马 等风险调整指标


    /**
     * 日均收益期望（%） =  (胜率×平均盈利) - (败率×平均亏损)
     */
    private double expectedDailyReturnPct;

    /**
     * 净值期望 = (1 + 日均盈利)^盈利天数 × (1 - 日均亏损)^亏损天数
     */
    private double expectedNav;
    /**
     * 净值期望 = 初始净值 × (1 + 日均收益期望) ^ 期数
     */
    private double expectedNav2;


    /**
     * 日均 调仓换股率（N%）
     */
    private double avgPosReplacePct;
    /**
     * 日均 交易费率（1‰ * N%）
     */
    private double avgFeePct;
    private double expectedNav1_1; // 净值期望 - 考虑交易费率
    private double expectedNav2_2; // 净值期望 - 考虑交易费率


    // ------------------------------------------------ 最大回撤/波峰/波谷 ------------------------------------------------


    /**
     * 最大回撤（%）
     */
    private double maxDrawdownPct;
    private double peakNav;            // 净值波峰
    private LocalDate peakDate;        // 净值波峰 日期
    private double troughNav;          // 净值波谷
    private LocalDate troughDate;      // 净值波谷 日期

    private double maxNav;             // max净值
    private LocalDate maxNavDate;      // max净值 日期
    private double minNav;             // min净值
    private LocalDate minNavDate;      // min净值 日期


    // ------------------------------------------------- 风险收益指标 ----------------------------------------------------


    /**
     * 卡玛比率 = 年化收益 / 最大回撤           （只看“最惨”那一波）
     *
     * 数值越高 说明：单位回撤 换来的 收益越多
     * 卡玛 对尾部风险更敏感，常用于：期货CTA、杠杆策略
     *
     *
     * ≥ 3	优秀，回撤控制极佳
     * 1–3	良好，可接受
     * < 1	每 1% 回撤换不到 1% 年化收益，偏脆弱
     */
    private double calmarRatio;


    /**
     * 无风险利率（小数）
     */
    private double riskFreeRate;

    /**
     * 夏普比率 = 超额收益 / 总波动              （总标准差：上下波动 都算风险）
     *
     *
     * 表示每承担一单位总风险（标准差），能获得多少单位的超额回报。
     * 数值越高，说明在承担相同风险的情况下，获得了更高的超额回报，或者在获得相同超额回报的情况下，承担了更低的风险。
     * 负的夏普比率表示投资组合的回报率低于无风险利率。
     */
    private double sharpeRatio;


    /**
     * Sortino比率 = (年化收益率 - 目标收益率) / 下行标准差              （下行标准差：只罚“跌”不罚“涨”）
     *
     *
     * Sortino 比率（Sortino Ratio）是 夏普比率 的“下行升级版”：
     * 它只把 低于目标收益（通常取 0 或无风险利率）的波动 视为风险，从而避免“上涨波动”被惩罚。
     *
     *
     * Sortino 更贴合投资者真实感受 —— 上涨再猛也不该被当成“风险”
     */
    private double sortinoRatio;
}