package com.bebopze.tdx.quant.common.util;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Map
 *
 * @author: bebopze
 * @date: 2025/7/17
 */
public class MapUtil {


    /**
     * 根据 value   ->   倒序
     *
     * @param map
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V extends Comparable<? super V>> Map<K, V> reverseSortByValue(Map<K, V> map) {
        // 倒序
        return sortByValue(map, Comparator.reverseOrder());
    }


    /**
     * 根据 value 排序
     *
     * @param map
     * @param comparator 自定义 比较器
     * @param <K>
     * @param <V>
     * @return
     */
    public static <K, V> Map<K, V> sortByValue(Map<K, V> map, Comparator<? super V> comparator) {
        if (map == null || map.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return map.entrySet().stream()
                  .sorted(Map.Entry.comparingByValue(comparator))   // 自定义 比较器
                  .collect(Collectors.toMap(
                          Map.Entry::getKey,
                          Map.Entry::getValue,
                          (oldValue, newValue) -> oldValue, // 冲突处理（保留旧值）
                          LinkedHashMap::new    // 保持排序顺序
                  ));
    }


}