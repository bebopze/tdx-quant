package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.constant.AccountConst;
import com.bebopze.tdx.quant.common.domain.dto.trade.AccountWarnDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.AccountWarnDTO.AccountSnapshot;
import com.bebopze.tdx.quant.common.domain.dto.trade.AccountWarnDTO.ConcentrationMetrics;
import com.bebopze.tdx.quant.common.domain.dto.trade.AccountWarnDTO.GuaranteeMetrics;
import com.bebopze.tdx.quant.common.domain.dto.trade.AccountWarnDTO.LeverageMetrics;
import com.bebopze.tdx.quant.common.domain.dto.trade.AccountWarnDTO.LiquidityMetrics;
import com.bebopze.tdx.quant.common.domain.dto.trade.AccountWarnDTO.PositionRisk;
import com.bebopze.tdx.quant.common.domain.dto.trade.AccountWarnDTO.RiskConfig;
import com.bebopze.tdx.quant.common.domain.dto.trade.AccountWarnDTO.RiskWarning;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * 股票融资账户 - 风控预警计算
 *
 * 设计原则：
 * 1. 只使用券商返回的当前快照，不伪造行情/净值/历史回撤；
 * 2. 规则确定性、可解释，每个命中项都返回 code / 指标 / 当前值 / 阈值 / 建议；
 * 3. 任何单一硬约束（平仓线、仓位超限、单票超限）都可以直接把整体等级抬到 CRITICAL；
 * 4. 数据缺失时返回 dataComplete=false，并给出 DATA_INCOMPLETE，禁止据此加仓。
 *
 * 阈值：
 * - 担保比例：>=200% SAFE；150%~200% WATCH；130%~150% WARN；<130% CRITICAL；
 * - 总仓位：总市值 / 净资产，超过账户上限 CRITICAL；达到上限 90% 预警、80% 关注；
 * - 单票仓位：超过单票上限 CRITICAL/WARN 分级；达到上限 80% 关注；
 * - 流动性：可买资金 / 净资产 <5% 预警、<10% 关注；
 * - 亏损：持仓盈亏 / 净资产 <-50% 预警、<-20% 关注。
 *
 * @author: bebopze
 * @date: 2026/11/15
 */
public final class AccountRiskCalculator {


    public static final String MODEL_VERSION = "ACCOUNT_RISK_V1";

    public static final double GUARANTEE_WATCH = 2.00;
    public static final double GUARANTEE_WARNING = 1.50;
    public static final double GUARANTEE_CLOSE = 1.30;

    public static final double POSITION_NEAR_RATIO = 0.90;
    public static final double POSITION_WATCH_RATIO = 0.80;
    public static final double STOCK_NEAR_RATIO = 0.80;

    public static final double LIQUIDITY_WARNING_PCT = 5.0;
    public static final double LIQUIDITY_WATCH_PCT = 10.0;

    public static final double LOSS_WARNING_PCT = -50.0;
    public static final double LOSS_WATCH_PCT = -20.0;

    private static final String SAFE = "SAFE";
    private static final String WATCH = "WATCH";
    private static final String WARN = "WARN";
    private static final String CRITICAL = "CRITICAL";


    private AccountRiskCalculator() {
    }


    /**
     * @param resp             券商融资账户快照（queryCreditNewPosV2）
     * @param rzAccount        是否融资账户
     * @param maxRzPct         最大融资比例（1~2.5，小数）
     * @param posLimitPct      账户总仓位上限（%）
     * @param stockPosLimitPct 单票仓位上限（%）
     */
    public static AccountWarnDTO calc(QueryCreditNewPosResp resp,
                                      boolean rzAccount,
                                      double maxRzPct,
                                      double posLimitPct,
                                      double stockPosLimitPct) {

        AccountWarnDTO dto = initDTO();

        if (resp == null) {
            dto.getDataIssues().add("券商融资账户接口返回空数据");
            addWarning(dto, "DATA_UNAVAILABLE", CRITICAL, "数据完整性",
                       null, null, null,
                       "账户快照为空，无法计算风控指标",
                       "检查券商登录会话后重试；数据缺失期间禁止新增融资买入。");
            finish(dto);
            return dto;
        }


        // ------------------------------------------------------------------ 配置

        if (!rzAccount) {
            maxRzPct = 1.0;
            posLimitPct = 100.0;
        }
        if (!(maxRzPct > 0) || maxRzPct > 5) {
            maxRzPct = rzAccount ? AccountConst.MAX_RZ_RATE : 1.0;
        }
        if (!(posLimitPct > 0)) {
            posLimitPct = maxRzPct * 100.0;
        }
        if (!(stockPosLimitPct > 0) || stockPosLimitPct > 100) {
            stockPosLimitPct = AccountConst.STOCK__POS_PCT_LIMIT;
        }

        // 账户总仓位上限 = min(配置的融资比例, 配置的持仓上限)
        double maxPositionRatio = Math.min(maxRzPct, posLimitPct / 100.0);
        if (!(maxPositionRatio > 0)) {
            maxPositionRatio = 1.0;
        }

        RiskConfig config = new RiskConfig();
        config.setRzAccount(rzAccount);
        config.setMaxRzPct(ratio(maxRzPct));
        config.setPositionLimitPct(pct(maxPositionRatio * 100.0));
        config.setSingleStockLimitPct(pct(stockPosLimitPct));
        config.setGuaranteeWatchLine(ratio(GUARANTEE_WATCH));
        config.setGuaranteeWarningLine(ratio(GUARANTEE_WARNING));
        config.setGuaranteeCloseLine(ratio(GUARANTEE_CLOSE));
        config.setModelVersion(MODEL_VERSION);
        dto.setConfig(config);


        // ------------------------------------------------------------------ 原始快照

        double totalAsset = d(resp.getTotalasset());
        double netAsset = d(resp.getNetasset());
        double totalLiability = d(resp.getTotalliability());
        double totalMarketValue = d(resp.getTotalmkval());
        double availableCash = d(resp.getAvalmoney());
        double availableMargin = d(resp.getMarginavl());
        double remainingCredit = d(resp.getAcreditavl());
        double financingDebt = d(resp.getFtotaldebts());
        double securitiesDebt = d(resp.getDtotaldebts());
        double profit = d(resp.getProfit());
        double currentProfit = d(resp.getCurprofit());

        List<CcStockInfo> stocks = resp.getStocks() == null
                ? Collections.emptyList()
                : new ArrayList<>(resp.getStocks());

        double positionMarketSum = 0.0;
        for (CcStockInfo stock : stocks) {
            if (stock != null) {
                positionMarketSum += stockMarketValue(stock);
            }
        }
        if (totalMarketValue <= 0 && positionMarketSum > 0) {
            totalMarketValue = positionMarketSum;
            dto.getDataIssues().add("接口总市值为空，已使用持仓市值合计");
        }

        double positionRatio = d(resp.getPosratio());
        if (positionRatio <= 0 && netAsset > 0) {
            positionRatio = totalMarketValue / netAsset;
        }

        Double realRate = firstPositive(resp.getRealrate(), resp.getMarginrates());
        Double maintainRate = positive(resp.getMarginrates());

        double availableBuyCap = availableCash + availableMargin;
        double availableBuyPct = netAsset > 0 ? availableBuyCap / netAsset * 100.0 : 0.0;
        Double marginCoveragePct = totalLiability > 0 ? availableBuyCap / totalLiability * 100.0 : null;

        double liabilityRatio = netAsset > 0 ? totalLiability / netAsset : 0.0;
        double profitRatioPct = netAsset > 0 ? profit / netAsset * 100.0 : 0.0;


        // ------------------------------------------------------------------ 账户快照

        AccountSnapshot account = new AccountSnapshot();
        account.setTotalAsset(money(totalAsset));
        account.setNetAsset(money(netAsset));
        account.setTotalLiability(money(totalLiability));
        account.setTotalMarketValue(money(totalMarketValue));
        account.setTotalPositionRatio(ratio(positionRatio));
        account.setTotalPositionRatioPct(pct(positionRatio * 100.0));
        account.setLiabilityRatio(ratio(liabilityRatio));
        account.setLiabilityRatioPct(pct(liabilityRatio * 100.0));
        account.setProfit(money(profit));
        account.setProfitRatioPct(pct(profitRatioPct));
        account.setCurrentProfit(money(currentProfit));
        account.setPositionCount(stocks.size());
        dto.setAccount(account);


        // ------------------------------------------------------------------ 担保比例

        GuaranteeMetrics guarantee = new GuaranteeMetrics();
        guarantee.setRealRate(realRate == null ? null : ratio(realRate));
        guarantee.setMaintainRate(maintainRate == null ? null : ratio(maintainRate));
        guarantee.setWatchLine(ratio(GUARANTEE_WATCH));
        guarantee.setWarningLine(ratio(GUARANTEE_WARNING));
        guarantee.setCloseLine(ratio(GUARANTEE_CLOSE));
        guarantee.setApplicable(totalLiability > 0);

        if (totalLiability <= 0) {
            guarantee.setZone("NO_LIABILITY");
        } else if (realRate == null) {
            guarantee.setZone("UNKNOWN");
        } else if (realRate >= GUARANTEE_WATCH) {
            guarantee.setZone(SAFE);
        } else if (realRate >= GUARANTEE_WARNING) {
            guarantee.setZone(WATCH);
        } else if (realRate >= GUARANTEE_CLOSE) {
            guarantee.setZone(WARN);
        } else {
            guarantee.setZone(CRITICAL);
        }

        if (realRate != null) {
            guarantee.setDistanceToWarningPct(pct((realRate - GUARANTEE_WARNING) * 100.0));
            guarantee.setDistanceToClosePct(pct((realRate - GUARANTEE_CLOSE) * 100.0));
        }
        dto.setGuarantee(guarantee);


        // ------------------------------------------------------------------ 数据质量（硬校验）

        if (!(totalAsset > 0)) {
            dto.getDataIssues().add("总资产为空或非正");
        }
        if (totalLiability < 0) {
            dto.getDataIssues().add("总负债为负");
        }
        if (totalMarketValue < 0) {
            dto.getDataIssues().add("总市值为负");
        }
        if (stocks.isEmpty() && totalMarketValue > 0) {
            dto.getDataIssues().add("持仓列表为空但总市值大于0");
        }
        if (totalLiability > 0 && realRate == null) {
            dto.getDataIssues().add("实时/维持担保比例均缺失");
        }

        if (!(netAsset > 0)) {
            dto.getDataIssues().add("净资产为空或非正，无法计算担保比例与仓位");
            addWarning(dto, "ACCOUNT_NET_ASSET_INVALID", CRITICAL, "净资产",
                       null, null, "元",
                       "净资产为空或非正，账户风险指标不可计算",
                       "立即核对券商账户资产与负债；暂停所有融资买入。");
            finish(dto);
            return dto;
        }


        // ------------------------------------------------------------------ 担保比例规则

        if (totalLiability > 0) {
            if (realRate == null) {
                addWarning(dto, "GUARANTEE_UNKNOWN", WARN, "实时担保比例",
                           null, pct(GUARANTEE_WARNING * 100.0), "%",
                           "券商返回的实时/维持担保比例均缺失，无法判断追保风险",
                           "核对券商接口数据；数据恢复前禁止新增融资买入。");
            } else {
                double realPct = realRate * 100.0;

                if (realRate < GUARANTEE_CLOSE) {
                    addWarning(dto, "GUARANTEE_BELOW_CLOSE", CRITICAL, "实时担保比例",
                               pct(realPct), pct(GUARANTEE_CLOSE * 100.0), "%",
                               String.format(Locale.ROOT,
                                             "实时担保比例 %.2f%% 低于平仓线130%%，低 %.2f 个百分点，存在强制平仓风险",
                                             realPct, (GUARANTEE_CLOSE * 100.0) - realPct),
                               "立即补充担保品或降低负债；暂停新增融资买入，优先处理高负债/高集中度持仓。");
                } else if (realRate < GUARANTEE_WARNING) {
                    addWarning(dto, "GUARANTEE_BELOW_WARNING", WARN, "实时担保比例",
                               pct(realPct), pct(GUARANTEE_WARNING * 100.0), "%",
                               String.format(Locale.ROOT,
                                             "实时担保比例 %.2f%% 处于130%%~150%%警戒区间，距平仓线 %.2f 个百分点",
                                             realPct, realPct - (GUARANTEE_CLOSE * 100.0)),
                               "尽快补充担保品或降低仓位，避免跌破130%平仓线；暂停新增融资。");
                } else if (realRate < GUARANTEE_WATCH) {
                    addWarning(dto, "GUARANTEE_BELOW_WATCH", WATCH, "实时担保比例",
                               pct(realPct), pct(GUARANTEE_WATCH * 100.0), "%",
                               String.format(Locale.ROOT,
                                             "实时担保比例 %.2f%% 处于150%%~200%%关注区间，距预警线 %.2f 个百分点",
                                             realPct, realPct - (GUARANTEE_WARNING * 100.0)),
                               "控制新增仓位，关注担保比例变化。");
                }
            }
        }


        // ------------------------------------------------------------------ 杠杆 / 仓位

        double utilization = maxPositionRatio > 0 ? positionRatio / maxPositionRatio : 0.0;
        LeverageMetrics leverage = new LeverageMetrics();
        leverage.setPositionRatio(ratio(positionRatio));
        leverage.setPositionRatioPct(pct(positionRatio * 100.0));
        leverage.setMaxPositionRatio(ratio(maxPositionRatio));
        leverage.setMaxPositionPct(pct(maxPositionRatio * 100.0));
        leverage.setUtilizationPct(pct(utilization * 100.0));
        leverage.setAvailableBuyPct(pct(availableBuyPct));
        dto.setLeverage(leverage);

        if (positionRatio > maxPositionRatio) {
            addWarning(dto, "LEVERAGE_OVER_LIMIT", CRITICAL, "总仓位",
                       pct(positionRatio * 100.0), pct(maxPositionRatio * 100.0), "%",
                       String.format(Locale.ROOT,
                                     "总仓位 %.2f%% 超过账户上限 %.2f%%，仓位利用率 %.2f%%",
                                     positionRatio * 100.0, maxPositionRatio * 100.0, utilization * 100.0),
                       "降低总仓位至上限以内，停止新增融资买入。");
        } else if (utilization >= POSITION_NEAR_RATIO) {
            addWarning(dto, "LEVERAGE_NEAR_LIMIT", WARN, "总仓位",
                       pct(positionRatio * 100.0), pct(maxPositionRatio * 100.0), "%",
                       String.format(Locale.ROOT,
                                     "总仓位 %.2f%% 已达到账户上限的 %.2f%%，仓位缓冲不足",
                                     positionRatio * 100.0, utilization * 100.0),
                       "暂停新增融资买入，预留仓位缓冲。");
        } else if (utilization >= POSITION_WATCH_RATIO) {
            addWarning(dto, "LEVERAGE_WATCH", WATCH, "总仓位",
                       pct(positionRatio * 100.0), pct(maxPositionRatio * 100.0), "%",
                       String.format(Locale.ROOT,
                                     "总仓位 %.2f%% 已达到账户上限的 %.2f%%，进入关注区间",
                                     positionRatio * 100.0, utilization * 100.0),
                       "控制新增仓位，关注仓位利用率。");
        }


        // ------------------------------------------------------------------ 集中度

        double singleStockLimitRatio = stockPosLimitPct / 100.0;
        List<PositionRisk> allPositions = new ArrayList<>();
        double maxSingleStockRatioPct = 0.0;
        PositionRisk maxSingleStock = null;
        String maxSingleStockLevel = null;
        String maxSingleStockMessage = null;

        for (CcStockInfo stock : stocks) {
            if (stock == null) {
                continue;
            }
            double marketValue = stockMarketValue(stock);
            double weightPct = totalMarketValue > 0 ? marketValue / totalMarketValue * 100.0 : 0.0;
            double stockRatio = stock.getPosratio() != null && stock.getPosratio() > 0
                    ? stock.getPosratio()
                    : (netAsset > 0 ? marketValue / netAsset : 0.0);
            double stockRatioPct = stockRatio * 100.0;

            String level = SAFE;
            String riskMessage = "";
            if (stockRatio > singleStockLimitRatio * 1.5) {
                level = CRITICAL;
                riskMessage = String.format(Locale.ROOT,
                                            "单票仓位 %.2f%% 超过单票上限 %.2f%% 的 1.5 倍", stockRatioPct, stockPosLimitPct);
            } else if (stockRatio > singleStockLimitRatio) {
                level = WARN;
                riskMessage = String.format(Locale.ROOT,
                                            "单票仓位 %.2f%% 超过单票上限 %.2f%%", stockRatioPct, stockPosLimitPct);
            } else if (stockRatio > singleStockLimitRatio * STOCK_NEAR_RATIO) {
                level = WATCH;
                riskMessage = String.format(Locale.ROOT,
                                            "单票仓位 %.2f%% 达到单票上限 %.2f%% 的80%%以上", stockRatioPct, stockPosLimitPct);
            }

            PositionRisk positionRisk = new PositionRisk();
            positionRisk.setCode(stock.getStkcode());
            positionRisk.setName(stock.getStkname());
            positionRisk.setMarketValue(money(marketValue));
            positionRisk.setWeightPct(pct(weightPct));
            positionRisk.setPositionRatioPct(pct(stockRatioPct));
            positionRisk.setProfit(money(stock.getProfit() == null ? 0.0 : stock.getProfit()));
            positionRisk.setProfitRatioPct(pct(stock.getProfitratio() == null ? 0.0 : stock.getProfitratio() * 100.0));
            positionRisk.setRiskLevel(level);
            positionRisk.setRiskMessage(riskMessage);
            allPositions.add(positionRisk);

            if (stockRatioPct > maxSingleStockRatioPct) {
                maxSingleStockRatioPct = stockRatioPct;
                maxSingleStock = positionRisk;
                maxSingleStockLevel = level;
                maxSingleStockMessage = riskMessage;
            }
        }

        allPositions.sort(Comparator.comparing(PositionRisk::getMarketValue, Comparator.nullsLast(Comparator.reverseOrder())));
        dto.setTopPositions(allPositions.size() <= 10
                                    ? allPositions
                                    : new ArrayList<>(allPositions.subList(0, 10)));

        double top1WeightPct = allPositions.isEmpty() ? 0.0 : num(allPositions.get(0).getWeightPct());
        double top5WeightPct = 0.0;
        for (int i = 0; i < Math.min(5, allPositions.size()); i++) {
            top5WeightPct += num(allPositions.get(i).getWeightPct());
        }

        ConcentrationMetrics concentration = new ConcentrationMetrics();
        concentration.setPositionCount(stocks.size());
        concentration.setTop1Code(allPositions.isEmpty() ? null : allPositions.get(0).getCode());
        concentration.setTop1Name(allPositions.isEmpty() ? null : allPositions.get(0).getName());
        concentration.setTop1MarketValue(allPositions.isEmpty() ? money(0) : allPositions.get(0).getMarketValue());
        concentration.setTop1WeightPct(pct(top1WeightPct));
        concentration.setTop5WeightPct(pct(top5WeightPct));
        concentration.setSingleStockLimitPct(pct(stockPosLimitPct));
        concentration.setMaxSingleStockRatioPct(pct(maxSingleStockRatioPct));
        dto.setConcentration(concentration);

        if (maxSingleStock != null && maxSingleStockLevel != null && !SAFE.equals(maxSingleStockLevel)) {
            String code = CRITICAL.equals(maxSingleStockLevel) || WARN.equals(maxSingleStockLevel)
                    ? "SINGLE_STOCK_OVER_LIMIT"
                    : "SINGLE_STOCK_NEAR_LIMIT";
            addWarning(dto, code, maxSingleStockLevel, "单票仓位",
                       pct(maxSingleStockRatioPct), pct(stockPosLimitPct), "%",
                       String.format(Locale.ROOT, "单票 %s(%s) %s", maxSingleStock.getCode(), maxSingleStock.getName(), maxSingleStockMessage),
                       CRITICAL.equals(maxSingleStockLevel)
                               ? "立即降低该单票仓位至上限以内，停止加仓。"
                               : "降低单票集中度至上限以内。");
        }

        if (top1WeightPct > 50.0 || top5WeightPct > 80.0) {
            addWarning(dto, "CONCENTRATION_HIGH", WATCH, "持仓集中度",
                       pct(Math.max(top1WeightPct, top5WeightPct)),
                       pct(top1WeightPct > 50.0 ? 50.0 : 80.0), "%",
                       String.format(Locale.ROOT,
                                     "持仓集中度偏高：第一大持仓占 %.2f%%，前五大持仓占 %.2f%%",
                                     top1WeightPct, top5WeightPct),
                       "分散持仓，降低单一标的/组合集中度。");
        }


        // ------------------------------------------------------------------ 流动性

        LiquidityMetrics liquidity = new LiquidityMetrics();
        liquidity.setAvailableCash(money(availableCash));
        liquidity.setAvailableMargin(money(availableMargin));
        liquidity.setAvailableBuyCap(money(availableBuyCap));
        liquidity.setAvailableBuyPct(pct(availableBuyPct));
        liquidity.setRemainingCredit(money(remainingCredit));
        liquidity.setFinancingDebt(money(financingDebt));
        liquidity.setSecuritiesDebt(money(securitiesDebt));
        liquidity.setMarginCoveragePct(marginCoveragePct == null ? null : pct(marginCoveragePct));
        dto.setLiquidity(liquidity);

        if (availableBuyPct < 0) {
            addWarning(dto, "LIQUIDITY_NEGATIVE", CRITICAL, "可用买入资金",
                       pct(availableBuyPct), pct(0), "%",
                       String.format(Locale.ROOT, "可用资金+可用保证金为负（%.2f%% 净资产），账户无缓冲", availableBuyPct),
                       "立即补充资金或降低仓位，避免被动平仓。");
        } else if (availableBuyPct < LIQUIDITY_WARNING_PCT) {
            addWarning(dto, "LIQUIDITY_WARNING", WARN, "可用买入资金",
                       pct(availableBuyPct), pct(LIQUIDITY_WARNING_PCT), "%",
                       String.format(Locale.ROOT, "可用买入资金仅占净资产 %.2f%%，低于5%%预警线", availableBuyPct),
                       "保留现金/保证金缓冲，降低仓位或补充资金。");
        } else if (availableBuyPct < LIQUIDITY_WATCH_PCT) {
            addWarning(dto, "LIQUIDITY_WATCH", WATCH, "可用买入资金",
                       pct(availableBuyPct), pct(LIQUIDITY_WATCH_PCT), "%",
                       String.format(Locale.ROOT, "可用买入资金占净资产 %.2f%%，低于10%%关注线", availableBuyPct),
                       "控制新增仓位，保留资金缓冲。");
        }

        // 可用资金/保证金 对总负债的覆盖（防止“占净资产尚可、但相对负债很薄”）
        if (marginCoveragePct != null) {
            if (marginCoveragePct < 5.0) {
                addWarning(dto, "LIQUIDITY_COVERAGE_LOW", WARN, "可买资金/总负债",
                           pct(marginCoveragePct), pct(5.0), "%",
                           String.format(Locale.ROOT, "可买资金/总负债仅 %.2f%%，低于5%%覆盖线", marginCoveragePct),
                           "保留现金/保证金缓冲，降低负债或补充资金。");
            } else if (marginCoveragePct < 10.0) {
                addWarning(dto, "LIQUIDITY_COVERAGE_WATCH", WATCH, "可买资金/总负债",
                           pct(marginCoveragePct), pct(10.0), "%",
                           String.format(Locale.ROOT, "可买资金/总负债 %.2f%%，低于10%%覆盖关注线", marginCoveragePct),
                           "关注负债与可用资金匹配度，避免满仓。");
            }
        }


        // ------------------------------------------------------------------ 亏损

        if (profitRatioPct < -100.0) {
            addWarning(dto, "LOSS_OVER_100", CRITICAL, "持仓盈亏/净资产",
                       pct(profitRatioPct), pct(-100.0), "%",
                       String.format(Locale.ROOT, "持仓亏损 %.2f%% 净资产，亏损超过净资产", profitRatioPct),
                       "立即降低风险敞口并复核追保/平仓风险，暂停新增融资买入。");
        } else if (profitRatioPct < LOSS_WARNING_PCT) {
            addWarning(dto, "LOSS_OVER_50", WARN, "持仓盈亏/净资产",
                       pct(profitRatioPct), pct(LOSS_WARNING_PCT), "%",
                       String.format(Locale.ROOT, "持仓亏损 %.2f%% 净资产，超过50%%预警线", profitRatioPct),
                       "复核持仓与止损纪律，控制回撤。");
        } else if (profitRatioPct < LOSS_WATCH_PCT) {
            addWarning(dto, "LOSS_OVER_20", WATCH, "持仓盈亏/净资产",
                       pct(profitRatioPct), pct(LOSS_WATCH_PCT), "%",
                       String.format(Locale.ROOT, "持仓亏损 %.2f%% 净资产，超过20%%关注线", profitRatioPct),
                       "关注持仓风险，复核止损计划。");
        }


        // ------------------------------------------------------------------ 数据完整性

        if (!dto.getDataIssues().isEmpty()) {
            addWarning(dto, "DATA_INCOMPLETE", WARN, "数据完整性",
                       null, null, null,
                       "券商返回数据存在缺失/异常：" + String.join("；", dto.getDataIssues()),
                       "核对券商接口返回后重试；数据缺失期间禁止新增融资买入。");
        }

        finish(dto);
        return dto;
    }


    // -----------------------------------------------------------------------------------------------------------------


    private static AccountWarnDTO initDTO() {
        AccountWarnDTO dto = new AccountWarnDTO();
        dto.setAsOf(LocalDateTime.now());
        dto.setSource("EastMoney MarginSearch/queryCreditNewPosV2");
        dto.setModelVersion(MODEL_VERSION);
        dto.setDataComplete(Boolean.TRUE);
        dto.setDataIssues(new ArrayList<>());
        dto.setWarnings(new ArrayList<>());
        dto.setActions(new ArrayList<>());
        dto.setTopPositions(new ArrayList<>());
        dto.setAccount(new AccountSnapshot());
        dto.setGuarantee(new GuaranteeMetrics());
        dto.setLeverage(new LeverageMetrics());
        dto.setConcentration(new ConcentrationMetrics());
        dto.setLiquidity(new LiquidityMetrics());
        return dto;
    }

    private static void finish(AccountWarnDTO dto) {
        List<RiskWarning> warnings = dto.getWarnings();
        dto.setDataComplete(dto.getDataIssues().isEmpty());

        String level = SAFE;
        for (RiskWarning warning : warnings) {
            if (rank(warning.getLevel()) > rank(level)) {
                level = warning.getLevel();
            }
        }
        dto.setRiskLevel(level);
        dto.setRiskLevelLabel(label(level));

        int score = baseScore(level);
        if (warnings.size() > 1) {
            score = Math.min(100, score + Math.min(15, (warnings.size() - 1) * 3));
        }
        dto.setRiskScore(score);

        Set<String> actions = new LinkedHashSet<>();
        if (warnings.isEmpty()) {
            String summary = Boolean.FALSE.equals(dto.getGuarantee().getApplicable())
                    ? "无融资负债，担保比例不适用；账户风控指标正常。"
                    : "账户风控指标正常。";
            dto.setRiskSummary(summary);
            actions.add("保持当前仓位与担保比例，定期复核。");
        } else {
            RiskWarning top = warnings.get(0);
            for (RiskWarning warning : warnings) {
                if (rank(warning.getLevel()) > rank(top.getLevel())) {
                    top = warning;
                }
                if (warning.getAction() != null && !warning.getAction().isEmpty()) {
                    actions.add(warning.getAction());
                }
            }
            dto.setRiskSummary(top.getMessage() + (warnings.size() > 1
                    ? String.format(Locale.ROOT, "（共 %d 项预警）", warnings.size())
                    : ""));
        }
        dto.setActions(new ArrayList<>(actions));
    }

    private static void addWarning(AccountWarnDTO dto,
                                   String code,
                                   String level,
                                   String metric,
                                   BigDecimal value,
                                   BigDecimal threshold,
                                   String unit,
                                   String message,
                                   String action) {
        RiskWarning warning = new RiskWarning();
        warning.setCode(code);
        warning.setLevel(level);
        warning.setMetric(metric);
        warning.setValue(value);
        warning.setThreshold(threshold);
        warning.setUnit(unit);
        warning.setMessage(message);
        warning.setAction(action);
        dto.getWarnings().add(warning);
    }


    private static int rank(String level) {
        if (CRITICAL.equals(level)) return 3;
        if (WARN.equals(level)) return 2;
        if (WATCH.equals(level)) return 1;
        return 0;
    }

    private static int baseScore(String level) {
        if (CRITICAL.equals(level)) return 100;
        if (WARN.equals(level)) return 60;
        if (WATCH.equals(level)) return 30;
        return 0;
    }

    private static String label(String level) {
        if (CRITICAL.equals(level)) return "严重";
        if (WARN.equals(level)) return "预警";
        if (WATCH.equals(level)) return "关注";
        return "正常";
    }

    private static double stockMarketValue(CcStockInfo stock) {
        return stock == null || stock.getMktval() == null ? 0.0 : stock.getMktval();
    }

    private static double d(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static Double positive(BigDecimal value) {
        return value != null && value.doubleValue() > 0 ? value.doubleValue() : null;
    }

    private static Double firstPositive(BigDecimal first, BigDecimal second) {
        Double value = positive(first);
        return value != null ? value : positive(second);
    }

    private static double num(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal ratio(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal pct(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }


}