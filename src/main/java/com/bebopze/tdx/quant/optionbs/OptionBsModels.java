package com.bebopze.tdx.quant.optionbs;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 期权 BS 回测 - 内存模型
 *
 * @author bebopze
 * @date 2026/11/26
 */
public class OptionBsModels {

    private OptionBsModels() {
    }

    /**
     * 期权合约 + 日K
     */
    public static class Contract {
        public String code;
        public String symbol;
        public String name;
        public String underlyingCode;
        public String underlyingName;
        public String callPut;       // C / P
        public double strike;
        public int month;            // yyyyMM
        public LocalDate startDate;
        public LocalDate expiryDate;
        public int multiplier;
        public LocalDate[] dates;
        public double[] closes;
        public long[] vols;

        public boolean alive(LocalDate d) {
            return (startDate == null || !d.isBefore(startDate)) && (expiryDate == null || !d.isAfter(expiryDate));
        }

        public int indexOf(LocalDate d) {
            if (dates == null || dates.length == 0) return -1;
            int lo = 0, hi = dates.length - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                int cmp = dates[mid].compareTo(d);
                if (cmp == 0) return mid;
                if (cmp < 0) lo = mid + 1;
                else hi = mid - 1;
            }
            return -1;
        }

        public double closeOn(LocalDate d) {
            int i = indexOf(d);
            return i >= 0 ? closes[i] : Double.NaN;
        }

        public double volOn(LocalDate d) {
            int i = indexOf(d);
            return i >= 0 && vols != null ? vols[i] : -1;
        }

        public double lastCloseOnOrBefore(LocalDate d) {
            if (dates == null || dates.length == 0) return Double.NaN;
            int lo = 0, hi = dates.length - 1, ans = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (!dates[mid].isAfter(d)) {
                    ans = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return ans >= 0 ? closes[ans] : Double.NaN;
        }
    }

    /**
     * 标的ETF / 指数 + 指标
     */
    public static class Underlying {
        public String code;
        public String name;
        public int multiplier = 10000;
        public LocalDate[] dates;
        public double[] closes;
        public double[] ma5;
        public double[] ma10;
        public double[] ma20;
        public double[] ssf;
        public Map<LocalDate, Integer> indexMap = new LinkedHashMap<>();

        public int indexOf(LocalDate d) {
            Integer i = indexMap.get(d);
            return i == null ? -1 : i;
        }

        public double close(LocalDate d) {
            int i = indexOf(d);
            return i >= 0 ? closes[i] : lastCloseOnOrBefore(d);
        }

        public double lastCloseOnOrBefore(LocalDate d) {
            if (dates == null || dates.length == 0) return Double.NaN;
            int lo = 0, hi = dates.length - 1, ans = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (!dates[mid].isAfter(d)) {
                    ans = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return ans >= 0 ? closes[ans] : Double.NaN;
        }

        public double ma(int n, LocalDate d) {
            int i = indexOf(d);
            if (i < 0) return Double.NaN;
            double[] a = n == 5 ? ma5 : n == 10 ? ma10 : ma20;
            return a == null ? Double.NaN : a[i];
        }

        public double maPrev(int n, LocalDate d) {
            int i = indexOf(d);
            if (i <= 0) return Double.NaN;
            double[] a = n == 5 ? ma5 : n == 10 ? ma10 : ma20;
            return a == null ? Double.NaN : a[i - 1];
        }

        public double ssf(LocalDate d) {
            int i = indexOf(d);
            return i >= 0 && ssf != null ? ssf[i] : Double.NaN;
        }

        public double ssfPrev(LocalDate d) {
            int i = indexOf(d);
            return i > 0 && ssf != null ? ssf[i - 1] : Double.NaN;
        }

        public boolean has(LocalDate d) {
            return indexOf(d) >= 0;
        }
    }

    /**
     * 大盘信号
     */
    public static class Signal {
        public LocalDate date;
        public int type;             // 1 底 / 3 顶
        public int prevStatus;
        public Integer lowDay;
        public Integer highDay;
        public Double ma50Pct;
        public int cycleId;
    }

    /**
     * 持仓
     */
    public static class Position {
        public Contract contract;
        public int qty;
        public double avgCost;
        public LocalDate openDate;
        public int cycleId;
        public String openReason;
        public int initialQty;
        public int soldQty;
        public double sellAmount;
        public double realizedPnl;
        public double maxPnlPct = -1e9;
        public double minPnlPct = 1e9;
        public int nextSingleTp = 2;   // 2 → +100%, 3 → +200% ...
        public double lastPrice = Double.NaN;

        public double costAmount() {
            return qty * avgCost * contract.multiplier;
        }

        public double marketValue() {
            return qty * lastPrice * contract.multiplier;
        }
    }

    /**
     * 交易记录
     */
    public static class Trade {
        public LocalDate date;
        public LocalTime time;
        public int seq;
        public int type;             // 1 买 2 卖
        public Contract contract;
        public double price;
        public int qty;
        public double amount;
        public double fee;
        public String reasonCode;
        public String reasonDesc;
        public Double triggerValue;
        public double cashAfter;
        public int qtyAfter;
        public int cycleId;
        public LocalDate signalDate;
    }

    /**
     * 每日净值
     */
    public static class Equity {
        public LocalDate date;
        public double nav;
        public double equity;
        public double cash;
        public double marketValue;
        public double positionPct;
        public double dailyReturnPct;
        public double cumReturnPct;
        public double drawdownPct;
        public Double benchmarkClose;
        public Double benchmarkNav;
        public Double benchmarkReturnPct;
        public Integer signalType;
        public String signalStatus;
        public int positionCount;
        public double fee;
        public boolean stale;
    }

    /**
     * 平仓记录
     */
    public static class CloseRecord {
        public int cycleId;
        public String underlyingCode;
        public Contract contract;
        public LocalDate openDate;
        public LocalDate closeDate;
        public int qty;
        public double avgCost;
        public double avgSellPrice;
        public double realizedPnl;
        public double pnlPct;
        public int holdDays;
        public double maxPnlPct;
        public double minPnlPct;
        public String closeReason;
        public String closeReasonDesc;
    }

    /**
     * 每日持仓快照
     */
    public static class PositionRow {
        public LocalDate date;
        public Contract contract;
        public int qty;
        public double avgCost;
        public double closePrice;
        public double costAmount;
        public double marketValue;
        public double unrealizedPnl;
        public double pnlPct;
        public double weightPct;
        public int holdDays;
        public String openReason;
        public String moneyness;
        public boolean stale;
    }

    /**
     * 回测结果
     */
    public static class Result {
        public OptionBsParams params;
        public long taskId;
        public String taskNo;
        public LocalDate actualStartDate;
        public LocalDate actualEndDate;
        public double initialCapital;
        public double finalEquity;
        public double totalReturnPct;
        public double annualReturnPct;
        public double maxDrawdownPct;
        public LocalDate maxDrawdownStart;
        public LocalDate maxDrawdownEnd;
        public int maxDrawdownDays;
        public double sharpe;
        public double sortino;
        public double calmar;
        public double volatilityPct;
        public double winRatePct;
        public double profitFactor;
        public int totalTrades;
        public int buyTrades;
        public int sellTrades;
        public int totalDays;
        public double avgPositionPct;
        public double totalFee;
        public int signalCount;
        public long durationMs;
        public String status = "SUCCESS";
        public String errorMsg;

        public List<Equity> equities = new ArrayList<>();
        public List<PositionRow> positionRows = new ArrayList<>();
        public List<Trade> trades = new ArrayList<>();
        public List<CloseRecord> closeRecords = new ArrayList<>();
        public List<Signal> signals = new ArrayList<>();
        public Map<String, Object> monthlyReturns = new LinkedHashMap<>();
        public List<Map<String, Object>> sellReasonStats = new ArrayList<>();
        public List<Map<String, Object>> underlyingContribution = new ArrayList<>();
        public Map<String, Object> riskMetrics = new LinkedHashMap<>();
    }
}
