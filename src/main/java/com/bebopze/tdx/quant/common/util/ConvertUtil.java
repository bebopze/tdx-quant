package com.bebopze.tdx.quant.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * convert
 *
 * @author: bebopze
 * @date: 2025/10/10
 */
public class ConvertUtil {


    public static Set<String> str2Set(String stringListStr) {
        return split(stringListStr).collect(Collectors.toSet());
    }


    public static List<String> str2List(String stringListStr) {
        return split(stringListStr).collect(Collectors.toList());
    }


    public static List<Long> str2LongList(String longListStr) {

        return split(longListStr)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }


    public static List<Integer> str2IntList(String intListStr) {

        return split(intListStr)
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }


    /**
     * 统一拆分逻辑，过滤 空值 与 null
     *
     * @param str
     * @return
     */
    private static Stream<String> split(String str) {

        return str == null ? Stream.empty() : Arrays.stream(str.split(","))
                                                    .map(String::trim)
                                                    .filter(StringUtils::isNotBlank);
    }


}