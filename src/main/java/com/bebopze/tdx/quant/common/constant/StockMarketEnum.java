package com.bebopze.tdx.quant.common.constant;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Objects;


/**
 * A股 - 交易所                          支持   A股code / ETF code   匹配
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@AllArgsConstructor
public enum StockMarketEnum {


    // ------------------- A股


    // 000	0
    // 001	0
    // 002	0
    // 003	0
    // 300	0
    // 301	0


    // 个股 - 0xxxxx
    // 个股 - 3xxxxx
    // ETF - 15xxxx
    SZ("深交所", 0, "sz", "SA", "SZ", Lists.newArrayList("0", "3", "1")),


    // 600	1
    // 601	1
    // 603	1
    // 605	1
    // 688	1
    // 689	1


    // 个股 - 6xxxxx
    // ETF - 5xxxxx
    // 板块 - 88xxxx
    SH("上交所", 1, "sh", "HA", "SH", Lists.newArrayList("6", "5", "8")),


    // 92   2


    // 个股 - 92xxxx
    BJ("北交所", 2, "bj", "B", "BJ", Lists.newArrayList("9")),


    // ------------------- 港股


    // 00   01   02   03   04   05   06   07   09
    // 8
    HK_ZB("香港主板", 31, "31", "", "", Lists.newArrayList("00", "01", "02", "03", "04", "05", "06", "07", "09", "8")),


    // 08
    HK_CYB("香港创业板", 48, "48", "", "", Lists.newArrayList("08")),


    // ------------------- 美股


    US("美股", 74, "74", "", "", Lists.newArrayList());


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


    // -----------------------------------------------------------------------------------------------------------------


    private static final List<StockMarketEnum> A_enums = Lists.newArrayList(SZ, SH, BJ);
    private static final List<StockMarketEnum> HK_enums = Lists.newArrayList(HK_ZB, HK_CYB);

    public static StockMarketEnum getByStockCode(String stockCode) {


        // ---------- A股


        if (StockTypeEnum.isAStock_ETF_block(stockCode)) {
            // A股（前1位）
            String codePrefix = stockCode.trim().substring(0, 1);

            for (StockMarketEnum value : A_enums) {
                if (value.stockCodePrefixList.contains(codePrefix)) {
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
                if (value.stockCodePrefixList.contains(codePrefix_2) || value.stockCodePrefixList.contains(codePrefix_1)) {
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

    public static String getMarketSymbol(Integer tdxMarketType) {
        StockMarketEnum stockMarketEnum = getByTdxMarketType(tdxMarketType);
        return stockMarketEnum == null ? null : stockMarketEnum.tdxMarketTypeSymbol;
    }


    public static Integer getTdxMarketType(String stockCode) {
        StockMarketEnum stockMarketEnum = getByStockCode(stockCode);
        return stockMarketEnum == null ? null : stockMarketEnum.tdxMarketType;
    }


}