package com.bebopze.tdx.quant.common.constant;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Objects;


/**
 * A股 - 交易所
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@AllArgsConstructor
public enum StockMarketEnum {


    // 000	0
    // 001	0
    // 002	0
    // 003	0
    // 300	0
    // 301	0
    SZ("深交所", 0, "sz", "SA", Lists.newArrayList("00", "30")),


    // 600	1
    // 601	1
    // 603	1
    // 605	1
    // 688	1
    // 689	1
    SH("上交所", 1, "sh", "HA", Lists.newArrayList("60", "68")),


    // 430	2
    // 830	2
    // 831	2
    // 832	2
    // 833	2
    // 834	2
    // 835	2
    // 836	2
    // 837	2
    // 838	2
    // 839	2
    // 870	2
    // 871	2
    // 872	2
    // 873	2
    // 920	2
    BJ("北交所", 2, "bj", "B", Lists.newArrayList("43", "83", "87", "92"));


    /**
     * A股 交易所（深交所、上交所、北交所）
     */
    @Getter
    private String marketDesc;

    /**
     * 通达信 - 交易所 类型
     */
    @Getter
    private Integer tdxMarketType;

    /**
     * 通达信 - 交易所 code
     */
    @Getter
    private String tdxMarketTypeSymbol;

    /**
     * 东方财富 - 交易所 类型
     */
    @Getter
    private String eastMoneyMarket;

    /**
     * A股 - 股票代码 前缀（前2位）
     */
    @Getter
    private List<String> stockCodePrefixList;


    public static StockMarketEnum getByStockCode(String stockCode) {

        // 前2位
        String codePrefix = stockCode.trim().substring(0, 2);

        for (StockMarketEnum value : StockMarketEnum.values()) {

            if (value.stockCodePrefixList.contains(codePrefix)) {
                return value;
            }
        }
        return null;
    }


    /**
     * stockCode  ->  东财 market
     *
     * @param stockCode
     * @return
     */
    public static String getEastMoneyMarketByStockCode(String stockCode) {
        return Objects.requireNonNull(getByStockCode(stockCode)).getEastMoneyMarket();
    }


    public static StockMarketEnum getByTdxMarketType(Integer tdxMarketType) {
        for (StockMarketEnum value : StockMarketEnum.values()) {
            if (value.getTdxMarketType().equals(tdxMarketType)) {
                return value;
            }
        }
        return null;
    }


    public static String getMarketSymbol(Integer tdxMarketType) {
        return Objects.requireNonNull(getByTdxMarketType(tdxMarketType)).getTdxMarketTypeSymbol();
    }

    public static String getMarketSymbol(String stockCode) {
        return Objects.requireNonNull(getByStockCode(stockCode)).getTdxMarketTypeSymbol();
    }

}