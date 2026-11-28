package com.bebopze.tdx.quant.common.constant.option;

import com.google.common.collect.Maps;

import java.util.Map;


/**
 * 期权标的（ETF期权标的 + 指数期权标的）
 *
 * @author: bebopze
 * @date: 2026/11/27
 */
public class UnderlyingCodeConst {


    /**
     * ETF期权标的
     */
    public static final Map<String, String> ETF_OPTION_UNDERLYING__CODE_NAME__MAP = Map.of("588000", "科创50ETF华夏",
                                                                                           "588080", "科创50ETF易方达",

                                                                                           "159915", "创业板ETF易方达",

                                                                                           "510050", "上证50ETF华夏",

                                                                                           "159901", "深证100ETF易方达",

                                                                                           "510300", "沪深300ETF华泰柏瑞",
                                                                                           "159919", "沪深300ETF嘉实",

                                                                                           "510500", "中证500ETF南方",
                                                                                           "159922", "中证500ETF嘉实");


    /**
     * 指数期权标的
     */
    public static final Map<String, String> INDEX_OPTION_UNDERLYING__CODE_NAME__MAP = Map.of("IH50", "上证50",
                                                                                             "IF300", "沪深300",
                                                                                             "IM1000", "中证1000");


    /**
     * 期权标的（ETF期权标的 + 指数期权标的）
     */
    public static final Map<String, String> OPTION_UNDERLYING__CODE_NAME__MAP = Maps.newTreeMap();


    /**
     * 短代码 -> base_option_underlying.code
     *
     * 例如：IM -> IM1000，IF -> IF300，IH -> IH50。
     */
    private static final Map<String, String> OPTION_UNDERLYING__ALIAS = Map.of(
            "IH", "IH50",
            "IF", "IF300",
            "IM", "IM1000"
    );


    static {
        OPTION_UNDERLYING__CODE_NAME__MAP.putAll(ETF_OPTION_UNDERLYING__CODE_NAME__MAP);
        OPTION_UNDERLYING__CODE_NAME__MAP.putAll(INDEX_OPTION_UNDERLYING__CODE_NAME__MAP);
    }


    /**
     * 统一转为 base_option_underlying.code（ETF 代码不变；指数别名映射）
     */
    public static String toUnderlyingCode(String code) {
        if (code == null) {
            return null;
        }
        String c = code.trim();
        return OPTION_UNDERLYING__ALIAS.getOrDefault(c, c);
    }


    public static String nameOf(String code) {
        return OPTION_UNDERLYING__CODE_NAME__MAP.get(toUnderlyingCode(code));
    }


}