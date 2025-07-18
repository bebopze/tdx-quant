package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.QaBlockNewRelaStockHisDO;
import com.bebopze.tdx.quant.dal.service.IQaBlockNewRelaStockHisService;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.IndexService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.util.MapUtil.reverseSortByValue;


/**
 * 大盘量化
 *
 * @author: bebopze
 * @date: 2025/7/13
 */
@Slf4j
@Service
public class IndexServiceImpl implements IndexService {


    // BacktestCache data = new BacktestCache();
    BacktestCache data = InitDataServiceImpl.data;


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private InitDataService initDataService;


    @Autowired
    private IQaBlockNewRelaStockHisService qaBlockNewRelaStockHisService;


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public void nDayHighTask(int N) {

        data = initDataService.initData();

        calcNDayHigh(N);
    }


    @Override
    public Map<String, Integer> nDayHighRate(LocalDate date, int N) {

        List<QaBlockNewRelaStockHisDO> entityList = qaBlockNewRelaStockHisService.listByDateAndLimit(date, N);


        Map<String, Integer> rateMap = Maps.newHashMap();
        entityList.forEach(e -> {

            // String result = e.getResult();
            // String result = e.getYjhyLv1Result();   // 1级 研究行业  ->  30个
            String result = e.getPthyLv2Result();      // 2级 普通行业  ->  56个

            List<BlockTopInfo> infoList = JSON.parseArray(result, BlockTopInfo.class);


            BlockTopInfo blockTopInfo = infoList.get(0);
            String blockCode = blockTopInfo.getBlockCode();
            String blockName = blockTopInfo.getBlockName();


            rateMap.merge(blockCode + "-" + blockName, 1, Integer::sum);
        });


        // 按 value 倒序排序
        return reverseSortByValue(rateMap);
    }


    private void calcNDayHigh(int N) {


        // --------------------------------------------------- 日期 - 百日新高（个股code 列表）


        // 日期 - 百日新高（个股code 列表）
        Map<LocalDate, Set<String>> date_stockCodeSet__highMap = Maps.newConcurrentMap();


        // 遍历计算   =>   每日 - 百日新高（个股code 列表）
        data.stockDOList.parallelStream().forEach(stockDO -> {

            try {
                String stockCode = stockDO.getCode();


                StockFun fun = data.stockFunMap.computeIfAbsent(stockCode, k -> new StockFun(k, stockDO));


                // N日新高
                // boolean[] N日新高_arr = fun.N日新高(N);
                // boolean[] N日新高_arr = fun.创N日新高(N);

                boolean[] N日新高_arr = fun.百日新高(N);
                // date - idx
                Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                // 日期 - N日新高（code列表）
                dateIndexMap.forEach((date, idx) -> {
                    if (N日新高_arr[idx]) {
                        date_stockCodeSet__highMap.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(stockCode);
                    }
                });


            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });


        // --------------------------------------------------- 按 板块 分类


        qaBlockNewRelaStockHisService.deleteAll(1L, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__highMap);
        sortMap.forEach(this::blockSum);
    }


    /**
     * 按 板块 分类统计
     *
     * @param date
     * @param filter_stockCodeSet
     */
    private void blockSum(LocalDate date, Set<String> filter_stockCodeSet) {


        QaBlockNewRelaStockHisDO entity = new QaBlockNewRelaStockHisDO();
        entity.setBlockNewId(1L);
        entity.setDate(date);
        entity.setStockIdList(String.join(",", filter_stockCodeSet));
        entity.setType(1);


        List<BlockTopInfo> pthy_lv1_List = Lists.newArrayList();
        List<BlockTopInfo> pthy_lv2_List = Lists.newArrayList();
        List<BlockTopInfo> pthy_lv3_List = Lists.newArrayList();


        List<BlockTopInfo> yjhy_lv1_List = Lists.newArrayList();
        List<BlockTopInfo> yjhy_lv2_List = Lists.newArrayList();
        List<BlockTopInfo> yjhy_lv3_List = Lists.newArrayList();

        List<BlockTopInfo> gnList = Lists.newArrayList();


        // -------------------------------------------------------------------------------------------------------------


        // 百日新高   =>   板块 - 个股列表


        // 4-概念板块
        Map<String, Set<String>> gn_map = Maps.newHashMap();


        // 只关联 level3     =>     leve2/leve1   ->   根据 level3 倒推计算


        // 2-普通行业
        Map<String, Set<String>> pthy_1_map = Maps.newHashMap();
        Map<String, Set<String>> pthy_2_map = Maps.newHashMap();
        Map<String, Set<String>> pthy_3_map = Maps.newHashMap();


        // 12-研究行业
        Map<String, Set<String>> yjhy_1_map = Maps.newHashMap();
        Map<String, Set<String>> yjhy_2_map = Maps.newHashMap();
        Map<String, Set<String>> yjhy_3_map = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------


        // 遍历板块
        data.blockDOList.forEach(blockDO -> {


            String blockCode = blockDO.getCode();
            Integer type = blockDO.getType();
            Integer endLevel = blockDO.getEndLevel();


            // 板块 - 个股列表
            Set<String> blockCode_stockCodeSet = data.blockCode_stockCodeSet_Map.getOrDefault(blockCode, Collections.emptySet());


            // 百日新高（个股列表）
            filter_stockCodeSet.forEach(stockCode -> {


                // 百日新高（个股）  ->   in 板块
                if (blockCode_stockCodeSet.contains(stockCode)) {


                    // 板块 - 分类（百日新高-个股）


                    // tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；


                    // 2-普通行业 - 一级/二级/三级分类（细分行业）
                    if (Objects.equals(type, 2) && Objects.equals(endLevel, 1)) {
                        pthy_3_map.computeIfAbsent(blockCode, k -> Sets.newHashSet()).add(stockCode);
                    }
                    // 4-概念板块
                    else if (Objects.equals(type, 4)) {
                        gn_map.computeIfAbsent(blockCode, k -> Sets.newHashSet()).add(stockCode);
                    }
                    // 12-研究行业 - 一级/二级/三级分类
                    else if (Objects.equals(type, 12) && Objects.equals(endLevel, 1)) {
                        yjhy_3_map.computeIfAbsent(blockCode, k -> Sets.newHashSet()).add(stockCode);
                    }
                }
            });


            // ----------------- info
            BlockTopInfo info = new BlockTopInfo();
            info.setBlockId(data.block__codeIdMap.get(blockCode));
            info.setBlockCode(blockCode);
            info.setBlockName(data.block__codeNameMap.get(blockCode));


            if (Objects.equals(type, 2) && Objects.equals(endLevel, 1)) {

                info.setStockCodeSet(pthy_3_map.get(blockCode));
                pthy_lv3_List.add(info);

            } else if (Objects.equals(type, 4)) {

                info.setStockCodeSet(gn_map.get(blockCode));
                gnList.add(info);

            } else if (Objects.equals(type, 12) && Objects.equals(endLevel, 1)) {

                info.setStockCodeSet(yjhy_3_map.get(blockCode));
                yjhy_lv3_List.add(info);
            }
        });


        // -------------------------------------------------------------------------------------------------------------


        // ----------------- level 1/2

        pthy_3_map.forEach((blockCode, stockCodeSet) -> {

            BaseBlockDO block_lv2 = BacktestCache.getPBlock(blockCode, 2);
            pthy_2_map.computeIfAbsent(block_lv2.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);


            BaseBlockDO block_lv1 = BacktestCache.getPBlock(block_lv2.getCode(), 1);
            pthy_1_map.computeIfAbsent(block_lv1.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);
        });


        yjhy_3_map.forEach((blockCode, stockCodeSet) -> {

            BaseBlockDO block_lv2 = BacktestCache.getPBlock(blockCode, 2);
            yjhy_2_map.computeIfAbsent(block_lv2.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);


            BaseBlockDO block_lv1 = BacktestCache.getPBlock(block_lv2.getCode(), 1);
            yjhy_1_map.computeIfAbsent(block_lv1.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);
        });


        // --------------- 概念 - LV2-普通行业

        gn_map.forEach((blockCode, stockCodeSet) -> {

            BaseBlockDO block_lv2 = BacktestCache.getPBlock(blockCode, 2);
            pthy_2_map.computeIfAbsent(block_lv2.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);


            BaseBlockDO block_lv1 = BacktestCache.getPBlock(block_lv2.getCode(), 1);
            pthy_1_map.computeIfAbsent(block_lv1.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);
        });


        // -------------------------------------------------------------------------------------------------------------


        convertInfo__addList(pthy_2_map, pthy_lv2_List);
        convertInfo__addList(pthy_1_map, pthy_lv1_List);


        convertInfo__addList(yjhy_2_map, yjhy_lv2_List);
        convertInfo__addList(yjhy_1_map, yjhy_lv1_List);


        // -------------------------------------------------------------------------------------------------------------


        // 行业 + 概念
        List<BlockTopInfo> bkList = Lists.newArrayList();


        bkList.addAll(gnList);
        // bkList.addAll(yjhy_lv1_List);
        bkList.addAll(pthy_lv2_List);


        // -------------------------------------------------------------------------------------------------------------


        // 百日新高 result  ->  DB


        entity.setYjhyLv1Result(JSON.toJSONString(sortAndRank(yjhy_lv1_List)));
        entity.setYjhyLv2Result(JSON.toJSONString(sortAndRank(yjhy_lv2_List)));
        entity.setYjhyLv3Result(JSON.toJSONString(sortAndRank(yjhy_lv3_List)));

        entity.setPthyLv1Result(JSON.toJSONString(sortAndRank(pthy_lv1_List)));
        entity.setPthyLv2Result(JSON.toJSONString(sortAndRank(pthy_lv2_List)));
        entity.setPthyLv3Result(JSON.toJSONString(sortAndRank(pthy_lv3_List)));

        entity.setGnResult(JSON.toJSONString(sortAndRank(gnList)));


        entity.setResult(JSON.toJSONString(sortAndRank(bkList)));

        qaBlockNewRelaStockHisService.save(entity);
    }


    private void convertInfo__addList(Map<String, Set<String>> pthy_2_map, List<BlockTopInfo> pthy_lv2_List) {

        pthy_2_map.forEach((blockCode, stockCodeSet) -> {

            // ----------------- info
            BlockTopInfo info = new BlockTopInfo();
            info.setBlockId(data.block__codeIdMap.get(blockCode));
            info.setBlockCode(blockCode);
            info.setBlockName(data.block__codeNameMap.get(blockCode));

            info.setStockCodeSet(stockCodeSet);
            pthy_lv2_List.add(info);
        });
    }


    private List<BlockTopInfo> sortAndRank(List<BlockTopInfo> infoList) {

        List<BlockTopInfo> sortList = infoList.stream().filter(e -> e.getSize() > 0).sorted(Comparator.comparing(BlockTopInfo::getSize).reversed()).collect(Collectors.toList());
        for (int i = 0; i < sortList.size(); i++) {
            sortList.get(i).setRank(i + 1);
        }

        return sortList;
    }


// -----------------------------------------------------------------------------------------------------------------


    @Data
    public static class ResultInfo {

        // 1-概念；2-普通行业；3-研究行业；
        private int type;

        private int level = 3;

        private List<BlockTopInfo> infoList;
    }


    @Data
    public static class BlockTopInfo {

        private Long blockId;
        private String blockCode;
        private String blockName;

        private Set<String> stockCodeSet;
        private int size = 0;

        // 排名
        private int rank = 1;


        public int getSize() {
            return stockCodeSet != null ? stockCodeSet.size() : 0;
        }
    }


}