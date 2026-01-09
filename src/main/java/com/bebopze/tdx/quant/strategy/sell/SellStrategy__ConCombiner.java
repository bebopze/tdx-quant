package com.bebopze.tdx.quant.strategy.sell;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


/**
 * S策略组合
 *
 * @author: bebopze
 * @date: 2025/12/3
 */
@Slf4j
public class SellStrategy__ConCombiner {


    // 破位（短期趋势）
    private static final Set<String> conKeyList_1 = Sets.newHashSet("SSF空");

    // 破位（中期趋势）
    private static final Set<String> conKeyList_2 = Sets.newHashSet("MA20空", "月空_MA20空");

    // 高抛
    private static final Set<String> conKeyList_3 = Sets.newHashSet("C_SSF_偏离率", "H_SSF_偏离率");


    // 盈利策略
    private static final Set<String> conKeyList_4 = Sets.newHashSet("盈亏>5", "盈亏>10", "盈亏>15", "盈亏>20", "盈亏>30", "盈亏>50", "盈亏>75", "盈亏>100");


    // 亏损策略
    private static final Set<String> conKeyList_5 = Sets.newHashSet("盈亏<-7", "盈亏<-10", "盈亏<-13");


    // -----------------------------------------------------------------------------------------------------------------


    // 必选项（触发 即S）
    private static final Set<String> conKeyList_11 = Sets.newHashSet("高位爆量上影大阴", "跌停_MA5空_MA10空");


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 是否卖出       =>       conList   ->   任一 true
     *
     * @param conKeySet
     * @param conMap
     * @return
     */
    public static boolean calcCon(Set<String> conKeySet, Map<String, Boolean> conMap) {


        for (String key : conKeySet) {
            try {
                if (conMap.get(key)) {
                    return true;
                }
            } catch (Exception ex) {
                log.error("calcCon - err     >>>     conKeySet : {} , errMsg : {}", conKeySet, ex.getMessage(), ex);
                return false;
            }
        }

        return false;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 从 5 个条件组中，每组选 0 或 1 个，总共至少选 2 个，生成所有组合
     *
     * @return 所有可能的组合列表
     */
    public static List<Set<String>> generateCombinations() {
        return generateCombinations(1);
    }


    /**
     * 从 5 个条件组中，每组选 0 或 1 个，总共至少选 N（>= 1）个，生成所有组合
     *
     * @param N 支持指定最小数量 N（N>=1）
     * @return
     */
    public static List<Set<String>> generateCombinations(int N) {
        N = Math.max(1, N); // 至少 1 个


        List<Set<String>> result = new ArrayList<>();

        // 每组增加一个 null 表示“不选”
        Set<String> g1 = new HashSet<>(conKeyList_1);
        g1.add(null); // null 表示本组不选
        Set<String> g2 = new HashSet<>(conKeyList_2);
        g2.add(null);
        Set<String> g3 = new HashSet<>(conKeyList_3);
        g3.add(null);


        for (String c1 : g1) {
            for (String c2 : g2) {
                for (String c3 : g3) {


                    Set<String> combo = new HashSet<>();
                    if (c1 != null) combo.add(c1);
                    if (c2 != null) combo.add(c2);
                    if (c3 != null) combo.add(c3);


                    // 只保留至少 N（>=2） 个条件的组合
                    if (combo.size() >= N) {


                        // 必选组  必须包含 c11
                        combo.addAll(conKeyList_11);


                        result.add(combo);
                    }
                }
            }
        }


        return result;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 测试
    public static void main(String[] args) {


        List<Set<String>> combinations = generateCombinations(1);


        System.out.println("共生成 " + combinations.size() + " 种组合（每组选 0~1，总数 ≥2）：\n");


        // 打印
        combinations.forEach(System.out::println);
    }


}
