package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.cache.TopBlockCache;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.BlockNewIdEnum;
import com.bebopze.tdx.quant.common.constant.BlockTypeEnum;
import com.bebopze.tdx.quant.common.constant.ThreadPoolType;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopBlock2DTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopChangePctDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopStockDTO;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.ParallelCalcUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaBlockNewRelaStockHisDO;
import com.bebopze.tdx.quant.dal.entity.QaTopBlockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.dal.service.IQaBlockNewRelaStockHisService;
import com.bebopze.tdx.quant.dal.service.IQaTopBlockService;
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
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
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

    @Autowired
    private IBaseBlockService baseBlockService;


    @Lazy
    @Autowired
    private BacktestStrategy backtestStrategy;

    @Autowired
    private IQaBlockNewRelaStockHisService qaBlockNewRelaStockHisService;


    @Autowired
    private IQaTopBlockService qaTopBlockService;

    @Autowired
    private TopBlockCache topBlockCache;


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public void refreshAll() {
        log.info("-------------------------------- TopBlock - refreshAll     >>>     start");


        bkyd2Task();


        // ---------------------------------------


        // 1- N日新高
        nDayHighTask(100);

        // 2- N日涨幅 - TOP榜
        changePctTopTask(10);

        // 3-均线极多头
        extremeBullMAStackTask();

        // 4-均线大多头
        bullMAStackTask();


        // 11-RPS红
        rpsRedTask(85);

        // 12-大均线多头
        longTermMABullStackTask();

        // TODO   13-二阶段
        // stage2Task();


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
        calcLongTermMABullStack();


        log.info("-------------------------------- longTermMABullStackTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void bullMAStackTask() {
        log.info("-------------------------------- bullMAStackTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache();


        // 均线大多头
        calcBullMAStack();


        log.info("-------------------------------- bullMAStackTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void extremeBullMAStackTask() {
        log.info("-------------------------------- extremeBullMAStackTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache();


        // 均线极多头
        calcExtremeBullMAStackTask();


        log.info("-------------------------------- extremeBullMAStackTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
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


    @TotalTime
    @Override
    public void bkyd2Task_v1() {
        log.info("-------------------------------- bkyd2Task_v1     >>>     start");
        long start = System.currentTimeMillis();


        initCache();


        calcBkyd2_v1();


        log.info("-------------------------------- bkyd2Task_v1     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public void bkyd2Task() {

        initCache();

        calc_bkyd2();

        calcTopInfoTask();
    }


    @Override
    public List<TopBlockDTO> topBlockList(LocalDate date) {


        List<QaTopBlockDO> lastEntityList = qaTopBlockService.lastN(date, 10);


        Map<String, Integer> topBlock__codeCountMap = Maps.newHashMap();
        Map<String, Integer> topStock__codeCountMap = Maps.newHashMap();


        lastEntityList.forEach(e -> {

            // 主线板块
            Set<String> topBlockCodeSet = e.getTopBlockCodeJsonSet();
            // 主线个股
            Set<String> topStockCodeSet = e.getTopStockCodeJsonSet();


            topBlockCodeSet.forEach(topBlockCode -> {
                topBlock__codeCountMap.merge(topBlockCode, 1, Integer::sum);
            });

            topStockCodeSet.forEach(topStockCode -> {
                topStock__codeCountMap.merge(topStockCode, 1, Integer::sum);
            });
        });


        // -------------------------------------------------------------------------------------------------------------


        QaTopBlockDO entity = lastEntityList.get(0); // 倒序
        if (entity == null) {
            return Lists.newArrayList();
        }


        // -------------------------------------------------------------------------------------------------------------


        // 上榜日期
        // Map<String, TopChangePctDTO> block_topDateInfo_map = topBlockCache.stock_topDateInfo_map(date, StockTypeEnum.TDX_BLOCK);


        Map<String, TopChangePctDTO> block_topDateInfo_map = entity.getTopBlockList().stream()
                                                                   .collect(Collectors.toMap(TopChangePctDTO::getCode, Function.identity()));


        // -------------------------------------------------------------------------------------------------------------


        // 主线板块
        Set<String> topBlockCodeSet = entity.getTopBlockCodeJsonSet();
        // 主线个股
        Set<String> allTopStockCodeSet = entity.getTopStockCodeJsonSet();


        return topBlockCodeSet.parallelStream()
                              .map(blockCode -> {


                                  TopBlockDTO topBlock = new TopBlockDTO();
                                  topBlock.setDate(entity.getDate());


                                  // 主线板块
                                  topBlock.setBlockCode(blockCode);
                                  topBlock.setBlockName(topBlockCache.getBlockName(blockCode));
                                  topBlock.setTopDays(topBlock__codeCountMap.get(blockCode));


                                  // 当前 主线板块  ->  主线个股 列表
                                  topBlock.setTopStockList(topBlockCache.getTopStockList(blockCode, allTopStockCodeSet, topStock__codeCountMap));


                                  // 上榜涨幅
                                  // topBlock.setChangePctDTO(topBlockCache.changePctInfo(blockCode, topBlock.getDate(), block_topDateInfo_map));
                                  topBlock.setChangePctDTO(block_topDateInfo_map.get(blockCode));


                                  return topBlock;
                              })
                              .sorted(Comparator.comparing(TopBlockDTO::getTopDays).reversed())
                              .sorted(Comparator.comparing(TopBlockDTO::getTopStockSize).reversed())
                              .collect(Collectors.toList());
    }


    @Override
    public List<TopStockDTO> topStockList(LocalDate date) {


        List<QaTopBlockDO> lastEntityList = qaTopBlockService.lastN(date, 10);


        Map<String, Integer> topBlock__codeCountMap = Maps.newHashMap();
        Map<String, Integer> topStock__codeCountMap = Maps.newHashMap();


        lastEntityList.forEach(e -> {

            // 主线板块
            Set<String> topBlockCodeSet = e.getTopBlockCodeJsonSet();
            // 主线个股
            Set<String> topStockCodeSet = e.getTopStockCodeJsonSet();


            topBlockCodeSet.forEach(topBlockCode -> {
                topBlock__codeCountMap.merge(topBlockCode, 1, Integer::sum);
            });

            topStockCodeSet.forEach(topStockCode -> {
                topStock__codeCountMap.merge(topStockCode, 1, Integer::sum);
            });
        });


        // -------------------------------------------------------------------------------------------------------------


        QaTopBlockDO entity = lastEntityList.get(0); // 倒序
        if (entity == null) {
            return Lists.newArrayList();
        }


        // -------------------------------------------------------------------------------------------------------------


        // 上榜日期
        // Map<String, TopChangePctDTO> stock_topDateInfo_map = topBlockCache.stock_topDateInfo_map(date, StockTypeEnum.A_STOCK);


        Map<String, TopChangePctDTO> sotck_topDateInfo_map = entity.getTopStockList().stream()
                                                                   .collect(Collectors.toMap(TopChangePctDTO::getCode, Function.identity()));


        // -------------------------------------------------------------------------------------------------------------


        // 主线板块
        Set<String> allTopBlockCodeSet = entity.getTopBlockCodeJsonSet();
        // 主线个股
        Set<String> topStockCodeSet = entity.getTopStockCodeJsonSet();


        return topStockCodeSet.parallelStream()
                              .map(stockCode -> {


                                  TopStockDTO topStock = new TopStockDTO();
                                  topStock.setDate(entity.getDate());


                                  // 主线个股
                                  topStock.setStockCode(stockCode);
                                  topStock.setStockName(topBlockCache.getStockName(stockCode));
                                  topStock.setTopDays(topStock__codeCountMap.get(stockCode));


                                  // 当前 主线个股  ->  主线板块 列表
                                  topStock.setTopBlockList(topBlockCache.getTopBlockList(stockCode, allTopBlockCodeSet, topBlock__codeCountMap));


                                  // 上榜涨幅
                                  // topStock.setChangePctDTO(topBlockCache.changePctInfo(stockCode, topStock.getDate(), stock_topDateInfo_map));
                                  topStock.setChangePctDTO(sotck_topDateInfo_map.get(stockCode));


                                  return topStock;
                              })
                              .sorted(Comparator.comparing(TopStockDTO::getTopDays).reversed())
                              .sorted(Comparator.comparing(TopStockDTO::getTopBlockSize).reversed())
                              .collect(Collectors.toList());
    }


    private void calc_bkyd2() {

        // 主线板块
        Map<LocalDate, Set<String>> date_topBlockCodeSet__map = calcTopBlock();

        // 主线个股
        Map<LocalDate, Set<String>> date_topStockCodeSet__map = calcTopStock(date_topBlockCodeSet__map);


        saveOrUpdate(date_topBlockCodeSet__map, date_topStockCodeSet__map);
    }


    private Map<LocalDate, Set<String>> calcTopBlock() {


        // 日期 - 板块_月多2（板块code 列表）
        Map<LocalDate, Set<String>> date_bkyd2__map = Maps.newConcurrentMap();


        // -------------------------------------------------------------------------------------------------------------


        // 遍历计算   =>   每日 - 板块-月多2（板块code 列表）
        ParallelCalcUtil.chunkForEachWithProgress(data.blockDOList, 200, chunk -> {


            chunk.forEach(blockDO -> {

                try {
                    String blockCode = blockDO.getCode();
                    BlockFun fun = data.getOrCreateBlockFun(blockDO);


                    ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // ----------------------------------------------------------------------


                    // 过滤 非RPS板块
                    if (ArrayUtils.isEmpty(extDataArrDTO.rps10)) {
                        return;
                    }


                    // ----------------------------------------------------------------------


                    // 日期 - 板块_月多2（code列表）
                    dateIndexMap.forEach((date, idx) -> {


                        // LV3（板块-月多2 -> 月多 + RPS红 + SSF多）
                        boolean 月多 = extDataArrDTO.月多[idx];
                        boolean RPS红 = extDataArrDTO.RPS红[idx];
                        boolean SSF多 = extDataArrDTO.SSF多[idx];


                        // LV3（板块-月多2 -> 月多 + RPS红 + SSF多）
                        if (月多 && RPS红 && SSF多) {
                            date_bkyd2__map.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(blockCode);
                        }
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        });


        return date_bkyd2__map;
    }

    private Map<LocalDate, Set<String>> calcTopStock(Map<LocalDate, Set<String>> date_topBlockCodeSet__map) {


        // 日期 - 主线个股（个股code 列表）
        Map<LocalDate, Set<String>> date_topStockCodeSet__map = Maps.newConcurrentMap();


        // -------------------------------------------------------------------------------------------------------------


        // 遍历计算   =>   每日 - 均线极多头（个股code 列表）
        ParallelCalcUtil.chunkForEachWithProgress(data.stockDOList, 200, chunk -> {


            chunk.forEach(stockDO -> {

                try {
                    String stockCode = stockDO.getCode();
                    StockFun fun = data.getOrCreateStockFun(stockDO);


                    ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // 日期 - 板块_月多2（code列表）
                    dateIndexMap.forEach((date, idx) -> {


                        // 主线个股（N100日新高 + 月多 + IN主线）
                        // boolean N100日新高 = extDataArrDTO.N100日新高[idx];
                        boolean 百日新高 = extDataArrDTO.百日新高[idx];
                        boolean 月多 = extDataArrDTO.月多[idx];


                        // ---------------------------------------------------------------------------------------------


                        // 当日  ->  主线板块 列表
                        Set<String> topBlockCodeSet = date_topBlockCodeSet__map.getOrDefault(date, Sets.newHashSet());

                        // 当前个股  ->  板块列表
                        Set<String> blockCodeSet = data.stockCode_blockCodeSet_Map.getOrDefault(stockCode, Sets.newHashSet());


                        // IN主线（个股板块 与 主线板块   ->   存在交集）
                        boolean IN主线 = !CollectionUtils.intersection(topBlockCodeSet, blockCodeSet).isEmpty();


                        // ---------------------------------------------------------------------------------------------


                        if (百日新高 && 月多 && IN主线) {
                            date_topStockCodeSet__map.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(stockCode);
                        }
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        });


        return date_topStockCodeSet__map;
    }

    private void saveOrUpdate(Map<LocalDate, Set<String>> date_topBlockCodeSet__map,
                              Map<LocalDate, Set<String>> date_topStockCodeSet__map) {


        Map<LocalDate, Long> dateIdMap = qaTopBlockService.dateIdMap();


        // 日期 -> 正序
        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_topBlockCodeSet__map);


        // -------------------------------------------------------------------------------------------------------------


//        long maxId_val = dateIdMap.values().stream().max(Long::compareTo).orElse(0L);
//        AtomicLong maxId = new AtomicLong(maxId_val);
//
//        sortMap.keySet().forEach(date -> dateIdMap.computeIfAbsent(date, k -> maxId.incrementAndGet()));
//
//
//        // -----------------
//
//
//        // 并行
//        sortMap.entrySet().parallelStream().forEach(entry -> {
//            LocalDate date = entry.getKey();
//            Set<String> topBlockCodeSet = entry.getValue();
//
//
//            QaTopBlockDO entity = new QaTopBlockDO();
//            // 交易日
//            entity.setDate(date);
//
//            // 当日 主线板块
//            entity.setTopBlockCodeSet(JSON.toJSONString(convert2TopDTOList(topBlockCodeSet)));
//
//            // 当日 主线个股
//            Set<String> topStockCodeSet = date_topStockCodeSet__map.getOrDefault(date, Sets.newHashSet());
//            entity.setTopStockCodeSet(JSON.toJSONString(convert2TopDTOList(topStockCodeSet)));
//
//
//            entity.setId(dateIdMap.get(date));
//            qaTopBlockService.saveOrUpdate(entity);
//        });


        // -------------------------------------------------------------------------------------------------------------


        // save DB
        sortMap.forEach((date, topBlockCodeSet) -> {

            QaTopBlockDO entity = new QaTopBlockDO();
            // 交易日
            entity.setDate(date);

            // 当日 主线板块
            entity.setTopBlockCodeSet(JSON.toJSONString(convert2TopDTOList(topBlockCodeSet)));

            // 当日 主线个股
            Set<String> topStockCodeSet = date_topStockCodeSet__map.getOrDefault(date, Sets.newHashSet());
            entity.setTopStockCodeSet(JSON.toJSONString(convert2TopDTOList(topStockCodeSet)));


            entity.setId(dateIdMap.get(date));
            qaTopBlockService.saveOrUpdate(entity);
        });
    }


    private List<TopChangePctDTO> convert2TopDTOList(Set<String> topCodeSet) {
        return topCodeSet.stream().map(TopChangePctDTO::new).collect(Collectors.toList());
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 计算 上榜日期、涨幅       ->       首次上榜日期（往前倒推）、跌出榜单日期  、 上榜涨幅
     */
    private void calcTopInfoTask() {


        // -------------------------------------------------------------------------------------------------------------


        List<QaTopBlockDO> topSortListAll = qaTopBlockService.list()
                                                             .stream()
                                                             .sorted(Comparator.comparing(QaTopBlockDO::getDate))
                                                             .collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        Map<LocalDate, Map<String, TopChangePctDTO>> date__blockCode_topDate__Map = calcTopInfoTask(data.blockDOList, topSortListAll);
        Map<LocalDate, Map<String, TopChangePctDTO>> date__stockCode_topDate__Map = calcTopInfoTask(data.stockDOList, topSortListAll);


        // -------------------------------------------------------------------------------------------------------------


        topSortListAll.parallelStream().forEach(e -> {


            // 日期
            LocalDate date = e.getDate();


            // ---------------------------------------------------------------------------------------------------------


            // ---------------------------------------------------------------


            // code - 主线 板块/个股
            Map<String, TopChangePctDTO> blockCode_topDate__Map = date__blockCode_topDate__Map.getOrDefault(date, Maps.newHashMap());
            Map<String, TopChangePctDTO> stockCode_topDate__Map = date__stockCode_topDate__Map.getOrDefault(date, Maps.newHashMap());


            // ---------------------------------------------------------------


            // 主线板块、主线个股   列表
            e.setTopBlockCodeSet(JSON.toJSONString(blockCode_topDate__Map.values()));
            e.setTopStockCodeSet(JSON.toJSONString(stockCode_topDate__Map.values()));


            // ---------------------------------------------------------------


            qaTopBlockService.updateById(e);
        });
    }


    /**
     * 计算 首次上榜日期（往前倒推）、跌出榜单日期
     */
    public Map<LocalDate, Map<String, TopChangePctDTO>> calcTopInfoTask(List dataList,
                                                                        List<QaTopBlockDO> topSortListAll) {


        // -------------------------------------------------------------------------------------------------------------


        Map<LocalDate, Integer> top__dateIdxMap = Maps.newHashMap();
        for (int i = 0; i < topSortListAll.size(); i++) {
            top__dateIdxMap.put(topSortListAll.get(i).getDate(), i);
        }


        // -------------------------------------------------------------------------------------------------------------


        Map<LocalDate, Map<String, TopChangePctDTO>> date__code_topDate__Map = Maps.newConcurrentMap();


        AtomicInteger COUNT = new AtomicInteger();


        // -------------------------------------------------------------------------------------------------------------


        // 遍历计算   =>   每日  -  code_topDate
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(/*data.stockDOList*/ dataList, 200, chunk -> {


            chunk.forEach(stockDO -> {


                try {
                    String code = stockDO instanceof BaseStockDO ? ((BaseStockDO) stockDO).getCode() : ((BaseBlockDO) stockDO).getCode();
                    StockFun fun = stockDO instanceof BaseStockDO ? data.getOrCreateStockFun((BaseStockDO) stockDO) : data.getOrCreateBlockFun((BaseBlockDO) stockDO);


                    KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
                    ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // -------------------------------------------------------------------------------------------------


                    log.info("calcTopInfoTask     >>>     count : {} , code : {}", COUNT.incrementAndGet(), code);


                    // -------------------------------------------------------------------------------------------------


                    // 非RPS 板块
                    if (ArrayUtils.isEmpty(extDataArrDTO.rps10)) {
                        return;
                    }


                    // -------------------------------------------------------------------------------------------------


                    boolean[] SSF空 = extDataArrDTO.SSF空;
                    boolean[] MA20空 = extDataArrDTO.MA20空;

                    boolean[] SSF_MA20空 = TdxExtFun.con_or(SSF空, MA20空);


                    int[] BARSLAST__SSF_MA20空 = TdxFun.BARSLAST(SSF_MA20空);


                    // -------------------------------------------------------------------------------------------------


                    boolean[] 下SSF = extDataArrDTO.下SSF;
                    boolean[] 下MA20 = extDataArrDTO.下MA20;

                    boolean[] 下SSF_MA20 = TdxExtFun.con_or(下SSF, 下MA20);


                    int[] BARSNEXT__下SSF_MA20 = TdxFun.BARSNEXT(下SSF_MA20);


                    // -------------------------------------------------------------------------------------------------


                    // date  -  code_topDate
                    dateIndexMap.forEach((date, kline_idx) -> {


                        // ---------------------------------------------------------------------------------------------


                        Integer topBaseIdx = top__dateIdxMap.get(date);

                        // 当日（基准日期）  ->   无主线
                        if (topBaseIdx == null) {
                            log.debug("topBaseIdx == null   -   当日->无主线     >>>     {}", date);
                            return;
                        }


                        // 今日date（基准日期）  ->   前后 各50条
                        List<QaTopBlockDO> topEntity__before50_after50 = topSortListAll;


                        // ---------------------------------------------------------------------------------------------


                        // 基准日期   ->   主线 entity
                        QaTopBlockDO base__topBlockDO = topEntity__before50_after50.get(topBaseIdx);


                        // 当日   ->   主线板块/个股   列表
                        Set<String> base__topCodeSet = TopBlockCache.getTopCodeSet(base__topBlockDO, code);
                        // 当前板块/个股       当日 -> IN主线
                        if (!base__topCodeSet.contains(code)) {

//                            Set<String> topBlockCodeSet = base__topBlockDO.getTopBlockCodeJsonSet();
//                            Set<String> topStockCodeSet = base__topBlockDO.getTopStockCodeJsonSet();
//
//                            log.warn("当前板块/个股 当日->非主线     >>>     {} , {} , topBlockCodeSet : {} , topStockCodeSet : {}",
//                                     date, code, topBlockCodeSet, topStockCodeSet);
                            return;
                        }


                        // ---------------------------------------------------------------------------------------------


                        // ---------------------------- topStartDate


                        // top区间 - 起始日期   ->   距今天数
                        int last__SSF_MA20空__days = BARSLAST__SSF_MA20空[kline_idx];
                        // top区间 - 起始日期   ->   idx
                        int topStartIdx = Math.max(topBaseIdx - last__SSF_MA20空__days, 0);


                        // 逐日 遍历top区间
                        for (int i = topStartIdx; i <= topBaseIdx; i++) {
                            QaTopBlockDO start__topBlockDO = topEntity__before50_after50.get(i);

                            LocalDate topStartDate = start__topBlockDO.getDate();
                            Set<String> start__topCodeSet = TopBlockCache.getTopCodeSet(start__topBlockDO, code);


                            // 当前个股     ==>     当日 IN主线     =>     当日 -> topStartDate
                            if (start__topCodeSet.contains(code)) {

                                // date  -  code_topDate
                                date__code_topDate__Map.computeIfAbsent(date, k -> Maps.newConcurrentMap())
                                                       .computeIfAbsent(code, k -> new TopChangePctDTO(code))
                                                       .setTopStartDate(topStartDate);

                                break;
                            }
                        }


                        // ---------------------------------------------------------------------------------------------


                        // date  -  code_topDate
                        TopChangePctDTO topChangePctDTO = date__code_topDate__Map.computeIfAbsent(date, k -> Maps.newConcurrentMap())
                                                                                 .get(code);


                        if (null == topChangePctDTO || null == topChangePctDTO.getTopStartDate()) {

                            // QaTopBlockDO base__topBlockDO = topEntity__before50_after50.get(topBaseIdx);
                            LocalDate base_date = base__topBlockDO.getDate();
                            Set<String> topBlockCodeSet = base__topBlockDO.getTopBlockCodeJsonSet();
                            Set<String> topStockCodeSet = base__topBlockDO.getTopStockCodeJsonSet();

                            log.error("topChangePctDTO = null   -   当前个股 当日   非主线     >>>     {} , {} , topBlockCodeSet : {} , topStockCodeSet : {}",
                                      date, code, topBlockCodeSet, topStockCodeSet);
                            return;
                        }


                        // ---------------------------------------------------------------------------------------------


                        // ---------------------------- topEndDate


                        // 今日   ->   top区间 - end日期
                        int next__下SSF_MA20__days = BARSNEXT__下SSF_MA20[kline_idx];


                        // top区间 - end日期   ->   idx
                        int topEndIdx;
                        if (next__下SSF_MA20__days == -1) {
                            topEndIdx = topEntity__before50_after50.size() - 1;
                        } else {
                            topEndIdx = Math.min(topBaseIdx + next__下SSF_MA20__days, topEntity__before50_after50.size() - 1);
                        }


                        LocalDate topEndDate = topEntity__before50_after50.get(topEndIdx).getDate();
                        topChangePctDTO.setTopEndDate(topEndDate);


                        // ---------------------------------------------------------------------------------------------


                        // ---------------------------- 上榜涨幅


                        Integer date_idx = kline_idx;
                        Integer nextDate_idx = Math.min(date_idx + 1, dateIndexMap.size() - 1);

                        Integer startTopDate_idx = dateIndexMap.get(topChangePctDTO.topStartDate);
                        Integer endTopDate_idx = dateIndexMap.get(topChangePctDTO.topEndDate);


//                        if (null == date_idx || null == startTopDate_idx || null == endTopDate_idx) {
//                            log.error("idx = null     >>>     code : {} , date : {} , startTopDate : {} , endTopDate : {}",
//                                      code, date, topChangePctDTO.topStartDate, topChangePctDTO.topEndDate);
//                        }


                        // 停牌
                        endTopDate_idx = endTopDate_idx == null ? date_idx : endTopDate_idx;


                        double date_close = klineArrDTO.close[date_idx];
                        double nextDate_idx_close = klineArrDTO.close[nextDate_idx];
                        double startTopDate_idx_close = klineArrDTO.close[startTopDate_idx];
                        double endTopDate_idx_close = klineArrDTO.close[endTopDate_idx];


                        double start2Today_changePct = date_close / startTopDate_idx_close * 100 - 100;
                        double start2End_changePct = endTopDate_idx_close / startTopDate_idx_close * 100 - 100;
                        double today2Next_changePct = nextDate_idx_close / date_close * 100 - 100;
                        double today2End_changePct = endTopDate_idx_close / date_close * 100 - 100;


                        topChangePctDTO.setStart2Today_changePct(NumUtil.of(start2Today_changePct));
                        topChangePctDTO.setStart2End_changePct(NumUtil.of(start2End_changePct));
                        topChangePctDTO.setToday2Next_changePct(NumUtil.of(today2Next_changePct));
                        topChangePctDTO.setToday2End_changePct(NumUtil.of(today2End_changePct));
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });


        }, ThreadPoolType.IO_INTENSIVE_2);


        return date__code_topDate__Map;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Cacheable(value = "topBlockRate", key = "#blockNewId + '_' + #date + '_' + #resultType+ '_' + #N", sync = true)
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


                // 过滤当前 已走弱 板块   ->   !SSF多
                if (block_SSF多(blockCode, date)) {
                    rateMap.merge(blockCode + "-" + blockName, 1, Integer::sum);
                }
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


            // -------------------------------------------–


            // 过滤当前 已走弱 板块   ->   !SSF多
            if (!block_SSF多(blockCode, date)) {
                return;
            }


            // -------------------------------------------–


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
    public List<TopBlock2DTO> topBlockRateInfo(int blockNewId, LocalDate date, int resultType, int N) {


        initCache();


        // -----------------------------------------


        List<QaBlockNewRelaStockHisDO> entityList = qaBlockNewRelaStockHisService.listByBlockNewIdDateAndLimit(blockNewId, date, N);


        Map<String, List<TopBlock2DTO.TopStockDTO>> blockCode_topStockList_map = Maps.newHashMap();


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
            List<TopBlock2DTO.TopStockDTO> topStockDTOList = stockCode_rps强度_map.entrySet().stream().map(entry -> {

                                                                                      String stockCode = entry.getKey();
                                                                                      double rps三线和 = entry.getValue();
                                                                                      String stockName = data.stock__codeNameMap.get(stockCode);

                                                                                      return new TopBlock2DTO.TopStockDTO(stockCode, stockName, rps三线和);
                                                                                  })
                                                                                  .sorted(Comparator.comparing(TopBlock2DTO.TopStockDTO::getRps三线和).reversed())
                                                                                  .limit(10)
                                                                                  .collect(Collectors.toList());


            blockCode_topStockList_map.put(blockCode, topStockDTOList);
        }


        // block - TOP （主线板块 TOP1 - 天数）
        List<TopBlock2DTO> topBlockList = rateMap.entrySet().stream().map(entry -> {
                                                     String block = entry.getKey();
                                                     int topDay = entry.getValue();

                                                     String[] blockArr = block.split("-");


                                                     String blockCode = blockArr[0];
                                                     String blockName = blockArr[1];
                                                     List<TopBlock2DTO.TopStockDTO> topStockList = blockCode_topStockList_map.get(blockCode);

                                                     TopBlock2DTO topBlockDTO = new TopBlock2DTO(blockCode, blockName, topDay, topStockList);
                                                     return topBlockDTO;
                                                 })
                                                 .sorted(Comparator.comparing(TopBlock2DTO::getTopDay).reversed())
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


//    @Data
//    @AllArgsConstructor
//    public static class TopBlockDTO {
//        private String blockCode;
//        private String blockName;
//        private int topDay;
//
//        // 当日最强
//        private List<TopStockDTO> topStockList;
//    }
//
//    @Data
//    @AllArgsConstructor
//    public static class TopStockDTO {
//        private String stockCode;
//        private String stockName;
//
//        private double rps三线和;
//    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 通用计算函数：根据给定的条件筛选每日股票，并按板块分类存储。
     *
     * @param blockNewId   主线板块ID，用于删除旧数据 和 调用blockSum
     * @param calcFunction 接收 StockFun，返回 boolean[] 数组的函数。（该数组的每个元素对应一个日期，true 表示该日期该股票符合条件。）
     */
    private void calcGenericBlockNew(Integer blockNewId,
                                     // 传入 StockFun，返回 boolean[]
                                     Function<StockFun, boolean[]> calcFunction) {


        // --------------------------------------------------- 日期 - 结果（个股code 列表）
        Map<LocalDate, Set<String>> date_stockCodeSet_Map = Maps.newConcurrentMap();


        // 遍历计算   =>   每日 - 结果（个股code 列表）
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(data.stockDOList, 200, chunk -> {


            chunk.forEach(stockDO -> {
                String stockCode = stockDO.getCode();


                try {
                    StockFun fun = data.getOrCreateStockFun(stockDO);
                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // 计算布尔数组
                    boolean[] resultArr = calcFunction.apply(fun);


                    // 安全检查：确保数组长度与日期索引映射大小匹配（或至少不小于最大索引）
                    // int arrLength = resultArr.length;


                    // 日期 - 结果（code列表）
                    dateIndexMap.forEach((date, idx) -> {
                        // 检查索引是否在数组范围内
                        if (/*idx != null && idx >= 0 && idx < arrLength && */resultArr[idx]) {
                            date_stockCodeSet_Map.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(stockCode);
                        }
                    });


                } catch (Exception e) {
                    log.error("主线板块Task [{}] - err     >>>     stockCode : {} , errMsg : {}",
                              BlockNewIdEnum.getDescByBlockNewId(blockNewId), stockCode, e.getMessage(), e);
                }
            });
        });


        // --------------------------------------------------- 按 板块 分类
        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet_Map);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcNDayHigh_2(int N) {
        calcGenericBlockNew(
                BlockNewIdEnum.百日新高.getBlockNewId(), // 传入板块ID
                fun -> fun.百日新高(N)                   // 传入计算逻辑 (fun) -> boolean[]
        );
    }

    private void calcChangePctTop_2(int N, double limitChangePct) {


        calcGenericBlockNew(

                BlockNewIdEnum.涨幅榜.getBlockNewId(), // 传入板块ID


                // 将 double[] 转换为 boolean[] 的逻辑封装在 lambda 内
                fun -> {
                    double[] changePctArr = TdxExtFun.changePct(fun.getClose(), N);
                    boolean[] result = new boolean[changePctArr.length];
                    for (int i = 0; i < changePctArr.length; i++) {
                        result[i] = changePctArr[i] > limitChangePct; // 使用外部变量 limitChangePct
                    }
                    return result;
                }                                    // 传入计算逻辑 (fun) -> boolean[]
        );
    }

    private void calcRpsRed_2(double RPS) {
        calcGenericBlockNew(
                BlockNewIdEnum.RPS红.getBlockNewId(), // 传入板块ID
                fun -> fun.RPS红(RPS)                 // 传入计算逻辑 (fun) -> boolean[]
        );
    }

    private void calcStage2_2() {
        calcGenericBlockNew(
                BlockNewIdEnum.二阶段.getBlockNewId(), // 传入板块ID
                fun -> fun.二阶段()                    // 传入计算逻辑 (fun) -> boolean[]
        );
    }

    private void calcLongTermMABullStack_2() {
        calcGenericBlockNew(
                BlockNewIdEnum.大均线多头.getBlockNewId(), // 传入板块ID
                fun -> fun.大均线多头()                    // 传入计算逻辑 (fun) -> boolean[]
        );
    }


    // ...


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


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


    private void calcExtremeBullMAStackTask() {


        // --------------------------------------------------- 日期 - 均线极多头（个股code 列表）


        // 日期 - 均线大多头（个股code 列表）
        Map<LocalDate, Set<String>> date_stockCodeSet__topMap = Maps.newConcurrentMap();


        // 遍历计算   =>   每日 - 均线极多头（个股code 列表）
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(data.stockDOList, 200, chunk -> {


            chunk.forEach(stockDO -> {

                try {
                    String stockCode = stockDO.getCode();
                    StockFun fun = data.getOrCreateStockFun(stockDO);


                    // 均线极多头
                    boolean[] 均线极多头_arr = fun.均线极多头();

                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // 日期 - 均线大多头（code列表）
                    dateIndexMap.forEach((date, idx) -> {

                        if (均线极多头_arr[idx]) {
                            date_stockCodeSet__topMap.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(stockCode);
                        }
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        });


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.均线极多头.getBlockNewId();


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
                .parallelStream()
                .forEach((date__block_type_lv) -> {

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
        qaBlockNewRelaStockHisService.saveBatch(new TreeMap<>(date_entity_map).values());
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcBkyd2_v1() {


        // 日期 - 板块_月多2（个股code 列表）
        Map<LocalDate, Set<String>> date_bkyd2__map = Maps.newConcurrentMap();


        // --------------------------------------------------- 日期 - 均线极多头（个股code 列表）


        // 遍历计算   =>   每日 - 板块-月多2（个股code 列表）
        ParallelCalcUtil.chunkForEachWithProgress(data.blockDOList, 200, chunk -> {


            chunk.forEach(blockDO -> {

                try {
                    String blockCode = blockDO.getCode();
                    BlockFun fun = data.getOrCreateBlockFun(blockDO);


                    ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    // ----------------------------------------------------------------------


                    // 过滤 非RPS板块
                    if (ArrayUtils.isEmpty(extDataArrDTO.rps10)) {
                        return;
                    }


                    // ----------------------------------------------------------------------


                    // 日期 - 板块_月多2（code列表）
                    dateIndexMap.forEach((date, idx) -> {


                        // LV3（板块-月多2 -> 月多 + RPS红 + SSF多）
                        boolean 月多 = extDataArrDTO.月多[idx];
                        boolean RPS红 = extDataArrDTO.RPS红[idx];
                        boolean SSF多 = extDataArrDTO.SSF多[idx];


                        // LV3（板块-月多2 -> 月多 + RPS红 + SSF多）
                        if (月多 && RPS红 && SSF多) {
                            date_bkyd2__map.computeIfAbsent(date, k -> Sets.newConcurrentHashSet()).add(blockCode);
                        }
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });
        });


        // -------------------------------------------------------------------------------------------------------------


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.板块_月多2.getBlockNewId();


        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_bkyd2__map);
        sortMap.forEach((date, blockCodeSet) -> blockSum(date, blockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


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


    /**
     * 当前板块   ->   当日 SSF多
     *
     * @param blockCode
     * @param date
     * @return
     */
    private boolean block_SSF多(String blockCode, LocalDate date) {

        BlockFun fun = (CollectionUtils.isNotEmpty(data.blockDOList)) ? data.getOrCreateBlockFun(blockCode) : data.getOrCreateBlockFun(baseBlockService.getByCode(blockCode));


        ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
        boolean[] SSF多_arr = extDataArrDTO.SSF多;


        // 非RPS板块（LV1/LV2  ->  无扩展数据）      =>       实时计算
        if (ArrayUtils.isEmpty(SSF多_arr)) {
            SSF多_arr = fun.SSF多();
        }


        Integer idx = BacktestCache.tradeDateIdx(fun.getDate(), fun.getDateIndexMap(), date);
        return idx != null && SSF多_arr[idx];
    }


    // -----------------------------------------------------------------------------------------------------------------


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