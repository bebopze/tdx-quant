package com.bebopze.tdx.quant.optionbs;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 期权 BS 回测 - 持久化 / 查询（原生 JDBC，Spring 与 CLI 共用）
 *
 * @author bebopze
 * @date 2026/11/26
 */
@Slf4j
public class OptionBsRepository {

    private final OptionBsDb db;

    public OptionBsRepository(OptionBsDb db) {
        this.db = db;
    }


    // =================================================================================================================
    //                                                  任务
    // =================================================================================================================


    public String nextTaskNo() throws SQLException {
        String prefix = "OBT" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        String sql = "SELECT task_no FROM obt_task WHERE task_no LIKE ? ORDER BY task_no DESC LIMIT 1";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int n = 1;
                if (rs.next()) {
                    String last = rs.getString(1);
                    n = Integer.parseInt(last.substring(last.lastIndexOf('-') + 1)) + 1;
                }
                return prefix + String.format("%04d", n);
            }
        }
    }

    public long createTask(OptionBsParams p, String taskNo) throws SQLException {
        String sql = "INSERT INTO obt_task(task_no, task_name, status, progress, start_date, end_date, initial_capital, " +
                "underlying_codes, signal_mode, month_mode, strike_mode, params_json, engine_version, data_version, creator) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, taskNo);
            ps.setString(2, p.getTaskName());
            ps.setInt(3, 0);
            ps.setInt(4, 0);
            ps.setDate(5, java.sql.Date.valueOf(p.getStartDate()));
            ps.setDate(6, java.sql.Date.valueOf(p.getEndDate()));
            ps.setBigDecimal(7, BigDecimal.valueOf(p.getInitialCapital()));
            ps.setString(8, String.join(",", p.getUnderlyingCodes()));
            ps.setString(9, p.getSignalMode());
            ps.setString(10, p.getMonthMode());
            ps.setString(11, String.join(",", p.getStrikes()));
            ps.setString(12, JSON.toJSONString(p));
            ps.setString(13, p.getEngineVersion());
            ps.setString(14, p.getDataVersion());
            ps.setString(15, p.getCreator());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            throw new SQLException("createTask no id");
        }
    }

    public void updateTask(long id, int status, int progress, String error) throws SQLException {
        String sql = "UPDATE obt_task SET status=?, progress=?, error_msg=?, " +
                "started_at=IF(started_at IS NULL AND ?=1, NOW(), started_at), " +
                "finished_at=IF(? IN (2,3,4), NOW(), finished_at) WHERE id=?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, status);
            ps.setInt(2, progress);
            ps.setString(3, error);
            ps.setInt(4, status);
            ps.setInt(5, status);
            ps.setLong(6, id);
            ps.executeUpdate();
        }
    }

    public void updateProgress(long id, int progress) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE obt_task SET progress=? WHERE id=?")) {
            ps.setInt(1, progress);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void log(long taskId, String level, String message) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO obt_task_log(task_id, log_time, level, logger, message) VALUES(?,?,?,?,?)")) {
            ps.setLong(1, taskId);
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, level);
            ps.setString(4, "OptionBs");
            ps.setString(5, message.length() > 990 ? message.substring(0, 990) : message);
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("OptionBsRepository - log fail : {}", e.getMessage());
        }
    }


    // =================================================================================================================
    //                                                  结果落库
    // =================================================================================================================


    public void saveResult(long taskId, OptionBsModels.Result r) throws SQLException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                delete(c, "obt_daily_equity", taskId);
                delete(c, "obt_daily_position", taskId);
                delete(c, "obt_trade", taskId);
                delete(c, "obt_close_record", taskId);
                delete(c, "obt_signal_event", taskId);

                batchEquity(c, taskId, r.equities);
                batchPositions(c, taskId, r.positionRows);
                batchTrades(c, taskId, r.trades);
                batchCloses(c, taskId, r.closeRecords);
                batchSignals(c, taskId, r.signals);

                String sql = "UPDATE obt_task SET actual_start_date=?, final_equity=?, total_return_pct=?, annual_return_pct=?, " +
                        "max_drawdown_pct=?, max_drawdown_start=?, max_drawdown_end=?, max_drawdown_days=?, sharpe_ratio=?, " +
                        "sortino_ratio=?, calmar_ratio=?, volatility_pct=?, win_rate_pct=?, profit_factor=?, total_trades=?, " +
                        "buy_trades=?, sell_trades=?, total_days=?, avg_position_pct=?, total_fee=?, signal_count=?, " +
                        "duration_ms=?, status=2, progress=100, finished_at=NOW() WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    int i = 1;
                    setDate(ps, i++, r.actualStartDate);
                    ps.setBigDecimal(i++, bd(r.finalEquity));
                    ps.setBigDecimal(i++, bd(r.totalReturnPct));
                    ps.setBigDecimal(i++, bd(r.annualReturnPct));
                    ps.setBigDecimal(i++, bd(r.maxDrawdownPct));
                    setDate(ps, i++, r.maxDrawdownStart);
                    setDate(ps, i++, r.maxDrawdownEnd);
                    ps.setInt(i++, r.maxDrawdownDays);
                    ps.setBigDecimal(i++, bd(r.sharpe));
                    ps.setBigDecimal(i++, bd(r.sortino));
                    ps.setBigDecimal(i++, bd(r.calmar));
                    ps.setBigDecimal(i++, bd(r.volatilityPct));
                    ps.setBigDecimal(i++, bd(r.winRatePct));
                    ps.setBigDecimal(i++, bd(r.profitFactor));
                    ps.setInt(i++, r.totalTrades);
                    ps.setInt(i++, r.buyTrades);
                    ps.setInt(i++, r.sellTrades);
                    ps.setInt(i++, r.totalDays);
                    ps.setBigDecimal(i++, bd(r.avgPositionPct));
                    ps.setBigDecimal(i++, bd(r.totalFee));
                    ps.setInt(i++, r.signalCount);
                    ps.setLong(i++, r.durationMs);
                    ps.setLong(i, taskId);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
        log.info("OptionBsRepository - saveResult done taskId={} equity={} positions={} trades={} closes={}",
                 taskId, r.equities.size(), r.positionRows.size(), r.trades.size(), r.closeRecords.size());
    }

    private void delete(Connection c, String table, long taskId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM " + table + " WHERE task_id=?")) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }
    }

    private void batchEquity(Connection c, long taskId, List<OptionBsModels.Equity> list) throws SQLException {
        String sql = "INSERT INTO obt_daily_equity(task_id,trade_date,nav,equity,cash,market_value,position_pct,daily_return_pct," +
                "cum_return_pct,drawdown_pct,benchmark_close,benchmark_nav,benchmark_return_pct,signal_type,signal_status," +
                "position_count,fee,stale_flag) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int n = 0;
            for (OptionBsModels.Equity e : list) {
                int i = 1;
                ps.setLong(i++, taskId);
                ps.setDate(i++, java.sql.Date.valueOf(e.date));
                ps.setBigDecimal(i++, bd(e.nav));
                ps.setBigDecimal(i++, bd(e.equity));
                ps.setBigDecimal(i++, bd(e.cash));
                ps.setBigDecimal(i++, bd(e.marketValue));
                ps.setBigDecimal(i++, bd(e.positionPct));
                ps.setBigDecimal(i++, bd(e.dailyReturnPct));
                ps.setBigDecimal(i++, bd(e.cumReturnPct));
                ps.setBigDecimal(i++, bd(e.drawdownPct));
                setBd(ps, i++, e.benchmarkClose);
                setBd(ps, i++, e.benchmarkNav);
                setBd(ps, i++, e.benchmarkReturnPct);
                if (e.signalType == null) ps.setNull(i++, java.sql.Types.TINYINT);
                else ps.setInt(i++, e.signalType);
                ps.setString(i++, e.signalStatus);
                ps.setInt(i++, e.positionCount);
                ps.setBigDecimal(i++, bd(e.fee));
                ps.setInt(i, e.stale ? 1 : 0);
                ps.addBatch();
                if (++n % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
        }
    }

    private void batchPositions(Connection c, long taskId, List<OptionBsModels.PositionRow> list) throws SQLException {
        String sql = "INSERT INTO obt_daily_position(task_id,trade_date,underlying_code,underlying_name,option_code,option_name," +
                "call_put_type,strike_price,contract_month,expiry_date,multiplier,qty,avg_cost,close_price,cost_amount,market_value," +
                "unrealized_pnl,pnl_pct,weight_pct,hold_days,open_reason,moneyness,stale_flag) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int n = 0;
            for (OptionBsModels.PositionRow x : list) {
                int i = 1;
                ps.setLong(i++, taskId);
                ps.setDate(i++, java.sql.Date.valueOf(x.date));
                ps.setString(i++, x.contract.underlyingCode);
                ps.setString(i++, x.contract.underlyingName);
                ps.setString(i++, x.contract.code);
                ps.setString(i++, x.contract.name);
                ps.setString(i++, x.contract.callPut);
                ps.setBigDecimal(i++, bd(x.contract.strike));
                ps.setInt(i++, x.contract.month);
                setDate(ps, i++, x.contract.expiryDate);
                ps.setInt(i++, x.contract.multiplier);
                ps.setInt(i++, x.qty);
                ps.setBigDecimal(i++, bd(x.avgCost));
                ps.setBigDecimal(i++, bd(x.closePrice));
                ps.setBigDecimal(i++, bd(x.costAmount));
                ps.setBigDecimal(i++, bd(x.marketValue));
                ps.setBigDecimal(i++, bd(x.unrealizedPnl));
                ps.setBigDecimal(i++, bd(x.pnlPct));
                ps.setBigDecimal(i++, bd(x.weightPct));
                ps.setInt(i++, x.holdDays);
                ps.setString(i++, x.openReason);
                ps.setString(i++, x.moneyness);
                ps.setInt(i, x.stale ? 1 : 0);
                ps.addBatch();
                if (++n % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
        }
    }

    private void batchTrades(Connection c, long taskId, List<OptionBsModels.Trade> list) throws SQLException {
        String sql = "INSERT INTO obt_trade(task_id,trade_date,trade_time,seq,trade_type,underlying_code,underlying_name,option_code," +
                "option_name,call_put_type,strike_price,contract_month,price,qty,multiplier,amount,fee,reason_code,reason_desc," +
                "trigger_value,cash_after,qty_after,cycle_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int n = 0;
            for (OptionBsModels.Trade t : list) {
                int i = 1;
                ps.setLong(i++, taskId);
                ps.setDate(i++, java.sql.Date.valueOf(t.date));
                ps.setTime(i++, t.time == null ? java.sql.Time.valueOf("15:00:00") : java.sql.Time.valueOf(t.time));
                ps.setInt(i++, t.seq);
                ps.setInt(i++, t.type);
                ps.setString(i++, t.contract.underlyingCode);
                ps.setString(i++, t.contract.underlyingName);
                ps.setString(i++, t.contract.code);
                ps.setString(i++, t.contract.name);
                ps.setString(i++, t.contract.callPut);
                ps.setBigDecimal(i++, bd(t.contract.strike));
                ps.setInt(i++, t.contract.month);
                ps.setBigDecimal(i++, bd(t.price));
                ps.setInt(i++, t.qty);
                ps.setInt(i++, t.contract.multiplier);
                ps.setBigDecimal(i++, bd(t.amount));
                ps.setBigDecimal(i++, bd(t.fee));
                ps.setString(i++, t.reasonCode);
                ps.setString(i++, t.reasonDesc);
                if (t.triggerValue == null) ps.setNull(i++, java.sql.Types.DECIMAL);
                else ps.setBigDecimal(i++, bd(t.triggerValue));
                ps.setBigDecimal(i++, bd(t.cashAfter));
                ps.setInt(i++, t.qtyAfter);
                ps.setInt(i, t.cycleId);
                ps.addBatch();
                if (++n % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
        }
    }

    private void batchCloses(Connection c, long taskId, List<OptionBsModels.CloseRecord> list) throws SQLException {
        String sql = "INSERT INTO obt_close_record(task_id,cycle_id,underlying_code,option_code,option_name,call_put_type,open_date," +
                "close_date,qty,avg_cost,avg_sell_price,realized_pnl,pnl_pct,hold_days,max_pnl_pct,min_pnl_pct,close_reason,close_reason_desc) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            int n = 0;
            for (OptionBsModels.CloseRecord x : list) {
                int i = 1;
                ps.setLong(i++, taskId);
                ps.setInt(i++, x.cycleId);
                ps.setString(i++, x.contract.underlyingCode);
                ps.setString(i++, x.contract.code);
                ps.setString(i++, x.contract.name);
                ps.setString(i++, x.contract.callPut);
                ps.setDate(i++, java.sql.Date.valueOf(x.openDate));
                ps.setDate(i++, java.sql.Date.valueOf(x.closeDate));
                ps.setInt(i++, x.qty);
                ps.setBigDecimal(i++, bd(x.avgCost));
                ps.setBigDecimal(i++, bd(x.avgSellPrice));
                ps.setBigDecimal(i++, bd(x.realizedPnl));
                ps.setBigDecimal(i++, bd(x.pnlPct));
                ps.setInt(i++, x.holdDays);
                ps.setBigDecimal(i++, bd(x.maxPnlPct));
                ps.setBigDecimal(i++, bd(x.minPnlPct));
                ps.setString(i++, x.closeReason);
                ps.setString(i, x.closeReasonDesc);
                ps.addBatch();
                if (++n % 1000 == 0) ps.executeBatch();
            }
            ps.executeBatch();
        }
    }

    private void batchSignals(Connection c, long taskId, List<OptionBsModels.Signal> list) throws SQLException {
        String sql = "INSERT INTO obt_signal_event(task_id,signal_date,exec_date,prev_status,signal_type,cycle_id," +
                "market_low_day,market_high_day,ma50_pct,processed_flag) VALUES(?,?,?,?,?,?,?,?,?,1)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (OptionBsModels.Signal s : list) {
                int i = 1;
                ps.setLong(i++, taskId);
                ps.setDate(i++, java.sql.Date.valueOf(s.date));
                ps.setDate(i++, java.sql.Date.valueOf(s.date));
                ps.setInt(i++, s.prevStatus);
                ps.setInt(i++, s.type);
                ps.setInt(i++, s.cycleId);
                if (s.lowDay == null) ps.setNull(i++, java.sql.Types.INTEGER);
                else ps.setInt(i++, s.lowDay);
                if (s.highDay == null) ps.setNull(i++, java.sql.Types.INTEGER);
                else ps.setInt(i++, s.highDay);
                if (s.ma50Pct == null) ps.setNull(i++, java.sql.Types.DECIMAL);
                else ps.setBigDecimal(i, bd(s.ma50Pct));
                ps.addBatch();
            }
            ps.executeBatch();
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
                                    String sortDir) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE deleted_flag=0");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (task_name LIKE ? OR task_no LIKE ? OR creator LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            args.add(kw);
            args.add(kw);
            args.add(kw);
        }
        if (status != null) {
            where.append(" AND status=?");
            args.add(status);
        }
        if (signalMode != null && !signalMode.isBlank()) {
            where.append(" AND signal_mode=?");
            args.add(signalMode);
        }
        if (underlyingCode != null && !underlyingCode.isBlank()) {
            where.append(" AND underlying_codes LIKE ?");
            args.add("%" + underlyingCode + "%");
        }
        if (startDate != null && !startDate.isBlank()) {
            where.append(" AND end_date>=?");
            args.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            where.append(" AND start_date<=?");
            args.add(endDate);
        }

        String order = switch (sortBy == null ? "" : sortBy) {
            case "id" -> "id";
            case "initialCapital" -> "initial_capital";
            case "finalEquity" -> "final_equity";
            case "annualReturn" -> "annual_return_pct";
            case "maxDD" -> "max_drawdown_pct";
            case "sharpe" -> "sharpe_ratio";
            case "winRate" -> "win_rate_pct";
            case "totalTrades" -> "total_trades";
            case "createdAt" -> "gmt_create";
            default -> "total_return_pct";
        };
        String dir = "asc".equalsIgnoreCase(sortDir) ? "ASC" : "DESC";

        String countSql = "SELECT COUNT(*) FROM obt_task" + where;
        int total;
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(countSql)) {
            bind(ps, args);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getInt(1);
            }
        }

        String sql = "SELECT id,task_no,task_name,status,progress,start_date,end_date,actual_start_date,initial_capital," +
                "final_equity,total_return_pct,annual_return_pct,max_drawdown_pct,sharpe_ratio,calmar_ratio,win_rate_pct," +
                "total_trades,total_days,duration_ms,favorite_flag,creator,gmt_create,underlying_codes,signal_mode,month_mode " +
                "FROM obt_task" + where + " ORDER BY " + order + " " + dir + " LIMIT ?,?";
        List<Object> args2 = new ArrayList<>(args);
        args2.add((page - 1) * size);
        args2.add(size);

        List<Map<String, Object>> list = queryMaps(sql, args2);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("list", list);
        res.put("total", total);
        res.put("page", page);
        res.put("size", size);
        res.put("pages", (total + size - 1) / size);
        return res;
    }

    public Map<String, Object> getTask(long id) throws SQLException {
        Map<String, Object> t = queryOne("SELECT * FROM obt_task WHERE id=?", id);
        if (t == null) return null;
        Object pj = t.get("paramsJson");
        if (pj != null) {
            try {
                t.put("params", JSON.parseObject(String.valueOf(pj)));
            } catch (Exception ignore) {
            }
        }
        return t;
    }

    public List<Map<String, Object>> equity(long id, String from, String to) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM obt_daily_equity WHERE task_id=?");
        List<Object> args = new ArrayList<>();
        args.add(id);
        if (from != null && !from.isBlank()) {
            sql.append(" AND trade_date>=?");
            args.add(from);
        }
        if (to != null && !to.isBlank()) {
            sql.append(" AND trade_date<=?");
            args.add(to);
        }
        sql.append(" ORDER BY trade_date");
        return queryMaps(sql.toString(), args);
    }

    public Map<String, Object> positions(long id, String date, int page, int size) throws SQLException {
        String d = date;
        if (d == null || d.isBlank()) {
            Map<String, Object> one = queryOne("SELECT MAX(trade_date) d FROM obt_daily_position WHERE task_id=?", id);
            d = one == null || one.get("d") == null ? null : String.valueOf(one.get("d"));
        }
        if (d == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("list", List.of());
            empty.put("total", 0);
            empty.put("date", null);
            return empty;
        }
        int total = intOne("SELECT COUNT(*) FROM obt_daily_position WHERE task_id=? AND trade_date=?", id, d);
        List<Map<String, Object>> list = queryMaps(
                "SELECT * FROM obt_daily_position WHERE task_id=? AND trade_date=? ORDER BY market_value DESC LIMIT ?,?",
                id, d, (page - 1) * size, size);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("list", list);
        res.put("total", total);
        res.put("date", d);
        res.put("page", page);
        res.put("size", size);
        return res;
    }

    public List<String> positionDates(long id) throws SQLException {
        List<String> list = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT DISTINCT trade_date FROM obt_daily_position WHERE task_id=? ORDER BY trade_date")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getDate(1).toString());
            }
        }
        return list;
    }

    public Map<String, Object> trades(long id,
                                      int page,
                                      int size,
                                      String type,
                                      String reason,
                                      String from,
                                      String to) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE task_id=?");
        List<Object> args = new ArrayList<>();
        args.add(id);
        if (type != null && !type.isBlank()) {
            where.append(" AND trade_type=?");
            args.add(Integer.parseInt(type));
        }
        if (reason != null && !reason.isBlank()) {
            where.append(" AND reason_code=?");
            args.add(reason);
        }
        if (from != null && !from.isBlank()) {
            where.append(" AND trade_date>=?");
            args.add(from);
        }
        if (to != null && !to.isBlank()) {
            where.append(" AND trade_date<=?");
            args.add(to);
        }
        int total = intOne("SELECT COUNT(*) FROM obt_trade" + where, args.toArray());
        List<Object> args2 = new ArrayList<>(args);
        args2.add((page - 1) * size);
        args2.add(size);
        List<Map<String, Object>> list = queryMaps("SELECT * FROM obt_trade" + where +
                                                           " ORDER BY trade_date DESC, seq DESC LIMIT ?,?", args2);
        return pageResult(list, total, page, size);
    }

    public Map<String, Object> closes(long id, int page, int size, String reason) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE task_id=?");
        List<Object> args = new ArrayList<>();
        args.add(id);
        if (reason != null && !reason.isBlank()) {
            where.append(" AND close_reason=?");
            args.add(reason);
        }
        int total = intOne("SELECT COUNT(*) FROM obt_close_record" + where, args.toArray());
        List<Object> args2 = new ArrayList<>(args);
        args2.add((page - 1) * size);
        args2.add(size);
        List<Map<String, Object>> list = queryMaps("SELECT * FROM obt_close_record" + where +
                                                           " ORDER BY close_date DESC, id DESC LIMIT ?,?", args2);
        return pageResult(list, total, page, size);
    }

    public List<Map<String, Object>> signals(long id) throws SQLException {
        return queryMaps("SELECT * FROM obt_signal_event WHERE task_id=? ORDER BY signal_date", id);
    }

    public List<Map<String, Object>> logs(long id, int limit) throws SQLException {
        return queryMaps("SELECT * FROM obt_task_log WHERE task_id=? ORDER BY id DESC LIMIT ?", id, limit);
    }

    public List<Map<String, Object>> sellReasons(long id) throws SQLException {
        return queryMaps("SELECT reason_code, reason_desc, COUNT(*) cnt FROM obt_trade WHERE task_id=? AND trade_type=2 " +
                                 "GROUP BY reason_code, reason_desc ORDER BY cnt DESC", id);
    }

    public List<Map<String, Object>> contribution(long id) throws SQLException {
        return queryMaps("SELECT underlying_code, SUM(realized_pnl) pnl, COUNT(*) cnt FROM obt_close_record " +
                                 "WHERE task_id=? GROUP BY underlying_code ORDER BY pnl DESC", id);
    }

    public List<Map<String, Object>> underlyings() throws SQLException {
        return queryMaps("SELECT underlying_code, MIN(underlying_name) underlying_name, underlying_type, COUNT(*) contract_count, " +
                                 "MIN(start_date) start_date, MAX(end_date) end_date FROM base_option " +
                                 "WHERE underlying_code IN ('159915','588000','510500','510300','000852') " +
                                 "GROUP BY underlying_code, underlying_type ORDER BY underlying_code");
    }


    // =================================================================================================================
    //                                                  工具
    // =================================================================================================================


    private Map<String, Object> pageResult(List<Map<String, Object>> list, int total, int page, int size) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("list", list);
        res.put("total", total);
        res.put("page", page);
        res.put("size", size);
        res.put("pages", (total + size - 1) / size);
        return res;
    }

    public List<Map<String, Object>> queryMaps(String sql, List<Object> args) throws SQLException {
        return queryMaps(sql, args == null ? new Object[0] : args.toArray());
    }

    public int execute(String sql, Object... args) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, java.util.Arrays.asList(args));
            return ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> queryMaps(String sql, Object... args) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, java.util.Arrays.asList(args));
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= n; i++) {
                        row.put(camel(md.getColumnLabel(i)), normalize(rs.getObject(i)));
                    }
                    list.add(row);
                }
            }
        }
        return list;
    }

    public Map<String, Object> queryOne(String sql, Object... args) throws SQLException {
        List<Map<String, Object>> list = queryMaps(sql, args);
        return list.isEmpty() ? null : list.get(0);
    }

    private int intOne(String sql, Object... args) throws SQLException {
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, java.util.Arrays.asList(args));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private void bind(PreparedStatement ps, List<Object> args) throws SQLException {
        for (int i = 0; i < args.size(); i++) {
            Object v = args.get(i);
            if (v instanceof LocalDate d) ps.setDate(i + 1, java.sql.Date.valueOf(d));
            else ps.setObject(i + 1, v);
        }
    }

    private static String camel(String s) {
        StringBuilder b = new StringBuilder();
        boolean up = false;
        for (char ch : s.toCharArray()) {
            if (ch == '_') {
                up = true;
            } else if (up) {
                b.append(Character.toUpperCase(ch));
                up = false;
            } else {
                b.append(ch);
            }
        }
        return b.toString();
    }

    private static Object normalize(Object v) {
        if (v instanceof java.sql.Date d) return d.toLocalDate().toString();
        if (v instanceof java.sql.Time t) return t.toLocalTime().toString();
        if (v instanceof java.sql.Timestamp t) return t.toLocalDateTime().toString().replace('T', ' ');
        if (v instanceof java.time.LocalDateTime t) return t.toString().replace('T', ' ');
        if (v instanceof BigDecimal b) return b;
        return v;
    }

    private static BigDecimal bd(double v) {
        return Double.isNaN(v) || Double.isInfinite(v) ? BigDecimal.ZERO : BigDecimal.valueOf(v);
    }

    private static void setBd(PreparedStatement ps, int i, Double v) throws SQLException {
        if (v == null || Double.isNaN(v)) ps.setNull(i, java.sql.Types.DECIMAL);
        else ps.setBigDecimal(i, bd(v));
    }

    private static void setDate(PreparedStatement ps, int i, LocalDate d) throws SQLException {
        if (d == null) ps.setNull(i, java.sql.Types.DATE);
        else ps.setDate(i, java.sql.Date.valueOf(d));
    }


}