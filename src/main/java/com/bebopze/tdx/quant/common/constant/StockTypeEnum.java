package com.bebopze.tdx.quant.common.constant;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


/**
 * 股票类型：1-A股；2-ETF；
 *
 * @author: bebopze
 * @date: 2025/7/29
 */
@Getter
@AllArgsConstructor
public enum StockTypeEnum {


    //  北交所   "43", "83", "87"   ->   已废弃（已全部迁移到 92）


    //    A_STOCK(1, "A股", Lists.newArrayList("00", "30", "60", "68", "92", "43", "83", "87")),
    A_STOCK(1, "A股", Lists.newArrayList("0", "3", "6", "9", "4", "8")),


    //    ETF(2, "ETF", Lists.newArrayList("15", "16", "50", "51", "56", "58")),
    ETF(2, "ETF", Lists.newArrayList("1", "5")),


    //    TDX_BLOCK(3, "板块", Lists.newArrayList("88")),
    TDX_BLOCK(3, "板块", Lists.newArrayList("8")),


    ;


    public final Integer type;

    public final String desc;

    /**
     * 个股（A股/ETF） -  股票代码 前缀（前2位）
     */
    private final List<String> stockCodePrefixList;


    public static String getDescByType(Integer type) {
        for (StockTypeEnum value : StockTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value.desc;
            }
        }
        return null;
    }


    public static StockTypeEnum getByStockCode(String stockCode) {
        // 前1位
        String codePrefix = stockCode.trim().substring(0, 1);

        for (StockTypeEnum value : StockTypeEnum.values()) {
            if (value.stockCodePrefixList.contains(codePrefix)) {
                return value;
            }
        }
        return null;
    }

    public static Integer getTypeByStockCode(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return stockTypeEnum == null ? null : stockTypeEnum.type;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static boolean isStock(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return A_STOCK.equals(stockTypeEnum);
    }

    public static boolean isETF(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return ETF.equals(stockTypeEnum);
    }

    public static boolean isBlock(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return TDX_BLOCK.equals(stockTypeEnum);
    }


}