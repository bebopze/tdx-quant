package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.BlockNewIdEnum;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaBlockNewRelaStockHisDO;
import com.bebopze.tdx.quant.dal.service.IQaBlockNewRelaStockHisService;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
public class TopBlockServiceImpl implements TopBlockService {


    // BacktestCache data = new BacktestCache();
    BacktestCache data = InitDataServiceImpl.data;


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private InitDataService initDataService;

    @Autowired
    @Lazy
    private BacktestStrategy backtestStrategy;

    @Autowired
    private IQaBlockNewRelaStockHisService qaBlockNewRelaStockHisService;


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public void refreshAll() {
        int N = 10;


        nDayHighTask(N);
        changePctTopTask(N);
    }


    @Override
    public void nDayHighTask(int N) {

        data = initDataService.initData();

        calcNDayHigh(N);
    }


    @Override
    public void changePctTopTask(int N) {

        data = initDataService.initData();


        // N日涨幅 > 25%
        calcChangePctTop(N, 25.0);
    }


    @Override
    public Map<String, Integer> topBlockRate(int blockNewId, LocalDate date, int resultType, int N) {

        List<QaBlockNewRelaStockHisDO> entityList = qaBlockNewRelaStockHisService.listByBlockNewIdDateAndLimit(blockNewId, date, N);


        Map<String, Integer> rateMap = Maps.newHashMap();
        entityList.forEach(e -> {


            // String result = e.getResult();
            // String result = e.getYjhyLv1Result();      // 1级 研究行业  ->  30个
            // String result = e.getPthyLv2Result();      // 2级 普通行业  ->  56个


            String result = null;
            if (resultType == 2) {
                result = e.getPthyLv2Result();      //  2级  普通行业  ->  56个
            } else if (resultType == 4) {
                result = e.getGnResult();           // (3级) 概念板块  ->  380个
            } else if (resultType == 12) {
                result = e.getYjhyLv1Result();      //  1级  研究行业  ->  30个
            } else if (resultType == 0) {
                result = e.getResult();             //  2级  普通行业   +   (3级) 概念板块
            }


            List<BlockTopInfoDTO> infoList = JSON.parseArray(result, BlockTopInfoDTO.class);


            BlockTopInfoDTO blockTopInfoDTO = infoList.get(0);
            String blockCode = blockTopInfoDTO.getBlockCode();
            String blockName = blockTopInfoDTO.getBlockName();


            rateMap.merge(blockCode + "-" + blockName, 1, Integer::sum);
        });


        // 按 value 倒序排序
        return reverseSortByValue(rateMap);
    }


    @Override
    public List<TopBlockDTO> topBlockRateInfo(int blockNewId, LocalDate date, int resultType, int N) {

        List<QaBlockNewRelaStockHisDO> entityList = qaBlockNewRelaStockHisService.listByBlockNewIdDateAndLimit(blockNewId, date, N);


        Map<String, List<TopStockDTO>> blockCode_topStockList_map = Maps.newHashMap();


        Map<String, Integer> rateMap = Maps.newHashMap();
        for (QaBlockNewRelaStockHisDO e : entityList) {


            // String result = e.getResult();
            // String result = e.getYjhyLv1Result();      // 1级 研究行业  ->  30个
            // String result = e.getPthyLv2Result();      // 2级 普通行业  ->  56个


            String result = null;
            if (resultType == 2) {
                result = e.getPthyLv2Result();      //  2级  普通行业  ->  56个
            } else if (resultType == 4) {
                result = e.getGnResult();           // (3级) 概念板块  ->  380个
            } else if (resultType == 12) {
                result = e.getYjhyLv1Result();      //  1级  研究行业  ->  30个
            } else if (resultType == 0) {
                result = e.getResult();             //  2级  普通行业   +   (3级) 概念板块
            }


            List<BlockTopInfoDTO> dtoList = JSON.parseArray(result, BlockTopInfoDTO.class);

            BlockTopInfoDTO dto = dtoList.get(0);
            String blockCode = dto.getBlockCode();
            String blockName = dto.getBlockName();


            rateMap.merge(blockCode + "-" + blockName, 1, Integer::sum);


            // ---------------------------------------------------------------------------------------------------------


            // TOP1 板块  -  stockCodeSet
            Map<String, Double> stockCode_rps强度_map = Maps.newHashMap();
            for (String stockCode : dto.getStockCodeSet()) {


                if (stockCode.length() < 6) {
                    // 保证6位补零（反序列化 bug ： 002755   ->   2755）
                    stockCode = String.format("%06d", Integer.parseInt(stockCode));


                    log.debug("topBlockRateInfo - 反序列化bug：补0     >>>     stockCode : {} , stockName : {}",
                              stockCode, data.codeStockMap.get(stockCode).getName());
                }


                BaseStockDO stockDO = data.codeStockMap.get(stockCode);


                StockFun fun = data.stockFunMap.computeIfAbsent(stockCode, k -> new StockFun(k, stockDO));
                Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                Integer idx = dateIndexMap.get(date);
                // 停牌  /  当前date -> 非交易日
                int count = 0;
                while (idx == null && count++ < 50) {
                    // 交易日 往前一位
                    date = backtestStrategy.tradeDateDecr(date);
                    idx = dateIndexMap.get(date);
                }


                // 个股 - RPS强度
                double RPS三线和 = fun.RPS三线和()[idx];
                stockCode_rps强度_map.put(stockCode, NumUtil.of(RPS三线和));
            }


            // 按 rps强度 倒序排序
            Map<String, Double> rpsSortMap = reverseSortByValue(stockCode_rps强度_map);
            log.debug("topBlockRateInfo     >>>     block : {} , stock_rpsSortMap : {}", blockCode + "-" + blockName, JSON.toJSONString(rpsSortMap));


            // stock - TOP10（RPS强度）
            List<TopStockDTO> topStockDTOList = stockCode_rps强度_map.entrySet().stream().map(entry -> {

                                                                         String stockCode = entry.getKey();
                                                                         double rps三线和 = entry.getValue();
                                                                         String stockName = data.stock__codeNameMap.get(stockCode);

                                                                         return new TopStockDTO(stockCode, stockName, rps三线和);

                                                                     }).sorted(Comparator.comparing(TopStockDTO::getRps三线和).reversed())
                                                                     .limit(10)
                                                                     .collect(Collectors.toList());


            blockCode_topStockList_map.put(blockCode, topStockDTOList);
        }


        // block - TOP （主线板块 TOP1 - 天数）
        List<TopBlockDTO> topBlockList = rateMap.entrySet().stream().map(entry -> {
                                                    String block = entry.getKey();
                                                    int topDay = entry.getValue();

                                                    String[] blockArr = block.split("-");


                                                    String blockCode = blockArr[0];
                                                    String blockName = blockArr[1];
                                                    List<TopStockDTO> topStockList = blockCode_topStockList_map.get(blockCode);

                                                    TopBlockDTO topBlockDTO = new TopBlockDTO(blockCode, blockName, topDay, topStockList);
                                                    return topBlockDTO;
                                                })
                                                .sorted(Comparator.comparing(TopBlockDTO::getTopDay).reversed())
                                                .collect(Collectors.toList());


        return topBlockList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class TopBlockDTO {
        private String blockCode;
        private String blockName;
        private int topDay;

        // 当日最强
        private List<TopStockDTO> topStockList;
    }

    @Data
    @AllArgsConstructor
    public static class TopStockDTO {
        private String stockCode;
        private String stockName;

        private double rps三线和;
    }


    // -----------------------------------------------------------------------------------------------------------------


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


        Integer blockNewId = BlockNewIdEnum.百日新高.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__highMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    private void calcChangePctTop(int N, double limitChangePct) {


        // --------------------------------------------------- 日期 - N日涨幅 TOP100（个股code 列表）


        // 日期 - 涨幅榜（个股code 列表）
        Map<LocalDate, Set<String>> date_stockCodeSet__topMap = Maps.newConcurrentMap();


        // 遍历计算   =>   每日 - 涨幅榜（个股code 列表）
        data.stockDOList.parallelStream().forEach(stockDO -> {

            try {
                String stockCode = stockDO.getCode();


                StockFun fun = data.stockFunMap.computeIfAbsent(stockCode, k -> new StockFun(k, stockDO));


                // N日涨幅
                double[] N日涨幅_arr = TdxExtFun.changePct(fun.getClose(), N);

                Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                // 日期 - N日新高（code列表）
                dateIndexMap.forEach((date, idx) -> {

                    double N日涨幅 = N日涨幅_arr[idx];

                    // N涨幅 > 25%
                    if (N日涨幅 > limitChangePct) {
                        date_stockCodeSet__topMap.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(stockCode);
                    }
                });


            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.涨幅榜.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    /**
     * 按 板块 分类统计
     *
     * @param date
     * @param filter_stockCodeSet
     * @param blockNewId          1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-均线大多头；
     */
    private void blockSum(LocalDate date, Set<String> filter_stockCodeSet, Integer blockNewId) {


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


        // 全量 TOP个股   ->   全量板块
        Set<BaseBlockDO> topBlockDOSet = Sets.newHashSet();


        // 百日新高（个股列表）   ->   按  板块  分类
        filter_stockCodeSet.forEach(stockCode -> {


            // TOP个股 - 板块列表
            Set<String> blockCodeSet = data.stockCode_blockCodeSet_Map.getOrDefault(stockCode, Collections.emptySet());


            blockCodeSet.forEach(blockCode -> {

                BaseBlockDO blockDO = data.codeBlockMap.get(blockCode);
                topBlockDOSet.add(blockDO);


                // 板块 - 分类（百日新高-个股）
                Integer type = blockDO.getType();
                Integer endLevel = blockDO.getEndLevel();


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

            });
        });


        // -----------------------------------------------------------------------------------


        // ----------------- map -> DTO

        List<BlockTopInfoDTO> pthy_lv3_List = convertMap2DTOList(pthy_3_map);
        List<BlockTopInfoDTO> gnList = convertMap2DTOList(gn_map);
        List<BlockTopInfoDTO> yjhy_lv3_List = convertMap2DTOList(yjhy_3_map);


        // -----------------------------------------------------------------------------------


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


//        // --------------- 概念 - LV2-普通行业（废止）
//
//        gn_map.forEach((blockCode, stockCodeSet) -> {
//
//            BaseBlockDO block_lv2 = BacktestCache.getPBlock(blockCode, 2);
//            if (null != block_lv2) {
//                pthy_2_map.computeIfAbsent(block_lv2.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);
//
//                BaseBlockDO block_lv1 = BacktestCache.getPBlock(block_lv2.getCode(), 1);
//                pthy_1_map.computeIfAbsent(block_lv1.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);
//            }
//        });


        // -------------------------------------------------------------------------------------------------------------


        List<BlockTopInfoDTO> pthy_lv2_List = convertMap2DTOList(pthy_2_map);
        List<BlockTopInfoDTO> pthy_lv1_List = convertMap2DTOList(pthy_1_map);


        List<BlockTopInfoDTO> yjhy_lv2_List = convertMap2DTOList(yjhy_2_map);
        List<BlockTopInfoDTO> yjhy_lv1_List = convertMap2DTOList(yjhy_1_map);


        // -------------------------------------------------------------------------------------------------------------


        // 行业 + 概念
        List<BlockTopInfoDTO> resultList = Lists.newArrayList();

        resultList.addAll(gnList);
        resultList.addAll(pthy_lv2_List);


        // -------------------------------------------------------------------------------------------------------------


        // 百日新高 result  ->  DB


        QaBlockNewRelaStockHisDO entity = new QaBlockNewRelaStockHisDO();

        // 1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-均线大多头；
        entity.setBlockNewId(blockNewId);
        entity.setDate(date);
        entity.setStockIdList(String.join(",", filter_stockCodeSet));


        entity.setYjhyLv1Result(JSON.toJSONString(sortAndRank(yjhy_lv1_List)));
        entity.setYjhyLv2Result(JSON.toJSONString(sortAndRank(yjhy_lv2_List)));
        entity.setYjhyLv3Result(JSON.toJSONString(sortAndRank(yjhy_lv3_List)));

        entity.setPthyLv1Result(JSON.toJSONString(sortAndRank(pthy_lv1_List)));
        entity.setPthyLv2Result(JSON.toJSONString(sortAndRank(pthy_lv2_List)));
        entity.setPthyLv3Result(JSON.toJSONString(sortAndRank(pthy_lv3_List)));

        entity.setGnResult(JSON.toJSONString(sortAndRank(gnList)));

        entity.setResult(JSON.toJSONString(sortAndRank(resultList)));


        qaBlockNewRelaStockHisService.save(entity);
    }


    /**
     * map -> DTO List
     *
     * @param blockCode_stockCodeSet_map
     */
    private List<BlockTopInfoDTO> convertMap2DTOList(Map<String, Set<String>> blockCode_stockCodeSet_map) {
        List<BlockTopInfoDTO> dtoList = Lists.newArrayList();

        blockCode_stockCodeSet_map.forEach((blockCode, stockCodeSet) -> {

            // ----------------- dto
            BlockTopInfoDTO dto = new BlockTopInfoDTO();

            dto.setBlockId(data.block__codeIdMap.get(blockCode));
            dto.setBlockCode(blockCode);
            dto.setBlockName(data.block__codeNameMap.get(blockCode));

            dto.setStockCodeSet(stockCodeSet);

            dtoList.add(dto);
        });

        return dtoList;
    }


    private List<BlockTopInfoDTO> sortAndRank(List<BlockTopInfoDTO> dtoList) {

        List<BlockTopInfoDTO> sortList = dtoList.stream()
                                                .filter(e -> e.getSize() > 0)
                                                .sorted(Comparator.comparing(BlockTopInfoDTO::getSize).reversed())
                                                .collect(Collectors.toList());

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

        private List<BlockTopInfoDTO> infoList;
    }


    @Data
    public static class BlockTopInfoDTO {

        private Long blockId;
        private String blockCode;
        private String blockName;

        // @JSONField(deserializeUsing = StringSetDeserializer.class)
        private Set<String> stockCodeSet;
        private int size = 0;

        // 排名
        private int rank = 1;


        public int getSize() {
            return stockCodeSet != null ? stockCodeSet.size() : 0;
        }
    }


}