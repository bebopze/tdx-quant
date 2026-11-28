package com.bebopze.tdx.quant.optionbs;

import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.constant.option.UnderlyingCodeConst;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import com.bebopze.tdx.quant.common.util.CompressUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.mapper.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
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
    private final BaseOptionMapper optionMapper;
    private final BaseOptionUnderlyingMapper underlyingMapper;


    public OptionBsDataLoader(QaMarketMidCycleMapper signalMapper,
                              BaseOptionMapper optionMapper,
                              BaseOptionUnderlyingMapper underlyingMapper) {
        this.signalMapper = signalMapper;
        this.optionMapper = optionMapper;
        this.underlyingMapper = underlyingMapper;
    }


    public OptionBsData load(OptionBsParams p) throws Exception {
        OptionBsData data = new OptionBsData();

        LocalDate from = p.getStartDate().minusDays(420);
        LocalDate to = p.getEndDate().plusDays(5);

        // ---------------- 1、信号 ----------------
        loadSignals(data, p);

        // ---------------- 2、标的（ETF / 指数） ----------------
        // 统一使用 base_option_underlying 的代码：IF300/IH50/IM1000。
        p.setUnderlyingCodes(new ArrayList<>(p.getUnderlyingCodes().stream()
                                                 .map(UnderlyingCodeConst::toUnderlyingCode)
                                                 .distinct().toList()));
        p.setBenchmarkCode(UnderlyingCodeConst.toUnderlyingCode(p.getBenchmarkCode()));

        Set<String> needCodes = new LinkedHashSet<>(p.getUnderlyingCodes());
        needCodes.add(p.getBenchmarkCode()); // 基准
        for (String code : needCodes) {
            OptionBsModels.Underlying u = loadUnderlying(code, from, to);
            if (u != null) data.underlyings.put(code, u);
        }

        // 标的缺失 -> 从标的集合剔除（不影响其余标的）
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
            data.bullBearByDate.put(d, row.getMarketBullBearStatus());

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


    /**
     * 期权标的行情统一从 base_option_underlying 读取。
     * 期权标的行情统一使用 IF300/IH50/IM1000。
     */
    private OptionBsModels.Underlying loadUnderlying(String code, LocalDate from, LocalDate to) {
        String underlyingCode = UnderlyingCodeConst.toUnderlyingCode(code);
        BaseOptionUnderlyingDO row = underlyingMapper.getByCode(underlyingCode);
        if (row == null) {
            log.warn("OptionBsDataLoader - base_option_underlying 无行情 code={} alias={}", code, underlyingCode);
            return null;
        }

        List<KlineDTO> bars = parseKline(row.getKlineHis(), row.getKlineHisRawLen(), from, to);
        if (bars.isEmpty()) {
            log.warn("OptionBsDataLoader - base_option_underlying K线为空 code={} alias={}", code, underlyingCode);
            return null;
        }

        int multiplier = row.getMultiplier() == null ? 0 : row.getMultiplier();
        return buildUnderlying(code, row.getName(), multiplier, bars);
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
