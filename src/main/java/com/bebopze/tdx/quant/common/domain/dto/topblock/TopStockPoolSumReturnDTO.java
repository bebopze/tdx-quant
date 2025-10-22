package com.bebopze.tdx.quant.common.domain.dto.topblock;

import lombok.Data;

import java.time.LocalDate;


/**
 * 主线个股 列表   ->   收益率 汇总统计
 *
 * @author: bebopze
 * @date: 2025/10/21
 */
@Data
public class TopStockPoolSumReturnDTO {


    /**
     * 回测-起始日期
     */
    private LocalDate startDate;

    /**
     * 回测-结束日期
     */
    private LocalDate endDate;

    /**
     * 初始资金
     */
    private double initialCapital;

    /**
     * 结束资金
     */
    private double finalCapital;


    /**
     * 交易总笔数
     */
//    private Integer totalTrade;

    /**
     * 交易总金额
     */
//    private BigDecimal totalTradeAmount;

    /**
     * 初始净值
     */
    private double initialNav;

    /**
     * 结束净值
     */
    private double finalNav;

    /**
     * 总天数
     */
    private Integer totalDay;

    /**
     * 总收益率（%）
     */
    private double totalReturnPct;

    /**
     * 年化收益率（%）
     */
    private double annualReturnPct;

    /**
     * 胜率（%）
     */
    private double winPct;

    /**
     * 盈亏比
     */
    private double profitFactor;

    /**
     * 期望值% = (胜率×平均盈利) - (败率×平均亏损)
     */
    private double expectedValuePct;

    /**
     * 净值期望 = (1 + 平均盈利)^盈利天数 × (1 - 平均亏损)^亏损天数
     * 净值期望 = 初始净值 × (1 + 期望值) ^ 期数
     */
    private double expectedNav;


    /**
     * 最大回撤（%）
     */
//    private double maxDrawdownPct;


    /**
     * 卡玛比率 = 年化收益 / 最大回撤           （只看“最惨”那一波）
     *
     * 数值越高 说明：单位回撤 换来的 收益越多
     * 卡玛 对尾部风险更敏感，常用于：期货CTA、杠杆策略
     */
//    private double calmarRatio;


    /**
     * 盈利天数 占比  =  盈利天数 / 总天数
     */
//    private double profitDayPct;

    /**
     * 夏普比率 = 超额收益 / 总波动              （总标准差：上下波动 都算风险）
     */
//    private double sharpeRatio;


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
//    private double sortinoRatio;

    /**
     * 最大回撤-JSON详情
     */
//    private String drawdownResult;


}