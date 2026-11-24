package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.convert.ConvertOptionTQuote;
import com.bebopze.tdx.quant.common.domain.dto.option.MonthOptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionEtfInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionKlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionUnderlyingDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionUnderlyingKlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseOptionService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.OptionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * 期权
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
@Slf4j
@Service
public class OptionServiceImpl implements OptionService {


    /**
     * 默认标的（无参时 用 300ETF 的交易日历）
     */
    private static final String DEFAULT_UNDERLYING_CODE = "510300";

    /**
     * 标的ETF K线 缺失时的备选（按顺序取第一个有数据的）
     */
    private static final List<String> FALLBACK_UNDERLYING_CODE_LIST = Lists.newArrayList("510300", "510050", "159915");


    /**
     * T型报价 缓存（同一 date+underlyingCode 60s 内复用；历史日期不变、当日盘中 60s 刷新）
     */
    private final Cache<String, List<MonthOptionDTO>> tQuoteCache = Caffeine.newBuilder()
                                                                            .maximumSize(256)
                                                                            .expireAfterWrite(Duration.ofSeconds(60))
                                                                            .build();

    /**
     * 标的ETF 日K 缓存（5min；用于 内在价值/平值/杠杆 与 标的K线图）
     */
    private final Cache<String, List<KlineDTO>> etfKlineCache = Caffeine.newBuilder()
                                                                        .maximumSize(32)
                                                                        .expireAfterWrite(Duration.ofMinutes(5))
                                                                        .build();


    @Autowired
    private IBaseOptionService baseOptionService;

    @Autowired
    private IBaseStockService baseStockService;


    // -----------------------------------------------------------------------------------------------------------------
    //                                                T型报价
    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public List<MonthOptionDTO> listOptionInDate(LocalDate date) {
        return listOptionInDate(null, date);
    }


    @Override
    public List<MonthOptionDTO> listOptionInDate(String underlyingCode, LocalDate date) {

        LocalDate queryDate = date == null ? LocalDate.now() : date;
        String underlyingCodeVal = StringUtils.hasText(underlyingCode) ? underlyingCode.trim() : null;

        String cacheKey = queryDate + "|" + underlyingCodeVal;

        List<MonthOptionDTO> cached = tQuoteCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }


        // ---------------------------------------------------------------- 1、该日期 存续合约

        List<BaseOptionDO> contractList = baseOptionService.listOptionInDate(queryDate, underlyingCodeVal);
        if (CollectionUtils.isEmpty(contractList)) {
            log.info("listOptionInDate - empty     >>>     date : {} , underlyingCode : {}", queryDate, underlyingCodeVal);
            tQuoteCache.put(cacheKey, Lists.newArrayList());
            return Lists.newArrayList();
        }


        // ---------------------------------------------------------------- 2、各合约 kline_his

        Map<String, String> code2KlineJson = baseOptionService.klineHisMapInDate(queryDate, underlyingCodeVal);


        // ---------------------------------------------------------------- 3、各标的 当日行情

        Map<String, KlineDTO> underlying2Bar = new HashMap<>(8);
        contractList.stream()
                    .map(BaseOptionDO::getUnderlyingCode)
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(code -> underlying2Bar.put(code, etfBarOnOrBefore(code, queryDate)));


        // ---------------------------------------------------------------- 4、交易日历（到期日 顺延 用）

        List<LocalDate> tradeDateList = tradeDates(underlyingCodeVal, null, null);


        // ---------------------------------------------------------------- 5、组装

        List<MonthOptionDTO> result = ConvertOptionTQuote.build(queryDate, contractList, code2KlineJson,
                                                                underlying2Bar, tradeDateList);

        tQuoteCache.put(cacheKey, result);

        log.info("listOptionInDate - suc     >>>     date : {} , underlyingCode : {} , contractSize : {} , klineSize : {} , monthSize : {}",
                 queryDate, underlyingCodeVal, contractList.size(), code2KlineJson.size(), result.size());

        return result;
    }


    @Override
    public List<OptionUnderlyingDTO> listUnderlyingInDate(LocalDate date) {

        LocalDate queryDate = date == null ? LocalDate.now() : date;

        // 1、该日期 存续合约（含 end_date 兜底 的 精确存续判定）
        List<BaseOptionDO> contractList = baseOptionService.listOptionInDate(queryDate, null);
        if (CollectionUtils.isEmpty(contractList)) {
            return Lists.newArrayList();
        }

        List<LocalDate> tradeDateList = tradeDates(null, null, null);

        Map<String, List<BaseOptionDO>> underlying_list_map = new LinkedHashMap<>();
        for (BaseOptionDO contract : contractList) {
            String underlyingCode = contract.getUnderlyingCode();
            if (!StringUtils.hasText(underlyingCode)) {
                continue;
            }

            LocalDate expiryDate = ConvertOptionTQuote.effectiveExpiry(contract, tradeDateList);
            if (expiryDate != null && expiryDate.isBefore(queryDate)) {
                continue;
            }

            underlying_list_map.computeIfAbsent(underlyingCode, k -> new ArrayList<>()).add(contract);
        }


        // 2、逐标的 组装
        List<OptionUnderlyingDTO> dtoList = new ArrayList<>();
        for (Map.Entry<String, List<BaseOptionDO>> entry : underlying_list_map.entrySet()) {

            String underlyingCode = entry.getKey();
            List<BaseOptionDO> list = entry.getValue();

            OptionUnderlyingDTO dto = new OptionUnderlyingDTO();
            dto.setUnderlyingCode(underlyingCode);
            dto.setUnderlyingName(list.get(0).getUnderlyingName());
            dto.setContractCount(list.size());
            dto.setMonthCount((int) list.stream().map(BaseOptionDO::getContractMonth).filter(Objects::nonNull).distinct().count());

            dto.setStartDate(list.stream().map(BaseOptionDO::getStartDate).filter(Objects::nonNull)
                                 .min(LocalDate::compareTo).orElse(null));
            dto.setEndDate(list.stream().map(c -> ConvertOptionTQuote.effectiveExpiry(c, tradeDateList))
                               .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null));

            // 标的ETF 行情
            KlineDTO bar = etfBarOnOrBefore(underlyingCode, queryDate);
            if (bar == null) {
                dto.setMissingEtfKline(true);
            } else {
                dto.setMissingEtfKline(false);
                dto.setTradeDate(bar.getDate());
                dto.setClose(price(bar.getClose(), 4));
                dto.setChangePrice(price(bar.getChangePrice(), 4));
                dto.setChangePct(price(bar.getChangePct(), 2));

                if (dto.getClose() != null && dto.getChangePrice() != null) {
                    dto.setPrevClose(dto.getClose().subtract(dto.getChangePrice()));
                }
            }

            BaseStockDO stock = getStock(underlyingCode);
            dto.setEtfName(stock == null ? null : stock.getName());

            dtoList.add(dto);
        }

        // 排序：合约数 降序，其次 code 升序
        dtoList.sort(Comparator.comparing(OptionUnderlyingDTO::getContractCount, Comparator.reverseOrder())
                               .thenComparing(OptionUnderlyingDTO::getUnderlyingCode));

        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                合约 / 标的 K线
    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public OptionKlineDTO optionKline(String optionCode) {

        BaseOptionDO entity = baseOptionService.getByCode(optionCode);

        OptionKlineDTO dto = new OptionKlineDTO();
        if (entity == null) {
            return dto;
        }

        BeanUtils.copyProperties(entity, dto);

        List<KlineDTO> klineList = ConvertOptionTQuote.str2DTOList(entity.getKlineHisStr());

        dto.setKlineList(klineList);
        dto.setBarCount(klineList.size());

        return dto;
    }


    @Override
    public OptionUnderlyingKlineDTO underlyingKline(String underlyingCode, LocalDate date, Integer months) {

        LocalDate queryDate = date == null ? LocalDate.now() : date;
        int monthVal = (months == null || months <= 0) ? 3 : months;

        LocalDate from = queryDate.minusMonths(monthVal);
        LocalDate to = queryDate.plusMonths(monthVal);

        OptionUnderlyingKlineDTO dto = new OptionUnderlyingKlineDTO();
        dto.setUnderlyingCode(underlyingCode);
        dto.setDate(queryDate);
        dto.setMonths(monthVal);
        dto.setFrom(from);
        dto.setTo(to);

        BaseStockDO stock = getStock(underlyingCode);
        if (stock != null) {
            dto.setEtfName(stock.getName());
        }

        // 标的name 取 期权表口径（如 300ETF）
        String underlyingName = optionUnderlyingName(underlyingCode);
        dto.setUnderlyingName(underlyingName);

        List<KlineDTO> allBars = etfBars(underlyingCode);
        if (CollectionUtils.isEmpty(allBars)) {
            dto.setKlineList(Lists.newArrayList());
            dto.setBarCount(0);
            return dto;
        }

        List<KlineDTO> barList = new ArrayList<>();
        for (KlineDTO bar : allBars) {
            if (bar.getDate() == null) {
                continue;
            }
            if (bar.getDate().isBefore(from) || bar.getDate().isAfter(to)) {
                continue;
            }
            barList.add(bar);
        }

        dto.setKlineList(barList);
        dto.setBarCount(barList.size());

        KlineDTO quoteBar = etfBarOnOrBefore(underlyingCode, queryDate);
        if (quoteBar != null) {
            dto.setQuoteDate(quoteBar.getDate());
            dto.setClose(price(quoteBar.getClose(), 4));
            dto.setChangePrice(price(quoteBar.getChangePrice(), 4));
            dto.setChangePct(price(quoteBar.getChangePct(), 2));
            if (dto.getClose() != null && dto.getChangePrice() != null) {
                dto.setPrevClose(dto.getClose().subtract(dto.getChangePrice()));
            }
        }

        return dto;
    }


    @Override
    public List<LocalDate> tradeDates(String underlyingCode, LocalDate from, LocalDate to) {

        String codeVal = StringUtils.hasText(underlyingCode) ? underlyingCode.trim() : null;

        List<LocalDate> dateList = etfBars(codeVal).stream()
                                                   .map(KlineDTO::getDate)
                                                   .filter(Objects::nonNull)
                                                   .filter(d -> from == null || !d.isBefore(from))
                                                   .filter(d -> to == null || !d.isAfter(to))
                                                   .sorted()
                                                   .toList();

        if (!dateList.isEmpty()) {
            return dateList;
        }

        // 该标的无 ETF 日K -> 回退
        for (String fallbackCode : FALLBACK_UNDERLYING_CODE_LIST) {
            if (Objects.equals(fallbackCode, codeVal)) {
                continue;
            }
            List<LocalDate> fallbackList = etfBars(fallbackCode).stream()
                                                                .map(KlineDTO::getDate)
                                                                .filter(Objects::nonNull)
                                                                .filter(d -> from == null || !d.isBefore(from))
                                                                .filter(d -> to == null || !d.isAfter(to))
                                                                .sorted()
                                                                .toList();
            if (!fallbackList.isEmpty()) {
                return fallbackList;
            }
        }

        return Lists.newArrayList();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public OptionDTO info(String optionCode) {

        BaseOptionDO entity = baseOptionService.getByCode(optionCode);

        OptionDTO dto = new OptionDTO();
        if (entity != null) {
            BeanUtils.copyProperties(entity, dto);
        }

        return dto;
    }


    @Override
    public OptionEtfInfoDTO optionEtfInfo(String optionCode) {

        return null;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  内部工具
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 标的ETF 全量日K（缓存；解析失败/缺失 返回空列表）
     */
    private List<KlineDTO> etfBars(String underlyingCode) {

        if (!StringUtils.hasText(underlyingCode)) {
            return Lists.newArrayList();
        }

        List<KlineDTO> barList = etfKlineCache.get(underlyingCode, code -> {
            BaseStockDO stock = getStock(code);
            if (stock == null) {
                return new ArrayList<KlineDTO>();
            }
            return ConvertOptionTQuote.str2DTOList(stock.getKlineHisStr());
        });

        return barList == null ? Lists.newArrayList() : barList;
    }


    /**
     * 标的ETF 在 date 当日（或之前最近一根） 的日K（已重算 涨跌额/涨跌幅/振幅）
     *
     * <p>
     * 通达信导出的 kline_his 中 振幅/涨跌幅/涨跌额 恒为 0，这里用「前一根收盘」重算。
     */
    private KlineDTO etfBarOnOrBefore(String underlyingCode, LocalDate date) {

        List<KlineDTO> barList = etfBars(underlyingCode);
        int idx = indexBarOnOrBefore(barList, date);
        if (idx < 0) {
            return null;
        }

        KlineDTO bar = ConvertOptionTQuote.copyOf(barList.get(idx));
        ConvertOptionTQuote.normalizeChange(bar, idx > 0 ? barList.get(idx - 1) : null);
        return bar;
    }


    /**
     * 升序 K 线列表 -> [<= date] 的最近一根 的下标（二分）
     */
    private static int indexBarOnOrBefore(List<KlineDTO> barList, LocalDate date) {

        if (CollectionUtils.isEmpty(barList) || date == null) {
            return -1;
        }

        int lo = 0;
        int hi = barList.size() - 1;
        int idx = -1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            LocalDate midDate = barList.get(mid).getDate();

            if (midDate == null) {
                hi = mid - 1;
                continue;
            }

            if (!midDate.isAfter(date)) {
                idx = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return idx;
    }


    private BaseStockDO getStock(String code) {
        try {
            return StringUtils.hasText(code) ? baseStockService.getByCode(code) : null;
        } catch (Exception e) {
            log.warn("OptionServiceImpl#getStock - err     >>>     code : {} , errMsg : {}", code, e.getMessage());
            return null;
        }
    }


    /**
     * 期权表口径 的 标的name（如 300ETF）；无合约时 回退 ETF 全名
     */
    private String optionUnderlyingName(String underlyingCode) {

        if (!StringUtils.hasText(underlyingCode)) {
            return null;
        }

        try {
            return baseOptionService.getUnderlyingName(underlyingCode);
        } catch (Exception e) {
            log.warn("OptionServiceImpl#optionUnderlyingName - err     >>>     code : {} , errMsg : {}",
                     underlyingCode, e.getMessage());
        }

        return null;
    }


    private static BigDecimal price(Double val, int scale) {
        if (val == null || Double.isNaN(val) || Double.isInfinite(val)) {
            return null;
        }
        return BigDecimal.valueOf(val).setScale(scale, RoundingMode.HALF_UP);
    }

}
