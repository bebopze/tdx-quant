package com.bebopze.tdx.quant.common.constant;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


/**
 * A股 - 交易所                          支持   A股code / ETF code   匹配
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

    // ETF - 15xxxx
    SZ("深交所", 0, "sz", "SA", "SZ", Lists.newArrayList("00", "30", "15")),


    // 600	1
    // 601	1
    // 603	1
    // 605	1
    // 688	1
    // 689	1

    // ETF - 5xxxxx
    // 板块 - 88xxxx
    SH("上交所", 1, "sh", "HA", "SH", Lists.newArrayList("60", "68", "51", "56", "58","88")),


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
    BJ("北交所", 2, "bj", "B", "BJ", Lists.newArrayList("43", "83", "87", "92"));


    /**
     * A股 交易所（深交所、上交所、北交所）
     */
    @Getter
    private String marketDesc;

    /**
     * 通达信 - 交易所 类型（0-深圳；1-上海；2-北京；）
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
     * 雪球 - 交易所 类型
     */
    @Getter
    private String xueqiuMarket;


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

    public static StockMarketEnum getByTdxMarketType(Integer tdxMarketType) {
        for (StockMarketEnum value : StockMarketEnum.values()) {
            if (value.tdxMarketType.equals(tdxMarketType)) {
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
        StockMarketEnum stockMarketEnum = getByStockCode(stockCode);
        return stockMarketEnum == null ? null : stockMarketEnum.eastMoneyMarket;
    }


    public static String getMarketSymbol(String stockCode) {
        StockMarketEnum stockMarketEnum = getByStockCode(stockCode);
        return stockMarketEnum == null ? null : stockMarketEnum.tdxMarketTypeSymbol;
    }


    public static String getXueqiuMarket(String stockCode) {
        StockMarketEnum stockMarketEnum = getByStockCode(stockCode);
        return stockMarketEnum == null ? null : stockMarketEnum.xueqiuMarket;
    }

    public static String getMarketSymbol(Integer tdxMarketType) {
        StockMarketEnum stockMarketEnum = getByTdxMarketType(tdxMarketType);
        return stockMarketEnum == null ? null : stockMarketEnum.tdxMarketTypeSymbol;
    }


    public static Integer getTdxMarketType(String stockCode) {
        StockMarketEnum stockMarketEnum = getByStockCode(stockCode);
        return stockMarketEnum == null ? null : stockMarketEnum.tdxMarketType;
    }

}
