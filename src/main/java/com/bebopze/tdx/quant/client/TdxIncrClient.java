package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.TdxFormatCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;


/**
 * TQ 批量日线。日期取自行情，禁止把本机日期赋给无日期的快照。
 *
 * @author bebopze
 * @date 2026/11/15
 */
@Slf4j
@Component
public class TdxIncrClient {


    protected static JSONObject call(String method, Map<String, Object> params) {
        return TdxTqHttpClient.call(method, params);
    }


    public List<LocalDate> tradingDates(LocalDate end, int count) {
        Assert.isTrue(count > 0, "TQ 交易日数量必须大于 0");


        JSONObject result = call("get_trading_dates", Map.of("market", "SH",
                                                             "start_time", "",
                                                             "end_time", DateTimeUtil.format_yyyyMMdd(end),
                                                             "count", count));

        JSONArray values = result.getJSONArray("Value");
        Assert.notEmpty(values, "TQ 交易日历为空，请下载上证指数盘后数据");


        return values.stream()
                     .map(v -> DateTimeUtil.parseDate_yyyyMMdd(v.toString()))
                     .filter(d -> !d.isAfter(end))
                     .distinct()
                     .sorted()
                     .toList();
    }


    /**
     * 包含区间前一个交易日，用于昨收及复权口径校验；fill_data=false 不制造停牌 K 线。
     */
    public Map<String, List<KlineDTO>> dailyBars(List<String> codes, LocalDate start, LocalDate end) {
        if (CollectionUtils.isEmpty(codes)) {
            return Map.of();
        }

        Assert.isTrue(start != null && end != null && !start.isAfter(end), "TQ 日线日期区间非法");


        JSONObject response = call("get_market_data",

                                   Map.of("stock_list", TdxFormatCodeUtil.formatCodes(codes),
                                          "field_list", List.of(),
                                          "period", "1d",
                                          "start_time", DateTimeUtil.format_yyyyMMdd(start),
                                          "end_time", DateTimeUtil.format_yyyyMMdd(end),
                                          // TDX 本地日线为【不复权】；增量必须保持同一复权口径，否则会污染历史序列
                                          "count", 0,
                                          "dividend_type", "none",
                                          "fill_data", false));


        // TdxTqAPI.get_market_data(codes, PeriodEnum.DAY, start, end);

        return parseDailyBars(response, codes, start, end);
    }


    static Map<String, List<KlineDTO>> parseDailyBars(JSONObject response, List<String> requested,
                                                      LocalDate start, LocalDate end) {

        JSONObject values = response.containsKey("Value") ? response.getJSONObject("Value") : response;
        Assert.notNull(values, "TQ 日线响应缺少 Value");


        Map<String, List<KlineDTO>> result = new LinkedHashMap<>();

        for (String raw : requested) {

            String code = TdxFormatCodeUtil.formatCode(raw);

            // 停牌/无数据/响应缺单只：该只返回空，不拖垮整批
            if (!values.containsKey(code) && !values.containsKey(raw)) {
                log.warn("TQ 日线响应缺少证券，跳过：{}", code);
                result.put(raw, List.of());
                continue;
            }


            try {
                JSONObject data = values.getJSONObject(values.containsKey(code) ? code : raw);
                List<KlineDTO> bars = new ArrayList<>();
                if (data == null || data.isEmpty()) {
                    result.put(raw, bars);
                    continue;
                }


                JSONArray dates = data.getJSONArray("Date");
                JSONArray closes = data.getJSONArray("Close");
                if (dates == null || closes == null) {
                    throw new IllegalStateException("TQ 日线字段不完整：" + code);
                }


                for (String field : List.of("Date", "Open", "High", "Low", "Close", "Volume", "Amount")) {
                    JSONArray array = data.getJSONArray(field);
                    if (array == null || array.size() != dates.size()) {
                        throw new IllegalStateException("TQ 日线数组长度不一致：" + code + "/" + field);
                    }
                }


                for (int i = 0; i < dates.size(); i++) {
                    LocalDate date = DateTimeUtil.parseDate_yyyyMMdd(dates.getString(i));
                    if (date.isBefore(start) || date.isAfter(end)) continue;
                    KlineDTO bar = new KlineDTO();
                    bar.setDate(date);
                    bar.setOpen(number(data, "Open", i));
                    bar.setHigh(number(data, "High", i));
                    bar.setLow(number(data, "Low", i));
                    bar.setClose(number(data, "Close", i));
                    double volume = number(data, "Volume", i);
                    // 日线 A股/ETF 为股；板块为手。与 snapshot 的总手口径不同。
                    if (StockTypeEnum.isBlock(raw)) volume *= 100;
                    bar.setVol((long) volume);
                    bar.setAmo(number(data, "Amount", i) * 10_000);

                    if (bar.getOpen() == 0 && bar.getClose() == 0 && volume == 0) continue;

                    if (bar.getOpen() <= 0 || bar.getClose() <= 0 || bar.getLow() <= 0
                            || bar.getHigh() < Math.max(bar.getOpen(), bar.getClose())
                            || bar.getLow() > Math.min(bar.getOpen(), bar.getClose()) || volume < 0 || bar.getAmo() < 0) {

                        throw new IllegalStateException("TQ 非法日线：" + code + "/" + date);
                    }

                    bars.add(bar);
                }

                bars.sort(Comparator.comparing(KlineDTO::getDate));
                result.put(raw, bars);


            } catch (RuntimeException e) {

                log.warn("TQ 日线解析失败，跳过：{} , err : {}", code, e.getMessage());
                result.put(raw, List.of());
            }
        }

        return result;
    }


    private static double number(JSONObject data, String field, int index) {
        Double value = data.getJSONArray(field).getDouble(index);
        Assert.isTrue(value != null && Double.isFinite(value), "TQ 非法数值：" + field);

        return value;
    }


}