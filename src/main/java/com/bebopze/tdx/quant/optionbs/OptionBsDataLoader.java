package com.bebopze.tdx.quant.optionbs;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import com.bebopze.tdx.quant.common.util.CompressUtil;
import com.bebopze.tdx.quant.common.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 期权 BS 回测 - 数据加载（只加载所需标的/区间，K线解析为原始数组）
 *
 * @author bebopze
 * @date 2026/11/26
 */
@Slf4j
public class OptionBsDataLoader {

    private final OptionBsDb db;

    public OptionBsDataLoader(OptionBsDb db) {
        this.db = db;
    }

    public OptionBsData load(OptionBsParams p) throws Exception {
        OptionBsData data = new OptionBsData();

        LocalDate from = p.getStartDate().minusDays(420);
        LocalDate to = p.getEndDate().plusDays(5);

        // ---------------- 1、信号 ----------------
        loadSignals(data, p);

        // ---------------- 2、标的（ETF / 指数） ----------------
        Set<String> needCodes = new LinkedHashSet<>(p.getUnderlyingCodes());
        needCodes.add(p.getBenchmarkCode()); // 基准
        for (String code : needCodes) {
            if ("000852".equals(code)) {
                OptionBsModels.Underlying idx = loadIndex(code, "中证1000", from, to);
                if (idx != null) data.underlyings.put(code, idx);
            } else {
                OptionBsModels.Underlying u = loadEtf(code, from, to);
                if (u != null) data.underlyings.put(code, u);
            }
        }

        // 000852 拉取失败 -> 从标的集合剔除（不影响其余标的）
        p.getUnderlyingCodes().removeIf(c -> data.underlyings.get(c) == null || data.underlyings.get(c).closes == null
                || data.underlyings.get(c).closes.length == 0);

        data.benchmark = data.underlyings.get(p.getBenchmarkCode());
        if (data.benchmark == null) {
            data.benchmark = data.underlyings.values().stream().findFirst().orElse(null);
        }

        // ---------------- 3、期权合约 ----------------
        loadContracts(data, p);

        // ---------------- 4、交易日历 ----------------
        buildCalendar(data, p);

        log.info("OptionBsDataLoader - 完成 underlyings={} contracts={} calendar={} signals={}",
                 data.underlyings.size(),
                 data.contracts.values().stream().mapToInt(List::size).sum(),
                 data.calendar.size(), data.statusByDate.size());
        return data;
    }


    // ---------------------------------------------------------------------------------------------------------------


    private void loadSignals(OptionBsData data, OptionBsParams p) throws Exception {
        String sql = "SELECT date, market_mid_status, market_low_day, market_high_day, ma50_pct " +
                "FROM qa_market_mid_cycle WHERE date <= ? ORDER BY date";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(p.getEndDate()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate d = rs.getDate(1).toLocalDate();
                    int status = rs.getInt(2);
                    data.statusByDate.put(d, status);

                    Number lowDayN = (Number) rs.getObject(3);
                    Number highDayN = (Number) rs.getObject(4);
                    Integer lowDay = lowDayN == null ? null : lowDayN.intValue();
                    Integer highDay = highDayN == null ? null : highDayN.intValue();
                    java.math.BigDecimal ma50 = rs.getBigDecimal(5);

                    OptionBsModels.Signal s = new OptionBsModels.Signal();
                    s.date = d;
                    s.type = status;
                    s.lowDay = lowDay;
                    s.highDay = highDay;
                    s.ma50Pct = ma50 == null ? null : ma50.doubleValue();
                    data.signalMeta.put(d, s);
                }
            }
        }
        log.info("OptionBsDataLoader - 信号 {} 条", data.statusByDate.size());
    }


    private OptionBsModels.Underlying loadEtf(String code, LocalDate from, LocalDate to) throws Exception {
        // 期权行权价是未复权价格：优先使用“未复权 ETF 日K”缓存/腾讯拉取；
        // base_stock.kline_his 可能是前复权，直接用于内在价值会严重失真（仅作兜底）。
        List<KlineDTO> raw = loadCache(code, from, to, "TENCENT_RAW");
        LocalDate needTo = to.isAfter(LocalDate.now()) ? LocalDate.now() : to;
        boolean covered = raw.size() >= 1000
                && !raw.get(raw.size() - 1).getDate().isBefore(needTo.minusDays(10));
        if (!covered) {
            // 缓存覆盖不足（例如先跑过短区间）：重新拉取并覆盖
            List<KlineDTO> fetched = fetchFromTencent(code, from, to);
            if (!fetched.isEmpty()) {
                raw = fetched;
                saveKline(code, raw, "TENCENT_RAW");
            }
        }
        if (!raw.isEmpty()) {
            return buildUnderlying(code, etfName(code), 10000, raw);
        }
        log.warn("OptionBsDataLoader - {} 未复权行情缺失，回退 base_stock（复权价，行权价可能失真）", code);
        return loadEtfFromStock(code, from, to);
    }

    private OptionBsModels.Underlying loadEtfFromStock(String code, LocalDate from, LocalDate to) throws Exception {
        String sql = "SELECT code, name, kline_his, kline_his_raw_len FROM base_stock WHERE code = ? LIMIT 1";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.warn("OptionBsDataLoader - 标的无行情 code={}", code);
                    return null;
                }
                String name = rs.getString(2);
                byte[] blob = rs.getBytes(3);
                Number rawLenN = (Number) rs.getObject(4);
                Integer rawLen = rawLenN == null ? null : rawLenN.intValue();
                List<KlineDTO> bars = parseKline(blob, rawLen, from, to);
                return buildUnderlying(code, name, 10000, bars);
            }
        }
    }

    private String etfName(String code) {
        return switch (code) {
            case "159915" -> "创业板ETF易方达";
            case "588000" -> "科创50ETF华夏";
            case "510500" -> "中证500ETF南方";
            case "510300" -> "沪深300ETF华泰柏瑞";
            default -> code;
        };
    }


    private OptionBsModels.Underlying loadIndex(String code,
                                                String name,
                                                LocalDate from,
                                                LocalDate to) throws Exception {
        List<KlineDTO> bars = loadCache(code, from, to, null);
        if (bars.isEmpty()) {
            log.info("OptionBsDataLoader - 指数K线缓存为空，开始拉取 code={}", code);
            bars = fetchFromTencent(code, from, to);
            if (bars.isEmpty()) bars = fetchIndexFromEastMoney(code, from, to);
            if (!bars.isEmpty()) saveKline(code, bars, "TENCENT");
        }
        if (bars.isEmpty()) {
            log.warn("OptionBsDataLoader - 指数K线缺失，剔除 code={}", code);
            return null;
        }
        return buildUnderlying(code, name, 100, bars);
    }

    private List<KlineDTO> loadCache(String code, LocalDate from, LocalDate to, String source) throws Exception {
        List<KlineDTO> list = new ArrayList<>();
        String sql = "SELECT trade_date, open, high, low, close FROM obt_index_kline WHERE code = ? AND trade_date BETWEEN ? AND ?" +
                (source == null ? "" : " AND source = ?") + " ORDER BY trade_date";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setDate(2, java.sql.Date.valueOf(from));
            ps.setDate(3, java.sql.Date.valueOf(to));
            if (source != null) ps.setString(4, source);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KlineDTO k = new KlineDTO();
                    k.setDate(rs.getDate(1).toLocalDate());
                    k.setOpen(rs.getBigDecimal(2) == null ? 0 : rs.getBigDecimal(2).doubleValue());
                    k.setHigh(rs.getBigDecimal(3) == null ? 0 : rs.getBigDecimal(3).doubleValue());
                    k.setLow(rs.getBigDecimal(4) == null ? 0 : rs.getBigDecimal(4).doubleValue());
                    k.setClose(rs.getBigDecimal(5) == null ? 0 : rs.getBigDecimal(5).doubleValue());
                    list.add(k);
                }
            }
        }
        return list;
    }

    private void saveKline(String code, List<KlineDTO> bars, String source) throws Exception {
        String sql = "INSERT INTO obt_index_kline(code, trade_date, open, high, low, close, source) " +
                "VALUES(?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE close=VALUES(close),open=VALUES(open),high=VALUES(high),low=VALUES(low),source=VALUES(source)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int n = 0;
            for (KlineDTO k : bars) {
                ps.setString(1, code);
                ps.setDate(2, java.sql.Date.valueOf(k.getDate()));
                ps.setBigDecimal(3, java.math.BigDecimal.valueOf(k.getOpen()));
                ps.setBigDecimal(4, java.math.BigDecimal.valueOf(k.getHigh()));
                ps.setBigDecimal(5, java.math.BigDecimal.valueOf(k.getLow()));
                ps.setBigDecimal(6, java.math.BigDecimal.valueOf(k.getClose()));
                ps.setString(7, source);
                ps.addBatch();
                if (++n % 500 == 0) ps.executeBatch();
            }
            ps.executeBatch();
        }
    }

    private List<KlineDTO> fetchIndexFromEastMoney(String code, LocalDate from, LocalDate to) {
        try {
            // 000852 中证1000：东财 secid=1.000852
            String secid = "1." + code;
            String url = "https://push2his.eastmoney.com/api/qt/stock/kline/get?" +
                    "fields1=f1,f2,f3,f4,f5,f6&fields2=f51,f52,f53,f54,f55,f56,f57" +
                    "&klt=101&fqt=1&beg=0&end=20500101&secid=" + secid;

            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
            headers.put("Referer", "https://quote.eastmoney.com/");
            String body;
            try {
                body = HttpUtil.doGet(url, headers);
            } catch (Exception e1) {
                HttpClient client = HttpClient.newBuilder()
                                              .version(HttpClient.Version.HTTP_1_1)
                                              .connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
                body = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
            }
            JSONObject json = JSON.parseObject(body);
            JSONObject data = json.getJSONObject("data");
            if (data == null) return new ArrayList<>();
            JSONArray klines = data.getJSONArray("klines");
            if (klines == null) return new ArrayList<>();

            List<KlineDTO> list = new ArrayList<>();
            for (int i = 0; i < klines.size(); i++) {
                String[] a = klines.getString(i).split(",");
                if (a.length < 5) continue;
                LocalDate d = LocalDate.parse(a[0]);
                if (d.isBefore(from) || d.isAfter(to)) continue;
                KlineDTO k = new KlineDTO();
                k.setDate(d);
                k.setOpen(Double.parseDouble(a[1]));
                k.setClose(Double.parseDouble(a[2]));
                k.setHigh(Double.parseDouble(a[3]));
                k.setLow(Double.parseDouble(a[4]));
                list.add(k);
            }
            log.info("OptionBsDataLoader - 东财指数K线 code={} size={}", code, list.size());
            if (!list.isEmpty()) return list;
        } catch (Exception e) {
            log.warn("OptionBsDataLoader - 东财指数K线拉取失败 code={} : {}", code, e.getMessage());
        }
        return fetchFromTencent(code, from, to);
    }

    private List<KlineDTO> fetchFromTencent(String code, LocalDate from, LocalDate to) {
        try {
            String symbol = (code.startsWith("159") ? "sz" : "sh") + code;
            String url = "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=" + symbol + ",day,,,2000,";
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0");
            String body = HttpUtil.doGet(url, headers);
            JSONObject json = JSON.parseObject(body);
            JSONObject data = json.getJSONObject("data");
            if (data == null) return new ArrayList<>();
            JSONObject one = data.getJSONObject(symbol);
            if (one == null) return new ArrayList<>();
            JSONArray klines = one.getJSONArray("day");
            if (klines == null) klines = one.getJSONArray("qfqday");
            if (klines == null) return new ArrayList<>();

            List<KlineDTO> list = new ArrayList<>();
            for (int i = 0; i < klines.size(); i++) {
                JSONArray a = klines.getJSONArray(i);
                if (a.size() < 5) continue;
                LocalDate d = LocalDate.parse(a.getString(0));
                if (d.isBefore(from) || d.isAfter(to)) continue;
                KlineDTO k = new KlineDTO();
                k.setDate(d);
                k.setOpen(Double.parseDouble(a.getString(1)));
                k.setClose(Double.parseDouble(a.getString(2)));
                k.setHigh(Double.parseDouble(a.getString(3)));
                k.setLow(Double.parseDouble(a.getString(4)));
                if (a.size() > 5) k.setVol((long) Double.parseDouble(a.getString(5)));
                list.add(k);
            }
            log.info("OptionBsDataLoader - 腾讯K线 code={} size={} first={}", code, list.size(),
                     list.isEmpty() ? null : list.get(0).getDate());
            return list;
        } catch (Exception e) {
            log.warn("OptionBsDataLoader - 腾讯K线拉取失败 code={} : {}", code, e.getMessage());
            return new ArrayList<>();
        }
    }

    private OptionBsModels.Underlying buildUnderlying(String code, String name, int multiplier, List<KlineDTO> bars) {
        if (bars == null || bars.isEmpty()) return null;
        bars.sort(Comparator.comparing(KlineDTO::getDate));

        int n = bars.size();
        OptionBsModels.Underlying u = new OptionBsModels.Underlying();
        u.code = code;
        u.name = name;
        u.multiplier = multiplier;
        u.dates = new LocalDate[n];
        u.closes = new double[n];
        for (int i = 0; i < n; i++) {
            KlineDTO k = bars.get(i);
            u.dates[i] = k.getDate();
            u.closes[i] = k.getClose();
            u.indexMap.put(k.getDate(), i);
        }
        u.ma5 = TdxFun.MA(u.closes, 5);
        u.ma10 = TdxFun.MA(u.closes, 10);
        u.ma20 = TdxFun.MA(u.closes, 20);
        u.ssf = TdxExtFun.SSF(u.closes);
        return u;
    }


    private void loadContracts(OptionBsData data, OptionBsParams p) throws Exception {
        List<String> codes = p.getUnderlyingCodes();
        if (codes.isEmpty()) return;

        StringBuilder in = new StringBuilder();
        for (int i = 0; i < codes.size(); i++) {
            if (i > 0) in.append(',');
            in.append('?');
        }

        String sql = "SELECT code, symbol, name, call_put_type, strike_price, multiplier, underlying_code, underlying_name, " +
                "contract_month, start_date, end_date, kline_his, kline_his_raw_len " +
                "FROM base_option WHERE underlying_code IN (" + in + ") " +
                "AND start_date <= ? AND end_date >= ?";

        int total = 0, parse = 0;
        LocalDate kFrom = p.getStartDate().minusDays(5);
        LocalDate kTo = p.getEndDate().plusDays(2);

        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int idx = 1;
            for (String code : codes) ps.setString(idx++, code);
            ps.setDate(idx++, java.sql.Date.valueOf(p.getEndDate()));
            ps.setDate(idx++, java.sql.Date.valueOf(p.getStartDate()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    total++;
                    byte[] blob = rs.getBytes(12);
                    Number rawLenN = (Number) rs.getObject(13);
                    Integer rawLen = rawLenN == null ? null : rawLenN.intValue();
                    if (blob == null) continue;

                    List<KlineDTO> bars;
                    try {
                        bars = parseKline(blob, rawLen, kFrom, kTo);
                    } catch (Exception e) {
                        continue;
                    }
                    if (bars.isEmpty()) continue;

                    OptionBsModels.Contract ct = new OptionBsModels.Contract();
                    ct.code = rs.getString(1);
                    ct.symbol = rs.getString(2);
                    ct.name = rs.getString(3);
                    ct.callPut = rs.getString(4);
                    ct.strike = rs.getBigDecimal(5) == null ? 0 : rs.getBigDecimal(5).doubleValue();
                    ct.multiplier = rs.getInt(6);
                    ct.underlyingCode = rs.getString(7);
                    ct.underlyingName = rs.getString(8);
                    ct.month = rs.getInt(9);
                    ct.startDate = rs.getDate(10).toLocalDate();
                    ct.expiryDate = rs.getDate(11).toLocalDate();

                    int n = bars.size();
                    ct.dates = new LocalDate[n];
                    ct.closes = new double[n];
                    ct.vols = new long[n];
                    for (int i = 0; i < n; i++) {
                        ct.dates[i] = bars.get(i).getDate();
                        ct.closes[i] = bars.get(i).getClose();
                        ct.vols[i] = bars.get(i).getVol();
                    }

                    data.contracts.computeIfAbsent(ct.underlyingCode, k -> new ArrayList<>()).add(ct);
                    if (data.firstOptionDate == null || ct.startDate.isAfter(data.firstOptionDate)) {
                        // 首个可交易日：取所有合约 start_date 的最小值
                    }
                    parse++;
                    if (parse % 2000 == 0) log.info("OptionBsDataLoader - 合约解析 {} / {}", parse, total);
                }
            }
        }

        for (List<OptionBsModels.Contract> list : data.contracts.values()) {
            list.sort(Comparator.comparingInt((OptionBsModels.Contract x) -> x.month)
                                .thenComparing(x -> x.callPut)
                                .thenComparingDouble(x -> x.strike));
        }
        log.info("OptionBsDataLoader - 合约 {} / 解析 {}", total, parse);
    }


    private List<KlineDTO> parseKline(byte[] blob, Integer rawLen, LocalDate from, LocalDate to) {
        if (blob == null || blob.length == 0) return new ArrayList<>();
        byte[] raw = CompressUtil.zstdDecompress(blob, rawLen);
        if (raw == null || raw.length == 0) return new ArrayList<>();
        String json = new String(raw, StandardCharsets.UTF_8);
        List<KlineDTO> all = ConvertStockKline.str2DTOList(json);
        if (all == null || all.isEmpty()) return new ArrayList<>();
        List<KlineDTO> list = new ArrayList<>(all.size());
        for (KlineDTO k : all) {
            if (k.getDate() == null) continue;
            if (k.getDate().isBefore(from) || k.getDate().isAfter(to)) continue;
            list.add(k);
        }
        return list;
    }


    private void buildCalendar(OptionBsData data, OptionBsParams p) {
        List<LocalDate> cal = new ArrayList<>();
        if (data.benchmark != null && data.benchmark.dates != null) {
            for (LocalDate d : data.benchmark.dates) {
                if (!d.isBefore(p.getStartDate()) && !d.isAfter(p.getEndDate())) cal.add(d);
            }
        }
        if (cal.isEmpty()) {
            Set<LocalDate> set = new LinkedHashSet<>();
            for (OptionBsModels.Underlying u : data.underlyings.values()) {
                if (u.dates == null) continue;
                for (LocalDate d : u.dates) {
                    if (!d.isBefore(p.getStartDate()) && !d.isAfter(p.getEndDate())) set.add(d);
                }
            }
            cal = new ArrayList<>(set);
        }
        cal.sort(Comparator.naturalOrder());
        data.calendar = cal;
    }
}
