package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.BlockNewIdEnum;
import com.bebopze.tdx.quant.common.constant.BlockTypeEnum;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.ParallelCalcUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaBlockNewRelaStockHisDO;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.dal.service.IQaBlockNewRelaStockHisService;
import com.bebopze.tdx.quant.indicator.BlockFun;
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
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.cache.BacktestCache.*;
import static com.bebopze.tdx.quant.common.util.MapUtil.reverseSortByValue;


/**
 * 主线板块
 *
 * @author: bebopze
 * @date: 2025/7/13
 */
@Slf4j
@Service
public class TopBlockServiceImpl implements TopBlockService {


    BacktestCache data = InitDataServiceImpl.data;


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private InitDataService initDataService;

    @Autowired
    private IBaseStockService baseStockService;

    @Lazy
    @Autowired
    private BacktestStrategy backtestStrategy;

    @Autowired
    private IQaBlockNewRelaStockHisService qaBlockNewRelaStockHisService;


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public void refreshAll() {
        log.info("-------------------------------- TopBlock - refreshAll     >>>     start");


        // 1- N日新高
        nDayHighTask(100);

        // 2- N日涨幅 - TOP榜
        changePctTopTask(10);

        // 3-RPS红
        rpsRedTask(85);

        // TODO   4-二阶段
        // stage2Task();

        // 5-大均线多头
        longTermMABullStackTask();

        // TODO   6-均线大多头
        // bullMAStackTask();


        // GC
        // data.clear();


        // 11- 板块AMO-Top1
        blockAmoTopTask();


        // GC
        data.clear();


        log.info("-------------------------------- TopBlock - refreshAll     >>>     end");
    }


    @TotalTime
    @Override
    public void nDayHighTask(int N) {
        log.info("-------------------------------- nDayHighTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache();

        calcNDayHigh(N);


        log.info("-------------------------------- nDayHighTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void changePctTopTask(int N) {
        log.info("-------------------------------- changePctTopTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache();


        // N日涨幅 > 25%
        calcChangePctTop(N, 25.0);


        log.info("-------------------------------- changePctTopTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void rpsRedTask(double RPS) {
        log.info("-------------------------------- rpsRedTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache();


        // RPS红
        calcRpsRed(RPS);


        log.info("-------------------------------- rpsRedTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void stage2Task() {
        log.info("-------------------------------- stage2Task     >>>     start");
        long start = System.currentTimeMillis();


        initCache();


        // TODO   二阶段
        calcStage2();


        log.info("-------------------------------- stage2Task     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void longTermMABullStackTask() {
        log.info("-------------------------------- longTermMABullStackTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache();


        // 大均线多头
        //calcBullMAStack();
        calcLongTermMABullStack();


        log.info("-------------------------------- longTermMABullStackTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void bullMAStackTask() {
        log.info("-------------------------------- bullMAStackTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache();


        // TODO   均线大多头
        calcBullMAStack();


        log.info("-------------------------------- bullMAStackTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void blockAmoTopTask() {
        log.info("-------------------------------- blockAmoTopTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache();

        // 2min 6s     -     21.4s
        calcBlockAmoTop();

        // totalTime : 21.7s
        // calcBlockAmoTop2();


        log.info("-------------------------------- blockAmoTopTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public Map<String, Integer> topBlockRate(int blockNewId, LocalDate date, int resultType, int N) {


        // ------------------------------------- hyLevel -> 缺省值：    LV1-研究行业   LV2-普通行业   LV3-概念板块
        int hyLevel = 0;

        if (resultType == 2) {
            hyLevel = 2;                //  2级  普通行业  ->  56个
        } else if (resultType == 4) {
            hyLevel = 3;                // (3级) 概念板块  ->  380个
        } else if (resultType == 12) {
            hyLevel = 1;                //  1级  研究行业  ->  30个
        } else if (resultType == 0) {
            hyLevel = 0;                //  2级  普通行业   +   (3级) 概念板块
        }


        return topBlockRate(blockNewId, date, resultType, hyLevel, N);
    }

    @Override
    public Map<String, Integer> topBlockRate(int blockNewId, LocalDate date, int resultType, Integer hyLevel, int N) {

        if (hyLevel == null) {
            return topBlockRate(blockNewId, date, resultType, N);
        }


        // ------------------------------------------------------------------------------


        List<QaBlockNewRelaStockHisDO> entityList = qaBlockNewRelaStockHisService.listByBlockNewIdDateAndLimit(blockNewId, date, N);


        Map<String, Integer> rateMap = Maps.newHashMap();
        entityList.forEach(e -> {


            String result = getResultByTypeAndLevel(e, resultType, hyLevel);
            List<BlockTopInfoDTO> infoList = JSON.parseArray(result, BlockTopInfoDTO.class);


            if (CollectionUtils.isNotEmpty(infoList)) {

                BlockTopInfoDTO blockTopInfoDTO = infoList.get(0);
                String blockCode = blockTopInfoDTO.getBlockCode();
                String blockName = blockTopInfoDTO.getBlockName();


                rateMap.merge(blockCode + "-" + blockName, 1, Integer::sum);
            }
        });


        // 按 value 倒序排序
        return reverseSortByValue(rateMap);
    }

    @Override
    public List<ResultTypeLevelRateDTO> topBlockRateAll(int blockNewId, LocalDate date, int N) {


        List<QaBlockNewRelaStockHisDO> entityList = qaBlockNewRelaStockHisService.listByBlockNewIdDateAndLimit(blockNewId, date, N);


        // key - totalDay
        Map<String, Integer> rateMap = Maps.newHashMap();
        entityList.forEach(e -> {


            // 概念板块
            List<BlockTopInfoDTO> gn_list = JSON.parseArray(e.getGnResult(), BlockTopInfoDTO.class);

            // 普通行业
            List<BlockTopInfoDTO> pthy_lv1_List = JSON.parseArray(e.getPthyLv1Result(), BlockTopInfoDTO.class);
            List<BlockTopInfoDTO> pthy_lv2_List = JSON.parseArray(e.getPthyLv2Result(), BlockTopInfoDTO.class);
            List<BlockTopInfoDTO> pthy_lv3_List = JSON.parseArray(e.getPthyLv3Result(), BlockTopInfoDTO.class);

            // 研究行业
            List<BlockTopInfoDTO> yjhy_lv1_List = JSON.parseArray(e.getYjhyLv1Result(), BlockTopInfoDTO.class);
            List<BlockTopInfoDTO> yjhy_lv2_List = JSON.parseArray(e.getYjhyLv2Result(), BlockTopInfoDTO.class);
            List<BlockTopInfoDTO> yjhy_lv3_List = JSON.parseArray(e.getYjhyLv3Result(), BlockTopInfoDTO.class);


            // ------------------------------------------------------------


            // 取 Top1   ->   resultType + hyLevel + blockCode + blockName


            // 4-概念
            convertKey___mergeSum(rateMap, 4, 1, gn_list);
            // convertKey___mergeSum(rateMap, 4, 2, gn_list);  // DEL
            // convertKey___mergeSum(rateMap, 4, 3, gn_list);  // DEL

            // 2-普通行业
            convertKey___mergeSum(rateMap, 2, 1, pthy_lv1_List);
            convertKey___mergeSum(rateMap, 2, 2, pthy_lv2_List);
            convertKey___mergeSum(rateMap, 2, 3, pthy_lv3_List);

            // 12-研究行业
            convertKey___mergeSum(rateMap, 12, 1, yjhy_lv1_List);
            convertKey___mergeSum(rateMap, 12, 2, yjhy_lv2_List);
            convertKey___mergeSum(rateMap, 12, 3, yjhy_lv3_List);
        });


        // --------------------------------------------------------------------------------


        // 按 value 倒序排序
        Map<String, Integer> sort__rateMap = reverseSortByValue(rateMap);


        // --------------------------------------------------------------------------------


        Map<String, ResultTypeLevelRateDTO> typeRateMap = Maps.newHashMap();


        sort__rateMap.forEach((key, totalDay) -> {

            String[] keyArr = key.split("-");

            int resultType = Integer.parseInt(keyArr[0]);
            int hyLevel = Integer.parseInt(keyArr[1]);

            String blockCode = keyArr[2];
            String blockName = keyArr[3];


            RateMapDTO rateMapDTO = new RateMapDTO(/*resultType, hyLevel,*/ blockCode, blockName, totalDay);


            // -------------------------------------------–


            ResultTypeLevelRateDTO dto = new ResultTypeLevelRateDTO();
            dto.setResultType(resultType);
            dto.setHyLevel(hyLevel);


            typeRateMap.computeIfAbsent(resultType + "-" + hyLevel, k -> dto).getDtoList().add(rateMapDTO);
        });


        return Lists.newArrayList(typeRateMap.values()).stream().sorted(Comparator.comparing(ResultTypeLevelRateDTO::getResultType)).collect(Collectors.toList());
    }


    @Data
    public static class ResultTypeLevelRateDTO {

        private int resultType;

        private int hyLevel;


        List<RateMapDTO> dtoList = Lists.newArrayList();


        // ----------------------


        public String getResultTypeDesc() {
            return BlockTypeEnum.getDescByType(resultType);
        }
    }


    @Data
    @AllArgsConstructor
    public static class RateMapDTO {

        // private int resultType;
        // private int hyLevel;

        private String blockCode;
        private String blockName;

        private int totalDay;
    }


    private String convertKey___mergeSum(Map<String, Integer> rateMap,

                                         int resultType, int hyLevel, List<BlockTopInfoDTO> resultInfoList) {

        // 取 Top1
        BlockTopInfoDTO blockTop = resultInfoList.get(0);

        // resultType + hyLevel + blockCode + blockName
        String key = resultType + "-" + hyLevel + "-" + blockTop.getBlockCode() + "-" + blockTop.getBlockName();


        // Top1 天数累计
        rateMap.merge(key, 1, Integer::sum);


        return key;
    }


    @Override
    public List<TopBlockDTO> topBlockRateInfo(int blockNewId, LocalDate date, int resultType, int N) {


        initCache();


        // -----------------------------------------


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
                              stockCode, data.codeStockMap.getOrDefault(stockCode, new BaseStockDO()).getName());
                }


                // 基金北向 - 过滤   ->   Cache 中不存在
                BaseStockDO stockDO = data.codeStockMap.getOrDefault(stockCode, baseStockService.getByCode(stockCode));


                StockFun fun = data.getOrCreateStockFun(stockDO);
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


    private void initCache() {
        data = initDataService.initData();
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * resultType + level   ->   result
     *
     * @param e
     * @param resultType
     * @param hyLevel
     * @return
     */
    private String getResultByTypeAndLevel(QaBlockNewRelaStockHisDO e, int resultType, int hyLevel) {
        String result = null;


        // 2-普通行业
        if (resultType == 2) {

            if (hyLevel == 1) {
                result = e.getPthyLv1Result();      //  1级  普通行业  ->   13个
            } else if (hyLevel == 2) {
                result = e.getPthyLv2Result();      //  2级  普通行业  ->   56个
            } else if (hyLevel == 3) {
                result = e.getPthyLv3Result();      //  3级  普通行业  ->  110个（细分行业）
            }

        }

        // 4-概念板块
        else if (resultType == 4) {

            result = e.getGnResult();               // (3级) 概念板块  ->  270个

        }

        // 12-研究行业
        else if (resultType == 12) {

            if (hyLevel == 1) {
                result = e.getYjhyLv1Result();      //  1级  研究行业  ->   30个
            } else if (hyLevel == 2) {
                result = e.getYjhyLv2Result();      //  2级  研究行业  ->  127个
            } else if (hyLevel == 3) {
                result = e.getYjhyLv3Result();      //  3级  研究行业  ->  344个
            }

        } else if (resultType == 0) {
            result = e.getResult();                 //  2级  普通行业   +   (3级) 概念板块  ->  380个
        }


        return result;
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
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(data.stockDOList, 200, chunk -> {


            chunk.forEach(stockDO -> {

                try {

                    String stockCode = stockDO.getCode();
                    StockFun fun = data.getOrCreateStockFun(stockDO);


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
                    log.error("处理股票 {} 失败", stockDO.getCode(), e);
                }
            });
        });


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.百日新高.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__highMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcChangePctTop(int N, double limitChangePct) {


        // --------------------------------------------------- 日期 - N日涨幅榜（个股code 列表）


        // 日期 - 涨幅榜（个股code 列表）
        Map<LocalDate, Set<String>> date_stockCodeSet__topMap = Maps.newConcurrentMap();


        // 遍历计算   =>   每日 - 涨幅榜（个股code 列表）
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(data.stockDOList, 200, chunk -> {


            chunk.forEach(stockDO -> {

                try {
                    String stockCode = stockDO.getCode();
                    StockFun fun = data.getOrCreateStockFun(stockDO);


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
        });


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.涨幅榜.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcRpsRed(double RPS) {


        // --------------------------------------------------- 日期 - RPS红（个股code 列表）


        // 日期 - RPS红（个股code 列表）
        Map<LocalDate, Set<String>> date_stockCodeSet__topMap = Maps.newConcurrentMap();


        // 遍历计算   =>   每日 - RPS红（个股code 列表）
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(data.stockDOList, 200, chunk -> {


            chunk.forEach(stockDO -> {

                try {
                    String stockCode = stockDO.getCode();
                    StockFun fun = data.getOrCreateStockFun(stockDO);


                    // RPS红（ RPS一线红(95) || RPS双线红(90) || RPS三线红(85) ）
                    boolean[] RPS红_arr = fun.RPS红(RPS);

                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // 日期 - RPS红（code列表）
                    dateIndexMap.forEach((date, idx) -> {

                        if (RPS红_arr[idx]) {
                            date_stockCodeSet__topMap.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(stockCode);
                        }
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        });


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.RPS红.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcStage2() {


        // --------------------------------------------------- 日期 - 二阶段（个股code 列表）


        // 日期 - 二阶段（个股code 列表）
        Map<LocalDate, Set<String>> date_stockCodeSet__topMap = Maps.newConcurrentMap();


        // 遍历计算   =>   每日 - 二阶段（个股code 列表）
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(data.stockDOList, 200, chunk -> {


            chunk.forEach(stockDO -> {

                try {
                    String stockCode = stockDO.getCode();
                    StockFun fun = data.getOrCreateStockFun(stockDO);


                    // 二阶段
                    boolean[] RPS红_arr = fun.二阶段();

                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // 日期 - 二阶段（code列表）
                    dateIndexMap.forEach((date, idx) -> {

                        if (RPS红_arr[idx]) {
                            date_stockCodeSet__topMap.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(stockCode);
                        }
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        });


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.二阶段.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));

    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcLongTermMABullStack() {


        // --------------------------------------------------- 日期 - 大均线多头（个股code 列表）


        // 日期 - 大均线多头（个股code 列表）
        Map<LocalDate, Set<String>> date_stockCodeSet__topMap = Maps.newConcurrentMap();


        // 遍历计算   =>   每日 - 大均线多头（个股code 列表）
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(data.stockDOList, 200, chunk -> {


            chunk.forEach(stockDO -> {

                try {
                    String stockCode = stockDO.getCode();
                    StockFun fun = data.getOrCreateStockFun(stockDO);


                    // 大均线多头
                    boolean[] 大均线多头_arr = fun.大均线多头();

                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // 日期 - 大均线多头（code列表）
                    dateIndexMap.forEach((date, idx) -> {

                        if (大均线多头_arr[idx]) {
                            date_stockCodeSet__topMap.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(stockCode);
                        }
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        });


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.大均线多头.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcBullMAStack() {


        // --------------------------------------------------- 日期 - 均线大多头（个股code 列表）


        // 日期 - 均线大多头（个股code 列表）
        Map<LocalDate, Set<String>> date_stockCodeSet__topMap = Maps.newConcurrentMap();


        // 遍历计算   =>   每日 - 均线大多头（个股code 列表）
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(data.stockDOList, 200, chunk -> {


            chunk.forEach(stockDO -> {

                try {
                    String stockCode = stockDO.getCode();
                    StockFun fun = data.getOrCreateStockFun(stockDO);


                    // 均线大多头
                    boolean[] 均线大多头_arr = fun.均线大多头();

                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // 日期 - 均线大多头（code列表）
                    dateIndexMap.forEach((date, idx) -> {

                        if (均线大多头_arr[idx]) {
                            date_stockCodeSet__topMap.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(stockCode);
                        }
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        });


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.均线大多头.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcBlockAmoTop() {
        AtomicInteger x = new AtomicInteger(0);


        // 日期-板块类型-板块lv       -       AMO_blockCode_TreeMap
        // <groupKey: date|type|level, <AMO DESC, blockCode>>
        Map<String, TreeMap<Double, String>> date__block_type_lv_____amo_blockCode_TreeMap_____map = Maps.newHashMap();


        ParallelCalcUtil.chunkForEachWithProgress(data.dateList, 200, chunk -> {


            // data.dateList.forEach(date -> {
            chunk.forEach(date -> {


                for (BaseBlockDO blockDO : data.blockDOList) {


                    // 每 1 万次打印一次 debug 日志
                    if (x.incrementAndGet() % 100000 == 0) {
                        log.warn("calcBlockAmoTop     >>>     循环次数 x = " + x.get());
                    }


                    if (null == blockDO) {
                        log.debug("calcBlockAmoTop     >>>     date : {} , blockDO : {}", date, blockDO);
                        continue;
                    }


                    String blockCode = blockDO.getCode();


                    BlockFun fun = data.getOrCreateBlockFun(blockDO);


                    // value   ->   amo_blockCode_TreeMap
                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
                    double amo = getByDate(fun.getAmo(), dateIndexMap, date);


                    if (Double.isNaN(amo) || amo < 1) {
                        log.debug("calcBlockAmoTop     >>>     date : {} , blockCode : {} , amo : {}", date, blockCode, amo);
                        continue;
                    }


                    // key   ->   date|blockType|blockLevel
                    String key = date + "|" + blockDO.getType() + "|" + blockDO.getLevel();


                    // val   ->   AMO_blockCode_TreeMap
                    TreeMap<Double, String> amo_blockCode_Top1TreeMap = date__block_type_lv_____amo_blockCode_TreeMap_____map
                            .computeIfAbsent(key, k -> new TreeMap<>(Comparator.reverseOrder()));

                    // put 新 K-V
                    amo_blockCode_Top1TreeMap.put(amo, blockCode);

                    // 只保留 Top1
                    if (amo_blockCode_Top1TreeMap.size() > 1) {
                        // 移除排名第一以外的 所有k-v
                        amo_blockCode_Top1TreeMap.pollLastEntry();
                    }
                }
            });
        });


        // -------------------------------------------------------------------------------------------------------------


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.板块AMO_TOP1.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, QaBlockNewRelaStockHisDO> date_entity_map = Maps.newConcurrentMap();


        date__block_type_lv_____amo_blockCode_TreeMap_____map
                .keySet()
                        .

                parallelStream().

                forEach((date__block_type_lv) ->

                        {

                            // k -> v
                            TreeMap<Double, String> amo_blockCode_TreeMap = date__block_type_lv_____amo_blockCode_TreeMap_____map.get(date__block_type_lv);


                            // key   ->   date|blockType|blockLevel
                            String[] keyArr = date__block_type_lv.split("\\|");

                            LocalDate date = DateTimeUtil.parseDate_yyyy_MM_dd(keyArr[0]);


                            QaBlockNewRelaStockHisDO entity = date_entity_map.computeIfAbsent(date, k -> new QaBlockNewRelaStockHisDO());

                            entity.setBlockNewId(blockNewId);
                            entity.setDate(date);
                            entity.setStockIdList(null);


                            Map.Entry<Double, String> top1 = amo_blockCode_TreeMap.firstEntry();
                            if (top1 != null) {

                                double amo = top1.getKey();
                                String blockCode = top1.getValue();


                                BaseBlockDO top1_blockDO = data.codeBlockMap.get(blockCode);


                                blockAmoTop(date__block_type_lv, top1_blockDO, blockNewId, entity);
                            }
                        });


        // dateSort  ->  save
        qaBlockNewRelaStockHisService.saveBatch(new TreeMap<>(date_entity_map).

                                                        values());
    }


    private void calcBlockAmoTop2() {


        Integer blockNewId = BlockNewIdEnum.板块AMO_TOP1.getBlockNewId();
        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        // -------------------------------------------------------------------------------------------------------------
        AtomicInteger x = new AtomicInteger(0);


        List<BlockAmoRecord> kvList = Lists.newArrayList();

        data.dateList.forEach(date -> {


            for (BaseBlockDO blockDO : data.blockDOList) {


                // 每 1 万次打印一次 debug 日志
                if (x.incrementAndGet() % 100000 == 0) {
                    log.warn("calcBlockAmoTop     >>>     循环次数 x = " + x.get());
                }


                if (null == blockDO) {
                    log.debug("calcBlockAmoTop     >>>     date : {} , blockDO : {}", date, blockDO);
                    continue;
                }


                String blockCode = blockDO.getCode();


                BlockFun fun = data.getOrCreateBlockFun(blockDO);


                // value   ->   amo_blockCode_TreeMap
                Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
                double amo = getByDate(fun.getAmo(), dateIndexMap, date);


                if (Double.isNaN(amo) || amo < 1) {
                    log.debug("calcBlockAmoTop     >>>     date : {} , blockCode : {} , amo : {}", date, blockCode, amo);
                    continue;
                }


                kvList.add(new BlockAmoRecord(date, blockDO.getType(), blockDO.getLevel(), amo, blockCode));
            }
        });


        log.info("calcBlockAmoTop - kvList     >>>     共收集 {} 条有效记录", kvList.size());


        // -------------------------------------------------------------------------------------------------------------


        // 2. 按 key 分组：date|type|level
        Map<String, List<BlockAmoRecord>> grouped = kvList.stream()
                                                          .collect(Collectors.groupingBy(
                                                                  r -> r.date + "|" + r.type + "|" + r.level,
                                                                  // 使用 ConcurrentHashMap 便于 parallelStream 安全写入
                                                                  ConcurrentHashMap::new,
                                                                  Collectors.toList()
                                                          ));


        log.info("calcBlockAmoTop - grouped     >>>     共分组 {} 个 key", grouped.size());


        // -------------------------------------------------------------------------------------------------------------


        // 3. 并行处理每组，取 AMO 最大的记录
        Map<LocalDate, QaBlockNewRelaStockHisDO> dateEntityMap = new ConcurrentHashMap<>();

        grouped.values().parallelStream().forEach(blockList -> {
            // 按 AMO 降序排序，取第一个
            blockList.sort((a, b) -> Double.compare(b.amo, a.amo));
            BlockAmoRecord top1 = blockList.get(0);

            // 构建或获取实体
            QaBlockNewRelaStockHisDO entity = dateEntityMap.computeIfAbsent(top1.date, k -> {
                QaBlockNewRelaStockHisDO e = new QaBlockNewRelaStockHisDO();
                e.setBlockNewId(blockNewId);
                e.setDate(top1.date);
                e.setStockIdList(null);
                return e;
            });

            // 获取板块信息并设置
            BaseBlockDO top1BlockDO = data.codeBlockMap.get(top1.blockCode);
            if (top1BlockDO != null) {
                // 假设 blockAmoTop 方法用于填充 entity 的其他字段
                blockAmoTop(top1.date + "|" + top1.type + "|" + top1.level, top1BlockDO, blockNewId, entity);
            }
        });

        log.info("calcBlockAmoTop     >>>     并行处理完成，共生成 {} 个日期实体", dateEntityMap.size());


        // -------------------------------------------------------------------------------------------------------------


        // 4. 按日期排序并批量保存
        List<QaBlockNewRelaStockHisDO> sortedEntities = new TreeMap<>(dateEntityMap).values().stream()
                                                                                    .sorted(Comparator.comparing(QaBlockNewRelaStockHisDO::getDate))
                                                                                    .collect(Collectors.toList());


        qaBlockNewRelaStockHisService.saveBatch(sortedEntities);


        log.info("calcBlockAmoTop     >>>     批量保存完成，共保存 {} 条", sortedEntities.size());
    }


    /**
     * 板块AMO  -  TOP1
     *
     * @param date__blockType_lv
     * @param top1_blockDO
     * @param blockNewId
     * @param entity
     */
    private void blockAmoTop(String date__blockType_lv, BaseBlockDO top1_blockDO, Integer blockNewId,
                             QaBlockNewRelaStockHisDO entity) {


        // key   ->   date|blockType|blockLevel
        String[] keyArr = date__blockType_lv.split("\\|");

        LocalDate date = DateTimeUtil.parseDate_yyyy_MM_dd(keyArr[0]);
        // Integer blockType = Integer.valueOf(keyArr[1]);
        // Integer blockLevel = Integer.valueOf(keyArr[2]);


        // -------------------------------------------


        Integer type = top1_blockDO.getType();
        Integer level = top1_blockDO.getLevel();


        // -------------------------------------------


        if (type == 2) {
            if (level == 1) {
                entity.setPthyLv1Result(JSON.toJSONString(Lists.newArrayList(convert2DTO(top1_blockDO))));
            } else if (level == 2) {
                entity.setPthyLv2Result(JSON.toJSONString(Lists.newArrayList(convert2DTO(top1_blockDO))));
            } else if (level == 3) {
                entity.setPthyLv3Result(JSON.toJSONString(Lists.newArrayList(convert2DTO(top1_blockDO))));
            }
        }

        //
        else if (type == 4) {
            entity.setGnResult(JSON.toJSONString(Lists.newArrayList(convert2DTO(top1_blockDO))));
        }

        //
        else if (type == 12) {
            if (level == 1) {
                entity.setYjhyLv1Result(JSON.toJSONString(Lists.newArrayList(convert2DTO(top1_blockDO))));
            } else if (level == 2) {
                entity.setYjhyLv2Result(JSON.toJSONString(Lists.newArrayList(convert2DTO(top1_blockDO))));
            } else if (level == 3) {
                entity.setYjhyLv3Result(JSON.toJSONString(Lists.newArrayList(convert2DTO(top1_blockDO))));
            }
        }


        log.debug("blockAmoTop     >>>     date : {} , blockCode : {}", date, top1_blockDO.getCode());
    }

    private BlockTopInfoDTO convert2DTO(BaseBlockDO top1_blockDO) {


        String blockCode = top1_blockDO.getCode();


        // ----------------- dto


        BlockTopInfoDTO dto = new BlockTopInfoDTO();
        dto.setBlockId(top1_blockDO.getId());
        dto.setBlockCode(blockCode);
        dto.setBlockName(top1_blockDO.getName());

        dto.setStockCodeSet(null);


        return dto;
    }


// -----------------------------------------------------------------------------------------------------------------


    /**
     * 按 板块 分类统计
     *
     * @param date
     * @param filter_stockCodeSet
     * @param blockNewId          1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；
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

            BaseBlockDO block_lv2 = data.getPBlock(blockCode, 2);
            pthy_2_map.computeIfAbsent(block_lv2.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);


            BaseBlockDO block_lv1 = data.getPBlock(block_lv2.getCode(), 1);
            pthy_1_map.computeIfAbsent(block_lv1.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);
        });


        yjhy_3_map.forEach((blockCode, stockCodeSet) -> {

            BaseBlockDO block_lv2 = data.getPBlock(blockCode, 2);
            yjhy_2_map.computeIfAbsent(block_lv2.getCode(), k -> Sets.newHashSet()).addAll(stockCodeSet);


            BaseBlockDO block_lv1 = data.getPBlock(block_lv2.getCode(), 1);
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

        // 1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；
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


//    @Data
//    public static class ResultInfo {
//
//        // 1-概念；2-普通行业；3-研究行业；
//        private int type;
//
//        private int level = 3;
//
//        private List<BlockTopInfoDTO> infoList;
//    }


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


// ------------------------------------------


//    /**
//     * 板块AMO  -  TOP1
//     */
//    @Data
//    public static class TopBlockAmo {
//
//        private Long id;
//
//        private String code;
//        private String name;
//
//        private String codePath;
//        private String namePath;
//
//        private Long parentId;
//
//        private Integer type;
//        private Integer level;
//        private Integer endLevel;
//    }


    /**
     * 用于临时存储 板块AMO 数据的记录类
     */
    @Data
    @AllArgsConstructor
    public static class BlockAmoRecord {
        LocalDate date;
        Integer type;
        Integer level;
        double amo;
        String blockCode;
    }


}