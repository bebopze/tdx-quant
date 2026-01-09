package com.bebopze.tdx.quant.strategy.buy;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


/**
 * B策略组合
 *
 * @author: bebopze
 * @date: 2025/8/9
 */
@Slf4j
public class BuyStrategy__ConCombiner {


    // ----------------------------------------------- AND -------------------------------------------------------------


    // 短期趋势
    private static final List<String> conKeyList_1 = Lists.newArrayList(/*"XZZB", "SSF多",*/ "MA20多");
//    private static final List<String> conKeyList_1 = Lists.newArrayList();

    // 新高
    private static final List<String> conKeyList_2 = Lists.newArrayList("百日新高", "N60日新高"/*, "N100日新高", "历史新高"*/);

    // 长期趋势（均线）
    private static final List<String> conKeyList_3 = Lists.newArrayList(/*"月多", "均线预萌出",*/ /*"均线萌出",*/ "小均线多头", "大均线多头"/*, "均线大多头", "均线极多头"*/);

    // RPS强度
    private static final List<String> conKeyList_4 = Lists.newArrayList(/*"RPS红",*/ "RPS一线红", "RPS双线红", "RPS三线红");


    // 买点1 -> 低吸
//    private static final List<String> conKeyList_5 = Lists.newArrayList("C_SSF_偏离率<5"/*, "C_MA_偏离率<3", "C_MA_偏离率<5", "C_MA_偏离率<7"*/);
//    private static final List<String> conKeyList_5 = Lists.newArrayList("C_SSF_偏离率<5", /*"C_MA5_偏离率<5", "C_MA10_偏离率<5",*/ "C_MA20_偏离率<5", "C_MA30_偏离率<5",
//            /*"C_MA50_偏离率<5",*/ "C_MA60_偏离率<5"/*, "C_MA100_偏离率<5"*/);
    private static final List<String> conKeyList_5 = Lists.newArrayList("C_SSF_偏离率<5", "C_MA5_偏离率<5", "C_MA10_偏离率<5", "C_MA20_偏离率<5");


    // 中期涨幅_MA20（限高）
    private static final List<String> conKeyList_6 = Lists.newArrayList("中期涨幅<35", "中期涨幅<50", "中期涨幅<100");
    // 长期涨幅_MA250（限高）
    private static final List<String> conKeyList_7 = Lists.newArrayList("中期涨幅N250<150", "中期涨幅N250<350", "中期涨幅N250<700", "中期涨幅N250<1500");


    // TODO   回踩支撑线
    private static final List<String> conKeyList_8 = Lists.newArrayList("C_短期MA_偏离率<5", "C_中期MA_偏离率<5", "C_长期MA_偏离率<5");


    // TODO   必选组
    private static final List<String> conKeyList_11 = Lists.newArrayList("XZZB", "SSF多", "MA200多");
//    private static final List<String> conKeyList_11 = Lists.newArrayList(/*"XZZB",*/ "上MA200", "上MA250", "MA200多", "MA250多");


    // ----------------------------------------------- OR --------------------------------------------------------------


    // 买点2 -> 经典买点
    private static final List<String> or_conKeyList_1 = Lists.newArrayList("首次三线红", "口袋支点");

    // 均线形态
    private static final List<String> or_conKeyList_2 = Lists.newArrayList("月多", "均线预萌出");


    // -----------------------------------------------------------------------------------------------------------------


    // SSF多,MA20多
    // N60日新高,N100日新高                  - 历史新高（24）
    // 月多,均线预萌出,均线萌出,大均线多头
    // RPS红,RPS一线红,                     - RPS双线红（26）,RPS三线红（26）


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 是否买入       =>       conList   ->   全为 true
     *
     * @param conKeySet
     * @param conMap
     * @return
     */
    public static boolean calcCon(Set<String> conKeySet, Map<String, Boolean> conMap) {


//        // 必选组 -> OR_1
//        boolean or_flag = false;
//        for (String or_key : or_conKeyList_1) {
//            if (conMap.getOrDefault(or_key, false)) {
//                or_flag = true;
//                break;
//            }
//        }
//        if (!or_flag) {
//            return false;
//        }
//
//        // 必选组 -> OR_2
//        boolean or_flag_2 = false;
//        for (String or_key : or_conKeyList_2) {
//            if (conMap.getOrDefault(or_key, false)) {
//                or_flag_2 = true;
//                break;
//            }
//        }
//        if (!or_flag_2) {
//            return false;
//        }


        // 随机组 -> AND
        for (String key : conKeySet) {
            try {
                if (!conMap.getOrDefault(key, false)) {
                    return false;
                }
            } catch (Exception ex) {
                log.error("calcCon - err     >>>     key : {} , conKeySet : {} , errMsg : {}", key, conKeySet, ex.getMessage(), ex);
                return false;
            }
        }

        return true;
    }

    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 从 5 个条件组中，每组选 0 或 1 个，总共至少选 2 个，生成所有组合
     *
     * @return 所有可能的组合列表
     */
    public static List<Set<String>> generateCombinations() {
        return generateCombinations(2);
    }


    /**
     * 从 5 个条件组中，每组选 0 或 1 个，总共至少选 N（>= 2）个，生成所有组合
     *
     * @param N 支持指定最小数量 N（N>=2）
     * @return
     */
    public static List<Set<String>> generateCombinations(int N) {
        // TODO   N = Math.max(2, N); // 至少 2 个

        List<Set<String>> result = new ArrayList<>();

        // 每组增加一个 null 表示“不选”
        List<String> g1 = new ArrayList<>(conKeyList_1);
        g1.add(0, null); // null 表示本组不选
        List<String> g2 = new ArrayList<>(conKeyList_2);
        g2.add(0, null);
        List<String> g3 = new ArrayList<>(conKeyList_3);
        g3.add(0, null);
        List<String> g4 = new ArrayList<>(conKeyList_4);
        g4.add(0, null);
        List<String> g5 = new ArrayList<>(conKeyList_5);
        g5.add(0, null);
        List<String> g6 = new ArrayList<>(conKeyList_6);
        g6.add(0, null);
        List<String> g7 = new ArrayList<>(conKeyList_7);
        g7.add(0, null);


        for (String c1 : g1) {
            for (String c2 : g2) {
                for (String c3 : g3) {
                    for (String c4 : g4) {
                        for (String c5 : g5) {
                            for (String c6 : g6) {
                                for (String c7 : g7) {


                                    Set<String> combo = new HashSet<>();
                                    if (c1 != null) combo.add(c1);
                                    if (c2 != null) combo.add(c2);
                                    if (c3 != null) combo.add(c3);
                                    if (c4 != null) combo.add(c4);
                                    if (c5 != null) combo.add(c5);
                                    if (c6 != null) combo.add(c6);
                                    if (c7 != null) combo.add(c7);


                                    // 只保留至少 N（>=2） 个条件的组合
                                    if (combo.size() >= N) {

                                        // 必选组
                                        combo.addAll(conKeyList_11);

                                        result.add(combo);
                                    }
                                }
                            }
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


        int N = 3;
        List<Set<String>> combinations = generateCombinations(N);


        System.out.println("共生成 " + combinations.size() + " 种组合（每组选 0~1，总数 ≥" + N + "）：\n");


        // 打印
        combinations.forEach(System.out::println);
    }


}