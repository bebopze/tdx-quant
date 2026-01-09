package com.bebopze.tdx.quant.strategy.buy;

import java.util.*;

/**
 * 组合生成器（修复：将 orRequired 当作 OR 组加入）
 *
 * @author: bebopze
 * @date: 2025/12/17
 */
public class QuantConditionCombiner {


    // -------------------------------- OR_AND groups (组内元素可以同时出现，任意子集) --------------------------------
    private static final List<String> conKeyList_OR_AND_1 = Arrays.asList(/*"XZZB",*/ "SSF多", "MA20多");
    private static final List<String> conKeyList_OR_AND_2 = Arrays.asList("月多", "均线预萌出", "小均线多头", "大均线多头");
    private static final List<String> conKeyList_OR_AND_3 = Arrays.asList(/*"RPS红",*/ "RPS一线红", "RPS双线红", "RPS三线红");
    private static final List<String> conKeyList_OR_AND_4 = Arrays.asList("百日新高", "N60日新高"/*, "N100日新高", "历史新高"*/);
    //    private static final List<String> conKeyList_OR_AND_5 = Arrays.asList("C_短期MA_偏离率<5", "C_中期MA_偏离率<5", "C_长期MA_偏离率<5");
    private static final List<String> conKeyList_OR_AND_5 = Arrays.asList();


    // -------------------------------- OR groups (组内元素不可同时出现，最多选一个) --------------------------------
    private static final List<String> conKeyList_OR_1 = Arrays.asList("C_SSF_偏离率<5", "C_MA5_偏离率<5", "C_MA20_偏离率<5", "C_MA60_偏离率<5", "C_MA100_偏离率<5");
    private static final List<String> conKeyList_OR_2 = Arrays.asList("中期涨幅<35", "中期涨幅<50", "中期涨幅<100");


    // 必须固定包含的条件（会被追加到每个组合中）
    private static final List<String> mustHave = Arrays.asList("XZZB", /*"SSF多",*/ "MA200多");


    // 至少包含其中一个（OR 必须项） —— 现在我们把它也作为一个 OR 组
    private static final List<String> orRequired = Arrays.asList("首次三线红", "口袋支点");


    /**
     * 生成所有组合
     *
     * @param N 可选组上至少选中的数量（不包含 mustHave 固定列表），与原逻辑一致，通常 >= 2
     * @return 组合列表（每个组合为 List<String>）
     */
    public static List<Set<String>> generateCombinationsNew(int N) {
        if (N < 0) N = 0;

        // 1) 收集 OR_AND 组与 OR 组
        List<List<String>> orAndGroups = Arrays.asList(
                conKeyList_OR_AND_1,
                conKeyList_OR_AND_2,
                conKeyList_OR_AND_3,
                conKeyList_OR_AND_4,
                conKeyList_OR_AND_5
        );

        List<List<String>> orGroups = Arrays.asList(
                conKeyList_OR_1,
                conKeyList_OR_2
                // 注意：orRequired 不在这里，下面会单独加入为 OR 组
        );

        // 2) 为每个组构建“该组可能的选择项（每项是 List<String>）”
        List<List<List<String>>> groupOptions = new ArrayList<>();

        // OR_AND 组：每个组的选项为该组的所有子集（包含空集合）
        for (List<String> group : orAndGroups) {
            groupOptions.add(generateAllSubsets(group)); // 包含空子集
        }

        // OR 组：每组选项为 {空, [e1], [e2], ...}
        for (List<String> group : orGroups) {
            groupOptions.add(buildOrOptions(group));
        }

        // *** 关键修复：把 orRequired 单独作为一个 OR 组加入（否则后续检查会把全部组合过滤掉） ***
        groupOptions.add(buildOrOptions(orRequired));

        // 3) 笛卡尔积遍历所有选择（每组选择一项），构建组合
        List<List<String>> results = new ArrayList<>();
        backtrackCombine(groupOptions, 0, new ArrayList<>(), results, N);

        // 4) 对每个组合，追加 mustHave（如果 not present），并确保 orRequired 至少包含一个（保险检查）
        Set<String> seen = new LinkedHashSet<>(); // 用于去重（顺序稳定）
        List<Set<String>> finalList = new ArrayList<>();

        for (List<String> baseCombo : results) {
            // 去重和排序保持稳定：先使用 LinkedHashSet 保持插入顺序
            LinkedHashSet<String> comboSet = new LinkedHashSet<>(baseCombo);
            // 追加 mustHave（如果尚未包含）
            for (String m : mustHave) comboSet.add(m);

            // 检查 orRequired: 必须包含 orRequired 中至少一个（作为双重保险）
            boolean hasOrReq = false;
            for (String r : orRequired) {
                if (comboSet.contains(r)) {
                    hasOrReq = true;
                    break;
                }
            }
            if (!hasOrReq) continue;

            // 转为 List
//            List<String> finalCombo = new ArrayList<>(comboSet);
            Set<String> finalCombo = comboSet;

            // 去重：用字符串 key
            String key = String.join("||", finalCombo);
            if (!seen.contains(key)) {
                seen.add(key);
                finalList.add(finalCombo);
            }
        }

        return finalList;
    }

    // 生成 list 的所有子集（包含空集合）
    private static List<List<String>> generateAllSubsets(List<String> list) {
        List<List<String>> subsets = new ArrayList<>();
        int n = list.size();
        int total = 1 << n; // 2^n
        for (int mask = 0; mask < total; mask++) {
            List<String> sub = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    sub.add(list.get(i));
                }
            }
            subsets.add(sub);
        }
        return subsets;
    }

    // 构建 OR 组的选项集合：空 + 每个单项
    private static List<List<String>> buildOrOptions(List<String> group) {
        List<List<String>> opts = new ArrayList<>();
        opts.add(Collections.emptyList()); // 不选
        for (String e : group) {
            opts.add(Collections.singletonList(e));
        }
        return opts;
    }

    // 回溯：按 groupOptions 笛卡尔积构造 base 组合（未包含 mustHave），并筛选至少 N（基于 base 的大小）
    private static void backtrackCombine(List<List<List<String>>> groupOptions,
                                         int idx,
                                         List<String> cur,
                                         List<List<String>> results,
                                         int N) {
        if (idx == groupOptions.size()) {
            // 满足最小选中数（N）条件
            // 注意：N 是要求在可选组中至少选 N 个（不包括 mustHave）
            if (cur.size() >= N) {
                results.add(new ArrayList<>(cur));
            }
            return;
        }

        for (List<String> choice : groupOptions.get(idx)) {
            // append choice
            int beforeSize = cur.size();
            if (!choice.isEmpty()) cur.addAll(choice);

            backtrackCombine(groupOptions, idx + 1, cur, results, N);

            // 回退
            while (cur.size() > beforeSize) cur.remove(cur.size() - 1);
        }
    }


    // ----------------------------- 示例 main -----------------------------


    public static void main(String[] args) {
        // 示例：要求至少选 2 个（可选组中）
        int N = 2;
        List<Set<String>> combos = generateCombinationsNew(N);
        System.out.println("共生成组合数量: " + combos.size());
        // 打印前 80 个示例（避免控制台爆炸）
        int limit = Math.min(80, combos.size());
        for (int i = 0; i < limit; i++) {
            System.out.println((i + 1) + ". " + combos.get(i));
        }
    }


}