package com.bebopze.tdx.quant.strategy.buy;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 策略组合
 *
 * @author: bebopze
 * @date: 2025/8/9
 */
@Slf4j
public class BuyStrategy__ConCombiner {


    // 短期趋势
    private static final List<String> conKeyList_1 = Lists.newArrayList("SSF多", "MA20多");

    // 新高
    private static final List<String> conKeyList_2 = Lists.newArrayList("N60日新高", "N100日新高", "历史新高");

    // 长期趋势（均线）
    private static final List<String> conKeyList_3 = Lists.newArrayList("月多", "均线预萌出", "均线萌出", "大均线多头");

    // RPS强度
    private static final List<String> conKeyList_4 = Lists.newArrayList("RPS红", "RPS一线红", "RPS双线红", "RPS三线红");


    // -----------------------------------------------------------------------------------------------------------------

    // SSF多,MA20多
    // N60日新高,N100日新高                  - 历史新高（24）
    // 月多,均线预萌出,均线萌出,大均线多头
    // RPS红,RPS一线红,                     - RPS双线红（26）,RPS三线红（26）


    // -----------------------------------------------------------------------------------------------------------------


    public static final List<String> conKeyList = Lists.newArrayList(conKeyList_1);

    static {
        conKeyList.addAll(conKeyList_2);
        conKeyList.addAll(conKeyList_3);
        conKeyList.addAll(conKeyList_4);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 是否买入       =>       conList   ->   全为 true
     *
     * @param conKeyList
     * @param conMap
     * @return
     */
    public static boolean calcCon(List<String> conKeyList, Map<String, Boolean> conMap) {

        for (String key : conKeyList) {

            if (!conMap.get(key)) {
                return false;
            }
        }

        return true;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 从 4 个条件组中，每组选 0 或 1 个，总共至少选 2 个，生成所有组合
     *
     * @return 所有可能的组合列表
     */
    public static List<List<String>> generateCombinations() {
        return generateCombinations(2);
    }


    /**
     * 从 4 个条件组中，每组选 0 或 1 个，总共至少选 N（>= 2）个，生成所有组合
     *
     * @param N 支持指定最小数量 N（N>=2）
     * @return
     */
    public static List<List<String>> generateCombinations(int N) {
        N = Math.max(2, N); // 至少 2 个

        List<List<String>> result = new ArrayList<>();

        // 每组增加一个 null 表示“不选”
        List<String> g1 = new ArrayList<>(conKeyList_1);
        g1.add(0, null); // null 表示本组不选
        List<String> g2 = new ArrayList<>(conKeyList_2);
        g2.add(0, null);
        List<String> g3 = new ArrayList<>(conKeyList_3);
        g3.add(0, null);
        List<String> g4 = new ArrayList<>(conKeyList_4);
        g4.add(0, null);

        for (String c1 : g1) {
            for (String c2 : g2) {
                for (String c3 : g3) {
                    for (String c4 : g4) {
                        List<String> combo = new ArrayList<>();
                        if (c1 != null) combo.add(c1);
                        if (c2 != null) combo.add(c2);
                        if (c3 != null) combo.add(c3);
                        if (c4 != null) combo.add(c4);


                        // 只保留至少 N（>=2） 个条件的组合
                        if (combo.size() >= N) {
                            result.add(combo);
                        }
                    }
                }
            }
        }

        return result;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 测试
    public static void main(String[] args) {


        List<List<String>> combinations = generateCombinations();


        System.out.println("共生成 " + combinations.size() + " 种组合（每组选 0~1，总数 ≥2）：\n");


        // 打印
        combinations.forEach(System.out::println);
    }


}
