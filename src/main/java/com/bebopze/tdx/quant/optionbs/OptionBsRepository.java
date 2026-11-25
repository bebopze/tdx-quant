package com.bebopze.tdx.quant.optionbs;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.mapper.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 期权 BS 回测 - 持久化 / 查询（MyBatis-Plus Mapper + XML）
 *
 * @author bebopze
 * @date 2026/11/26
 */
@Slf4j
public class OptionBsRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final int BATCH_SIZE = 1000;

    private final ObtTaskMapper taskMapper;
    private final ObtDailyEquityMapper equityMapper;
    private final ObtDailyPositionMapper positionMapper;
    private final ObtTradeMapper tradeMapper;
    private final ObtCloseRecordMapper closeMapper;
    private final ObtSignalEventMapper signalMapper;
    private final ObtTaskLogMapper logMapper;
    private final ObtMetaMapper metaMapper;

    public OptionBsRepository(ObtTaskMapper taskMapper,
                              ObtDailyEquityMapper equityMapper,
                              ObtDailyPositionMapper positionMapper,
                              ObtTradeMapper tradeMapper,
                              ObtCloseRecordMapper closeMapper,
                              ObtSignalEventMapper signalMapper,
                              ObtTaskLogMapper logMapper,
                              ObtMetaMapper metaMapper) {
        this.taskMapper = taskMapper;
        this.equityMapper = equityMapper;
        this.positionMapper = positionMapper;
        this.tradeMapper = tradeMapper;
        this.closeMapper = closeMapper;
        this.signalMapper = signalMapper;
        this.logMapper = logMapper;
        this.metaMapper = metaMapper;
    }


    // =================================================================================================================
    //                                                  任务
    // =================================================================================================================


    public String nextTaskNo() {
        String prefix = "OBT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        String last = taskMapper.lastTaskNo(prefix);
        int n = 1;
        if (last != null && last.lastIndexOf('-') >= 0) {
            n = Integer.parseInt(last.substring(last.lastIndexOf('-') + 1)) + 1;
        }
        return prefix + String.format("%04d", n);
    }

    public long createTask(OptionBsParams p, String taskNo) {
        ObtTaskDO t = new ObtTaskDO();
        t.setTaskNo(taskNo);
        t.setTaskName(p.getTaskName());
        t.setStatus(0);
        t.setProgress(0);
        t.setStartDate(p.getStartDate());
        t.setEndDate(p.getEndDate());
        t.setInitialCapital(bd(p.getInitialCapital()));
        t.setUnderlyingCodes(String.join(",", p.getUnderlyingCodes()));
        t.setSignalMode(p.getSignalMode());
        t.setMonthMode(p.getMonthMode());
        t.setStrikeMode(String.join(",", p.getStrikes()));
        t.setParamsJson(JSON.toJSONString(p));
        t.setEngineVersion(p.getEngineVersion());
        t.setDataVersion(p.getDataVersion());
        t.setCreator(p.getCreator());
        taskMapper.insert(t);
        return t.getId();
    }

    public void updateTask(long id, int status, int progress, String error) {
        taskMapper.updateStatus(id, status, progress,
                                error != null && error.length() > 1000 ? error.substring(0, 1000) : error);
    }

    public void updateProgress(long id, int progress) {
        taskMapper.updateProgress(id, progress);
    }

    public void favorite(long id, int flag) {
        taskMapper.updateFavorite(id, flag);
    }

    public void logicDelete(long id) {
        taskMapper.logicDelete(id);
    }

    public void log(long taskId, String level, String message) {
        try {
            ObtTaskLogDO e = new ObtTaskLogDO();
            e.setTaskId(taskId);
            e.setLogTime(LocalDateTime.now());
            e.setLevel(level);
            e.setLogger("OptionBsEngine");
            e.setMessage(message != null && message.length() > 1000 ? message.substring(0, 1000) : message);
            logMapper.insert(e);
        } catch (Exception e) {
            log.warn("OptionBsRepository - log fail : {}", e.getMessage());
        }
    }


    // =================================================================================================================
    //                                                  结果落库
    // =================================================================================================================


    @Transactional(rollbackFor = Exception.class)
    public void saveResult(long taskId, OptionBsModels.Result r) {
        equityMapper.deleteByTaskId(taskId);
        positionMapper.deleteByTaskId(taskId);
        tradeMapper.deleteByTaskId(taskId);
        closeMapper.deleteByTaskId(taskId);
        signalMapper.deleteByTaskId(taskId);

        batchInsert(r.equities, (from, to) -> equityMapper.batchInsert(toEquity(taskId, r.equities.subList(from, to))));
        batchInsert(r.positionRows, (from, to) -> positionMapper.batchInsert(toPosition(taskId, r.positionRows.subList(from, to))));
        batchInsert(r.trades, (from, to) -> tradeMapper.batchInsert(toTrade(taskId, r.trades.subList(from, to))));
        batchInsert(r.closeRecords, (from, to) -> closeMapper.batchInsert(toClose(taskId, r.closeRecords.subList(from, to))));
        batchInsert(r.signals, (from, to) -> signalMapper.batchInsert(toSignal(taskId, r.signals.subList(from, to))));

        ObtTaskDO t = new ObtTaskDO();
        t.setId(taskId);
        t.setActualStartDate(r.actualStartDate);
        t.setFinalEquity(bd(r.finalEquity));
        t.setTotalReturnPct(bd(r.totalReturnPct));
        t.setAnnualReturnPct(bd(r.annualReturnPct));
        t.setMaxDrawdownPct(bd(r.maxDrawdownPct));
        t.setMaxDrawdownStart(r.maxDrawdownStart);
        t.setMaxDrawdownEnd(r.maxDrawdownEnd);
        t.setMaxDrawdownDays(r.maxDrawdownDays);
        t.setSharpeRatio(bd(r.sharpe));
        t.setSortinoRatio(bd(r.sortino));
        t.setCalmarRatio(bd(r.calmar));
        t.setVolatilityPct(bd(r.volatilityPct));
        t.setWinRatePct(bd(r.winRatePct));
        t.setProfitFactor(bd(r.profitFactor));
        t.setTotalTrades(r.totalTrades);
        t.setBuyTrades(r.buyTrades);
        t.setSellTrades(r.sellTrades);
        t.setTotalDays(r.totalDays);
        t.setAvgPositionPct(bd(r.avgPositionPct));
        t.setTotalFee(bd(r.totalFee));
        t.setSignalCount(r.signalCount);
        t.setDurationMs(r.durationMs);
        t.setStatus(2);
        t.setProgress(100);
        t.setFinishedAt(LocalDateTime.now());
        taskMapper.updateById(t);

        log.info("OptionBsRepository - saveResult done taskId={} equity={} positions={} trades={} closes={}",
                 taskId, r.equities.size(), r.positionRows.size(), r.trades.size(), r.closeRecords.size());
    }

    @FunctionalInterface
    private interface BatchWriter {
        void write(int from, int to);
    }

    private <T> void batchInsert(List<T> list, BatchWriter writer) {
        if (list == null || list.isEmpty()) return;
        for (int from = 0; from < list.size(); from += BATCH_SIZE) {
            writer.write(from, Math.min(list.size(), from + BATCH_SIZE));
        }
    }


    // =================================================================================================================
    //                                                  查询（API）
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
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        long total = taskMapper.countPage(keyword, status, signalMode, underlyingCode, start, end);
        int offset = (Math.max(page, 1) - 1) * size;
        List<ObtTaskDO> list = taskMapper.pageList(keyword, status, signalMode, underlyingCode, start, end,
                                                   sortBy, "asc".equalsIgnoreCase(sortDir), offset, size);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("list", toMaps(list));
        res.put("total", total);
        res.put("page", page);
        res.put("size", size);
        res.put("pages", (total + size - 1) / size);
        return res;
    }

    public Map<String, Object> getTask(long id) {
        ObtTaskDO task = taskMapper.selectById(id);
        if (task == null) return null;
        Map<String, Object> t = toMap(task);
        if (task.getParamsJson() != null) {
            try {
                t.put("params", JSON.parseObject(task.getParamsJson()));
            } catch (Exception ignore) {
            }
        }
        return t;
    }

    public List<Map<String, Object>> equity(long id, String from, String to) {
        return toMaps(equityMapper.listByTaskIdAndRange(id, parseDate(from), parseDate(to)));
    }

    public int equityCount(long id) {
        return equityMapper.countByTaskId(id);
    }

    public Map<String, Object> positions(long id, String date, int page, int size) {
        LocalDate d = parseDate(date);
        if (d == null) {
            d = positionMapper.maxTradeDate(id);
        }
        if (d == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("list", List.of());
            empty.put("total", 0);
            empty.put("date", null);
            return empty;
        }
        int total = positionMapper.countByTaskIdAndTradeDate(id, d);
        List<ObtDailyPositionDO> list = positionMapper.pageByTaskIdAndTradeDate(id, d, (page - 1) * size, size);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("list", toMaps(list));
        res.put("total", total);
        res.put("date", d.toString());
        res.put("page", page);
        res.put("size", size);
        return res;
    }

    public List<String> positionDates(long id) {
        List<String> list = new ArrayList<>();
        for (LocalDate d : positionMapper.listTradeDates(id)) list.add(d.toString());
        return list;
    }

    public Map<String, Object> trades(long id,
                                      int page,
                                      int size,
                                      String type,
                                      String reason,
                                      String from,
                                      String to) {
        Integer tradeType = type == null || type.isBlank() ? null : Integer.parseInt(type);
        LocalDate start = parseDate(from);
        LocalDate end = parseDate(to);
        long total = tradeMapper.countByTaskId(id, tradeType, reason, start, end);
        List<ObtTradeDO> list = tradeMapper.pageByTaskId(id, tradeType, reason, start, end, (page - 1) * size, size);
        return pageResult(toMaps(list), total, page, size);
    }

    public Map<String, Object> closes(long id, int page, int size, String reason, String from, String to) {
        LocalDate start = parseDate(from);
        LocalDate end = parseDate(to);
        long total = closeMapper.countByTaskId(id, reason, start, end);
        List<ObtCloseRecordDO> list = closeMapper.pageByTaskId(id, reason, start, end, (page - 1) * size, size);
        return pageResult(toMaps(list), total, page, size);
    }

    public List<Map<String, Object>> signals(long id) {
        return toMaps(signalMapper.listByTaskId(id));
    }

    public List<Map<String, Object>> logs(long id, int limit) {
        return toMaps(logMapper.listByTaskId(id, limit));
    }

    public List<Map<String, Object>> sellReasons(long id) {
        return tradeMapper.listSellReasons(id);
    }

    public List<Map<String, Object>> contribution(long id) {
        return closeMapper.listContribution(id);
    }

    public List<Map<String, Object>> underlyings() {
        List<Map<String, Object>> rows = metaMapper.listUnderlyings();
        for (Map<String, Object> row : rows) {
            if (row.get("startDate") instanceof java.sql.Date d) row.put("startDate", d.toLocalDate().toString());
            if (row.get("endDate") instanceof java.sql.Date d) row.put("endDate", d.toLocalDate().toString());
        }
        return rows;
    }

    public double avgHoldDays(long id) {
        BigDecimal v = closeMapper.avgHoldDays(id);
        return v == null ? 0 : v.doubleValue();
    }

    public List<Map<String, Object>> allPositions(long id) {
        return toMaps(positionMapper.listAllByTaskId(id));
    }

    public List<Map<String, Object>> allTrades(long id) {
        return toMaps(tradeMapper.listAllByTaskId(id));
    }

    public List<Map<String, Object>> allCloses(long id) {
        return toMaps(closeMapper.listAllByTaskId(id));
    }


    // =================================================================================================================
    //                                                  转换
    // =================================================================================================================


    private List<ObtDailyEquityDO> toEquity(long taskId, List<OptionBsModels.Equity> list) {
        List<ObtDailyEquityDO> rows = new ArrayList<>(list.size());
        for (OptionBsModels.Equity e : list) {
            ObtDailyEquityDO x = new ObtDailyEquityDO();
            x.setTaskId(taskId);
            x.setTradeDate(e.date);
            x.setNav(bd(e.nav));
            x.setEquity(bd(e.equity));
            x.setCash(bd(e.cash));
            x.setMarketValue(bd(e.marketValue));
            x.setPositionPct(bd(e.positionPct));
            x.setDailyReturnPct(bd(e.dailyReturnPct));
            x.setCumReturnPct(bd(e.cumReturnPct));
            x.setDrawdownPct(bd(e.drawdownPct));
            x.setBenchmarkClose(bd(e.benchmarkClose));
            x.setBenchmarkNav(bd(e.benchmarkNav));
            x.setBenchmarkReturnPct(bd(e.benchmarkReturnPct));
            x.setSignalType(e.signalType);
            x.setSignalStatus(e.signalStatus);
            x.setPositionCount(e.positionCount);
            x.setFee(bd(e.fee));
            x.setStaleFlag(e.stale ? 1 : 0);
            rows.add(x);
        }
        return rows;
    }

    private List<ObtDailyPositionDO> toPosition(long taskId, List<OptionBsModels.PositionRow> list) {
        List<ObtDailyPositionDO> rows = new ArrayList<>(list.size());
        for (OptionBsModels.PositionRow e : list) {
            ObtDailyPositionDO x = new ObtDailyPositionDO();
            x.setTaskId(taskId);
            x.setTradeDate(e.date);
            x.setUnderlyingCode(e.contract.underlyingCode);
            x.setUnderlyingName(e.contract.underlyingName);
            x.setOptionCode(e.contract.code);
            x.setOptionName(e.contract.name);
            x.setCallPutType(e.contract.callPut);
            x.setStrikePrice(bd(e.contract.strike));
            x.setContractMonth(e.contract.month);
            x.setExpiryDate(e.contract.expiryDate);
            x.setMultiplier(e.contract.multiplier);
            x.setQty(e.qty);
            x.setAvgCost(bd(e.avgCost));
            x.setClosePrice(bd(e.closePrice));
            x.setCostAmount(bd(e.costAmount));
            x.setMarketValue(bd(e.marketValue));
            x.setUnrealizedPnl(bd(e.unrealizedPnl));
            x.setPnlPct(bd(e.pnlPct));
            x.setWeightPct(bd(e.weightPct));
            x.setHoldDays(e.holdDays);
            x.setOpenReason(e.openReason);
            x.setMoneyness(e.moneyness);
            x.setStaleFlag(e.stale ? 1 : 0);
            rows.add(x);
        }
        return rows;
    }

    private List<ObtTradeDO> toTrade(long taskId, List<OptionBsModels.Trade> list) {
        List<ObtTradeDO> rows = new ArrayList<>(list.size());
        for (OptionBsModels.Trade e : list) {
            ObtTradeDO x = new ObtTradeDO();
            x.setTaskId(taskId);
            x.setTradeDate(e.date);
            x.setTradeTime(e.time == null ? LocalTime.of(15, 0) : e.time);
            x.setSeq(e.seq);
            x.setTradeType(e.type);
            x.setUnderlyingCode(e.contract.underlyingCode);
            x.setUnderlyingName(e.contract.underlyingName);
            x.setOptionCode(e.contract.code);
            x.setOptionName(e.contract.name);
            x.setCallPutType(e.contract.callPut);
            x.setStrikePrice(bd(e.contract.strike));
            x.setContractMonth(e.contract.month);
            x.setPrice(bd(e.price));
            x.setQty(e.qty);
            x.setMultiplier(e.contract.multiplier);
            x.setAmount(bd(e.amount));
            x.setFee(bd(e.fee));
            x.setReasonCode(e.reasonCode);
            x.setReasonDesc(e.reasonDesc);
            x.setTriggerValue(bd(e.triggerValue));
            x.setCashAfter(bd(e.cashAfter));
            x.setQtyAfter(e.qtyAfter);
            x.setCycleId(e.cycleId);
            x.setSignalDate(e.signalDate);
            x.setDeferredFlag(0);
            rows.add(x);
        }
        return rows;
    }

    private List<ObtCloseRecordDO> toClose(long taskId, List<OptionBsModels.CloseRecord> list) {
        List<ObtCloseRecordDO> rows = new ArrayList<>(list.size());
        for (OptionBsModels.CloseRecord e : list) {
            ObtCloseRecordDO x = new ObtCloseRecordDO();
            x.setTaskId(taskId);
            x.setCycleId(e.cycleId);
            x.setUnderlyingCode(e.underlyingCode);
            x.setOptionCode(e.contract.code);
            x.setOptionName(e.contract.name);
            x.setCallPutType(e.contract.callPut);
            x.setOpenDate(e.openDate);
            x.setCloseDate(e.closeDate);
            x.setQty(e.qty);
            x.setAvgCost(bd(e.avgCost));
            x.setAvgSellPrice(bd(e.avgSellPrice));
            x.setRealizedPnl(bd(e.realizedPnl));
            x.setPnlPct(bd(e.pnlPct));
            x.setHoldDays(e.holdDays);
            x.setMaxPnlPct(bd(e.maxPnlPct));
            x.setMinPnlPct(bd(e.minPnlPct));
            x.setCloseReason(e.closeReason);
            x.setCloseReasonDesc(e.closeReasonDesc);
            rows.add(x);
        }
        return rows;
    }

    private List<ObtSignalEventDO> toSignal(long taskId, List<OptionBsModels.Signal> list) {
        List<ObtSignalEventDO> rows = new ArrayList<>(list.size());
        for (OptionBsModels.Signal e : list) {
            ObtSignalEventDO x = new ObtSignalEventDO();
            x.setTaskId(taskId);
            x.setSignalDate(e.date);
            x.setExecDate(e.date);
            x.setPrevStatus(e.prevStatus);
            x.setSignalType(e.type);
            x.setCycleId(e.cycleId);
            x.setMarketLowDay(e.lowDay);
            x.setMarketHighDay(e.highDay);
            x.setMa50Pct(bd(e.ma50Pct));
            x.setProcessedFlag(1);
            rows.add(x);
        }
        return rows;
    }


    // =================================================================================================================
    //                                                  工具
    // =================================================================================================================


    private Map<String, Object> pageResult(List<Map<String, Object>> list, long total, int page, int size) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("list", list);
        res.put("total", total);
        res.put("page", page);
        res.put("size", size);
        res.put("pages", (total + size - 1) / size);
        return res;
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal bd(double v) {
        return Double.isNaN(v) || Double.isInfinite(v) ? BigDecimal.ZERO : BigDecimal.valueOf(v);
    }

    private static BigDecimal bd(Double v) {
        return v == null || Double.isNaN(v) || Double.isInfinite(v) ? null : BigDecimal.valueOf(v);
    }

    private static Map<String, Object> toMap(Object o) {
        return MAPPER.convertValue(o, new TypeReference<Map<String, Object>>() {
        });
    }

    private static List<Map<String, Object>> toMaps(List<?> list) {
        List<Map<String, Object>> rows = new ArrayList<>(list.size());
        for (Object o : list) rows.add(toMap(o));
        return rows;
    }


}
