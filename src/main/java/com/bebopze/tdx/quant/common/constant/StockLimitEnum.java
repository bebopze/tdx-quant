package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Set;


/**
 * A股 - 涨跌幅 限制                          仅支持  个股code  匹配       TODO     ETF 暂未支持
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@AllArgsConstructor
public enum StockLimitEnum {


    // 00
    // 60
    ZB("主板", 1, 10, Set.of("00", "60")),


    // 300
    // 301
    CY("创业板", 2, 20, Set.of("30")),


    // 688
    // 689
    KC("科创板", 3, 20, Set.of("68")),


    // 920

    //  "43", "83", "87"   ->   已废弃（已全部迁移到 92）
    BJ("北交所", 4, 30, Set.of("92"));


    /**
     * A股 类型desc（创业板、科创板、北交所）
     */
    @Getter
    private String marketDesc;

    /**
     * A股 类型type（主板、创业板、科创板、北交所）
     */
    @Getter
    private Integer marketType;

    /**
     * 涨跌幅 限制：5、10、20、30
     */
    @Getter
    private Integer chgPctLimit;


    /**
     * A股 - 股票代码 前缀（前2位）
     */
    @Getter
    private Set<String> stockCodePrefixSet;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 缺省值：10CM（主板）
     */
    private static final Integer DEFAULT_LIMIT = 10;


    /**
     * ST：5CM
     */
    private static final Integer ST_LIMIT = 5;


    /**
     * 创业板 / 科创板
     */
    private static final Integer LIMIT_20 = 20;

    /**
     * 北交所
     */
    private static final Integer LIMIT_30 = 30;


    // -----------------------------------------------------------------------------------------------------------------


    public static StockLimitEnum getByStockCode(String stockCode) {
        // 前2位
        String codePrefix = StringUtils.isBlank(stockCode) ? "" : stockCode.trim().substring(0, 2);

        for (StockLimitEnum value : StockLimitEnum.values()) {
            if (value.stockCodePrefixSet.contains(codePrefix)) {
                return value;
            }
        }
        return null;
    }


    /**
     * 获取 涨跌幅限制
     *
     * @param stockCode
     * @param stockName
     * @return
     */
    public static Integer getChgPctLimit(String stockCode, String stockName) {
        if (is20CM_ETF(stockCode, stockName)) return LIMIT_20;

        StockLimitEnum stockLimitEnum = getByStockCode(stockCode);
        return stockLimitEnum == null ? DEFAULT_LIMIT : stockLimitEnum.getChgPctLimit();
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 是否 20CM（含30CM）          （ 涨跌幅限制 >= 20% ）
     *
     * @param stockCode
     * @return
     */
    public static boolean is20CM(String stockCode, String stockName) {
        Integer chgPctLimit = getChgPctLimit(stockCode, stockName);
        return chgPctLimit >= 20;
    }


    /**
     * 是否 30CM
     *
     * @param stockCode
     * @return
     */
    public static boolean is30CM(String stockCode, String stockName) {
        Integer chgPctLimit = getChgPctLimit(stockCode, stockName);
        return chgPctLimit >= 30;
    }


    /**
     * 是否 10CM
     *
     * @param stockCode
     * @return
     */
    public static boolean is10CM(String stockCode, String stockName) {
        Integer chgPctLimit = getChgPctLimit(stockCode, stockName);
        return chgPctLimit <= 10 && !isST(stockName);
    }


    /**
     * 是否 5CM
     *
     * @param stockCode
     * @param stockName
     * @return
     */
    public static boolean is5CM(String stockCode, String stockName) {
        // ST（暂未进入退市 -> 可能退市）、*ST（退市中 -> 即将退市）

        //  ST     主板 5%，创业板/科创板 20%，北交所 30%
        // *ST     主板 5%，创业板/科创板 20%，北交所 30%

        Integer chgPctLimit = getChgPctLimit(stockCode, stockName);
        return chgPctLimit <= 10 && isST(stockName);
    }


    /**
     * 是否 ST / *ST
     *
     * @param stockName
     * @return
     */
    public static boolean isST(String stockName) {
        return stockName != null && stockName.contains("ST");
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 是否 20CM  ETF
     *
     * @param stockCode
     * @param stockName
     * @return
     */
    private static boolean is20CM_ETF(String stockCode, String stockName) {
        if (!StockTypeEnum.isETF(stockCode)) {
            return false;
        }


        // 588000 - 科创50ETF
        // ...

        // 159915 - 创业板ETF
        // ...


        if (Objects.equals(stockCode, "588000") || Objects.equals(stockCode, "159915")) {
            return true;
        }


        return null != stockName && (stockName.contains("科创") || stockName.contains("创业板"));
    }


}