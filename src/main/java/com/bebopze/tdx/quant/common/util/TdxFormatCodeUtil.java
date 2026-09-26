package com.bebopze.tdx.quant.common.util;

import com.bebopze.tdx.quant.common.constant.StockMarketEnum;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 通达信 TQ 证券代码 标准化工具（统一格式：600519.SH / 00700.HK / NVDA.US）
 *
 * @author: bebopze
 * @date: 2026/9/26
 */
public final class TdxFormatCodeUtil {


    // 前缀（SH600000 / SZ000001 / BJ920000 / HK00700）
    private static final Pattern PREFIX_CODE = Pattern.compile("^(SH|SZ|BJ|HK)([A-Z0-9]+)$");

    // 后缀（600000.SH / 000001.SZ / 920000.BJ / 00700.HK / NVDA.US）
    private static final Pattern SUFFIX_CODE = Pattern.compile("^([A-Z0-9]+)\\.(SH|SZ|BJ|HK|US)$");

    // 裸代码（6位数字=A股 / 5位数字=港股 / 1~10位字母=美股）
    private static final Pattern RAW_CODE = Pattern.compile("^(\\d{5,6}|[A-Z]{1,10})$");


    public static List<String> formatCodes(List<String> codes) {
        return codes.stream().map(TdxFormatCodeUtil::formatCode).toList();
    }


    /**
     * 标准化 通达信证券代码（统一格式：600519.SH）
     *
     * @param code 支持 600519、sh600519、600519.SH 三种格式
     *
     *             <p>
     *             裸代码存在跨市场歧义时，调用方应传 type 或直接传市场后缀。
     *             例如上证指数应传 {@code 000001.SH}，平安银行可传 {@code 000001} 或 {@code 000001.SZ}
     *             </p>
     * @return
     */
    public static String formatCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("通达信证券代码不能为空");
        }


        String up_code = code.trim().toUpperCase(Locale.ROOT);


        Matcher suffixMatcher = SUFFIX_CODE.matcher(up_code);
        if (suffixMatcher.matches()) {
            return suffixMatcher.group(1) + "." + suffixMatcher.group(2);
        }

        Matcher prefixMatcher = PREFIX_CODE.matcher(up_code);
        if (prefixMatcher.matches()) {
            return prefixMatcher.group(2) + "." + prefixMatcher.group(1);
        }

        if (!RAW_CODE.matcher(up_code).matches()) {
            throw new IllegalArgumentException("无效的通达信证券代码 : [" + code + "]");
        }


        return up_code + "." + StockMarketEnum.getMarketSymbol(up_code).toUpperCase(Locale.ROOT);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {

        System.out.println(formatCode("600519.SH"));
        System.out.println(formatCode("SH600519"));
        System.out.println(formatCode("600519"));

        System.out.println(formatCode("00700.HK"));
        System.out.println(formatCode("00700"));

        System.out.println(formatCode("NVDA.US"));
        System.out.println(formatCode("NVDA"));
    }


}