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
@AllArgsConstructor
public enum StockTypeEnum {


    A_STOCK(1, "A股", Lists.newArrayList()),

    ETF(2, "ETF", Lists.newArrayList("15", "51", "56", "58")),


    TDX_BLOCK(3, "板块", Lists.newArrayList("88")),


    ;


    public final Integer type;

    public final String desc;

    /**
     * 个股（A股/ETF） -  股票代码 前缀（前2位）
     */
    @Getter
    private List<String> stockCodePrefixList;


    public static String getDescByType(Integer type) {
        for (StockTypeEnum value : StockTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value.desc;
            }
        }
        return null;
    }


    public static StockTypeEnum getByStockCode(String stockCode) {
        // 前2位
        String codePrefix = stockCode.trim().substring(0, 2);

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


    public static boolean isBlock(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return TDX_BLOCK.equals(stockTypeEnum);
    }


}