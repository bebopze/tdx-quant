package com.bebopze.tdx.quant.common.util;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * List
 *
 * @author: bebopze
 * @date: 2025/8/2
 */
public class ListUtil {


    /**
     * 截取  前N条
     *
     * @param list
     * @param n
     * @param <E>
     * @return
     */
    public static <E> List<E> firstN(List<E> list, int n) {
        if (CollectionUtils.isEmpty(list) || n <= 0) {
            return Collections.emptyList();
        }


        int size = list.size();
        if (n >= size) {
            return list;
        }

        return list.subList(0, n);
    }


    /**
     * 截取  最后N条
     *
     * @param list
     * @param n    最后N条
     * @return
     */
    public static <E> List<E> lastN(List<E> list, int n) {
        if (CollectionUtils.isEmpty(list) || n <= 0) {
            return Collections.emptyList();
        }


        int size = list.size();
        if (n >= size) {
            return list;
        }

        return list.subList(size - n, size);
    }


    /**
     * 截取  最后1条
     *
     * @param list
     * @return
     */
    public static <E> E last(List<E> list) {
        return CollectionUtils.isEmpty(list) ? null : list.get(list.size() - 1);
    }


    public static int size(Collection list) {
        return list == null ? 0 : list.size();
    }


}