package com.bebopze.tdx.quant.common.constant;

import com.bebopze.tdx.quant.common.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


/**
 * 股票类型：1-A股；2-ETF；3-板块；
 *
 * @author: bebopze
 * @date: 2025/7/29
 */
@Getter
@AllArgsConstructor
public enum StockTypeEnum {


    //  北交所   "43", "83", "87"   ->   已废弃（已全部迁移到 92）


    //    A_STOCK(1, "A股", Lists.newArrayList("00", "30", "60", "68", "92")),
    A_STOCK(1, "A股", Lists.newArrayList("0", "3", "6", "9")),


    //    ETF(2, "ETF", Lists.newArrayList("15", "16", "50", "51", "56", "58")),
    ETF(2, "ETF", Lists.newArrayList("1", "5")),


    //    TDX_BLOCK(3, "板块", Lists.newArrayList("88")),
    TDX_BLOCK(3, "板块", Lists.newArrayList("8")),


    // ----------------------------------------------------


    HK_STOCK(11, "港股", Lists.newArrayList("")),

    US_STOCK(12, "美股", Lists.newArrayList("")),


    ;


    public final Integer type;

    public final String desc;

    /**
     * 个股（A股/ETF） -  股票代码 前缀（前2位）
     */
    private final List<String> stockCodePrefixList;


    // -----------------------------------------------------------------------------------------------------------------


    private static final List<StockTypeEnum> A_enums = Lists.newArrayList(A_STOCK, ETF, TDX_BLOCK);


    public static StockTypeEnum getByStockCode(String stockCode) {


        // -------------------------------------------------- A股/ETF/板块


        if (stockCode.length() == 6 && NumUtil.isNumber(stockCode)) {
            // 前1位
            String codePrefix = stockCode.trim().substring(0, 1);

            for (StockTypeEnum value : A_enums) {
                if (value.stockCodePrefixList.contains(codePrefix)) {
                    return value;
                }
            }
        }


        // -------------------------------------------------- 港美股


        // 港股（00700）
        if (stockCode.length() == 5 && NumUtil.isNumber(stockCode)) {
            return StockTypeEnum.HK_STOCK;
        }

        // 美股（SPY）
        if (NumUtil.isPureEnglish(stockCode)) {
            return StockTypeEnum.US_STOCK;
        }


        return null;
    }


    public static Integer getTypeByStockCode(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return stockTypeEnum == null ? null : stockTypeEnum.type;
    }


    public static String getDescByType(Integer type) {
        for (StockTypeEnum value : StockTypeEnum.values()) {
            if (value.type.equals(type)) {
                return value.desc;
            }
        }
        return null;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static boolean isAStock(String stockCode) {
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


    public static boolean isStock(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return A_STOCK.equals(stockTypeEnum) || ETF.equals(stockTypeEnum) || HK_STOCK.equals(stockTypeEnum) || US_STOCK.equals(stockTypeEnum);
    }


    public static boolean isAStock_ETF_block(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return A_STOCK.equals(stockTypeEnum) || ETF.equals(stockTypeEnum) || TDX_BLOCK.equals(stockTypeEnum);
    }


    public static boolean isHkStock(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return HK_STOCK.equals(stockTypeEnum);
    }


    public static boolean isUsStock(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return US_STOCK.equals(stockTypeEnum);
    }


    public static boolean isHkUsStock(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return HK_STOCK.equals(stockTypeEnum) || US_STOCK.equals(stockTypeEnum);
    }


}