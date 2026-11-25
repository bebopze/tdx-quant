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
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.mapper.*;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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


    private final QaMarketMidCycleMapper signalMapper;
    private final BaseStockMapper stockMapper;
    private final BaseOptionMapper optionMapper;
    private final ObtIndexKlineMapper indexKlineMapper;


    public OptionBsDataLoader(QaMarketMidCycleMapper signalMapper,
                              BaseStockMapper stockMapper,
                              BaseOptionMapper optionMapper,
                              ObtIndexKlineMapper indexKlineMapper) {
        this.signalMapper = signalMapper;
        this.stockMapper = stockMapper;
        this.optionMapper = optionMapper;
        this.indexKlineMapper = indexKlineMapper;
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


    private void loadSignals(OptionBsData data, OptionBsParams p) {
        List<QaMarketMidCycleDO> rows = signalMapper.listUpToDate(p.getEndDate());
        for (QaMarketMidCycleDO row : rows) {
            LocalDate d = row.getDate();
            int status = row.getMarketMidStatus() == null ? 0 : row.getMarketMidStatus();
            data.statusByDate.put(d, status);

            OptionBsModels.Signal sig = new OptionBsModels.Signal();
            sig.date = d;
            sig.type = status;
            sig.lowDay = row.getMarketLowDay();
            sig.highDay = row.getMarketHighDay();
            sig.ma50Pct = row.getMa50Pct() == null ? null : row.getMa50Pct().doubleValue();
            data.signalMeta.put(d, sig);
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

    private OptionBsModels.Underlying loadEtfFromStock(String code, LocalDate from, LocalDate to) {
        BaseStockDO stock = stockMapper.getByCode(code);
        if (stock == null) {
            log.warn("OptionBsDataLoader - 标的无行情 code={}", code);
            return null;
        }
        List<KlineDTO> bars = parseKline(stock.getKlineHis(), stock.getKlineHisRawLen(), from, to);
        return buildUnderlying(code, stock.getName(), 10000, bars);
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

    private List<KlineDTO> loadCache(String code, LocalDate from, LocalDate to, String source) {
        List<KlineDTO> list = new ArrayList<>();
        List<ObtIndexKlineDO> rows = indexKlineMapper.listByCodeAndRange(code, from, to);
        for (ObtIndexKlineDO e : rows) {
            if (source != null && !source.equals(e.getSource())) continue;
            KlineDTO k = new KlineDTO();
            k.setDate(e.getTradeDate());
            k.setOpen(e.getOpen() == null ? 0 : e.getOpen().doubleValue());
            k.setHigh(e.getHigh() == null ? 0 : e.getHigh().doubleValue());
            k.setLow(e.getLow() == null ? 0 : e.getLow().doubleValue());
            k.setClose(e.getClose() == null ? 0 : e.getClose().doubleValue());
            list.add(k);
        }
        return list;
    }

    private void saveKline(String code, List<KlineDTO> bars, String source) {
        List<ObtIndexKlineDO> rows = new ArrayList<>(bars.size());
        for (KlineDTO k : bars) {
            ObtIndexKlineDO e = new ObtIndexKlineDO();
            e.setCode(code);
            e.setTradeDate(k.getDate());
            e.setOpen(java.math.BigDecimal.valueOf(k.getOpen()));
            e.setHigh(java.math.BigDecimal.valueOf(k.getHigh()));
            e.setLow(java.math.BigDecimal.valueOf(k.getLow()));
            e.setClose(java.math.BigDecimal.valueOf(k.getClose()));
            e.setSource(source);
            rows.add(e);
        }
        for (int i = 0; i < rows.size(); i += 1000) {
            indexKlineMapper.batchUpsert(rows.subList(i, Math.min(rows.size(), i + 1000)));
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


    private void loadContracts(OptionBsData data, OptionBsParams p) {
        List<String> codes = p.getUnderlyingCodes();
        if (codes.isEmpty()) return;

        int total = 0, parse = 0;
        LocalDate kFrom = p.getStartDate().minusDays(5);
        LocalDate kTo = p.getEndDate().plusDays(2);

        List<BaseOptionDO> rows = optionMapper.listForBacktest(codes, p.getStartDate(), p.getEndDate());
        for (BaseOptionDO row : rows) {
            total++;
            byte[] blob = row.getKlineHis();
            if (blob == null) continue;

            List<KlineDTO> bars;
            try {
                bars = parseKline(blob, row.getKlineHisRawLen(), kFrom, kTo);
            } catch (Exception e) {
                continue;
            }
            if (bars.isEmpty()) continue;

            OptionBsModels.Contract ct = new OptionBsModels.Contract();
            ct.code = row.getCode();
            ct.symbol = row.getSymbol();
            ct.name = row.getName();
            ct.callPut = row.getCallPutType();
            ct.strike = row.getStrikePrice() == null ? 0 : row.getStrikePrice().doubleValue();
            ct.multiplier = row.getMultiplier() == null ? 0 : row.getMultiplier();
            ct.underlyingCode = row.getUnderlyingCode();
            ct.underlyingName = row.getUnderlyingName();
            ct.month = row.getContractMonth() == null ? 0 : row.getContractMonth();
            ct.startDate = row.getStartDate();
            ct.expiryDate = row.getEndDate();

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
            parse++;
            if (parse % 2000 == 0) log.info("OptionBsDataLoader - 合约解析 {} / {}", parse, total);
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
