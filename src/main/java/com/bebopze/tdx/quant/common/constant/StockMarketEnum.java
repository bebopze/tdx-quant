package com.bebopze.tdx.quant.common.constant;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * A股 - 交易所                          支持   A股code / ETF code   匹配
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@AllArgsConstructor
public enum StockMarketEnum {


    // ------------------- A股


    // 个股 - 00xxxx
    // 个股 - 30xxxx

    // ETF - 15xxxx
    // ETF - 16xxxx
    // ETF - 18xxxx
    SZ("深交所", 0, "sz", "SA", "SZ", "SZ", Set.of("00", "30", "15", "16", "18")),


    // 个股 - 60xxxx
    // 个股 - 68xxxx

    // ETF - 51xxxx
    // ETF - 52xxxx
    // ETF - 56xxxx
    // ETF - 58xxxx

    // 板块 - 88xxxx
    SH("上交所", 1, "sh", "HA", "SH", "SH", Set.of("60", "68", "51", "52", "56", "58", "88")),


    // 个股 - 92xxxx
    BJ("北交所", 2, "bj", "B", "BJ", "BJ", Set.of("92")),


    // ------------------- 港股


    // 00   01   02   03   04   05   06   07   09
    // 8
    HK_ZB("香港主板", 31, "31", "", "HK", "HK", Set.of("00", "01", "02", "03", "04", "05", "06", "07", "09", "8")),


    // 08
    HK_CYB("香港创业板", 48, "48", "", "HK", "HK", Set.of("08")),


    // ------------------- 美股


    US("美股", 74, "74", "", "US", "US", Set.of());


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
     * 通达信TQ - 交易所 类型
     */
    @Getter
    private String tdxTqMarket;


    /**
     * A股 - 股票代码 前缀（前2位）
     */
    @Getter
    private Set<String> stockCodePrefixSet;


    // -----------------------------------------------------------------------------------------------------------------


    private static final List<StockMarketEnum> A_enums = Lists.newArrayList(SZ, SH, BJ);
    private static final List<StockMarketEnum> HK_enums = Lists.newArrayList(HK_ZB, HK_CYB);

    public static StockMarketEnum getByStockCode(String stockCode) {


        // ---------- A股


        if (StockTypeEnum.isAStock_ETF_block(stockCode)) {
            // A股（前2位）
            String codePrefix = stockCode.trim().substring(0, 2);

            for (StockMarketEnum value : A_enums) {
                if (value.stockCodePrefixSet.contains(codePrefix)) {
                    return value;
                }
            }
        }


        // ---------- 港美股


        StockTypeEnum stockTypeEnum = StockTypeEnum.getByStockCode(stockCode);


        // 港股
        if (Objects.equals(stockTypeEnum, StockTypeEnum.HK_STOCK)) {
            // 港股（前2位）
            String codePrefix_2 = stockCode.trim().substring(0, 2);
            String codePrefix_1 = stockCode.trim().substring(0, 1);

            for (StockMarketEnum value : HK_enums) {
                if (value.stockCodePrefixSet.contains(codePrefix_2) || value.stockCodePrefixSet.contains(codePrefix_1)) {
                    return value;
                }
            }
        }


        // 美股
        if (Objects.equals(stockTypeEnum, StockTypeEnum.US_STOCK)) {
            return US;
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

    public static String getTdxTqMarket(String stockCode) {
        StockMarketEnum stockMarketEnum = getByStockCode(stockCode);
        return stockMarketEnum == null ? null : stockMarketEnum.tdxTqMarket;
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