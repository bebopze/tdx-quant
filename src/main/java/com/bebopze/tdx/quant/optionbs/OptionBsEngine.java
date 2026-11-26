package com.bebopze.tdx.quant.optionbs;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.optionbs.OptionBsModels.*;

/**
 * 期权 BS 回测引擎（日频、收盘价成交）
 *
 * <p>规则见 docs/期权回测/期权BS回测-企业级方案.md</p>
 *
 * @author bebopze
 * @date 2026/11/26
 */
@Slf4j
public class OptionBsEngine {

    private final OptionBsParams p;
    private final OptionBsData data;
    private final Result r;

    private final Map<String, Position> positions = new LinkedHashMap<>();
    private double cash;
    private double peakEquity;
    private double prevEquity;
    private double dayFee;
    private int tradeSeq;
    private int prevStatus;
    private int cycleId;
    private double accountTpBase;

    private boolean callSellActive, putSellActive;
    private boolean callBuyActive, putBuyActive;
    private boolean bottomRegimeActive, topRegimeActive;
    private final Map<String, Double> callSellRef = new LinkedHashMap<>();
    private final Map<String, Double> putSellRef = new LinkedHashMap<>();
    private final Map<String, Integer> callStrongStep = new LinkedHashMap<>();
    private final Map<String, Integer> callLeftStep = new LinkedHashMap<>();
    private final Map<String, Integer> putStrongStep = new LinkedHashMap<>();
    private final Map<String, Integer> putLeftStep = new LinkedHashMap<>();
    private final Set<String> callLeft2Done = new LinkedHashSet<>();
    private final Set<String> callRightDone = new LinkedHashSet<>();
    private final Set<String> putLeft2Done = new LinkedHashSet<>();
    private final Set<String> putRightDone = new LinkedHashSet<>();
    private final Map<String, Double> callBuyRef = new LinkedHashMap<>();
    private final Map<String, Double> putBuyRef = new LinkedHashMap<>();
    private final Map<String, Integer> callAddStep = new LinkedHashMap<>();
    private final Map<String, Integer> putAddStep = new LinkedHashMap<>();
    private final Map<String, Double> callBought = new LinkedHashMap<>();
    private final Map<String, Double> putBought = new LinkedHashMap<>();

    private List<Contract> callContracts = new ArrayList<>();
    private List<Contract> putContracts = new ArrayList<>();
    private String signalStatus = "IDLE";
    private Integer todaySignalType = null;

    public OptionBsEngine(OptionBsParams params, OptionBsData data) {
        this.p = params;
        this.data = data;
        this.r = new Result();
        this.r.params = params;
        this.cash = params.getInitialCapital();
        this.peakEquity = params.getInitialCapital();
        this.prevEquity = params.getInitialCapital();
        this.accountTpBase = params.getInitialCapital();
        this.r.initialCapital = params.getInitialCapital();
        this.r.taskId = -1;
    }

    public Result run() {
        long st = System.currentTimeMillis();
        if (data.benchmark != null) {
            this.prevStatus = statusBefore(data.calendar.isEmpty() ? p.getStartDate() : data.calendar.get(0));
        }
        r.actualStartDate = null;
        for (LocalDate date : data.calendar) {
            tradeSeq = 0;
            dayFee = 0;
            todaySignalType = null;

            int status = statusAt(date);
            if (status == 1 && prevStatus != 1) {
                onBottom(date, statusMeta(date));
            } else if (status == 3 && prevStatus != 3) {
                onTop(date, statusMeta(date));
            }
            prevStatus = status;

            markPrices(date);
            processRolls(date);
            settleExpiry(date);
            processSells(date);
            processBuys(date);
            markAndRecord(date);
        }

        computeMetrics();
        r.durationMs = System.currentTimeMillis() - st;
        return r;
    }


    // ---------------------------------------------------------------------------------------------------------------
    //                                              信号 / 周期
    // ---------------------------------------------------------------------------------------------------------------


    private void onBottom(LocalDate date, Signal meta) {
        if (bottomRegimeActive) {
            // 同一底周期内的 1/2/1 抖动：不重置预算、不重置参考价
            return;
        }
        bottomRegimeActive = true;
        topRegimeActive = false;
        callSellActive = false;   // 新底周期开始，停止卖出认购

        // 认沽：进入卖出阶段（底信号）
        activatePutSell(date);
        putBuyActive = false;

        // 认购：开启新建仓周期
        if (p.allowCalls()) {
            boolean canStart = callBought.isEmpty() || reenter("C", date)
                    || positions.values().stream().noneMatch(x -> "C".equals(x.contract.callPut));
            if (canStart) {
                cycleId++;
                callBuyActive = true;
                callBought.clear();
                callBuyRef.clear();
                callAddStep.clear();
                callContracts = selectContracts("C", date);
                log.info("OptionBsEngine - {} 中期底，开启认购周期 #{}，合约 {} 个", date, cycleId, callContracts.size());
            }
        }
        recordSignal(date, meta, 1);
    }

    private void onTop(LocalDate date, Signal meta) {
        if (topRegimeActive) {
            // 同一顶周期内的 3/4/3 抖动：不重置参考价
            return;
        }
        topRegimeActive = true;
        bottomRegimeActive = false;
        putSellActive = false;    // 新顶周期开始，停止卖出认沽

        // 认购：进入卖出阶段（顶信号）
        activateCallSell(date);
        callBuyActive = false;

        // 认沽：开启新建仓周期
        if (p.allowPuts()) {
            boolean canStart = putBought.isEmpty() || reenter("P", date)
                    || positions.values().stream().noneMatch(x -> "P".equals(x.contract.callPut));
            if (canStart) {
                cycleId++;
                putBuyActive = true;
                putBought.clear();
                putBuyRef.clear();
                putAddStep.clear();
                putContracts = selectContracts("P", date);
                log.info("OptionBsEngine - {} 中期顶，开启认沽周期 #{}，合约 {} 个", date, cycleId, putContracts.size());
            }
        }
        recordSignal(date, meta, 3);
    }

    private void activateCallSell(LocalDate date) {
        if (callSellActive) return;
        callSellActive = true;
        callSellRef.clear();
        callStrongStep.clear();
        callLeftStep.clear();
        callLeft2Done.clear();
        callRightDone.clear();
        for (String code : p.getUnderlyingCodes()) {
            Underlying u = data.underlyings.get(code);
            if (u != null && !Double.isNaN(u.close(date))) {
                callSellRef.put(code, u.close(date));
            }
        }
    }

    private void activatePutSell(LocalDate date) {
        if (putSellActive) return;
        putSellActive = true;
        putSellRef.clear();
        putStrongStep.clear();
        putLeftStep.clear();
        putLeft2Done.clear();
        putRightDone.clear();
        for (String code : p.getUnderlyingCodes()) {
            Underlying u = data.underlyings.get(code);
            if (u != null && !Double.isNaN(u.close(date))) {
                putSellRef.put(code, u.close(date));
            }
        }
    }

    private void recordSignal(LocalDate date, Signal meta, int type) {
        Signal s = new Signal();
        s.date = date;
        s.type = type;
        s.prevStatus = prevStatus;
        s.lowDay = meta == null ? null : meta.lowDay;
        s.highDay = meta == null ? null : meta.highDay;
        s.ma50Pct = meta == null ? null : meta.ma50Pct;
        s.cycleId = cycleId;
        r.signals.add(s);
        r.signalCount++;
        todaySignalType = type;
    }


    // ---------------------------------------------------------------------------------------------------------------
    //                                              卖出
    // ---------------------------------------------------------------------------------------------------------------


    private void processSells(LocalDate date) {
        if (callSellActive) {
            signalStatus = "TOP_SELL";
            sellSide("C", date);
        }
        if (putSellActive) {
            signalStatus = "BOTTOM_SELL";
            sellSide("P", date);
        }
        if (p.isEnableAccountTp()) {
            accountTakeProfit(date);
        }
        if (p.isEnableSingleTp()) {
            singleTakeProfit(date);
        }
        if (callBuyActive && !callSellActive) signalStatus = "BOTTOM_HOLD";
        else if (putBuyActive && !putSellActive) signalStatus = "TOP_HOLD";
    }

    private void sellSide(String side, LocalDate date) {
        for (String code : p.getUnderlyingCodes()) {
            Underlying u = data.underlyings.get(code);
            if (u == null) continue;
            double close = u.close(date);
            if (Double.isNaN(close)) continue;

            List<Position> list = positions.values().stream()
                                           .filter(x -> side.equals(x.contract.callPut) && code.equals(x.contract.underlyingCode))
                                           .collect(Collectors.toList());
            if (list.isEmpty()) continue;

            boolean isCall = "C".equals(side);

            // 1、右侧卖 - 清仓
            if (p.isEnableRight() && !(isCall ? callRightDone : putRightDone).contains(code)
                    && rightCondition(u, date, isCall)) {
                for (Position x : list) sell(x, x.qty, date, "RIGHT_CLEAR", "右侧卖·清仓", null);
                (isCall ? callRightDone : putRightDone).add(code);
                continue;
            }

            // 2、左侧卖2 (MA5/MA10)
            if (p.isEnableLeft2() && !(isCall ? callLeft2Done : putLeft2Done).contains(code)
                    && left2Condition(u, date, isCall)) {
                reduce(list, p.getLeft2SellPct(), date, "LEFT2", "左侧卖2（MA5/MA10）", null);
                (isCall ? callLeft2Done : putLeft2Done).add(code);
            }

            // 3、左侧卖1 - 每 ±5% 一档
            if (p.isEnableLeft1()) {
                Double ref = (isCall ? callSellRef : putSellRef).get(code);
                if (ref != null && ref > 0) {
                    double chg = close / ref - 1;
                    int step = isCall ? (int) Math.floor(-chg / p.getAddStepPct())
                            : (int) Math.floor(chg / p.getAddStepPct());
                    Map<String, Integer> map = isCall ? callLeftStep : putLeftStep;
                    int last = map.getOrDefault(code, 0);
                    while (step > last) {
                        reduce(list, p.getLeft1SellPct(), date, "LEFT1", "左侧卖1（±5%）",
                               -chg * 100);
                        last++;
                    }
                    map.put(code, Math.max(last, 0));
                }
            }

            // 4、强势卖出 - 每 ±5% 一档
            if (p.isEnableStrong()) {
                Double ref = (isCall ? callSellRef : putSellRef).get(code);
                if (ref != null && ref > 0) {
                    double chg = close / ref - 1;
                    int step = isCall ? (int) Math.floor(chg / p.getAddStepPct())
                            : (int) Math.floor(-chg / p.getAddStepPct());
                    Map<String, Integer> map = isCall ? callStrongStep : putStrongStep;
                    int last = map.getOrDefault(code, 0);
                    while (step > last) {
                        reduce(list, p.getStrongSellPct(), date, "STRONG_SELL", "强势卖出（±5%）",
                               chg * 100);
                        last++;
                    }
                    map.put(code, Math.max(last, 0));
                }
            }

            // 5、信号卖出 - 逐日 10%
            if (p.isEnableSignalSell()) {
                reduce(list, p.getSignalSellDailyPct(), date, "SIGNAL_SELL", "信号卖出（逐日）", null);
            }
        }
    }

    private boolean rightCondition(Underlying u, LocalDate d, boolean isCall) {
        double c = u.close(d), ma10 = u.ma(10, d), ma10p = u.maPrev(10, d);
        double ma20 = u.ma(20, d), ma20p = u.maPrev(20, d);
        double ssf = u.ssf(d), ssfp = u.ssfPrev(d);
        if (isCall) {
            // MA10空 / SSF空 / 下MA20 / MA20空
            return (c < ma10 && ma10 < ma10p)
                    || (c < ssf && ssf < ssfp)
                    || (c < ma20) || (c < ma20 && ma20 < ma20p);
        }
        // MA10多 / SSF多 / 上MA20 / MA20多
        return (c >= ma10 && ma10 >= ma10p)
                || (c >= ssf && ssf >= ssfp)
                || (c >= ma20) || (c >= ma20 && ma20 >= ma20p);
    }

    private boolean left2Condition(Underlying u, LocalDate d, boolean isCall) {
        double c = u.close(d), ma5 = u.ma(5, d), ma5p = u.maPrev(5, d);
        double ma10 = u.ma(10, d);
        if (isCall) {
            return (c < ma5) || (c < ma5 && ma5 < ma5p) || (c < ma10);
        }
        return (c >= ma5) || (c >= ma5 && ma5 >= ma5p) || (c >= ma10);
    }

    private void reduce(List<Position> list, double ratio, LocalDate date, String code, String desc, Double trigger) {
        for (Position x : list) {
            if (x.qty <= 0) continue;
            int q = Math.max(1, (int) Math.floor(x.qty * ratio));
            q = Math.min(q, x.qty);
            sell(x, q, date, code, desc, trigger);
        }
    }

    private void accountTakeProfit(LocalDate date) {
        if (positions.isEmpty()) return;
        double equity = equityAt(date);
        double step = p.getAccountTpStepPct();
        int guard = 0;
        // 每次触发后，以触发时的账户权益作为新基准，下一次再上涨 +50% 才触发
        while (equity >= accountTpBase * (1 + step) && guard++ < 20) {
            for (Position x : new ArrayList<>(positions.values())) {
                if (x.qty <= 0) continue;
                int q = Math.max(1, (int) Math.floor(x.qty * p.getAccountTpReducePct()));
                sell(x, Math.min(q, x.qty), date, "ACCOUNT_TP", "账户止盈（每 +50%）", step * 100);
            }
            equity = equityAt(date);
            accountTpBase = equity;
        }
    }

    private void singleTakeProfit(LocalDate date) {
        for (Position x : new ArrayList<>(positions.values())) {
            if (x.qty <= 0) continue;
            double price = x.lastPrice;
            if (Double.isNaN(price) || x.avgCost <= 0) continue;
            int guard = 0;
            while (x.qty > 0 && price / x.avgCost >= x.nextSingleTp && guard++ < 20) {
                int q = Math.max(1, (int) Math.floor(x.qty * p.getSingleTpReducePct()));
                sell(x, Math.min(q, x.qty), date, "SINGLE_TP", "单票止盈（每 +100%）",
                     (x.nextSingleTp - 1) * 100D);
                x.nextSingleTp++;
            }
        }
    }


    // ---------------------------------------------------------------------------------------------------------------
    //                                              买入
    // ---------------------------------------------------------------------------------------------------------------


    private void processBuys(LocalDate date) {
        if (callBuyActive) {
            double amount = capitalBase() * rule("C", date).daily();
            buySide("C", date, amount, "BUY_DAILY", "中期底-逐日建仓", null, null);
            processAddOn("C", date);
        }
        if (putBuyActive) {
            double amount = capitalBase() * rule("P", date).daily();
            buySide("P", date, amount, "BUY_DAILY", "中期顶-逐日建仓", null, null);
            processAddOn("P", date);
        }
    }

    private void processAddOn(String side, LocalDate date) {
        boolean isCall = "C".equals(side);
        RuleView rv = rule(side, date);
        Map<String, Double> refs = isCall ? callBuyRef : putBuyRef;
        Map<String, Integer> steps = isCall ? callAddStep : putAddStep;
        Map<String, Double> bought = isCall ? callBought : putBought;

        for (String code : p.getUnderlyingCodes()) {
            Underlying u = data.underlyings.get(code);
            if (u == null) continue;
            Double ref = refs.get(code);
            if (ref == null || ref <= 0) continue;
            double close = u.close(date);
            if (Double.isNaN(close)) continue;

            double chg = close / ref - 1;
            int step = isCall ? (int) Math.floor(-chg / rv.step())
                    : (int) Math.floor(chg / rv.step());
            int last = steps.getOrDefault(code, 0);
            while (step > last) {
                if (bought.getOrDefault(side, 0D) >= maxBudget(date, side)) break;
                double amount = capitalBase() * rv.add();
                String only = "TRIGGERED".equalsIgnoreCase(rv.scope()) ? code : null;
                double used = buySide(side, date, amount, "BUY_DIP",
                                      isCall ? "中期底-下跌加仓" : "中期顶-上涨加仓", chg * 100, only);
                if (used <= 0) break;
                last++;
            }
            steps.put(code, Math.max(last, 0));
        }
    }

    /**
     * 买入：按「标的等权 → 月份/档位等权」分配，整手向下取整
     */
    private double buySide(String side, LocalDate date, double amount, String reasonCode, String reasonDesc,
                           Double trigger, String onlyUnderlying) {
        if (amount <= 0) return 0;
        final double base = capitalBase();
        final RuleView rv = rule(side, date);
        final double maxBudget = maxBudget(date, side);
        final double reserve = base * rv.cash();
        double budget = maxBudget - (("C".equals(side) ? callBought : putBought).getOrDefault(side, 0D));
        if (budget <= 0) return 0;
        amount = Math.min(amount, budget);

        List<Contract> list = ("C".equals(side) ? callContracts : putContracts);
        Map<String, List<Contract>> byU = list.stream()
                                              .filter(c -> c.alive(date) && !c.startDate.isAfter(date))
                                              .filter(c -> c.expiryDate != null && ChronoUnit.DAYS.between(date, c.expiryDate) >= p.getMinBuyDteDays())
                                              .filter(c -> c.volOn(date) != 0)
                                              .filter(c -> onlyUnderlying == null || onlyUnderlying.equals(c.underlyingCode))
                                              .collect(Collectors.groupingBy(c -> c.underlyingCode, LinkedHashMap::new, Collectors.toList()));
        if (byU.isEmpty()) return 0;

        double perU = amount / byU.size();
        double usedTotal = 0;
        for (Map.Entry<String, List<Contract>> e : byU.entrySet()) {
            List<Contract> cs = e.getValue();
            double perC = perU / cs.size();
            for (Contract c : cs) {
                double price = c.closeOn(date);
                if (Double.isNaN(price) || price <= 0.001D) continue;
                double remain = maxBudget - (("C".equals(side) ? callBought : putBought).getOrDefault(side, 0D));
                double alloc = Math.min(perC, remain);
                if (alloc <= 0) break;
                int qty = (int) Math.floor(alloc / (price * c.multiplier));
                if (qty <= 0) continue;
                double cost = qty * price * c.multiplier;
                double fee = qty * p.getCommissionPerContract();
                if (cost + fee > cash - reserve) {
                    qty = (int) Math.floor((cash - reserve - fee) / (price * c.multiplier));
                    if (qty <= 0) continue;
                    cost = qty * price * c.multiplier;
                    fee = qty * p.getCommissionPerContract();
                }
                cash -= cost + fee;
                dayFee += fee;
                usedTotal += cost;

                Position pos = positions.get(c.code);
                if (pos == null) {
                    pos = new Position();
                    pos.contract = c;
                    pos.qty = qty;
                    pos.avgCost = price;
                    pos.openDate = date;
                    pos.cycleId = cycleId;
                    pos.openReason = reasonDesc;
                    pos.initialQty = qty;
                    pos.lastPrice = price;
                    positions.put(c.code, pos);
                } else {
                    double totalCost = pos.avgCost * pos.qty + price * qty;
                    pos.qty += qty;
                    pos.avgCost = totalCost / pos.qty;
                }

                if ("C".equals(side)) {
                    callBought.merge(side, cost, Double::sum);
                    callBuyRef.putIfAbsent(c.underlyingCode, data.underlyings.get(c.underlyingCode) == null
                            ? price : data.underlyings.get(c.underlyingCode).close(date));
                } else {
                    putBought.merge(side, cost, Double::sum);
                    putBuyRef.putIfAbsent(c.underlyingCode, data.underlyings.get(c.underlyingCode) == null
                            ? price : data.underlyings.get(c.underlyingCode).close(date));
                }

                Trade t = new Trade();
                t.date = date;
                t.time = LocalTime.of(15, 0);
                t.seq = ++tradeSeq;
                t.type = 1;
                t.contract = c;
                t.price = price;
                t.qty = qty;
                t.amount = cost;
                t.fee = fee;
                t.reasonCode = reasonCode;
                t.reasonDesc = reasonDesc;
                t.triggerValue = trigger;
                t.cashAfter = cash;
                t.qtyAfter = pos.qty;
                t.cycleId = cycleId;
                r.trades.add(t);
            }
        }

        if (usedTotal > 0 && r.actualStartDate == null) r.actualStartDate = date;
        return usedTotal;
    }

    private double capitalBase() {
        return "INITIAL".equalsIgnoreCase(p.getCapitalBase()) ? p.getInitialCapital() : currentEquity();
    }

    /**
     * 当前总资金 = 现金 + 持仓市值（价格已按当日收盘更新）
     */
    private double currentEquity() {
        double mv = 0;
        for (Position x : positions.values()) {
            double px = Double.isNaN(x.lastPrice) ? x.avgCost : x.lastPrice;
            mv += x.qty * px * x.contract.multiplier;
        }
        return cash + mv;
    }

    private record RuleView(double daily, double add, double step, double max,
                            double cash, String scope, boolean reenter) {
    }

    private boolean separateDirection() {
        return "SEPARATE".equalsIgnoreCase(p.getDirectionMode());
    }

    private boolean separateRegime() {
        return "SEPARATE".equalsIgnoreCase(p.getMarketRegimeMode());
    }

    private boolean isBear(LocalDate date) {
        Map.Entry<LocalDate, Integer> e = data.bullBearByDate.floorEntry(date);
        return e != null && e.getValue() != null && e.getValue() == 2;
    }

    private boolean reenter(String side, LocalDate date) {
        return rule(side, date).reenter();
    }

    /**
     * 建仓规则优先级：
     * 牛熊分开 + 多空分开 -> bullLong / bullShort / bearLong / bearShort
     * 仅牛熊分开 -> bull / bear
     * 仅多空分开 -> long / short
     * 都通用 -> common
     */
    private RuleView rule(String side, LocalDate date) {
        boolean call = "C".equals(side);
        boolean regSep = separateRegime();
        boolean dirSep = separateDirection();
        if (regSep && dirSep) {
            if (isBear(date)) {
                return call
                        ? new RuleView(p.getBearLongDailyBuyPct(), p.getBearLongAddBuyPct(), p.getBearLongAddStepPct(),
                                       p.getBearLongMaxPositionPct(), p.getBearLongCashReservePct(), p.getBearLongAddScope(), p.isBearLongReenterOnNewCycle())
                        : new RuleView(p.getBearShortDailyBuyPct(), p.getBearShortAddBuyPct(), p.getBearShortAddStepPct(),
                                       p.getBearShortMaxPositionPct(), p.getBearShortCashReservePct(), p.getBearShortAddScope(), p.isBearShortReenterOnNewCycle());
            }
            return call
                    ? new RuleView(p.getBullLongDailyBuyPct(), p.getBullLongAddBuyPct(), p.getBullLongAddStepPct(),
                                   p.getBullLongMaxPositionPct(), p.getBullLongCashReservePct(), p.getBullLongAddScope(), p.isBullLongReenterOnNewCycle())
                    : new RuleView(p.getBullShortDailyBuyPct(), p.getBullShortAddBuyPct(), p.getBullShortAddStepPct(),
                                   p.getBullShortMaxPositionPct(), p.getBullShortCashReservePct(), p.getBullShortAddScope(), p.isBullShortReenterOnNewCycle());
        }
        if (regSep) {
            return isBear(date)
                    ? new RuleView(p.getBearDailyBuyPct(), p.getBearAddBuyPct(), p.getBearAddStepPct(),
                                   p.getBearMaxPositionPct(), p.getBearCashReservePct(), p.getBearAddScope(), p.isBearReenterOnNewCycle())
                    : new RuleView(p.getBullDailyBuyPct(), p.getBullAddBuyPct(), p.getBullAddStepPct(),
                                   p.getBullMaxPositionPct(), p.getBullCashReservePct(), p.getBullAddScope(), p.isBullReenterOnNewCycle());
        }
        if (dirSep) {
            return call
                    ? new RuleView(p.getLongDailyBuyPct(), p.getLongAddBuyPct(), p.getLongAddStepPct(),
                                   p.getLongMaxPositionPct(), p.getLongCashReservePct(), p.getLongAddScope(), p.isLongReenterOnNewCycle())
                    : new RuleView(p.getShortDailyBuyPct(), p.getShortAddBuyPct(), p.getShortAddStepPct(),
                                   p.getShortMaxPositionPct(), p.getShortCashReservePct(), p.getShortAddScope(), p.isShortReenterOnNewCycle());
        }
        return new RuleView(p.getDailyBuyPct(), p.getAddBuyPct(), p.getAddStepPct(),
                            p.getMaxPositionPct(), p.getCashReservePct(), p.getAddScope(), p.isReenterOnNewCycle());
    }

    private double maxBudget(LocalDate date, String side) {
        RuleView rv = rule(side, date);
        double base = capitalBase();
        double reserve = base * rv.cash();
        return Math.min(base * rv.max(), base - reserve);
    }


    // ---------------------------------------------------------------------------------------------------------------
    //                                              卖出执行
    // ---------------------------------------------------------------------------------------------------------------


    private void sell(Position x, int qty, LocalDate date, String code, String desc, Double trigger) {
        if (qty <= 0 || x.qty <= 0) return;
        qty = Math.min(qty, x.qty);
        double price = x.lastPrice;
        if (Double.isNaN(price) || price < 0) price = x.avgCost;
        double amount = price * qty * x.contract.multiplier;
        double fee = qty * p.getCommissionPerContract();
        cash += amount - fee;
        dayFee += fee;

        x.qty -= qty;
        x.soldQty += qty;
        x.sellAmount += amount;
        x.realizedPnl += (price - x.avgCost) * qty * x.contract.multiplier - fee;
        x.maxPnlPct = Math.max(x.maxPnlPct, (price / x.avgCost - 1) * 100);
        x.minPnlPct = Math.min(x.minPnlPct, (price / x.avgCost - 1) * 100);

        Trade t = new Trade();
        t.date = date;
        t.time = LocalTime.of(15, 0);
        t.seq = ++tradeSeq;
        t.type = 2;
        t.contract = x.contract;
        t.price = price;
        t.qty = qty;
        t.amount = amount;
        t.fee = fee;
        t.reasonCode = code;
        t.reasonDesc = desc;
        t.triggerValue = trigger;
        t.cashAfter = cash;
        t.qtyAfter = x.qty;
        t.cycleId = x.cycleId;
        r.trades.add(t);

        if (x.qty <= 0) {
            CloseRecord cr = new CloseRecord();
            cr.cycleId = x.cycleId;
            cr.underlyingCode = x.contract.underlyingCode;
            cr.contract = x.contract;
            cr.openDate = x.openDate;
            cr.closeDate = date;
            cr.qty = x.soldQty;
            cr.avgCost = x.avgCost;
            cr.avgSellPrice = x.soldQty > 0 ? x.sellAmount / ((double) x.soldQty * x.contract.multiplier) : x.avgCost;
            cr.realizedPnl = x.realizedPnl;
            cr.pnlPct = (cr.avgSellPrice / x.avgCost - 1) * 100;
            cr.holdDays = (int) ChronoUnit.DAYS.between(x.openDate, date);
            cr.maxPnlPct = x.maxPnlPct;
            cr.minPnlPct = x.minPnlPct;
            cr.closeReason = code;
            cr.closeReasonDesc = desc;
            r.closeRecords.add(cr);
            positions.remove(x.contract.code);
        }
    }


    // ---------------------------------------------------------------------------------------------------------------
    //                                              到期 / 估值 / 记录
    // ---------------------------------------------------------------------------------------------------------------


    /**
     * 每日开盘前/卖出前：把持仓价格更新为当日收盘价（卖出按当日收盘成交）
     */
    private void markPrices(LocalDate date) {
        for (Position x : positions.values()) {
            double price = x.contract.closeOn(date);
            if (Double.isNaN(price)) price = x.contract.lastCloseOnOrBefore(date);
            if (Double.isNaN(price)) price = x.avgCost;
            x.lastPrice = price;
        }
    }

    /**
     * 临近到期自动换月：DTE < rollDteDays 时，卖出旧合约，买入同标的/同类型/同（或最近）行权价的
     * 下一可用月份（次月 / 当季 / 次季，要求 DTE >= minBuyDteDays）。
     */
    private void processRolls(LocalDate date) {
        for (Position old : new ArrayList<>(positions.values())) {
            LocalDate exp = old.contract.expiryDate;
            if (exp == null) continue;
            long dte = ChronoUnit.DAYS.between(date, exp);
            if (dte >= p.getRollDteDays()) continue;

            int qty = old.qty;
            if (qty <= 0) continue;

            Contract target = findRollTarget(old.contract, date);
            // 先卖出旧合约（按当日收盘；即使找不到目标也必须平掉，绝不持有到到期）
            sell(old, qty, date, "ROLL_SELL", "换月平仓（到期<" + p.getRollDteDays() + "天）", (double) dte);
            if (target == null) {
                log.info("OptionBsEngine - {} 换月无可用远月，已平仓 {} {}", date, old.contract.code, old.contract.name);
                continue;
            }
            double price = target.closeOn(date);
            if (Double.isNaN(price) || price <= 0.0005) {
                log.info("OptionBsEngine - {} 换月目标无有效价格，已平仓 {} -> {}", date, old.contract.code, target.code);
                continue;
            }
            int buyQty = qty;
            double fee = buyQty * p.getCommissionPerContract();
            double cost = buyQty * price * target.multiplier;
            if (cost + fee > cash) {
                buyQty = (int) Math.floor((cash - fee) / (price * target.multiplier));
                if (buyQty <= 0) continue;
                cost = buyQty * price * target.multiplier;
                fee = buyQty * p.getCommissionPerContract();
            }
            cash -= cost + fee;
            dayFee += fee;

            Position np = positions.get(target.code);
            if (np == null) {
                np = new Position();
                np.contract = target;
                np.qty = buyQty;
                np.avgCost = price;
                np.openDate = date;
                np.cycleId = old.cycleId;
                np.openReason = "换月建仓（次月/当季/次季）";
                np.initialQty = buyQty;
                np.lastPrice = price;
                positions.put(target.code, np);
            } else {
                double total = np.avgCost * np.qty + price * buyQty;
                np.qty += buyQty;
                np.avgCost = total / np.qty;
                np.lastPrice = price;
            }

            Trade t = new Trade();
            t.date = date;
            t.time = LocalTime.of(15, 0);
            t.seq = ++tradeSeq;
            t.type = 1;
            t.contract = target;
            t.price = price;
            t.qty = buyQty;
            t.amount = cost;
            t.fee = fee;
            t.reasonCode = "ROLL_BUY";
            t.reasonDesc = "换月建仓（次月/当季/次季）";
            t.triggerValue = (double) dte;
            t.cashAfter = cash;
            t.qtyAfter = np.qty;
            t.cycleId = old.cycleId;
            r.trades.add(t);

            replaceSelectedContract(old.contract, target);
            log.info("OptionBsEngine - {} 换月 {} -> {} (DTE {} -> {})", date,
                     old.contract.name, target.name, dte, ChronoUnit.DAYS.between(date, target.expiryDate));
        }
    }

    /**
     * 换月目标：同标的、同类型、月份大于当前；优先同行权价，其次最近行权价；要求 DTE >= minBuyDteDays
     */
    private Contract findRollTarget(Contract old, LocalDate date) {
        List<Contract> all = data.contracts.getOrDefault(old.underlyingCode, new ArrayList<>());
        List<Contract> valid = all.stream()
                                  .filter(c -> old.callPut.equals(c.callPut))
                                  .filter(c -> c.month > old.month)
                                  .filter(c -> c.expiryDate != null && ChronoUnit.DAYS.between(date, c.expiryDate) >= p.getMinBuyDteDays())
                                  .filter(c -> !c.code.equals(old.code))
                                  .filter(c -> !c.startDate.isAfter(date))
                                  .filter(c -> {
                                      double px = c.closeOn(date);
                                      return !Double.isNaN(px) && px > 0.0005;
                                  })
                                  .sorted(Comparator.comparingInt((Contract c) -> c.month)
                                                    .thenComparingDouble(c -> Math.abs(c.strike - old.strike)))
                                  .collect(Collectors.toList());
        if (valid.isEmpty()) return null;

        // 优先完全同行权价；否则在最早可用月份里取最近行权价
        int targetMonth = valid.get(0).month;
        Contract sameStrike = valid.stream()
                                   .filter(c -> c.month == targetMonth)
                                   .filter(c -> Math.abs(c.strike - old.strike) < 1e-9)
                                   .findFirst().orElse(null);
        if (sameStrike != null) return sameStrike;
        return valid.stream().filter(c -> c.month == targetMonth)
                    .min(Comparator.comparingDouble(c -> Math.abs(c.strike - old.strike))).orElse(null);
    }

    private void replaceSelectedContract(Contract old, Contract target) {
        List<Contract> list = "C".equals(old.callPut) ? callContracts : putContracts;
        boolean hasTarget = list.stream().anyMatch(c -> c.code.equals(target.code));
        list.removeIf(c -> c.code.equals(old.code));
        if (!hasTarget) list.add(target);
    }

    private void settleExpiry(LocalDate date) {
        for (Position x : new ArrayList<>(positions.values())) {
            LocalDate exp = x.contract.expiryDate;
            if (exp == null || date.isBefore(exp)) continue;
            Underlying u = data.underlyings.get(x.contract.underlyingCode);
            double ul = u == null ? x.avgCost : u.close(date);
            if (Double.isNaN(ul)) ul = x.avgCost;
            double intrinsic = "C".equals(x.contract.callPut)
                    ? Math.max(ul - x.contract.strike, 0)
                    : Math.max(x.contract.strike - ul, 0);
            double price = "INTRINSIC".equalsIgnoreCase(p.getExpiryMode()) ? intrinsic : x.lastPrice;
            if (Double.isNaN(price) || price < 0) price = 0;
            if (price == 0) price = 0.0001;
            x.lastPrice = price;
            sell(x, x.qty, date, "EXPIRY", "到期结算", null);
        }
    }

    private void markAndRecord(LocalDate date) {
        double mv = 0;
        boolean stale = false;
        for (Position x : positions.values()) {
            double price = x.contract.closeOn(date);
            if (Double.isNaN(price)) {
                price = x.contract.lastCloseOnOrBefore(date);
                stale = true;
            }
            if (Double.isNaN(price)) price = x.avgCost;
            x.lastPrice = price;
            mv += price * x.qty * x.contract.multiplier;
        }
        double equity = cash + mv;
        double dailyRet = prevEquity == 0 ? 0 : equity / prevEquity - 1;
        double cumRet = equity / p.getInitialCapital() - 1;
        peakEquity = Math.max(peakEquity, equity);
        double dd = peakEquity == 0 ? 0 : equity / peakEquity - 1;

        Equity e = new Equity();
        e.date = date;
        e.cash = cash;
        e.marketValue = mv;
        e.equity = equity;
        e.nav = equity / p.getInitialCapital();
        e.dailyReturnPct = dailyRet * 100;
        e.cumReturnPct = cumRet * 100;
        e.drawdownPct = dd * 100;
        e.positionPct = equity <= 0 ? 0 : mv / equity * 100;
        e.positionCount = positions.size();
        e.fee = dayFee;
        e.stale = stale;
        e.signalType = todaySignalType;
        e.signalStatus = signalStatus;
        if (data.benchmark != null) {
            double bc = data.benchmark.close(date);
            if (!Double.isNaN(bc)) {
                e.benchmarkClose = bc;
                double base = data.benchmark.close(data.calendar.get(0));
                if (!Double.isNaN(base) && base > 0) {
                    e.benchmarkNav = bc / base;
                    e.benchmarkReturnPct = (e.benchmarkNav - 1) * 100;
                }
            }
        }
        r.equities.add(e);
        prevEquity = equity;

        // 每日持仓快照
        for (Position x : positions.values()) {
            PositionRow row = new PositionRow();
            row.date = date;
            row.contract = x.contract;
            row.qty = x.qty;
            row.avgCost = x.avgCost;
            row.closePrice = x.lastPrice;
            row.costAmount = x.qty * x.avgCost * x.contract.multiplier;
            row.marketValue = x.qty * x.lastPrice * x.contract.multiplier;
            row.unrealizedPnl = row.marketValue - row.costAmount;
            row.pnlPct = row.costAmount == 0 ? 0 : row.unrealizedPnl / row.costAmount * 100;
            row.weightPct = mv == 0 ? 0 : row.marketValue / mv * 100;
            row.holdDays = (int) ChronoUnit.DAYS.between(x.openDate, date);
            row.openReason = x.openReason;
            row.stale = stale;
            Underlying u = data.underlyings.get(x.contract.underlyingCode);
            double ul = u == null ? Double.NaN : u.close(date);
            if (!Double.isNaN(ul)) {
                row.moneyness = "C".equals(x.contract.callPut)
                        ? (ul > x.contract.strike ? "ITM" : ul < x.contract.strike ? "OTM" : "ATM")
                        : (ul < x.contract.strike ? "ITM" : ul > x.contract.strike ? "OTM" : "ATM");
            }
            r.positionRows.add(row);
        }
    }


    // ---------------------------------------------------------------------------------------------------------------
    //                                              合约选择
    // ---------------------------------------------------------------------------------------------------------------


    private List<Contract> selectContracts(String side, LocalDate date) {
        List<Contract> out = new ArrayList<>();
        for (String code : p.getUnderlyingCodes()) {
            Underlying u = data.underlyings.get(code);
            List<Contract> all = data.contracts.getOrDefault(code, new ArrayList<>());
            if (u == null) continue;
            double spot = u.close(date);
            if (Double.isNaN(spot)) continue;

            Map<Integer, List<Contract>> byMonth = new TreeMap<>();
            int nowMonth = date.getYear() * 100 + date.getMonthValue();
            for (Contract c : all) {
                if (!side.equals(c.callPut)) continue;
                if (c.month < nowMonth) continue;
                if (!c.alive(date) || c.startDate.isAfter(date)) continue;
                if (c.expiryDate == null || ChronoUnit.DAYS.between(date, c.expiryDate) < p.getMinBuyDteDays())
                    continue;
                byMonth.computeIfAbsent(c.month, k -> new ArrayList<>()).add(c);
            }
            if (byMonth.isEmpty()) continue;

            List<Integer> months = new ArrayList<>(byMonth.keySet());
            List<Integer> chosen = chooseMonths(months, nowMonth);

            for (Integer m : chosen) {
                List<Contract> list = byMonth.get(m);
                if (list == null || list.isEmpty()) continue;
                List<Double> strikes = new ArrayList<>(new TreeSet<>(
                        list.stream().map(c -> c.strike).collect(Collectors.toList())));
                if (strikes.isEmpty()) continue;
                int atm = atmIndex(strikes, spot);
                for (String tag : p.getStrikes()) {
                    int off = switch (tag) {
                        case "ITM1" -> -1;
                        case "ATM" -> 0;
                        case "OTM1" -> 1;
                        case "OTM2" -> 2;
                        case "OTM3" -> 3;
                        default -> 0;
                    };
                    int idx = "C".equals(side) ? atm + off : atm - off;
                    if (idx < 0 || idx >= strikes.size()) continue;
                    double k = strikes.get(idx);
                    Contract match = list.stream().filter(c -> Math.abs(c.strike - k) < 1e-9)
                                         .findFirst().orElse(null);
                    if (match != null) out.add(match);
                }
            }
        }
        return out;
    }

    private List<Integer> chooseMonths(List<Integer> months, int nowMonth) {
        List<Integer> chosen = new ArrayList<>();
        if ("QUARTER".equalsIgnoreCase(p.getMonthMode())) {
            if (!months.isEmpty()) chosen.add(months.get(0));
            for (Integer m : months) {
                if (chosen.size() >= 3) break;
                if (m % 100 % 3 == 0 && !chosen.contains(m)) chosen.add(m);
            }
            for (Integer m : months) {
                if (chosen.size() >= 3) break;
                if (!chosen.contains(m)) chosen.add(m);
            }
        } else {
            for (Integer m : months) {
                if (chosen.size() >= 3) break;
                chosen.add(m);
            }
        }
        return chosen;
    }

    private int atmIndex(List<Double> strikes, double spot) {
        int best = 0;
        double diff = Double.MAX_VALUE;
        for (int i = 0; i < strikes.size(); i++) {
            double d = Math.abs(strikes.get(i) - spot);
            if (d < diff) {
                diff = d;
                best = i;
            } else if (Math.abs(d - diff) < 1e-9 && "HIGHER".equalsIgnoreCase(p.getAtmMode())) {
                best = i;
            }
        }
        return best;
    }


    // ---------------------------------------------------------------------------------------------------------------
    //                                              指标
    // ---------------------------------------------------------------------------------------------------------------


    private int statusAt(LocalDate date) {
        Map.Entry<LocalDate, Integer> e = data.statusByDate.floorEntry(date);
        return e == null ? 0 : e.getValue();
    }

    private int statusBefore(LocalDate date) {
        Map.Entry<LocalDate, Integer> e = data.statusByDate.lowerEntry(date);
        return e == null ? 0 : e.getValue();
    }

    private Signal statusMeta(LocalDate date) {
        Map.Entry<LocalDate, Signal> e = data.signalMeta.floorEntry(date);
        return e == null ? null : e.getValue();
    }

    private double equityAt(LocalDate date) {
        double mv = 0;
        for (Position x : positions.values()) {
            double price = x.contract.closeOn(date);
            if (Double.isNaN(price)) price = x.lastPrice;
            if (Double.isNaN(price)) price = x.avgCost;
            mv += price * x.qty * x.contract.multiplier;
        }
        return cash + mv;
    }

    private void computeMetrics() {
        if (r.equities.isEmpty()) {
            r.finalEquity = p.getInitialCapital();
            return;
        }
        r.actualEndDate = r.equities.get(r.equities.size() - 1).date;
        if (r.actualStartDate == null) r.actualStartDate = r.equities.get(0).date;

        Equity last = r.equities.get(r.equities.size() - 1);
        r.finalEquity = last.equity;
        r.totalReturnPct = (last.equity / p.getInitialCapital() - 1) * 100;
        double years = Math.max(0.01, ChronoUnit.DAYS.between(r.actualStartDate, r.actualEndDate) / 365.25);
        r.annualReturnPct = (Math.pow(last.equity / p.getInitialCapital(), 1 / years) - 1) * 100;

        double peak = -1e18;
        LocalDate peakDate = null;
        int ddDays = 0, maxDdDays = 0;
        for (Equity e : r.equities) {
            if (e.equity > peak) {
                peak = e.equity;
                peakDate = e.date;
                ddDays = 0;
            } else {
                ddDays++;
            }
            if (e.drawdownPct < r.maxDrawdownPct) {
                r.maxDrawdownPct = e.drawdownPct;
                r.maxDrawdownStart = peakDate;
                r.maxDrawdownEnd = e.date;
                maxDdDays = ddDays;
            }
        }
        r.maxDrawdownDays = maxDdDays;

        double[] rets = r.equities.stream().mapToDouble(x -> x.dailyReturnPct / 100).toArray();
        double mean = 0;
        for (double x : rets) mean += x;
        mean /= rets.length;
        double var = 0, down = 0;
        int downN = 0, winDays = 0;
        for (double x : rets) {
            var += (x - mean) * (x - mean);
            if (x < 0) {
                down += x * x;
                downN++;
            }
            if (x > 0) winDays++;
        }
        var /= Math.max(1, rets.length - 1);
        double std = Math.sqrt(var);
        r.volatilityPct = std * Math.sqrt(252) * 100;
        r.sharpe = std == 0 ? 0 : (mean - p.getRf() / 252) / std * Math.sqrt(252);
        r.sortino = down == 0 ? 0 : (mean - p.getRf() / 252) / Math.sqrt(down / rets.length) * Math.sqrt(252);
        r.calmar = r.maxDrawdownPct == 0 ? 0 : r.annualReturnPct / Math.abs(r.maxDrawdownPct);
        r.winRatePct = (double) winDays / rets.length * 100;
        r.avgPositionPct = r.equities.stream().mapToDouble(x -> x.positionPct).average().orElse(0);
        r.totalFee = r.equities.stream().mapToDouble(x -> x.fee).sum();
        r.totalTrades = r.trades.size();
        r.buyTrades = (int) r.trades.stream().filter(x -> x.type == 1).count();
        r.sellTrades = r.totalTrades - r.buyTrades;
        r.totalDays = r.equities.size();

        double win = 0, loss = 0;
        int winClose = 0;
        for (CloseRecord c : r.closeRecords) {
            if (c.realizedPnl >= 0) win += c.realizedPnl;
            else loss += Math.abs(c.realizedPnl);
            if (c.realizedPnl > 0) winClose++;
        }
        r.profitFactor = loss == 0 ? (win > 0 ? 99 : 0) : win / loss;

        // 月度收益
        Map<String, Double> monthly = new LinkedHashMap<>();
        LocalDate curMonth = null;
        double monthStartNav = 1;
        double prevNav = 1;
        for (Equity e : r.equities) {
            String mk = e.date.getYear() + "-" + String.format("%02d", e.date.getMonthValue());
            if (!mk.equals(curMonth == null ? null : curMonth.getYear() + "-" + String.format("%02d", curMonth.getMonthValue()))) {
                curMonth = e.date.withDayOfMonth(1);
                monthStartNav = prevNav;
            }
            monthly.put(mk, (e.nav / monthStartNav - 1) * 100);
            prevNav = e.nav;
        }
        r.monthlyReturns.putAll(monthly);

        // 卖出原因
        Map<String, Integer> reasonCount = new LinkedHashMap<>();
        for (Trade t : r.trades) {
            if (t.type == 2) reasonCount.merge(t.reasonDesc == null ? t.reasonCode : t.reasonDesc, 1, Integer::sum);
        }
        reasonCount.forEach((k, v) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reason", k);
            m.put("count", v);
            r.sellReasonStats.add(m);
        });

        // 标的贡献
        Map<String, Double> contrib = new LinkedHashMap<>();
        for (CloseRecord c : r.closeRecords) {
            contrib.merge(c.underlyingCode, c.realizedPnl, Double::sum);
        }
        double totalPnl = contrib.values().stream().mapToDouble(x -> x).sum();
        contrib.forEach((k, v) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("underlying", k);
            m.put("pnl", v);
            m.put("pct", totalPnl == 0 ? 0 : v / Math.abs(totalPnl) * 100);
            r.underlyingContribution.add(m);
        });

        // 风险指标
        Map<String, Object> rm = new LinkedHashMap<>();
        rm.put("volatilityPct", r.volatilityPct);
        rm.put("sharpe", r.sharpe);
        rm.put("sortino", r.sortino);
        rm.put("calmar", r.calmar);
        rm.put("maxDrawdownPct", r.maxDrawdownPct);
        rm.put("maxDrawdownDays", r.maxDrawdownDays);
        rm.put("winRatePct", r.winRatePct);
        rm.put("profitFactor", r.profitFactor);
        rm.put("avgPositionPct", r.avgPositionPct);
        rm.put("totalFee", r.totalFee);
        rm.put("trades", r.totalTrades);
        rm.put("signals", r.signalCount);
        rm.put("avgHoldDays", r.closeRecords.stream().mapToInt(x -> x.holdDays).average().orElse(0));
        rm.put("monthlyWinRate", monthly.isEmpty() ? 0 : monthly.values().stream().filter(x -> x > 0).count() * 100.0 / monthly.size());
        r.riskMetrics = rm;
    }
}
