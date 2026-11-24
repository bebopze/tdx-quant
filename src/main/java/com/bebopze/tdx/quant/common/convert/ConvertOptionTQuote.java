package com.bebopze.tdx.quant.common.convert;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.option.MonthOptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionQuoteDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;


/**
 * 期权 T型报价 组装器
 *
 * <p>
 * 输入：某日期 存续合约的简单信息（{@link BaseOptionDO}，不含 kline）+ 各合约 kline_his JSON + 各标的ETF 当日K线
 * <br>
 * 输出：按【标的 + 合约月份】分组的 T型报价（认购列表 / 认沽列表 / 行权价对齐列表 / 月度统计 / 派生指标）
 *
 * <p>
 * 数据来源说明：
 * <ul>
 * <li>行情：{@code base_option.kline_his}（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率,流通股本,总股本]）</li>
 * <li>标的价：{@code base_stock.kline_his}（同格式），用于 内在价值/时间价值/平值判定/杠杆</li>
 * </ul>
 *
 * @author: bebopze
 * @date: 2026/11/24
 */
@Slf4j
public class ConvertOptionTQuote {


    private static final BigDecimal ZERO_4 = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100");


    // -----------------------------------------------------------------------------------------------------------------
    //                                             kline_his  <->  List<String>
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * kline_his（JSON 字符串数组）  ->   List<String> klines
     *
     * @param klineHisStr base_option/base_stock 的 kline_his 解压后字符串
     * @return 升序 K 线字符串列表（解析失败 返回空列表）
     */
    public static List<String> str2StrList(String klineHisStr) {
        if (klineHisStr == null || klineHisStr.isEmpty()) {
            return Lists.newArrayList();
        }
        try {
            List<String> klines = JSON.parseArray(klineHisStr, String.class);
            return klines == null ? Lists.newArrayList() : klines;
        } catch (Exception e) {
            log.warn("ConvertOptionTQuote#str2StrList - err     >>>     errMsg : {}", e.getMessage());
            return Lists.newArrayList();
        }
    }


    /**
     * 取 [<= date] 的最近一根 K 线（K 线按日期升序，二分定位）
     *
     * @param klines 升序 K 线字符串列表
     * @param date   目标日期
     * @return 最近一根 K 线；date 之前无任何 K 线 返回 null
     */
    public static KlineDTO barOnOrBefore(List<String> klines, LocalDate date) {
        int idx = indexOnOrBefore(klines, date);
        return idx < 0 ? null : toKline(klines.get(idx));
    }


    /**
     * [<= date] 的最近一根 K 线 的下标（K 线按日期升序，二分定位）
     *
     * @return 下标；无 返回 -1
     */
    public static int indexOnOrBefore(List<String> klines, LocalDate date) {
        if (klines == null || klines.isEmpty() || date == null) {
            return -1;
        }

        String target = date.toString();

        int lo = 0;
        int hi = klines.size() - 1;
        int idx = -1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            String d = subDate(klines.get(mid));

            if (d == null) {
                // 脏数据：跳过（向前收缩）
                hi = mid - 1;
                continue;
            }

            if (d.compareTo(target) <= 0) {
                idx = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return idx;
    }


    /**
     * 取 [from, to] 区间的 K 线
     *
     * @param klines 升序 K 线字符串列表
     * @param from   起始日期（含）
     * @param to     结束日期（含）
     * @return K 线 DTO 列表（升序）
     */
    public static List<KlineDTO> barsBetween(List<String> klines, LocalDate from, LocalDate to) {
        List<KlineDTO> dtoList = Lists.newArrayList();
        if (klines == null || klines.isEmpty() || from == null || to == null) {
            return dtoList;
        }

        String f = from.toString();
        String t = to.toString();

        for (String kline : klines) {
            String d = subDate(kline);
            if (d == null) {
                continue;
            }
            if (d.compareTo(f) < 0 || d.compareTo(t) > 0) {
                continue;
            }

            KlineDTO dto = toKline(kline);
            if (dto != null) {
                dtoList.add(dto);
            }
        }

        return dtoList;
    }


    /**
     * 全部 K 线（升序，容错跳过脏数据）
     */
    public static List<KlineDTO> str2DTOList(String klineHisStr) {
        List<KlineDTO> dtoList = Lists.newArrayList();
        for (String kline : str2StrList(klineHisStr)) {
            KlineDTO dto = toKline(kline);
            if (dto != null) {
                dtoList.add(dto);
            }
        }
        return dtoList;
    }


    /**
     * 单根 K 线字符串 -> KlineDTO（容错）
     */
    public static KlineDTO toKline(String kline) {
        if (kline == null || kline.isEmpty() || kline.length() < 11) {
            return null;
        }
        try {
            return ConvertStockKline.kline2DTO(kline);
        } catch (Exception e) {
            log.warn("ConvertOptionTQuote#toKline - err     >>>     kline : {} , errMsg : {}", kline, e.getMessage());
            return null;
        }
    }

    /**
     * 取 K 线字符串的日期部分（yyyy-MM-dd）
     */
    private static String subDate(String kline) {
        if (kline == null || kline.length() < 10) {
            return null;
        }
        return kline.substring(0, 10);
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  T型报价 组装
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 组装 T型报价（按 标的 + 合约月份 分组）
     *
     * @param date           所选日期
     * @param contractList   该日期 存续合约（简单信息）
     * @param code2KlineJson code -> kline_his 解压后 JSON
     * @param underlying2Bar 标的code -> 标的ETF 当日（或之前最近一根）K线（可为 null）
     * @return 月份分组列表（按 标的code、合约月份 升序）
     */
    /**
     * 组装 T型报价（按 标的 + 合约月份 分组）
     *
     * @param tradeDateList 交易日列表（升序，标的ETF日K日期；用于 到期日 遇节假日 顺延，可为 null）
     */
    public static List<MonthOptionDTO> build(LocalDate date,
                                             List<BaseOptionDO> contractList,
                                             Map<String, String> code2KlineJson,
                                             Map<String, KlineDTO> underlying2Bar,
                                             List<LocalDate> tradeDateList) {

        List<MonthOptionDTO> result = Lists.newArrayList();
        if (contractList == null || contractList.isEmpty() || date == null) {
            return result;
        }

        // ---------------------------------------------------------------- 1、逐合约 取行情 + 分组


        Map<String, List<OptionQuoteDTO>> groupMap = new LinkedHashMap<>();
        Map<String, List<BaseOptionDO>> groupEntityMap = new LinkedHashMap<>();
        Map<String, LocalDate> groupQuoteDateMap = Maps.newHashMap();

        for (BaseOptionDO contract : contractList) {
            String code = contract.getCode();
            if (code == null) {
                continue;
            }

            // 存续校验：date 必须 <= 到期日（到期日 = max(表内end_date, 合约月份第4个星期三 顺延)）
            LocalDate expiryDate = effectiveExpiry(contract, tradeDateList);
            if (expiryDate != null && expiryDate.isBefore(date)) {
                continue;
            }

            String klineHisStr = code2KlineJson.get(code);
            if (klineHisStr == null) {
                continue;
            }

            List<String> klines = str2StrList(klineHisStr);
            int barIdx = indexOnOrBefore(klines, date);
            if (barIdx < 0) {
                // 该合约在 【所选日期之前】 没有任何行情（数据缺口），无法报价 -> 跳过
                continue;
            }

            KlineDTO bar = toKline(klines.get(barIdx));
            if (bar == null) {
                continue;
            }
            KlineDTO prevBar = barIdx > 0 ? toKline(klines.get(barIdx - 1)) : null;

            OptionQuoteDTO quote = toQuote(contract, bar, prevBar, date, expiryDate);

            String groupKey = groupKey(contract);
            groupMap.computeIfAbsent(groupKey, k -> Lists.newArrayList()).add(quote);
            groupEntityMap.computeIfAbsent(groupKey, k -> Lists.newArrayList()).add(contract);

            LocalDate barDate = quote.getBarDate();
            LocalDate old = groupQuoteDateMap.get(groupKey);
            if (old == null || (barDate != null && barDate.isAfter(old))) {
                groupQuoteDateMap.put(groupKey, barDate);
            }
        }


        // ---------------------------------------------------------------- 2、逐分组 组装


        List<String> groupKeyList = new ArrayList<>(groupMap.keySet());
        groupKeyList.sort(Comparator
                                  // 标的code
                                  .comparing((String k) -> groupEntityMap.get(k).get(0).getUnderlyingCode(),
                                             Comparator.nullsLast(Comparator.naturalOrder()))
                                  // 合约月份
                                  .thenComparing(k -> groupEntityMap.get(k).get(0).getContractMonth(),
                                                 Comparator.nullsLast(Comparator.naturalOrder())));

        for (String groupKey : groupKeyList) {
            List<OptionQuoteDTO> quoteList = groupMap.get(groupKey);
            BaseOptionDO sample = groupEntityMap.get(groupKey).get(0);

            List<OptionQuoteDTO> callList = quoteList.stream()
                                                     .filter(e -> "C".equals(e.getCallPutType()))
                                                     .sorted(Comparator.comparing(OptionQuoteDTO::getStrikePrice,
                                                                                  Comparator.nullsLast(Comparator.naturalOrder())))
                                                     .toList();

            List<OptionQuoteDTO> putList = quoteList.stream()
                                                    .filter(e -> "P".equals(e.getCallPutType()))
                                                    .sorted(Comparator.comparing(OptionQuoteDTO::getStrikePrice,
                                                                                 Comparator.nullsLast(Comparator.naturalOrder())))
                                                    .toList();

            MonthOptionDTO month = new MonthOptionDTO();
            month.setKey(groupKey);
            month.setUnderlyingCode(sample.getUnderlyingCode());
            month.setUnderlyingName(sample.getUnderlyingName());
            month.setContractMonth(sample.getContractMonth());
            month.setMonthLabel(monthLabel(sample.getContractMonth()));
            month.setDate(date);

            month.setCallDtoList(callList);
            month.setPutDtoList(putList);
            month.setCallCount(callList.size());
            month.setPutCount(putList.size());

            // 到期日 = 该月 合约 最大 end_date
            LocalDate monthExpiryDate = quoteList.stream().map(OptionQuoteDTO::getExpiryDate)
                                                 .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
            month.setExpiryDate(monthExpiryDate);
            month.setDaysToExpiry(daysBetween(date, monthExpiryDate));

            // 行情日
            LocalDate quoteDate = groupQuoteDateMap.get(groupKey);
            month.setQuoteDate(quoteDate);
            month.setHasData(date.equals(quoteDate));

            // 行权价列表（认购+认沽 去重升序）
            Set<BigDecimal> strikeSet = new TreeSet<>();
            quoteList.stream().map(OptionQuoteDTO::getStrikePrice).filter(Objects::nonNull).forEach(strikeSet::add);
            month.setStrikeList(new ArrayList<>(strikeSet));

            // 标的行情
            String underlyingCode = sample.getUnderlyingCode();
            KlineDTO underlyingBar = underlyingCode == null ? null : underlying2Bar.get(underlyingCode);
            month.setUnderlyingClose(price(underlyingBar == null ? null : underlyingBar.getClose()));
            month.setUnderlyingChangePct(pct(underlyingBar == null ? null : underlyingBar.getChangePct()));
            month.setUnderlyingTradeDate(underlyingBar == null ? null : underlyingBar.getDate());

            // 月度统计
            month.setCallVolume(sumVolume(callList));
            month.setPutVolume(sumVolume(putList));
            month.setCallAmount(sumAmount(callList));
            month.setPutAmount(sumAmount(putList));
            month.setPcrVolume(pcr(month.getPutVolume(), month.getCallVolume()));
            month.setPcrAmount(pcr(month.getPutAmount(), month.getCallAmount()));

            // 平值行权价 + 派生指标
            BigDecimal underlyingClose = month.getUnderlyingClose();
            BigDecimal atmStrike = atmStrike(month.getStrikeList(), underlyingClose);
            month.setAtmStrike(atmStrike);

            fillDerived(quoteList, month.getStrikeList(), atmStrike, underlyingClose);

            result.add(month);
        }

        return result;
    }


    private static String groupKey(BaseOptionDO contract) {
        return contract.getUnderlyingCode() + "-" + contract.getContractMonth();
    }


    /**
     * 单合约 行情 -> 报价（不含派生指标）
     */
    /**
     * 单合约 行情 -> 报价（不含派生指标）
     *
     * <p>
     * 注意：通达信导出的期权 kline_his 中「振幅/涨跌幅/涨跌额」恒为 0、成交额为 0，
     * 因此这里统一用「前一根收盘价」重算 涨跌额/涨跌幅，用 H/L 重算 振幅，保证 历史合约 与 存续合约 口径一致。
     */
    private static OptionQuoteDTO toQuote(BaseOptionDO contract, KlineDTO bar, KlineDTO prevBar,
                                          LocalDate date, LocalDate expiryDate) {
        OptionQuoteDTO quote = new OptionQuoteDTO();

        quote.setCode(contract.getCode());
        quote.setName(contract.getName());
        quote.setCallPutType(contract.getCallPutType());
        quote.setStrikePrice(contract.getStrikePrice());
        quote.setContractMonth(contract.getContractMonth());
        quote.setStartDate(contract.getStartDate());
        quote.setEndDate(contract.getEndDate());
        quote.setExpiryDate(expiryDate);
        quote.setDaysToExpiry(daysBetween(date, expiryDate));

        // ---------------------------------------------------------------- 行情

        quote.setBarDate(bar.getDate());
        quote.setStale(!date.equals(bar.getDate()));

        quote.setOpen(price(bar.getOpen()));
        quote.setHigh(price(bar.getHigh()));
        quote.setLow(price(bar.getLow()));
        quote.setClose(price(bar.getClose()));

        quote.setVolume(bar.getVol());
        quote.setAmount(price2(bar.getAmo()));

        // ---------------- 昨收 / 涨跌 / 振幅（统一重算，兼容 通达信 kline 的 0 值）

        BigDecimal close = quote.getClose();
        BigDecimal prevClose = null;

        if (prevBar != null && !Double.isNaN(prevBar.getClose()) && prevBar.getClose() > 0) {
            prevClose = price(prevBar.getClose());
        } else if (close != null && !Double.isNaN(bar.getChangePrice())) {
            // 首根：用 kline 内的 涨跌额 反推
            prevClose = close.subtract(price(bar.getChangePrice()));
        }

        quote.setPrevClose(prevClose);

        if (close != null && prevClose != null) {
            quote.setChangePrice(close.subtract(prevClose).setScale(4, RoundingMode.HALF_UP));
            if (prevClose.compareTo(BigDecimal.ZERO) > 0 && !Double.isNaN(bar.getChangePct()) && bar.getChangePct() != 0) {
                // kline 已算好 -> 直接复用（历史合约 与我方导入口径一致）
                quote.setChangePct(pct(bar.getChangePct()));
            } else if (prevClose.compareTo(BigDecimal.ZERO) > 0) {
                quote.setChangePct(quote.getChangePrice()
                                        .multiply(HUNDRED)
                                        .divide(prevClose, 2, RoundingMode.HALF_UP));
            }
        } else {
            quote.setChangePrice(price(bar.getChangePrice()));
            quote.setChangePct(pct(bar.getChangePct()));
        }

        // 振幅 = (H/L - 1) * 100
        if (quote.getHigh() != null && quote.getLow() != null
                && quote.getLow().compareTo(BigDecimal.ZERO) > 0) {
            quote.setRangePct(quote.getHigh().subtract(quote.getLow())
                                   .multiply(HUNDRED)
                                   .divide(quote.getLow(), 2, RoundingMode.HALF_UP));
        } else {
            quote.setRangePct(pct(bar.getRangePct()));
        }

        return quote;
    }


    /**
     * 填充 派生指标（内在价值 / 时间价值 / 盈亏平衡 / 杠杆 / 价值状态 / 距平值档数）
     */
    private static void fillDerived(List<OptionQuoteDTO> quoteList,
                                    List<BigDecimal> strikeList,
                                    BigDecimal atmStrike,
                                    BigDecimal underlyingClose) {

        int atmIdx = -1;
        if (strikeList != null && atmStrike != null) {
            atmIdx = strikeList.indexOf(atmStrike);
        }

        for (OptionQuoteDTO quote : quoteList) {
            BigDecimal strike = quote.getStrikePrice();
            BigDecimal close = quote.getClose();

            // ---------------- 平值 / 虚实

            if (underlyingClose != null && strike != null) {
                int cmp = underlyingClose.compareTo(strike);

                boolean itm = "C".equals(quote.getCallPutType()) ? cmp > 0 : cmp < 0;
                boolean atm = strike.compareTo(atmStrike) == 0;

                quote.setMoneyness(atm ? "ATM" : (itm ? "ITM" : "OTM"));

                if (atmIdx >= 0) {
                    int strikeIdx = strikeList.indexOf(strike);
                    if (strikeIdx >= 0) {
                        int diff = strikeIdx - atmIdx;
                        // 认购：行权价 > 平值 -> 虚值（正数）；认沽：行权价 < 平值 -> 虚值（正数）
                        quote.setOtmLevel("C".equals(quote.getCallPutType()) ? diff : -diff);
                    }
                }
            }

            // ---------------- 内在价值 / 时间价值 / 盈亏平衡 / 杠杆

            if (underlyingClose != null && strike != null) {
                BigDecimal intrinsic = "C".equals(quote.getCallPutType())
                        ? underlyingClose.subtract(strike).max(BigDecimal.ZERO)
                        : strike.subtract(underlyingClose).max(BigDecimal.ZERO);

                quote.setIntrinsicValue(intrinsic.setScale(4, RoundingMode.HALF_UP));

                if (close != null) {
                    quote.setTimeValue(close.subtract(intrinsic).setScale(4, RoundingMode.HALF_UP));

                    quote.setBreakEven(("C".equals(quote.getCallPutType())
                            ? strike.add(close)
                            : strike.subtract(close)).setScale(4, RoundingMode.HALF_UP));

                    if (close.compareTo(BigDecimal.ZERO) > 0) {
                        quote.setLeverage(underlyingClose.divide(close, 4, RoundingMode.HALF_UP));
                    }
                }
            }
        }
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  小工具
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 合约月份 的 第 4 个星期三（ETF期权 标准到期日）
     *
     * @param contractMonth yyyyMM
     * @return 第4个星期三
     */
    public static LocalDate fourthWednesday(int contractMonth) {
        int year = contractMonth / 100;
        int month = contractMonth % 100;

        LocalDate first = LocalDate.of(year, month, 1);
        // 1=Mon ... 7=Sun；星期三 = 3
        int offset = (3 - first.getDayOfWeek().getValue() + 7) % 7;
        LocalDate firstWednesday = first.plusDays(offset);

        return firstWednesday.plusWeeks(3);
    }


    /**
     * 到期日推导：合约月份 第 4 个星期三；若该日非交易日（法定节假日）则顺延到下一个交易日
     *
     * @param contractMonth yyyyMM
     * @param tradeDateList 交易日列表（升序；null/空 或 未覆盖该月份 -> 直接返回第4个星期三）
     * @return 推导到期日
     */
    public static LocalDate derivedExpiry(int contractMonth, List<LocalDate> tradeDateList) {

        LocalDate fourthWednesday = fourthWednesday(contractMonth);

        if (tradeDateList == null || tradeDateList.isEmpty()) {
            return fourthWednesday;
        }

        // 交易日历未覆盖该月份（未来月份） -> 不做顺延
        LocalDate maxTradeDate = tradeDateList.get(tradeDateList.size() - 1);
        if (fourthWednesday.isAfter(maxTradeDate)) {
            return fourthWednesday;
        }

        LocalDate expiry = fourthWednesday;
        for (int i = 0; i < 10; i++) {
            if (Collections.binarySearch(tradeDateList, expiry) >= 0) {
                return expiry;
            }
            expiry = expiry.plusDays(1);
        }
        return fourthWednesday;
    }


    /**
     * 有效到期日 = max(表内 end_date, 合约月份 第4个星期三 顺延)
     *
     * <p>
     * 背景：现网 {@code KlineServiceImpl#convertOptionDO} 会把 存续合约 的 end_date 覆写成「最后一根K线日期」，
     * 导致 `date <= end_date` 的存续判定失效（所有存续合约的 end_date 都是同一个数据更新日）。
     * 这里用「合约月份 第4个星期三」兜底，使 存续判定 与 到期日展示 不依赖该字段的正确性。
     *
     * @param contract      合约
     * @param tradeDateList 交易日列表
     * @return 有效到期日
     */
    public static LocalDate effectiveExpiry(BaseOptionDO contract, List<LocalDate> tradeDateList) {

        LocalDate endDate = contract == null ? null : contract.getEndDate();
        Integer contractMonth = contract == null ? null : contract.getContractMonth();

        if (contractMonth == null) {
            return endDate;
        }

        LocalDate derived = derivedExpiry(contractMonth, tradeDateList);

        if (endDate == null) {
            return derived;
        }
        return endDate.isAfter(derived) ? endDate : derived;
    }


    /**
     * K线 浅拷贝（用于「就地重算涨跌」而不污染缓存中的原始数据）
     */
    public static KlineDTO copyOf(KlineDTO src) {
        if (src == null) {
            return null;
        }
        KlineDTO dto = new KlineDTO();
        dto.setDate(src.getDate());
        dto.setOpen(src.getOpen());
        dto.setHigh(src.getHigh());
        dto.setLow(src.getLow());
        dto.setClose(src.getClose());
        dto.setVol(src.getVol());
        dto.setAmo(src.getAmo());
        dto.setRangePct(src.getRangePct());
        dto.setChangePct(src.getChangePct());
        dto.setChangePrice(src.getChangePrice());
        dto.setTurnoverPct(src.getTurnoverPct());
        dto.setLtgb(src.getLtgb());
        dto.setZgb(src.getZgb());
        return dto;
    }


    /**
     * 就地重算 涨跌额/涨跌幅/振幅
     *
     * <p>
     * 通达信导出的 kline_his 中「振幅/涨跌幅/涨跌额」恒为 0，这里统一用「前一根收盘」「H/L」重算，
     * 保证 标的ETF 涨跌幅、期权 涨跌幅 口径一致。
     *
     * @param bar     待处理 K线（会被修改）
     * @param prevBar 上一根 K线（可为 null）
     */
    public static void normalizeChange(KlineDTO bar, KlineDTO prevBar) {

        if (bar == null) {
            return;
        }

        double close = bar.getClose();
        if (Double.isNaN(close)) {
            return;
        }

        Double prevClose = null;
        if (prevBar != null && !Double.isNaN(prevBar.getClose()) && prevBar.getClose() > 0) {
            prevClose = prevBar.getClose();
        } else if (!Double.isNaN(bar.getChangePrice())) {
            prevClose = close - bar.getChangePrice();
        }

        if (prevClose != null && prevClose > 0) {
            bar.setChangePrice(round(close - prevClose, 4));
            bar.setChangePct(round((close / prevClose - 1) * 100, 2));
        }

        double high = bar.getHigh();
        double low = bar.getLow();
        if (!Double.isNaN(high) && !Double.isNaN(low) && low > 0) {
            bar.setRangePct(round((high / low - 1) * 100, 2));
        }
    }


    private static double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }


    public static String monthLabel(Integer contractMonth) {
        if (contractMonth == null) {
            return null;
        }
        return String.format("%04d-%02d", contractMonth / 100, contractMonth % 100);
    }

    public static Integer daysBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(from, to);
        return (int) Math.max(0, days);
    }

    private static BigDecimal price(Double val) {
        return num(val, 4);
    }

    private static BigDecimal price2(Double val) {
        return num(val, 2);
    }

    private static BigDecimal pct(Double val) {
        return num(val, 2);
    }

    private static BigDecimal num(Double val, int scale) {
        if (val == null || Double.isNaN(val) || Double.isInfinite(val)) {
            return null;
        }
        return BigDecimal.valueOf(val).setScale(scale, RoundingMode.HALF_UP);
    }

    private static Long sumVolume(List<OptionQuoteDTO> list) {
        long sum = 0;
        for (OptionQuoteDTO e : list) {
            if (e.getVolume() != null) {
                sum += e.getVolume();
            }
        }
        return sum;
    }

    private static BigDecimal sumAmount(List<OptionQuoteDTO> list) {
        BigDecimal sum = BigDecimal.ZERO;
        for (OptionQuoteDTO e : list) {
            if (e.getAmount() != null) {
                sum = sum.add(e.getAmount());
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal pcr(Number put, Number call) {
        if (put == null || call == null || call.doubleValue() <= 0) {
            return null;
        }
        return BigDecimal.valueOf(put.doubleValue())
                         .divide(BigDecimal.valueOf(call.doubleValue()), 4, RoundingMode.HALF_UP);
    }

    /**
     * 平值行权价：最接近标的价 的行权价
     */
    private static BigDecimal atmStrike(List<BigDecimal> strikeList, BigDecimal underlyingClose) {
        if (strikeList == null || strikeList.isEmpty() || underlyingClose == null) {
            return null;
        }
        BigDecimal best = null;
        BigDecimal bestDiff = null;

        for (BigDecimal strike : strikeList) {
            BigDecimal diff = strike.subtract(underlyingClose).abs();
            if (bestDiff == null || diff.compareTo(bestDiff) < 0) {
                bestDiff = diff;
                best = strike;
            }
        }
        return best;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        // 单元自测（不依赖 DB）：2025-05-13,21.06,21.45,21.97,20.89,8455131,18181107751.03,5.18,2.98,0.62,6.33
        List<String> klines = Lists.newArrayList(
                "2026-10-09,0.0700,0.0750,0.0690,0.0714,16822,12345678.00,8.57,0.71,0.0005,0.00,0,0",
                "2026-10-10,0.0714,0.0720,0.0700,0.0710,100,9999.00,2.80,-0.56,-0.0004,0.00,0,0"
        );

        System.out.println("barOnOrBefore(2026-10-09) = " + barOnOrBefore(klines, LocalDate.of(2026, 10, 9)));
        System.out.println("barOnOrBefore(2026-10-10) = " + barOnOrBefore(klines, LocalDate.of(2026, 10, 10)));
        System.out.println("barOnOrBefore(2026-10-01) = " + barOnOrBefore(klines, LocalDate.of(2026, 10, 1)));
        System.out.println("barOnOrBefore(2026-12-01) = " + barOnOrBefore(klines, LocalDate.of(2026, 12, 1)));

        System.out.println("barsBetween(10-09, 10-10)  = " + barsBetween(klines, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 31)).size());

        System.out.println("monthLabel(202612) = " + monthLabel(202612));
        System.out.println("daysBetween = " + daysBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 28)));

        // 组装自测
        BaseOptionDO call = new BaseOptionDO();
        call.setCode("10000001");
        call.setName("50ETF购12月2800");
        call.setCallPutType("C");
        call.setStrikePrice(new BigDecimal("2.8"));
        call.setContractMonth(202612);
        call.setUnderlyingCode("510050");
        call.setUnderlyingName("50ETF");
        call.setStartDate(LocalDate.of(2026, 4, 23));
        call.setEndDate(LocalDate.of(2026, 12, 23));

        BaseOptionDO put = new BaseOptionDO();
        put.setCode("10000002");
        put.setName("50ETF沽12月2800");
        put.setCallPutType("P");
        put.setStrikePrice(new BigDecimal("2.8"));
        put.setContractMonth(202612);
        put.setUnderlyingCode("510050");
        put.setUnderlyingName("50ETF");
        put.setEndDate(LocalDate.of(2026, 12, 23));

        Map<String, String> jsonMap = Maps.newHashMap();
        jsonMap.put("10000001", JSON.toJSONString(klines));
        jsonMap.put("10000002", JSON.toJSONString(klines));

        KlineDTO under = new KlineDTO();
        under.setDate(LocalDate.of(2026, 10, 9));
        under.setClose(2.9);

        Map<String, KlineDTO> underMap = Maps.newHashMap();
        underMap.put("510050", under);

        System.out.println("fourthWednesday(202612) = " + fourthWednesday(202612));
        System.out.println("fourthWednesday(202001) = " + fourthWednesday(202001));

        List<MonthOptionDTO> months = build(LocalDate.of(2026, 10, 9), Lists.newArrayList(call, put), jsonMap, underMap, null);
        for (MonthOptionDTO m : months) {
            System.out.println(JSON.toJSONString(m));
        }
    }

}
