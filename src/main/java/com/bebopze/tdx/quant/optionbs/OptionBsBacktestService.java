package com.bebopze.tdx.quant.optionbs;

import com.bebopze.tdx.quant.dal.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;


/**
 * 期权 BS 回测 - 应用服务（创建 / 异步执行 / 查询 / 导出）
 *
 * @author bebopze
 * @date 2026/11/26
 */
@Slf4j
@Service
public class OptionBsBacktestService {


    @Autowired
    private OptionBsRepository repo;

    @Autowired
    private QaMarketMidCycleMapper signalMapper;

    @Autowired
    private BaseOptionMapper optionMapper;

    @Autowired
    private BaseOptionUnderlyingMapper underlyingMapper;


    @Autowired
    @Qualifier("optionBsExecutor")
    private ExecutorService executor;

    private final Map<Long, Boolean> cancelFlags = new ConcurrentHashMap<>();
    private final Map<Long, Integer> progressMap = new ConcurrentHashMap<>();


    // =================================================================================================================
    //                                                  创建 / 执行
    // =================================================================================================================


    public Map<String, Object> submit(OptionBsParams p) {
        validate(p);
        String taskNo = repo.nextTaskNo();
        long id = repo.createTask(p, taskNo);
        cancelFlags.put(id, false);
        progressMap.put(id, 0);
        executor.submit(() -> runTask(id, p, taskNo));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("taskId", id);
        res.put("taskNo", taskNo);
        res.put("status", 0);
        return res;
    }

    private void validate(OptionBsParams p) {
        if (p.getTaskName() == null || p.getTaskName().isBlank())
            throw new IllegalArgumentException("任务名称不能为空");
        if (p.getStartDate() == null || p.getEndDate() == null) throw new IllegalArgumentException("回测区间不能为空");
        if (p.getStartDate().isAfter(p.getEndDate())) throw new IllegalArgumentException("开始日期不能晚于结束日期");
        if (p.getInitialCapital() < 10000) throw new IllegalArgumentException("本金至少 10,000");
        if (p.getUnderlyingCodes() == null || p.getUnderlyingCodes().isEmpty())
            throw new IllegalArgumentException("至少选择 1 个标的");
        if (p.getStrikes() == null || p.getStrikes().isEmpty())
            throw new IllegalArgumentException("至少选择 1 个行权价档位");
        if (!p.isEnableRight() && !p.isEnableLeft2() && !p.isEnableLeft1() && !p.isEnableStrong()
                && !p.isEnableSignalSell() && !p.isEnableAccountTp() && !p.isEnableSingleTp()) {
            throw new IllegalArgumentException("至少启用 1 条卖出规则");
        }
        if (p.getDailyBuyPct() <= 0 || p.getDailyBuyPct() > 1) throw new IllegalArgumentException("每日建仓比例非法");
        if (p.getMaxPositionPct() <= 0 || p.getMaxPositionPct() > 1) throw new IllegalArgumentException("仓位上限非法");
        p.setUnderlyingCodes(new ArrayList<>(p.getUnderlyingCodes()));
    }

    private void runTask(long id, OptionBsParams p, String taskNo) {
        try {
            repo.updateTask(id, 1, 1, null);
            repo.log(id, "INFO", "任务启动 " + taskNo + " capital=" + p.getInitialCapital());
            progressMap.put(id, 5);

            OptionBsDataLoader loader = new OptionBsDataLoader(signalMapper, optionMapper, underlyingMapper);
            OptionBsData data = loader.load(p);
            repo.updateProgress(id, 40);
            progressMap.put(id, 40);
            repo.log(id, "INFO", "数据加载完成 underlyings=" + data.underlyings.size()
                    + " contracts=" + data.contracts.values().stream().mapToInt(List::size).sum()
                    + " calendar=" + data.calendar.size());
            if (Boolean.TRUE.equals(cancelFlags.get(id))) {
                repo.updateTask(id, 4, 100, "用户取消");
                return;
            }

            OptionBsEngine engine = new OptionBsEngine(p, data);
            OptionBsModels.Result r = engine.run();
            r.taskId = id;
            r.taskNo = taskNo;
            progressMap.put(id, 80);
            repo.updateProgress(id, 80);
            repo.saveResult(id, r);
            progressMap.put(id, 100);
            repo.log(id, "INFO", "回测完成 final=" + String.format("%.2f", r.finalEquity)
                    + " total=" + String.format("%.2f%%", r.totalReturnPct)
                    + " trades=" + r.totalTrades + " closes=" + r.closeRecords.size());
        } catch (Exception e) {
            log.error("OptionBsBacktestService - task {} failed", id, e);
            try {
                repo.updateTask(id, 3, 100, e.toString());
                repo.log(id, "ERROR", e.toString());
            } catch (Exception ignore) {
            }
        } finally {
            cancelFlags.remove(id);
        }
    }

    public void cancel(long id) {
        cancelFlags.put(id, true);
        repo.updateTask(id, 4, 100, "用户取消");
        repo.log(id, "WARN", "用户取消任务");
    }


    // =================================================================================================================
    //                                                  查询
    // =================================================================================================================


    public Map<String, Object> page(int page,
                                    int size,
                                    String keyword,
                                    Integer status,
                                    String signalMode,
                                    String underlyingCode,
                                    String startDate,
                                    String endDate,
                                    String sortBy,
                                    String sortDir) {
        return repo.page(page, size, keyword, status, signalMode, underlyingCode, startDate, endDate, sortBy, sortDir);
    }

    public Map<String, Object> detail(long id) {
        Map<String, Object> task = repo.getTask(id);
        if (task == null) return null;
        Integer liveProgress = progressMap.get(id);
        task.put("progress", liveProgress != null ? liveProgress : task.get("progress"));
        task.put("equityCount", repo.equityCount(id));
        task.put("positionDates", repo.positionDates(id));
        return task;
    }

    public List<Map<String, Object>> equity(long id, String from, String to) {
        return repo.equity(id, from, to);
    }

    public Map<String, Object> positions(long id, String date, int page, int size) {
        return repo.positions(id, date, page, size);
    }

    public Map<String, Object> trades(long id,
                                      int page,
                                      int size,
                                      String type,
                                      String reason,
                                      String from,
                                      String to) {
        return repo.trades(id, page, size, type, reason, from, to);
    }

    public Map<String, Object> closes(long id,
                                      int page,
                                      int size,
                                      String reason,
                                      String from,
                                      String to) {
        return repo.closes(id, page, size, reason, from, to);
    }

    public List<Map<String, Object>> signals(long id) {
        return repo.signals(id);
    }

    public List<Map<String, Object>> logs(long id, int limit) {
        return repo.logs(id, limit);
    }

    public List<Map<String, Object>> underlyings() {
        return repo.underlyings();
    }

    public Map<String, Object> stats(long id) {
        return stats(id, null, null);
    }

    public Map<String, Object> stats(long id, String from, String to) {
        Map<String, Object> task = repo.getTask(id);
        List<Map<String, Object>> equity = repo.equity(id, from, to);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("task", task);
        res.put("equity", equity);

        // 月度收益
        Map<String, Double> monthly = new LinkedHashMap<>();
        String curMonth = null;
        double monthStartNav = 1.0, prevNav = 1.0, peak = 0;
        double maxDd = 0, var = 0, mean = 0, down = 0;
        int n = 0, downN = 0, winDays = 0;
        List<Double> rets = new ArrayList<>();
        for (Map<String, Object> e : equity) {
            String d = String.valueOf(e.get("tradeDate"));
            String mk = d.length() >= 7 ? d.substring(0, 7) : d;
            double nav = num(e.get("nav"));
            if (!mk.equals(curMonth)) {
                curMonth = mk;
                monthStartNav = prevNav;
            }
            monthly.put(mk, (nav / Math.max(monthStartNav, 1e-9) - 1) * 100);
            if (n > 0) {
                double r = nav / Math.max(prevNav, 1e-9) - 1;
                rets.add(r);
                mean += r;
                if (r < 0) {
                    down += r * r;
                    downN++;
                } else if (r > 0) winDays++;
            }
            prevNav = nav;
            peak = Math.max(peak, nav);
            maxDd = Math.min(maxDd, (nav / Math.max(peak, 1e-9) - 1) * 100);
            n++;
        }
        mean = rets.isEmpty() ? 0 : mean / rets.size();
        for (double r : rets) var += (r - mean) * (r - mean);
        var = rets.size() > 1 ? var / (rets.size() - 1) : 0;
        double std = Math.sqrt(var);
        rets.sort(Double::compare);
        double var95 = rets.isEmpty() ? 0 : rets.get(Math.max(0, (int) Math.floor(rets.size() * 0.05))) * 100;

        Map<String, Object> risk = new LinkedHashMap<>();
        risk.put("volatilityPct", std * Math.sqrt(252) * 100);
        risk.put("sharpe", std == 0 ? 0 : (mean - 0.02 / 252) / std * Math.sqrt(252));
        risk.put("sortino", down == 0 ? 0 : (mean - 0.02 / 252) / Math.sqrt(down / Math.max(1, rets.size())) * Math.sqrt(252));
        risk.put("maxDrawdownPct", maxDd);
        risk.put("var95Pct", var95);
        risk.put("winRatePct", rets.isEmpty() ? 0 : winDays * 100.0 / rets.size());
        double avgPos = equity.isEmpty() ? 0 : equity.stream().mapToDouble(x -> num(x.get("positionPct"))).average().orElse(0);
        double feeSum = equity.stream().mapToDouble(x -> num(x.get("fee"))).sum();
        double gain = 0, loss = 0;
        for (int i = 1; i < equity.size(); i++) {
            double d = num(equity.get(i).get("equity")) - num(equity.get(i - 1).get("equity"));
            if (d > 0) gain += d;
            else loss += Math.abs(d);
        }
        risk.put("avgPositionPct", avgPos);
        risk.put("profitFactor", loss > 0 ? gain / loss : 0);
        risk.put("totalFee", feeSum);
        risk.put("avgHoldDays", repo.avgHoldDays(id, from, to));
        risk.put("monthlyWinRate", monthly.isEmpty() ? 0 : monthly.values().stream().filter(x -> x > 0).count() * 100.0 / monthly.size());
        int sigCount = 0;
        for (Map<String, Object> sig : repo.signals(id)) {
            String d = String.valueOf(sig.get("signalDate"));
            if ((from == null || d.compareTo(from) >= 0) && (to == null || d.compareTo(to) <= 0)) sigCount++;
        }
        risk.put("signals", sigCount);
        res.put("riskMetrics", risk);
        res.put("monthlyReturns", monthly);
        res.put("sellReasons", repo.sellReasons(id, from, to));
        res.put("contribution", repo.contribution(id, from, to));
        res.put("signals", repo.signals(id));
        res.put("tradeCount", repo.tradeCount(id, from, to));
        return res;
    }


    // =================================================================================================================
    //                                                  导出
    // =================================================================================================================


    public String exportCsv(long id, String type) {
        if ("task".equalsIgnoreCase(type)) {
            Map<String, Object> t = repo.getTask(id);
            StringBuilder b = new StringBuilder("\ufeff");
            b.append("任务号,任务名称,状态,区间,本金,期末权益,总收益%,年化%,最大回撤%,夏普,卡玛,胜率%,交易笔数,天数,创建时间\n");
            if (t != null) {
                b.append(csv(t.get("taskNo"))).append(',').append(csv(t.get("taskName"))).append(',')
                 .append(csv(t.get("status"))).append(',').append(csv(t.get("startDate"))).append("~").append(csv(t.get("endDate"))).append(',')
                 .append(csv(t.get("initialCapital"))).append(',').append(csv(t.get("finalEquity"))).append(',')
                 .append(csv(t.get("totalReturnPct"))).append(',').append(csv(t.get("annualReturnPct"))).append(',')
                 .append(csv(t.get("maxDrawdownPct"))).append(',').append(csv(t.get("sharpeRatio"))).append(',')
                 .append(csv(t.get("calmarRatio"))).append(',').append(csv(t.get("winRatePct"))).append(',')
                 .append(csv(t.get("totalTrades"))).append(',').append(csv(t.get("totalDays"))).append(',')
                 .append(csv(t.get("gmtCreate"))).append('\n');
            }
            return b.toString();
        }

        List<Map<String, Object>> rows;
        String header;
        switch (type) {
            case "positions" -> {
                rows = repo.allPositions(id);
                header = "日期,标的,合约代码,合约名称,类型,行权价,月份,数量,成本价,收盘价,成本金额,市值,浮动盈亏,收益率%,权重%,持仓天数,建仓来源,虚实值";
            }
            case "trades" -> {
                rows = repo.allTrades(id);
                header = "日期,时间,序号,方向,标的,合约,类型,行权价,成交价,数量,金额,费用,触发规则,仓位比例,现金余额,成交后持仓";
            }
            case "closes" -> {
                rows = repo.allCloses(id);
                header = "开仓日,平仓日,标的,合约,类型,数量,均价成本,均价卖出,已实现盈亏,收益率%,持有天数,最大浮盈%,平仓原因";
            }
            default -> {
                rows = repo.equity(id, null, null);
                header = "日期,单位净值,总权益,当日收益%,累计收益%,回撤%,现金,持仓市值,仓位%,信号类型,信号状态,持仓数";
            }
        }

        StringBuilder b = new StringBuilder("\ufeff").append(header).append('\n');
        for (Map<String, Object> r : rows) {
            switch (type) {
                case "positions" ->
                        b.append(csv(r.get("tradeDate"))).append(',').append(csv(r.get("underlyingCode"))).append(',')
                         .append(csv(r.get("optionCode"))).append(',').append(csv(r.get("optionName"))).append(',')
                         .append(csv(r.get("callPutType"))).append(',').append(csv(r.get("strikePrice"))).append(',')
                         .append(csv(r.get("contractMonth"))).append(',').append(csv(r.get("qty"))).append(',')
                         .append(csv(r.get("avgCost"))).append(',').append(csv(r.get("closePrice"))).append(',')
                         .append(csv(r.get("costAmount"))).append(',').append(csv(r.get("marketValue"))).append(',')
                         .append(csv(r.get("unrealizedPnl"))).append(',').append(csv(r.get("pnlPct"))).append(',')
                         .append(csv(r.get("weightPct"))).append(',').append(csv(r.get("holdDays"))).append(',')
                         .append(csv(r.get("openReason"))).append(',').append(csv(r.get("moneyness"))).append('\n');
                case "trades" ->
                        b.append(csv(r.get("tradeDate"))).append(',').append(csv(r.get("tradeTime"))).append(',')
                         .append(csv(r.get("seq"))).append(',').append(1 == (int) num(r.get("tradeType")) ? "买入" : "卖出").append(',')
                         .append(csv(r.get("underlyingCode"))).append(',').append(csv(r.get("optionName"))).append(',')
                         .append(csv(r.get("callPutType"))).append(',').append(csv(r.get("strikePrice"))).append(',')
                         .append(csv(r.get("price"))).append(',').append(csv(r.get("qty"))).append(',')
                         .append(csv(r.get("amount"))).append(',').append(csv(r.get("fee"))).append(',')
                         .append(csv(r.get("reasonDesc"))).append(',').append(csv(r.get("triggerValue"))).append(',')
                         .append(csv(r.get("cashAfter"))).append(',').append(csv(r.get("qtyAfter"))).append('\n');
                case "closes" ->
                        b.append(csv(r.get("openDate"))).append(',').append(csv(r.get("closeDate"))).append(',')
                         .append(csv(r.get("underlyingCode"))).append(',').append(csv(r.get("optionName"))).append(',')
                         .append(csv(r.get("callPutType"))).append(',').append(csv(r.get("qty"))).append(',')
                         .append(csv(r.get("avgCost"))).append(',').append(csv(r.get("avgSellPrice"))).append(',')
                         .append(csv(r.get("realizedPnl"))).append(',').append(csv(r.get("pnlPct"))).append(',')
                         .append(csv(r.get("holdDays"))).append(',').append(csv(r.get("maxPnlPct"))).append(',')
                         .append(csv(r.get("closeReasonDesc"))).append('\n');
                default -> b.append(csv(r.get("tradeDate"))).append(',').append(csv(r.get("nav"))).append(',')
                            .append(csv(r.get("equity"))).append(',').append(csv(r.get("dailyReturnPct"))).append(',')
                            .append(csv(r.get("cumReturnPct"))).append(',').append(csv(r.get("drawdownPct"))).append(',')
                            .append(csv(r.get("cash"))).append(',').append(csv(r.get("marketValue"))).append(',')
                            .append(csv(r.get("positionPct"))).append(',').append(csv(r.get("signalType"))).append(',')
                            .append(csv(r.get("signalStatus"))).append(',').append(csv(r.get("positionCount"))).append('\n');
            }
        }
        return b.toString();
    }

    private static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0;
    }

    private static String csv(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
