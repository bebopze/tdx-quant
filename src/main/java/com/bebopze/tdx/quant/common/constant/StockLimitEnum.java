package com.bebopze.tdx.quant.common.constant;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


/**
 * A股 - 涨跌幅 限制                          仅支持  个股code  匹配       TODO     ETF 暂未支持
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@AllArgsConstructor
public enum StockLimitEnum {


    // 300
    // 301
    SZ("创业板", 1, 20, Lists.newArrayList("30")),


    // 688
    // 689
    SH("科创板", 2, 20, Lists.newArrayList("68")),


    // 430
    // 830
    // 831
    // 832
    // 833
    // 834
    // 835
    // 836
    // 837
    // 838
    // 839
    // 870
    // 871
    // 872
    // 873
    // 920
    BJ("北交所", 3, 30, Lists.newArrayList("43", "83", "87", "92"));


    /**
     * A股 类型desc（创业板、科创板、北交所）
     */
    @Getter
    private String marketDesc;

    /**
     * A股 类型type（创业板、科创板、北交所）
     */
    @Getter
    private Integer marketType;

    /**
     * 涨跌幅 限制：5、10、20、30
     */
    @Getter
    private Integer changePctLimit;


    /**
     * A股 - 股票代码 前缀（前2位）
     */
    @Getter
    private List<String> stockCodePrefixList;


    // ----------------------------------------------------------------------


    /**
     * 缺省值：10CM（主板）
     */
    private static final Integer DEFAULT_LIMIT = 10;


    /**
     * ST：5CM
     */
    private static final Integer ST_LIMIT = 5;


    // ----------------------------------------------------------------------


    public static StockLimitEnum getByStockCode(String stockCode) {
        // 前2位
        String codePrefix = stockCode.trim().substring(0, 2);

        for (StockLimitEnum value : StockLimitEnum.values()) {
            if (value.stockCodePrefixList.contains(codePrefix)) {
                return value;
            }
        }
        return null;
    }

    public static Integer getChangePctLimit(String stockCode, String stockName) {
        if (is5CM(stockName)) return ST_LIMIT;

        StockLimitEnum stockLimitEnum = getByStockCode(stockCode);
        return stockLimitEnum == null ? DEFAULT_LIMIT : stockLimitEnum.getChangePctLimit();
    }


    // ----------------------------------------------------------------------


    /**
     * 是否 20CM（含30CM）          （ 涨跌幅限制 >= 20% ）
     *
     * @param stockCode
     * @return
     */
    public static boolean is20CM(String stockCode, String stockName) {
        Integer changePctLimit = getChangePctLimit(stockCode, stockName);
        return changePctLimit >= 20;
    }


    /**
     * 是否 30CM
     *
     * @param stockCode
     * @return
     */
    public static boolean is30CM(String stockCode, String stockName) {
        Integer changePctLimit = getChangePctLimit(stockCode, stockName);
        return changePctLimit >= 30;
    }


    /**
     * 是否 10CM
     *
     * @param stockCode
     * @return
     */
    public static boolean is10CM(String stockCode, String stockName) {
        Integer changePctLimit = getChangePctLimit(stockCode, stockName);
        return changePctLimit <= 10;
    }


    /**
     * 是否 5CM
     *
     * @param stockName
     * @return
     */
    public static boolean is5CM(String stockName) {
        return stockName.contains("ST");
    }


}