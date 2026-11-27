package com.bebopze.tdx.quant.common.constant;

import com.bebopze.tdx.quant.common.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static com.bebopze.tdx.quant.common.constant.option.UnderlyingCodeConst.OPTION_UNDERLYING__CODE_NAME__MAP;


/**
 * 自定义 股票类型：1-A股；2-ETF；11-港股；12-美股；21-股指期货；      // 对应 base_stock 表 type 自定义字段
 *
 * @author: bebopze
 * @date: 2025/7/29
 */
@Getter
@AllArgsConstructor
public enum StockTypeEnum {


    //  北交所   "43", "83", "87"   ->   已废弃（已全部迁移到 92）


    //    A_STOCK(1, "A股", Lists.newArrayList("00", "30", "60", "68", "92")),
    A_STOCK(1, "A股", 2, Lists.newArrayList("0", "3", "6", "9")),


    //    ETF(2, "ETF", Lists.newArrayList("15", "16", "50", "51", "56", "58")),
    ETF(2, "ETF", 3, Lists.newArrayList("1", "5")),


    //    TDX_BLOCK(3, "板块", Lists.newArrayList("88")),
    TDX_BLOCK(3, "板块", 2, Lists.newArrayList("8")),


    // ----------------------------------------------------


    HK_STOCK(11, "港股", 3, Lists.newArrayList("")),

    US_STOCK(12, "美股", 3, Lists.newArrayList("")),


    // ----------------------------------------------------


    SIF_STOCK(21, "股指期货（中金所）", 2, Lists.newArrayList("IM", "IF", "IH")),


    // ----------------------------------------------------


    // ETF期权：1/9
    ETF_OPTION(101, "ETF期权", 4, Lists.newArrayList("1", "9")),

    // 股指期权：MO/IO/HO
    INDEX_OPTION(102, "股指期权", 1, Lists.newArrayList("MO", "IO", "HO")),


    ;


    /**
     * 自定义 股票类型（区分 A股/ETF/板块/港股/美股/股指期货/期权/）    ->     对应 base_stock 表 type 字段（ base_block、base_option 表 未记录此字段  ->  因为 已拆分 独立建表）
     */
    public final Integer type;
    /**
     * 描述
     */
    public final String desc;

    /**
     * 价格精度
     */
    public final Integer priceScale;

    /**
     * 个股（A股/ETF）/ 期权  -  股票代码 前缀（前1位）
     */
    private final List<String> stockCodePrefixList;


    // -----------------------------------------------------------------------------------------------------------------


    private static final List<StockTypeEnum> A_enums = Lists.newArrayList(A_STOCK, ETF, TDX_BLOCK);


    public static StockTypeEnum getByStockCode(String stockCode) {
        if (StringUtils.isBlank(stockCode)) {
            return null;
        }


        // -------------------------------------------------- A股/ETF/板块


        if (stockCode.length() == 6 && NumUtil.isIntNumber(stockCode)) {
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
        if (stockCode.length() == 5 && NumUtil.isIntNumber(stockCode)) {
            return StockTypeEnum.HK_STOCK;
        }

        // 美股（SPY）
        if (NumUtil.isPureEnglish(stockCode)) {
            return StockTypeEnum.US_STOCK;
        }


        // -------------------------------------------------- 股指期货


        // 股指期货（IM1000 / IF300 / IH50）
        if (stockCode.startsWith("I")) {
            // 前2位
            String codePrefix = stockCode.trim().substring(0, 2);

            if (SIF_STOCK.stockCodePrefixList.contains(codePrefix)) {
                return StockTypeEnum.SIF_STOCK;
            }
        }


        // -------------------------------------------------- 期权


        // ETF期权（10011565 / 90007858）
        // 上交所     10000001 ~ 19999999（ETF期权合约从此区间起始，按顺序对新挂牌合约编排，逐一递增，永不复用）
        // 深交所     90000001 ~ 91999999（ETF期权合约从此区间起始，按顺序对新挂牌合约编排，逐一递增，永不复用）
        if (stockCode.length() == 8 && NumUtil.isIntNumber(stockCode)) {
            // 前1位
            String codePrefix = stockCode.trim().substring(0, 1);

            if (ETF_OPTION.stockCodePrefixList.contains(codePrefix)) {
                return StockTypeEnum.ETF_OPTION;
            }
        }


        // 股指期权（MO2709-C-7000 / IO2709-P-3700 / HO2709-P-2500）
        if (stockCode.length() == 13) {
            // 前2位
            String codePrefix = stockCode.trim().substring(0, 2);

            if (INDEX_OPTION.stockCodePrefixList.contains(codePrefix)) {
                return StockTypeEnum.INDEX_OPTION;
            }
        }


        return null;
    }


    public static Integer getTypeByStockCode(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return stockTypeEnum == null ? null : stockTypeEnum.type;
    }


    public static Integer getPriceScale(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        // 默认值：2
        return stockTypeEnum == null ? 2 : stockTypeEnum.priceScale;
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


    /**
     * 期权
     *
     * @param stockCode
     * @return
     */
    public static boolean isOption(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return ETF_OPTION.equals(stockTypeEnum) || INDEX_OPTION.equals(stockTypeEnum);
    }

    /**
     * 期权标的
     *
     * @param stockCode
     * @return
     */
    public static boolean isOptionUnderlying(String stockCode) {
        return OPTION_UNDERLYING__CODE_NAME__MAP.containsKey(stockCode);
    }


    /**
     * 港美股/期权（ tdx市场类型#股票代码   ->   31#00700 / 74#SPY / 8#10011565 / 7#MO2709-C-7000）
     *
     * @param stockCode
     * @return
     */
    public static boolean isHkUsOpStock(String stockCode) {
        StockTypeEnum stockTypeEnum = getByStockCode(stockCode);
        return HK_STOCK.equals(stockTypeEnum) || US_STOCK.equals(stockTypeEnum) || ETF_OPTION.equals(stockTypeEnum) || INDEX_OPTION.equals(stockTypeEnum);
    }


}