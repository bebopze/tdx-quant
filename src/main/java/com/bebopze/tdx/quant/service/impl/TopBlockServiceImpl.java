package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.cache.TopBlockCache;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.*;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.*;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.MapUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.ParallelCalcUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.TradeService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
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

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;


    @Lazy
    @Autowired
    private BacktestStrategy backtestStrategy;

    @Autowired
    private IQaBlockNewRelaStockHisService qaBlockNewRelaStockHisService;


    @Autowired
    private IQaTopBlockService qaTopBlockService;

    @Autowired
    private TopBlockCache topBlockCache;

    @Lazy
    @Autowired
    private TradeService tradeService;


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public void refreshAll(UpdateTypeEnum updateTypeEnum) {
        log.info("-------------------------------- TopBlock - refreshAll     >>>     start");


        bkyd2Task(updateTypeEnum);


        // ---------------------------------------


        // 1- N日新高
        nDayHighTask(updateTypeEnum, 100);

        // 2- N日涨幅 - TOP榜
        changePctTopTask(updateTypeEnum, 10);

        // 3-均线极多头
        extremeBullMAStackTask(updateTypeEnum);

        // 4-均线大多头
        bullMAStackTask(updateTypeEnum);


        // 11-RPS红
        rpsRedTask(updateTypeEnum, 85);

        // 12-大均线多头
        longTermMABullStackTask(updateTypeEnum);

        // TODO   13-二阶段
        // stage2Task();


        // 11- 板块AMO-Top1
        blockAmoTopTask(updateTypeEnum);


        // GC
        data.clear();


        log.info("-------------------------------- TopBlock - refreshAll     >>>     end");
    }


    @TotalTime
    @Override
    public void nDayHighTask(UpdateTypeEnum updateTypeEnum, int N) {
        log.info("-------------------------------- nDayHighTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);

        calcNDayHigh(updateTypeEnum, N);


        log.info("-------------------------------- nDayHighTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void changePctTopTask(UpdateTypeEnum updateTypeEnum, int N) {
        log.info("-------------------------------- changePctTopTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);


        // N日涨幅 > 25%
        calcChangePctTop(updateTypeEnum, N, 25.0);


        log.info("-------------------------------- changePctTopTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void rpsRedTask(UpdateTypeEnum updateTypeEnum, double RPS) {
        log.info("-------------------------------- rpsRedTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);


        // RPS红
        calcRpsRed(updateTypeEnum, RPS);


        log.info("-------------------------------- rpsRedTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void stage2Task(UpdateTypeEnum updateTypeEnum) {
        log.info("-------------------------------- stage2Task     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);


        // TODO   二阶段
        calcStage2();


        log.info("-------------------------------- stage2Task     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void longTermMABullStackTask(UpdateTypeEnum updateTypeEnum) {
        log.info("-------------------------------- longTermMABullStackTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);


        // 大均线多头
        calcLongTermMABullStack(updateTypeEnum);


        log.info("-------------------------------- longTermMABullStackTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void bullMAStackTask(UpdateTypeEnum updateTypeEnum) {
        log.info("-------------------------------- bullMAStackTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);


        // 均线大多头
        calcBullMAStack(updateTypeEnum);


        log.info("-------------------------------- bullMAStackTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void extremeBullMAStackTask(UpdateTypeEnum updateTypeEnum) {
        log.info("-------------------------------- extremeBullMAStackTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);


        // 均线极多头
        calcExtremeBullMAStackTask(updateTypeEnum);


        log.info("-------------------------------- extremeBullMAStackTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }

    @TotalTime
    @Override
    public void blockAmoTopTask(UpdateTypeEnum updateTypeEnum) {
        log.info("-------------------------------- blockAmoTopTask     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);

        // 2min 6s     -     21.4s
        calcBlockAmoTop(updateTypeEnum);

        // totalTime : 21.7s
        // calcBlockAmoTop2();


        log.info("-------------------------------- blockAmoTopTask     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }


    @TotalTime
    @Override
    public void bkyd2Task_v1(UpdateTypeEnum updateTypeEnum) {
        log.info("-------------------------------- bkyd2Task_v1     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);


        calcBkyd2_v1();


        log.info("-------------------------------- bkyd2Task_v1     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @Override
    public void bkyd2Task(UpdateTypeEnum updateTypeEnum) {
        log.info("-------------------------------- bkyd2Task     >>>     start");
        long start = System.currentTimeMillis();


        initCache(updateTypeEnum);


        // dateIndexMap   +   saveOrUpdate     ->     自动跟随 initCache  ->  startDate、endDate （无需 updateTypeEnum）
        calc_bkyd2();


        // 日期-主线列表
        autoType___date_topList_Map(updateTypeEnum);

//        calcTopETFTask(updateTypeEnum);


        log.info("-------------------------------- bkyd2Task     >>>     end , {}", DateTimeUtil.formatNow2Hms(start));
    }


    private void calcTopETFTask(UpdateTypeEnum updateTypeEnum) {


        // -------------------------------------------------------------------------------------------------------------


        List<QaTopBlockDO> topSortListAll = qaTopBlockService.list()
                                                             .stream()
                                                             .sorted(Comparator.comparing(QaTopBlockDO::getDate))
                                                             .collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        topSortListAll.parallelStream()
                      .forEach(e -> {

                          //
                          Set<String> topBlockCodeSet = e.getTopBlockCodeJsonSet(TopTypeEnum.AUTO.type);


                          // TODO   板块 -> 行业ETF
                          List<BaseStockDO> ETF_stockDOList = baseBlockRelaStockService.listETFByBlockCodes(topBlockCodeSet);


                          List<TopChangePctDTO> list = Lists.newArrayList();
                          e.setTopEtfCodeSet(JSON.toJSONString(list));


                          TopPoolAvgPctDTO topPoolAvgPctDTO = new TopPoolAvgPctDTO();
                          e.setEtfAvgPct(JSON.toJSONString(topPoolAvgPctDTO));


                          // -------------------------------------------------------------------------------------------


//                          // old != null     +     new -> null     +     INCR_UPDATE
//                          if (StringUtils.isNotBlank(e.getTopEtfCodeSet()) && MapUtils.isEmpty(blockCode_topDate__Map)
//                                  && Objects.equals(UpdateTypeEnum.INCR, updateTypeEnum)) {
//                              return;
//                          }
//
//                          if (StringUtils.isNotBlank(e.getEtfAvgPct()) && MapUtils.isEmpty(stockCode_topDate__Map)
//                                  && Objects.equals(UpdateTypeEnum.INCR, updateTypeEnum)) {
//                              return;
//                          }


                          // -------------------------------------------------------------------------------------------


//                          if (StringUtils.isEmpty(e.getTopEtfCodeSetMan())) {
//                              e.setTopEtfCodeSetMan(e.getTopEtfCodeSet());
//                          }
//                          if (StringUtils.isEmpty(e.getEtfAvgPctMan())) {
//                              e.setEtfAvgPctMan(e.getEtfAvgPct());
//                          }


                          // -------------------------------------------------------------------------------------------


                          qaTopBlockService.updateById(e);
                      });
    }


    @Override
    public TopBlockPoolDTO topBlockList(LocalDate date, Integer type) {
        TopBlockPoolDTO dto = new TopBlockPoolDTO();


        // -------------------------------------------------------------------------------------------------------------


        List<QaTopBlockDO> lastEntityList = qaTopBlockService.lastN(date, 10);


        Map<String, Integer> topBlock__codeCountMap = Maps.newHashMap();
        Map<String, Integer> topStock__codeCountMap = Maps.newHashMap();


        lastEntityList.forEach(e -> {

            // 主线板块
            Set<String> topBlockCodeSet = e.getTopBlockCodeJsonSet(type);
            // 主线个股
            Set<String> topStockCodeSet = e.getTopStockCodeJsonSet(type);


            topBlockCodeSet.forEach(topBlockCode -> topBlock__codeCountMap.merge(topBlockCode, 1, Integer::sum));

            topStockCodeSet.forEach(topStockCode -> topStock__codeCountMap.merge(topStockCode, 1, Integer::sum));
        });


        // -------------------------------------------------------------------------------------------------------------


        QaTopBlockDO entity = CollectionUtils.isEmpty(lastEntityList) ? null : lastEntityList.get(0); // 倒序
        if (entity == null) {
            return dto;
        }


        // -------------------------------------------------------------------------------------------------------------


        dto.setTopBlockAvgPctDTO(entity.getTopBlockAvgPct(type));


        // -------------------------------------------------------------------------------------------------------------


        // 主线板块
        List<TopChangePctDTO> topBlockList = entity.getTopBlockList(type);
        // 主线个股
        List<TopChangePctDTO> topStockList = entity.getTopStockList(type);


        List<TopBlockDTO> topBlockDTOList = topBlockList.parallelStream()
                                                        .map(topBlockInfo -> {
                                                            String blockCode = topBlockInfo.getCode();


                                                            TopBlockDTO topBlockDTO = new TopBlockDTO();
                                                            topBlockDTO.setDate(entity.getDate());


                                                            // 主线板块
                                                            topBlockDTO.setBlockCode(blockCode);
                                                            topBlockDTO.setBlockName(topBlockInfo.getName());
                                                            topBlockDTO.setTopDays(topBlock__codeCountMap.get(blockCode));


                                                            // 当前 主线板块  ->  主线个股 列表
                                                            topBlockDTO.setTopStockList(topBlockCache.getTopStockList(blockCode, topStockList, topStock__codeCountMap));


                                                            // 上榜涨幅
                                                            topBlockDTO.setChangePctDTO(topBlockInfo);


                                                            return topBlockDTO;
                                                        })
                                                        .sorted(Comparator.comparing(TopBlockDTO::getTopDays).reversed())
                                                        .sorted(Comparator.comparing(TopBlockDTO::getTopStockSize).reversed())
                                                        .collect(Collectors.toList());

        dto.setTopBlockDTOList(topBlockDTOList);


        return dto;
    }


    @Override
    public TopStockPoolDTO topStockList(LocalDate date, Integer type) {
        TopStockPoolDTO dto = new TopStockPoolDTO();


        // -------------------------------------------------------------------------------------------------------------


        List<QaTopBlockDO> lastEntityList = qaTopBlockService.lastN(date, 10);


        // 持仓个股列表
        Set<String> pos__stockCodeSet = tradeService.getPosStockCodeSet(false);


        Map<String, Integer> topBlock__codeCountMap = Maps.newHashMap();
        Map<String, Integer> topStock__codeCountMap = Maps.newHashMap();


        lastEntityList.forEach(e -> {

            // 主线板块
            Set<String> topBlockCodeSet = e.getTopBlockCodeJsonSet(type);
            // 主线个股
            Set<String> topStockCodeSet = e.getTopStockCodeJsonSet(type);


            topBlockCodeSet.forEach(topBlockCode -> topBlock__codeCountMap.merge(topBlockCode, 1, Integer::sum));

            topStockCodeSet.forEach(topStockCode -> topStock__codeCountMap.merge(topStockCode, 1, Integer::sum));
        });


        // -------------------------------------------------------------------------------------------------------------


        QaTopBlockDO entity = CollectionUtils.isEmpty(lastEntityList) ? null : lastEntityList.get(0); // 倒序
        if (entity == null) {
            return dto;
        }


        // -------------------------------------------------------------------------------------------------------------


        // TODO   机选 -> 已计算
        dto.setTopStockAvgPctDTO(entity.getTopStockAvgPct(type));

        // TODO   人选 -> 实时计算


        // -------------------------------------------------------------------------------------------------------------


        // 主线板块
        List<TopChangePctDTO> topBlockList = entity.getTopBlockList(type);
        // 主线个股
        List<TopChangePctDTO> topStockList = entity.getTopStockList(type);


        List<TopStockDTO> topStockDTOList = topStockList.parallelStream()
                                                        .map(topStockInfo -> {
                                                            String stockCode = topStockInfo.getCode();


                                                            TopStockDTO topStock = new TopStockDTO();
                                                            topStock.setDate(entity.getDate());


                                                            // 主线个股
                                                            topStock.setStockCode(stockCode);
                                                            topStock.setStockName(topStockInfo.getName());
                                                            topStock.setTopDays(topStock__codeCountMap.get(stockCode));
                                                            topStock.setPosStockFlag(pos__stockCodeSet.contains(stockCode));


                                                            // 当前 主线个股  ->  主线板块 列表
                                                            topStock.setTopBlockList(topBlockCache.getTopBlockList(stockCode, topBlockList, topBlock__codeCountMap));


                                                            // 上榜涨幅
                                                            topStock.setChangePctDTO(topStockInfo);


                                                            return topStock;
                                                        })
                                                        .sorted(Comparator.comparing(TopStockDTO::getTopDays).reversed())
                                                        .sorted(Comparator.comparing(TopStockDTO::getTopBlockSize).reversed())
                                                        .collect(Collectors.toList());


        // 人选 -> TOP策略
        dto.setTopStockDTOList(topStockDTOList);


        // -------------------------------------------------------------------------------------------------------------


        // today
        todayInfo(date, dto);


        // -------------------------------------------------------------------------------------------------------------


        return dto;
    }


    private void todayInfo(LocalDate date, TopStockPoolDTO dto) {
        if (date.isBefore(LocalDate.now())) {
            return;
        }


        List<TopChangePctDTO> topChangePctList = Lists.newArrayList();
        dto.getTopStockDTOList().forEach(e -> {
            String stockCode = e.getStockCode();


            StockSnapshotKlineDTO r = KlineAPI.klineCache(stockCode);


            TopChangePctDTO changePctDTO = e.getChangePctDTO();
            // 次日涨跌幅（今日 实时涨跌幅）
            changePctDTO.setToday2Next_changePct(r.getChange_pct());
            changePctDTO.setToday2Next_openPct(of(r.getOpen() / r.getPrevClose() * 100 - 100));
            changePctDTO.setToday2Next_highPct(of(r.getHigh() / r.getPrevClose() * 100 - 100));
            changePctDTO.setToday2Next_lowPct(of(r.getLow() / r.getPrevClose() * 100 - 100));

            topChangePctList.add(changePctDTO);
        });


        // avg
        DoubleSummaryStatistics today2NextStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2Next_changePct).summaryStatistics();
        DoubleSummaryStatistics nextOpenPctStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2Next_openPct).summaryStatistics();
        DoubleSummaryStatistics nextHighPctStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2Next_highPct).summaryStatistics();
        DoubleSummaryStatistics nextLowPctStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2Next_lowPct).summaryStatistics();

        dto.getTopStockAvgPctDTO().setToday2Next_changePct(of(today2NextStats.getAverage()));
        dto.getTopStockAvgPctDTO().setToday2Next_openPct(of(nextOpenPctStats.getAverage()));
        dto.getTopStockAvgPctDTO().setToday2Next_highPct(of(nextHighPctStats.getAverage()));
        dto.getTopStockAvgPctDTO().setToday2Next_lowPct(of(nextLowPctStats.getAverage()));
    }


    @Override
    public int addTopStockSet(LocalDate date, Set<String> stockCodeSet, Integer type) {

        QaTopBlockDO entity = qaTopBlockService.getByDate(date);
        Assert.notNull(entity, String.format("[%s]：数据为空", date));


        // ---------------------------------------------


        // exist（机选 -> 全部）
        Set<String> exist_topStockCodeSet = entity.getTopStockCodeJsonSet(TopTypeEnum.AUTO.type);
        List<TopChangePctDTO> exist_topStockList = entity.getTopStockList(TopTypeEnum.AUTO.type);


        // ---------------------------------------------


        // 去重（已存在 -> 不重复添加）
        stockCodeSet.removeAll(exist_topStockCodeSet);


        // ---------------------------------------------


        // 计算 topInfo（上榜日、涨跌幅、...）
        List<TopChangePctDTO> new_topStockList = calcTopInfo(stockCodeSet, date);
        new_topStockList.forEach(e -> e.getTopTypeSet().add(type)); // type -> 人选


        // ---------------------------------------------


        // update
        exist_topStockList.addAll(new_topStockList);
        entity.setTopStockCodeSet(JSON.toJSONString(exist_topStockList));


//        // ---------------------------------------------
//
//
//        // 当日 个股列表   ->   涨跌幅汇总 均值计算
//        TopPoolAvgPctDTO stockPool__avgPctDTO = avgPct(exist_topStockList);
//
//
//        // 均值
//        entity.setStockAvgPct(JSON.toJSONString(stockPool__avgPctDTO));
//
//
//        // ---------------------------------------------


        qaTopBlockService.updateById(entity);


        return new_topStockList.size();
    }


    @Override
    public int delTopStockSet(LocalDate date, Set<String> stockCodeSet, Integer type) {

        QaTopBlockDO entity = qaTopBlockService.getByDate(date);
        Assert.notNull(entity, String.format("[%s]：数据为空", date));


        // exist（机选 -> 全部）
        List<TopChangePctDTO> exist_topStockList = entity.getTopStockList(TopTypeEnum.AUTO.type);


        // DEL（is_manual = false）
        int[] count = {0};
        exist_topStockList.forEach(e -> {
            if (stockCodeSet.contains(e.getCode())) {
                e.getTopTypeSet().remove(type);
                count[0]++;
            }
        });


        // update
        entity.setTopStockCodeSet(JSON.toJSONString(exist_topStockList));
        qaTopBlockService.updateById(entity);


        return count[0];
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
                        } else {
                            // 当日 无主线   ->   记录 空数据行
                            date_bkyd2__map.computeIfAbsent(date, k -> Sets.newConcurrentHashSet());
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
            entity.setTopBlockCodeSet(JSON.toJSONString(convert2InitTopDTOList(topBlockCodeSet)));

            // 当日 主线个股
            Set<String> topStockCodeSet = date_topStockCodeSet__map.getOrDefault(date, Sets.newHashSet());
            entity.setTopStockCodeSet(JSON.toJSONString(convert2InitTopDTOList(topStockCodeSet)));


            entity.setId(dateIdMap.get(date));
            qaTopBlockService.saveOrUpdate(entity);
        });
    }


    private List<TopChangePctDTO> convert2InitTopDTOList(Set<String> topCodeSet) {
        return topCodeSet.stream().map(code -> {
            String name = StockTypeEnum.isBlock(code) ? data.block__codeNameMap.get(code) : data.stock__codeNameMap.get(code);
            return new TopChangePctDTO(code, name);
        }).collect(Collectors.toList());
    }


    // -----------------------------------------------------------------------------------------------------------------


    private List<TopChangePctDTO> calcTopInfo(Set<String> stockCodeSet, LocalDate date) {


        // -------------------------------------------------------------------------------------------------------------


        List<QaTopBlockDO> topSortListAll = qaTopBlockService.beforeAfterN(date, 100)
                                                             .stream()
                                                             .sorted(Comparator.comparing(QaTopBlockDO::getDate))
                                                             .collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        List<BaseStockDO> filter_stockDOList = baseStockService.listByCodeList(stockCodeSet);
        // List<BaseStockDO> filter_stockDOList = data.stockDOList.stream().filter(e -> stockCodeSet.contains(e.getCode())).collect(Collectors.toList());


        Map<LocalDate, List<TopChangePctDTO>> date_topList_Map = autoType___date_topList_Map(filter_stockDOList, topSortListAll, false, date);


        // -------------------------------------------------------------------------------------------------------------


        return date_topList_Map.getOrDefault(date, Lists.newArrayList());
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 日期-主线列表（主线板块/个股）
     *
     * @param updateTypeEnum
     */
    private void autoType___date_topList_Map(UpdateTypeEnum updateTypeEnum) {


        // -------------------------------------------------------------------------------------------------------------


        List<QaTopBlockDO> topSortListAll = qaTopBlockService.list()
                                                             .stream()
                                                             .sorted(Comparator.comparing(QaTopBlockDO::getDate))
                                                             .collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // ------------------------------------------ date - topList（机选）


        // 机选 主线列表（主线板块/个股）（逐日计算筛选 topList   ->   顺带计算 涨跌幅、B/S信号、...）
        Map<LocalDate, List<TopChangePctDTO>> auto___date_topBlockList_Map = autoType___date_topList_Map(data.blockDOList, topSortListAll);
        Map<LocalDate, List<TopChangePctDTO>> auto___date_topStockList_Map = autoType___date_topList_Map(data.stockDOList, topSortListAll);


        // ------------------------------------------ date - topList（人选  ->  从 机选 中 2次筛选[策略]）


        Map<TopTypeEnum, Map<LocalDate, List<TopChangePctDTO>>> topType___date_topBlockList_Map = topType___date_topList_Map(auto___date_topBlockList_Map);
        Map<TopTypeEnum, Map<LocalDate, List<TopChangePctDTO>>> topType___date_topStockList_Map = topType___date_topList_Map(auto___date_topStockList_Map);


        // -------------------------------------------------------------------------------------------------------------


        // 逐日遍历
        topSortListAll.parallelStream().forEach(e -> {

            // 日期
            LocalDate date = e.getDate();

            if (!data.between(date)) {
                return;
            }


            // ---------------------------------------------------------------------------------------------------------


            // 主线板块/个股 列表（机选）
            List<TopChangePctDTO> auto___topBlockList = auto___date_topBlockList_Map.getOrDefault(date, Lists.newArrayList());
            List<TopChangePctDTO> auto___topStockList = auto___date_topStockList_Map.getOrDefault(date, Lists.newArrayList());


//            // 主线板块/个股 列表（人选）
//            List<TopChangePctDTO> manual___topBlockList = manual___date_topBlockList_Map.getOrDefault(date, Lists.newArrayList());
//            List<TopChangePctDTO> manual___topStockList = manual___date_topStockList_Map.getOrDefault(date, Lists.newArrayList());


            // ---------------------------------------------------------------------------------------------------------


            List<TopPoolAvgPctDTO> topBlockAvgPctList = fillTopTypeSet__and__addAvgPctList(date, auto___topBlockList, topType___date_topBlockList_Map);
            List<TopPoolAvgPctDTO> topStockAvgPctList = fillTopTypeSet__and__addAvgPctList(date, auto___topStockList, topType___date_topStockList_Map);


            // ---------------------------------------------------------------------------------------------------------


            // old != null     +     new -> null     +     INCR_UPDATE
            if (StringUtils.isNotBlank(e.getTopBlockCodeSet()) && CollectionUtils.isEmpty(auto___topBlockList)
                    && Objects.equals(UpdateTypeEnum.INCR, updateTypeEnum)) {
                return;
            }

            if (StringUtils.isNotBlank(e.getTopStockCodeSet()) && CollectionUtils.isEmpty(auto___topStockList)
                    && Objects.equals(UpdateTypeEnum.INCR, updateTypeEnum)) {
                return;
            }


            // ---------------------------------------------------------------------------------------------------------


//            // 非人选（is_manual = false）        从 机选列表 中   匹配出 非人选code     ==>     is_manual = false
//            manual___topList(auto___topBlockList, manual___topBlockList);
//            manual___topList(auto___topStockList, manual___topStockList);


            // ------------------------------------------ topList -> DB


            // 主线板块、主线个股   列表
            e.setTopBlockCodeSet(toJSONString(auto___topBlockList));
            e.setTopStockCodeSet(toJSONString(auto___topStockList));   // TODO   FastJSON序列化异常，使用备用方案（date=2023-02-06）


            // ---------------------------------------------------------------------------------------------------------


//            // ------------------------------------------ avg -> 计算
//
//
//            // 当日 板块/个股列表   ->   涨跌幅汇总 均值计算
//            TopPoolAvgPctDTO topBlockAvgPct_auto = avgPct(auto___topBlockList, TopTypeEnum.AUTO);
//            TopPoolAvgPctDTO topStockAvgPct_auto = avgPct(auto___topStockList, TopTypeEnum.AUTO);
//
//
//            TopPoolAvgPctDTO topBlockAvgPct_manual = avgPct(manual___topBlockList, TopTypeEnum.MANUAL.type);
//            TopPoolAvgPctDTO topStockAvgPct_manual = avgPct(manual___topStockList, TopTypeEnum.MANUAL.type);


            // ------------------------------------------ avg -> DB


//            List<TopPoolAvgPctDTO> topBlockAvgPctList = Lists.newArrayList(topBlockAvgPct_auto);
//            topBlockAvgPctList.add(topBlockAvgPct_manual);
//            topBlockAvgPctList.add(topBlockAvgPct_auto);
            e.setBlockAvgPct(toJSONString(topBlockAvgPctList));


//            List<TopPoolAvgPctDTO> topStockAvgPctList = Lists.newArrayList(topStockAvgPct_auto);
//            topStockAvgPctList.add(topStockAvgPct_manual);
//            topStockAvgPctList.add(topStockAvgPct_auto);
            e.setStockAvgPct(toJSONString(topStockAvgPctList));


            // ---------------------------------------------------------------------------------------------------------


            qaTopBlockService.updateById(e);
        });
    }


    /**
     * date - topList（人选  ->  从 机选 中 2次筛选[策略]）
     *
     * @param auto___date_topList_Map 机选列表
     * @return
     */

    private Map<TopTypeEnum, Map<LocalDate, List<TopChangePctDTO>>> topType___date_topList_Map(Map<LocalDate, List<TopChangePctDTO>> auto___date_topList_Map) {

        Map<TopTypeEnum, Map<LocalDate, List<TopChangePctDTO>>> topType___date_topList_Map = Maps.newHashMap();


        Arrays.stream(TopTypeEnum.values()).forEach(topTypeEnum -> {
            // 机选 -> continue
            if (Objects.equals(TopTypeEnum.AUTO, topTypeEnum)) {
                return;
            }


            // 人选策略
            Map<LocalDate, List<TopChangePctDTO>> manual_topType___date_topBlockList_Map = topType___date_topList_Map(auto___date_topList_Map, topTypeEnum);


            topType___date_topList_Map.put(topTypeEnum, manual_topType___date_topBlockList_Map);
        });


        return topType___date_topList_Map;
    }

    private List<TopPoolAvgPctDTO> fillTopTypeSet__and__addAvgPctList(LocalDate date,
                                                                      List<TopChangePctDTO> auto___topList,
                                                                      Map<TopTypeEnum, Map<LocalDate, List<TopChangePctDTO>>> topType___date_topList_Map) {


        List<TopPoolAvgPctDTO> topAvgPctList = Lists.newArrayList();

        // -------------------------------------------------------------------------------------------------------------


        // -------------------- 计算 avgPct（机选）


        // 当日 板块/个股列表   ->   涨跌幅汇总 均值计算
        TopPoolAvgPctDTO topBlockAvgPct_auto = avgPct(auto___topList, TopTypeEnum.AUTO);

        topAvgPctList.add(topBlockAvgPct_auto);


        // -------------------------------------------------------------------------------------------------------------


        Arrays.stream(TopTypeEnum.values()).forEach(topTypeEnum -> {

            // 机选 -> continue
            if (Objects.equals(TopTypeEnum.AUTO, topTypeEnum)) {
                return;
            }


            // topTypeSet   ->   date_topList_map
            Map<LocalDate, List<TopChangePctDTO>> type___date_topList_Map = topType___date_topList_Map.get(topTypeEnum);


            // date   ->   当日 topList
            List<TopChangePctDTO> type___topList = type___date_topList_Map.getOrDefault(date, Lists.newArrayList());


            // topTypeSet   =>   add -> topTypeEnum
            topList__addTopType(auto___topList, type___topList, topTypeEnum);


            // avg -> 计算
            TopPoolAvgPctDTO topAvgPct_type = avgPct(type___topList, topTypeEnum);


            topAvgPctList.add(topAvgPct_type);
        });


        return topAvgPctList;
    }


    /**
     * 人选 主线列表（主线板块/个股）  ==>   非人选code     ==>     is_manual = false
     *
     * @param auto_topList
     * @param type_topList
     */
    private void topList__addTopType(List<TopChangePctDTO> auto_topList,
                                     List<TopChangePctDTO> type_topList,
                                     TopTypeEnum topTypeEnum) {
        // 人选列表 codeSet
        Set<String> type_codeSet = type_topList.stream().map(TopChangePctDTO::getCode).collect(Collectors.toSet());

        auto_topList.stream()
                    .filter(e -> type_codeSet.contains(e.getCode()))
                    .forEach(e -> e.getTopTypeSet().add(topTypeEnum.type));
    }


    /**
     * 人选 主线列表（主线板块/个股） ->  TOP策略（从机选列表中   ->   3条规则各取 TOP50）
     *
     * @param auto___date_topList_Map 机选 主线列表（主线板块/个股）
     * @param N                       TOP 数量
     * @return 人选 TOP50 数据
     */
    private Map<LocalDate, List<TopChangePctDTO>> manual___date_topList_Map(Map<LocalDate, List<TopChangePctDTO>> auto___date_topList_Map,
                                                                            int N) {

        Map<LocalDate, List<TopChangePctDTO>> manual___date_topList_Map = Maps.newHashMap();


        auto___date_topList_Map.forEach((date, topList) -> {


            // 人选 -> TOP策略（3条规则各取 TOP50）


            // AMO  TOP50
            Set<TopChangePctDTO> rule1_set = topList.stream().sorted(Comparator.comparing(TopChangePctDTO::getAmo).reversed()).limit(N).collect(Collectors.toSet());
            // 当日涨幅 TOP50 / 中期涨幅  TOP50 / 近5日涨幅 TOP50
            Set<TopChangePctDTO> rule2_set = topList.stream().sorted(Comparator.comparing(TopChangePctDTO::getN3日涨幅).reversed()).limit(N).collect(Collectors.toSet());
            // RPS三线和 TOP50 / RPS五线和 TOP50
            Set<TopChangePctDTO> rule3_set = topList.stream().sorted(Comparator.comparing(TopChangePctDTO::getRPS三线和).reversed()).limit(N).collect(Collectors.toSet());


            // 并集（3条规则各取 TOP50 的并集）
            Set<TopChangePctDTO> rule_set = Sets.union(Sets.union(rule1_set, rule2_set), rule3_set);


            manual___date_topList_Map.put(date, Lists.newArrayList(rule_set));
        });


        return manual___date_topList_Map;
    }

    private Map<LocalDate, List<TopChangePctDTO>> topType___date_topList_Map(Map<LocalDate, List<TopChangePctDTO>> auto___date_topList_Map,
                                                                             TopTypeEnum topTypeEnum) {


        if (Objects.equals(TopTypeEnum.MANUAL, topTypeEnum) && !auto___date_topList_Map.isEmpty()) {
            // 板块 TOP10 / 个股 TOP50
            String code = auto___date_topList_Map.values().iterator().next().get(0).getCode();
            int topN = StockTypeEnum.isBlock(code) ? 10 : 50;

            return manual___date_topList_Map(auto___date_topList_Map, topN);
        }


        // -------------------------------------------------------------------------------------------------------------


        Map<LocalDate, List<TopChangePctDTO>> manual___date_topList_Map = Maps.newHashMap();


        auto___date_topList_Map.forEach((date, topList) -> {


            // 人选 -> TOP策略（3条规则各取 TOP50）


            Set<TopChangePctDTO> rule_set = Sets.newHashSet();

            try {
                if (Objects.equals(TopTypeEnum.历史新高, topTypeEnum)) {
                    rule_set = topList.stream().filter(e -> e.getBuySignalExtDataDTO().get历史新高()).collect(Collectors.toSet());
                } else if (Objects.equals(TopTypeEnum.极多头, topTypeEnum)) {
                    rule_set = topList.stream().filter(e -> e.getBuySignalExtDataDTO().get均线极多头()).collect(Collectors.toSet());
                } else if (Objects.equals(TopTypeEnum.RPS三线红, topTypeEnum)) {
                    rule_set = topList.stream().filter(e -> e.getBuySignalExtDataDTO().getRPS三线红()).collect(Collectors.toSet());
                } else if (Objects.equals(TopTypeEnum.十亿, topTypeEnum)) {
                    rule_set = topList.stream().filter(e -> e.getAmo() > 10_0000_0000).collect(Collectors.toSet());
                } /*else if (Objects.equals(TopTypeEnum.首次三线红, topTypeEnum)) {
                    rule_set = topList.stream().filter(e -> e.getBuySignalExtDataDTO().get首次三线红()).collect(Collectors.toSet());
                } else if (Objects.equals(TopTypeEnum.口袋支点, topTypeEnum)) {
                    rule_set = topList.stream().filter(e -> e.getBuySignalExtDataDTO().get口袋支点()).collect(Collectors.toSet());
                } else if (Objects.equals(TopTypeEnum.T0, topTypeEnum)) {
                    rule_set = topList.stream().filter(e -> e.getBuySignalExtDataDTO().getT0()).collect(Collectors.toSet());
                }*/ else if (Objects.equals(TopTypeEnum.涨停, topTypeEnum)) {
                    rule_set = topList.stream().filter(e -> e.getBuySignalExtDataDTO().get涨停()).collect(Collectors.toSet());
                }

            } catch (Exception ex) {
                log.error("topType___date_topList_Map 异常     >>>     topTypeEnum : {} , errMsg : {}", topTypeEnum, ex.getMessage(), ex);
            }


            manual___date_topList_Map.put(date, Lists.newArrayList(rule_set));
        });


        return manual___date_topList_Map;
    }


    /**
     * 安全的JSON序列化方法，避免FastJSON2的ArrayIndexOutOfBoundsException异常
     *
     * @param object 要序列化的对象
     * @return 序列化后的JSON字符串
     */
    private String toJSONString(Object object) {
        if (object == null) {
            return null;
        }

        try {
            // 首先尝试使用默认方式序列化
            return JSON.toJSONString(object);
        } catch (Exception ex) {
            log.warn("FastJSON序列化异常，使用备用方案     >>>     obj : {} , errMsg : {}", object, ex.getMessage(), ex);
            try {
                // 如果失败，尝试使用其他特性
                return JSON.toJSONString(object, JSONWriter.Feature.WriteMapNullValue);
            } catch (Exception ex2) {
                // String json = new Gson().toJson(object);
                log.error("FastJSON序列化失败     >>>     obj : {} , errMsg : {}", object, ex2.getMessage(), ex2);
                // 如果都失败了，返回空数组或空对象
                return object instanceof Collection ? "[]" : "{}";
            }
        }
    }

    public TopPoolAvgPctDTO avgPct(List<TopChangePctDTO> topChangePctList, TopTypeEnum topTypeEnum) {
        if (CollectionUtils.isEmpty(topChangePctList)) {
            return new TopPoolAvgPctDTO(topTypeEnum.type);
        }


        // 使用 Stream 和 DoubleSummaryStatistics 同时计算多个字段的平均值
        DoubleSummaryStatistics amoStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getAmo).summaryStatistics();
        DoubleSummaryStatistics prevCloseStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getPrev_close).summaryStatistics();
        DoubleSummaryStatistics todayCloseStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday_close).summaryStatistics();
        DoubleSummaryStatistics todayPctStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday_changePct).summaryStatistics();

        DoubleSummaryStatistics today2NextStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2Next_changePct).summaryStatistics();
        DoubleSummaryStatistics today2EndStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2End_changePct).summaryStatistics();
        DoubleSummaryStatistics today2MaxStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2Max_changePct).summaryStatistics();

        DoubleSummaryStatistics start2TodayStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getStart2Today_changePct).summaryStatistics();
        DoubleSummaryStatistics start2EndStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getStart2End_changePct).summaryStatistics();
        DoubleSummaryStatistics start2MaxStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getStart2Max_changePct).summaryStatistics();

        DoubleSummaryStatistics start2NextStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getStart2Next_changePct).summaryStatistics();
        DoubleSummaryStatistics start2Next3Stats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getStart2Next3_changePct).summaryStatistics();
        DoubleSummaryStatistics start2Next5Stats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getStart2Next5_changePct).summaryStatistics();
        DoubleSummaryStatistics start2Next10Stats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getStart2Next10_changePct).summaryStatistics();
        DoubleSummaryStatistics start2Next15Stats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getStart2Next15_changePct).summaryStatistics();
        DoubleSummaryStatistics start2Next20Stats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getStart2Next20_changePct).summaryStatistics();


        // 涨停数量
        long zt_count = topChangePctList.stream().filter(TopChangePctDTO::isZtFlag).count();
        // 次日   ->   O/H/L/C 涨跌幅（%）
        DoubleSummaryStatistics nextOpenPctStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2Next_openPct).summaryStatistics();
        DoubleSummaryStatistics nextHighPctStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2Next_highPct).summaryStatistics();
        DoubleSummaryStatistics nextLowPctStats = topChangePctList.stream().mapToDouble(TopChangePctDTO::getToday2Next_lowPct).summaryStatistics();


        Map<String, Integer> buy_countMap = Maps.newHashMap();
        topChangePctList.stream().flatMap(e -> e.getBuySignalSet().stream()).forEach(signal -> buy_countMap.merge(signal, 1, Integer::sum));

        Map<String, Integer> max_countMap = Maps.newHashMap();
        topChangePctList.stream().flatMap(e -> e.getMaxSignalSet().stream()).forEach(signal -> max_countMap.merge(signal, 1, Integer::sum));

        Map<String, Integer> sell_countMap = Maps.newHashMap();
        topChangePctList.stream().flatMap(e -> e.getSellSignalSet().stream()).forEach(signal -> sell_countMap.merge(signal, 1, Integer::sum));


        // -------------------------------------------------------------------------------------------------------------


        // 创建结果对象并设置平均值
        TopPoolAvgPctDTO avgPct = new TopPoolAvgPctDTO();

        avgPct.setType(topTypeEnum.type);
        avgPct.setTotal(topChangePctList.size());


        avgPct.setAmo(of(amoStats.getAverage()));
        avgPct.setPrev_close(of(prevCloseStats.getAverage()));
        avgPct.setToday_close(of(todayCloseStats.getAverage()));
        avgPct.setToday_changePct(of(todayPctStats.getAverage()));

        avgPct.setStart2Today_changePct(of(start2TodayStats.getAverage()));
        avgPct.setStart2End_changePct(of(start2EndStats.getAverage()));
        avgPct.setStart2Max_changePct(of(start2MaxStats.getAverage()));

        avgPct.setToday2Next_changePct(of(today2NextStats.getAverage()));
        avgPct.setToday2End_changePct(of(today2EndStats.getAverage()));
        avgPct.setToday2Max_changePct(of(today2MaxStats.getAverage()));

        avgPct.setStart2Next_changePct(of(start2NextStats.getAverage()));
        avgPct.setStart2Next3_changePct(of(start2Next3Stats.getAverage()));
        avgPct.setStart2Next5_changePct(of(start2Next5Stats.getAverage()));
        avgPct.setStart2Next10_changePct(of(start2Next10Stats.getAverage()));
        avgPct.setStart2Next15_changePct(of(start2Next15Stats.getAverage()));
        avgPct.setStart2Next20_changePct(of(start2Next20Stats.getAverage()));


        avgPct.setBuy_countMap(MapUtil.reverseSortByValue(buy_countMap));
        avgPct.setMax_countMap(MapUtil.reverseSortByValue(max_countMap));
        avgPct.setSell_countMap(MapUtil.reverseSortByValue(sell_countMap));


        avgPct.setZt_count((int) zt_count);
        avgPct.setToday2Next_openPct(of(nextOpenPctStats.getAverage()));
        avgPct.setToday2Next_highPct(of(nextHighPctStats.getAverage()));
        avgPct.setToday2Next_lowPct(of(nextLowPctStats.getAverage()));


        return avgPct;
    }


    public Map<LocalDate, List<TopChangePctDTO>> autoType___date_topList_Map(List dataList,
                                                                             List<QaTopBlockDO> topSortListAll) {

        return autoType___date_topList_Map(dataList, topSortListAll, true, null);
    }


    /**
     * 计算 首次上榜日期（往前倒推）、跌出榜单日期、涨跌幅
     *
     * @param dataList
     * @param topSortListAll
     * @param checkTop       是否校验 IN主线
     * @param baseDate       如果 baseDate != null   ->   仅计算 指定日期
     * @return
     */
    public Map<LocalDate, List<TopChangePctDTO>> autoType___date_topList_Map(List dataList,
                                                                             List<QaTopBlockDO> topSortListAll,
                                                                             boolean checkTop,
                                                                             LocalDate baseDate) {


        // -------------------------------------------------------------------------------------------------------------


        Map<LocalDate, Integer> top__dateIdxMap = Maps.newHashMap();
        for (int i = 0; i < topSortListAll.size(); i++) {
            top__dateIdxMap.put(topSortListAll.get(i).getDate(), i);
        }


        // -------------------------------------------------------------------------------------------------------------


        Map<LocalDate, Map<String, TopChangePctDTO>> date__code_topData__Map = Maps.newConcurrentMap();


        AtomicInteger COUNT = new AtomicInteger();
        long startTime = System.currentTimeMillis();


        // -------------------------------------------------------------------------------------------------------------


        // 遍历计算   =>   每日  -  code_topDate
        // ✅ 使用分片并行处理：每片 200 个股票
        ParallelCalcUtil.chunkForEachWithProgress(/*data.stockDOList*/ dataList, 200, chunk -> {


            chunk.forEach(stockDO -> {


                try {
                    String code = stockDO instanceof BaseStockDO ? ((BaseStockDO) stockDO).getCode() : ((BaseBlockDO) stockDO).getCode();
                    String name = stockDO instanceof BaseStockDO ? ((BaseStockDO) stockDO).getName() : ((BaseBlockDO) stockDO).getName();
                    StockFun fun = stockDO instanceof BaseStockDO ? data.getOrCreateStockFun((BaseStockDO) stockDO) : data.getOrCreateBlockFun((BaseBlockDO) stockDO);


                    KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
                    ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
                    Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                    double[] close = klineArrDTO.close;

                    int minIdx = 0;
                    int maxIdx = dateIndexMap.size() - 1;


                    // -------------------------------------------------------------------------------------------------


                    log.info("calcTopInfoTask     >>>     count : {} , time : {} , code : {}", COUNT.incrementAndGet(), DateTimeUtil.formatNow2Hms(startTime), code);


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


                    // ------------------------- 当前 板块/个股     逐日计算


                    // date  -  code_topDate
                    dateIndexMap.forEach((date, date_idx) -> {


                        // ---------------------------------------------------------------------------------------------


                        // 仅计算 指定日期
                        if (baseDate != null && !date.isEqual(baseDate)) {
                            return;
                        }


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


                        if (checkTop) {
                            // 当日   ->   主线板块/个股   列表
                            Set<String> base__topCodeSet = TopBlockCache.getTopCodeSet(base__topBlockDO, code, TopTypeEnum.AUTO.type);
                            // 当前板块/个股       当日 -> IN主线
                            if (!base__topCodeSet.contains(code)) {
                                return;
                            }
                        }


                        // ---------------------------------------------------------------------------------------------


                        // ---------------------------- topStartDate


                        // top区间 - 起始日期   ->   距今天数
                        int last__SSF_MA20空__days = BARSLAST__SSF_MA20空[date_idx];
                        // top区间 - 起始日期   ->   idx
                        int topStartIdx = Math.max(topBaseIdx - last__SSF_MA20空__days, 0);


                        // 逐日 遍历top区间
                        for (int i = topStartIdx; i <= topBaseIdx; i++) {
                            QaTopBlockDO start__topBlockDO = topEntity__before50_after50.get(i);

                            LocalDate topStartDate = start__topBlockDO.getDate();
                            Set<String> start__topCodeSet = TopBlockCache.getTopCodeSet(start__topBlockDO, code, TopTypeEnum.AUTO.type);


                            // 当前个股     ==>     当日 IN主线     =>     当日 -> topStartDate
                            if (start__topCodeSet.contains(code)) {

                                // date  -  code_topDate
                                date__code_topData__Map.computeIfAbsent(date, k -> Maps.newConcurrentMap())
                                                       .computeIfAbsent(code, k -> new TopChangePctDTO(code, name))
                                                       .setTopStartDate(topStartDate);

                                break;
                            }
                        }


                        // ---------------------------------------------------------------------------------------------


                        // date  -  code_topDate
                        TopChangePctDTO topChangePctDTO = date__code_topData__Map.computeIfAbsent(date, k -> Maps.newConcurrentMap())
                                                                                 .computeIfAbsent(code, k -> new TopChangePctDTO(code, name));


                        if (/*null == topChangePctDTO ||*/ null == topChangePctDTO.getTopStartDate()) {

                            Set<String> topBlockCodeSet = base__topBlockDO.getTopBlockCodeJsonSet(TopTypeEnum.AUTO.type);
                            Set<String> topStockCodeSet = base__topBlockDO.getTopStockCodeJsonSet(TopTypeEnum.AUTO.type);

                            log.error("topChangePctDTO = null   -   当前个股 当日   非主线     >>>     {} , {} , topBlockCodeSet : {} , topStockCodeSet : {}",
                                      date, code, topBlockCodeSet, topStockCodeSet);


                            if (checkTop) {
                                return;
                            }
                            topChangePctDTO.setTopStartDate(date);
                        }


                        // ---------------------------------------------------------------------------------------------


                        // ---------------------------- topEndDate


                        // 今日   ->   top区间 - end日期
                        int next__下SSF_MA20__days = BARSNEXT__下SSF_MA20[date_idx];


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


                        // 人选 -> 策略（TOP50）
                        fillTopStrategySortInfo(topChangePctDTO, klineArrDTO, extDataArrDTO, date_idx);


                        // ---------------------------------------------------------------------------------------------


                        // 上榜涨幅
                        calcTopChangePct(close, date_idx, minIdx, maxIdx, next__下SSF_MA20__days, fun, dateIndexMap, extDataArrDTO, topChangePctDTO);
                    });


                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            });


        }, ThreadPoolType.IO_INTENSIVE_2);


        // -------------------------------------------------------------------------------------------------------------


        // date_topList_Map
        return date__code_topData__Map.entrySet()
                                      .stream()
                                      .collect(Collectors.toMap(
                                              Map.Entry::getKey,                             // LocalDate
                                              e -> Lists.newArrayList(e.getValue().values()) // List<TopChangePctDTO>
                                      ));
    }

    private void fillTopStrategySortInfo(TopChangePctDTO topChangePctDTO,
                                         KlineArrDTO klineArrDTO,
                                         ExtDataArrDTO extDataArrDTO,
                                         Integer date_idx) {


        double today_close = klineArrDTO.close[date_idx];
        int max_idx = klineArrDTO.close.length - 1;


        // 是否涨停
        topChangePctDTO.setZtFlag(extDataArrDTO.涨停[date_idx]);
        // 次日   ->   开盘价/最高价/最低价/收盘价 涨跌幅（%）
        topChangePctDTO.setToday2Next_openPct(of(klineArrDTO.open[Math.min(date_idx + 1, max_idx)] / today_close * 100 - 100));
        topChangePctDTO.setToday2Next_highPct(of(klineArrDTO.high[Math.min(date_idx + 1, max_idx)] / today_close * 100 - 100));
        topChangePctDTO.setToday2Next_lowPct(of(klineArrDTO.low[Math.min(date_idx + 1, max_idx)] / today_close * 100 - 100));


        // 次2日   ->   开盘价/最高价/最低价/收盘价 涨跌幅（%）
        topChangePctDTO.setToday2N2_openPct(of(klineArrDTO.open[Math.min(date_idx + 2, max_idx)] / today_close * 100 - 100));
        topChangePctDTO.setToday2N2_highPct(of(klineArrDTO.high[Math.min(date_idx + 2, max_idx)] / today_close * 100 - 100));
        topChangePctDTO.setToday2N2_lowPct(of(klineArrDTO.low[Math.min(date_idx + 2, max_idx)] / today_close * 100 - 100));
        topChangePctDTO.setToday2N2_closePct(of(klineArrDTO.close[Math.min(date_idx + 2, max_idx)] / today_close * 100 - 100));


        topChangePctDTO.setAmo(klineArrDTO.amo[date_idx]);


        topChangePctDTO.setRPS三线和(extDataArrDTO.RPS三线和[date_idx]);
        topChangePctDTO.setRPS五线和(extDataArrDTO.RPS五线和[date_idx]);


        topChangePctDTO.set中期涨幅(extDataArrDTO.中期涨幅[date_idx]);
        topChangePctDTO.setN3日涨幅(extDataArrDTO.N3日涨幅[date_idx]);
        topChangePctDTO.setN5日涨幅(extDataArrDTO.N5日涨幅[date_idx]);
        topChangePctDTO.setN10日涨幅(extDataArrDTO.N10日涨幅[date_idx]);
        topChangePctDTO.setN20日涨幅(extDataArrDTO.N20日涨幅[date_idx]);
    }

    private void calcTopChangePct(double[] close,
                                  Integer date_idx,
                                  int minIdx,
                                  int maxIdx,
                                  int next__下SSF_MA20__days,
                                  StockFun fun,
                                  Map<LocalDate, Integer> dateIndexMap,
                                  ExtDataArrDTO extDataArrDTO,
                                  TopChangePctDTO topChangePctDTO) {


        // ---------------------------- 上榜涨幅


        Integer prevDate_idx = Math.max(date_idx - 1, minIdx);
        Integer nextDate_idx = Math.min(date_idx + 1, maxIdx);

        Integer startTopDate_idx = dateIndexMap.get(topChangePctDTO.topStartDate);
        Integer endTopDate_idx = dateIndexMap.get(topChangePctDTO.topEndDate);

        // 停牌
        endTopDate_idx = endTopDate_idx == null ? date_idx : endTopDate_idx;


        int start_maxTopDate_idx = maxCloseIdx(close, Math.min(startTopDate_idx + 1, endTopDate_idx), endTopDate_idx);
        int today_maxTopDate_idx = maxCloseIdx(close, nextDate_idx, endTopDate_idx);


        // ---------------------------------------------------------------------------------------------


        double date_close = close[date_idx];
        double prev_close = close[prevDate_idx];
        double nextDate_idx_close = close[nextDate_idx];
        double startTopDate_idx_close = close[startTopDate_idx];
        double endTopDate_idx_close = close[endTopDate_idx];

        double start_maxTopDate_idx_close = close[start_maxTopDate_idx];
        double today_maxTopDate_idx_close = close[today_maxTopDate_idx];


        // ---------------------------------------------------------------------------------------------


        double today_changePct = date_close / prev_close * 100 - 100;

        double start2Today_changePct = date_close / startTopDate_idx_close * 100 - 100;
        double start2End_changePct = endTopDate_idx_close / startTopDate_idx_close * 100 - 100;
        double start2Max_changePct = start_maxTopDate_idx_close / startTopDate_idx_close * 100 - 100;

        double today2Next_changePct = nextDate_idx_close / date_close * 100 - 100;
        double today2End_changePct = endTopDate_idx_close / date_close * 100 - 100;
        double today2Max_changePct = today_maxTopDate_idx_close / date_close * 100 - 100;

        double start2Next_changePct = close[Math.min(startTopDate_idx + 1, endTopDate_idx)] / startTopDate_idx_close * 100 - 100;
        double start2Next3_changePct = close[Math.min(startTopDate_idx + 3, endTopDate_idx)] / startTopDate_idx_close * 100 - 100;
        double start2Next5_changePct = close[Math.min(startTopDate_idx + 5, endTopDate_idx)] / startTopDate_idx_close * 100 - 100;
        double start2Next10_changePct = close[Math.min(startTopDate_idx + 10, endTopDate_idx)] / startTopDate_idx_close * 100 - 100;
        double start2Next15_changePct = close[Math.min(startTopDate_idx + 15, endTopDate_idx)] / startTopDate_idx_close * 100 - 100;
        double start2Next20_changePct = close[Math.min(startTopDate_idx + 20, endTopDate_idx)] / startTopDate_idx_close * 100 - 100;


        // ---------------------------------------------------------------------------------------------


        topChangePctDTO.setPrev_close(of(prev_close));
        topChangePctDTO.setToday_close(of(date_close));
        topChangePctDTO.setToday_changePct(of(today_changePct));

        topChangePctDTO.setToday2Next_changePct(of(today2Next_changePct));
        topChangePctDTO.setToday2End_changePct(of(today2End_changePct));
        topChangePctDTO.setToday2Max_changePct(of(today2Max_changePct));

        topChangePctDTO.setStart2Today_changePct(of(start2Today_changePct));
        topChangePctDTO.setStart2End_changePct(of(start2End_changePct));
        topChangePctDTO.setStart2Max_changePct(of(start2Max_changePct));

        topChangePctDTO.setStart2Next_changePct(of(start2Next_changePct));
        topChangePctDTO.setStart2Next3_changePct(of(start2Next3_changePct));
        topChangePctDTO.setStart2Next5_changePct(of(start2Next5_changePct));
        topChangePctDTO.setStart2Next10_changePct(of(start2Next10_changePct));
        topChangePctDTO.setStart2Next15_changePct(of(start2Next15_changePct));
        topChangePctDTO.setStart2Next20_changePct(of(start2Next20_changePct));


        // ---------------------------------------------------------------------------------------------


        List<ExtDataDTO> extDataDTOList = fun.getExtDataDTOList();


        topChangePctDTO.setBuySignalSet(convert__signalSet(extDataArrDTO, date_idx, true));                  // 上榜日-idx
        topChangePctDTO.setBuySignalExtDataDTO(convert__signalExtDataDTO(extDataDTOList, date_idx));              // 上榜日-idx

        // 今日 = maxTopDate
        int _start_maxTopDate_idx = start_maxTopDate_idx == date_idx ? start_maxTopDate_idx : -1;
        topChangePctDTO.setMaxSignalSet(convert__signalSet(extDataArrDTO, _start_maxTopDate_idx, true));     // 上榜Max-idx
        topChangePctDTO.setMaxSignalExtDataDTO(convert__signalExtDataDTO(extDataDTOList, _start_maxTopDate_idx)); // 上榜Max-idx

        // 今日 = prev_落榜日
        int _endTopDate_idx = next__下SSF_MA20__days != -1 && date_idx == endTopDate_idx - 1 ? endTopDate_idx : -1;
        topChangePctDTO.setSellSignalSet(convert__signalSet(extDataArrDTO, _endTopDate_idx, false));         // 落榜日-idx
        topChangePctDTO.setSellSignalExtDataDTO(convert__signalExtDataDTO(extDataDTOList, _endTopDate_idx));      // 落榜日-idx
    }


    @SneakyThrows
    private Set<String> convert__signalSet(ExtDataArrDTO extDataArrDTO, Integer idx, boolean flag) {
        Set<String> buySignalSet = Sets.newHashSet();
        if (idx == -1) {
            return buySignalSet;
        }


        // 获取类声明的所有字段，包括私有字段
        Field[] fields = extDataArrDTO.getClass().getDeclaredFields();


        // 遍历每个字段
        for (Field field : fields) {

            // 设置字段可以访问（处理 private 字段）
            field.setAccessible(true);

            // 获取字段的值
            Object arrayValue = field.get(extDataArrDTO);


            // 确保字段值是一个 boolean 数组
            if (arrayValue instanceof boolean[]) {
                boolean[] booleanArray = (boolean[]) arrayValue;

                // 检查索引是否在数组范围内
                if (idx >= 0 && idx < booleanArray.length) {
                    boolean fieldValue = booleanArray[idx];

                    // 如果值为 true/false，将字段名添加到集合中
                    if (fieldValue == flag) {
                        buySignalSet.add(field.getName());
                    }
                }
            }
        }


        return buySignalSet;
    }


    private ExtDataDTO convert__signalExtDataDTO(List<ExtDataDTO> extDataDTOList, Integer idx) {
        return idx == -1 ? null : extDataDTOList.get(idx);
    }


    private int maxCloseIdx(double[] close, int start, int end) {
        int maxCloseIdx = start;
        for (int i = start; i <= end; i++) {
            if (close[maxCloseIdx] < close[i]) {
                maxCloseIdx = i;
            }
        }
        return maxCloseIdx;
    }

    private double maxClose(double[] close, int start, int end) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = start; i <= end; i++) {
            max = Math.max(max, close[i]);
        }
        return max;
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


        initCache(UpdateTypeEnum.ALL);


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
                stockCode_rps强度_map.put(stockCode, of(RPS三线和));
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


    private void initCache(UpdateTypeEnum updateTypeEnum) {
        if (Objects.equals(UpdateTypeEnum.INCR, updateTypeEnum)) {
            initDataService.incrUpdateInitData();
        } else {
            data = initDataService.initData();
        }
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


    private void calcNDayHigh(UpdateTypeEnum updateTypeEnum, int N) {


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


//        // --------------------------------------------------- 按 板块 分类
//
//
//        Integer blockNewId = BlockNewIdEnum.百日新高.getBlockNewId();
//
//
//        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);
//
//
//        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__highMap);
//        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));


        // --------------------------------------------------- 按 板块 分类


        Integer blockNewId = BlockNewIdEnum.百日新高.getBlockNewId();
        Set<LocalDate> dateSet = date_stockCodeSet__highMap.keySet();


        delOld(blockNewId, dateSet, updateTypeEnum);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__highMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcChangePctTop(UpdateTypeEnum updateTypeEnum, int N, double limitChangePct) {


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
        Set<LocalDate> dateSet = date_stockCodeSet__topMap.keySet();


        delOld(blockNewId, dateSet, updateTypeEnum);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcRpsRed(UpdateTypeEnum updateTypeEnum, double RPS) {


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
        Set<LocalDate> dateSet = date_stockCodeSet__topMap.keySet();


        delOld(blockNewId, dateSet, updateTypeEnum);


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


    private void calcLongTermMABullStack(UpdateTypeEnum updateTypeEnum) {


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
        Set<LocalDate> dateSet = date_stockCodeSet__topMap.keySet();


        delOld(blockNewId, dateSet, updateTypeEnum);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcBullMAStack(UpdateTypeEnum updateTypeEnum) {


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
        Set<LocalDate> dateSet = date_stockCodeSet__topMap.keySet();


        delOld(blockNewId, dateSet, updateTypeEnum);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcExtremeBullMAStackTask(UpdateTypeEnum updateTypeEnum) {


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
        Set<LocalDate> dateSet = date_stockCodeSet__topMap.keySet();


        delOld(blockNewId, dateSet, updateTypeEnum);


        Map<LocalDate, Set<String>> sortMap = new TreeMap<>(date_stockCodeSet__topMap);
        sortMap.forEach((date, stockCodeSet) -> blockSum(date, stockCodeSet, blockNewId));
    }


    // -----------------------------------------------------------------------------------------------------------------


    private void calcBlockAmoTop(UpdateTypeEnum updateTypeEnum) {
        AtomicInteger x = new AtomicInteger(0);


        // 日期-板块类型-板块lv       -       AMO_blockCode_TreeMap
        // <groupKey: date|type|level, <AMO DESC, blockCode>>
        Map<String, TreeMap<Double, String>> date__block_type_lv_____amo_blockCode_TreeMap_____map = Maps.newHashMap();


        ParallelCalcUtil.chunkForEachWithProgress(data.dateList, 200, chunk -> {


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


//        qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);


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


        // -------------------------------------------------------------------------------------------------------------


        // del old
        delOld(blockNewId, date_entity_map.keySet(), updateTypeEnum);


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


    private void delOld(Integer blockNewId, Set<LocalDate> dateSet, UpdateTypeEnum updateTypeEnum) {
        if (Objects.equals(UpdateTypeEnum.INCR, updateTypeEnum)) {
            qaBlockNewRelaStockHisService.deleteByDateSet(blockNewId, dateSet);
        } else if (Objects.equals(UpdateTypeEnum.ALL, updateTypeEnum)) {
            qaBlockNewRelaStockHisService.deleteAll(blockNewId, null);
        } else {
            throw new BizException("updateTypeEnum 异常：" + updateTypeEnum);
        }
    }

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


        Integer idx = tradeDateIdx(fun.getDate(), fun.getDateIndexMap(), date);
        return idx != null && SSF多_arr[idx];
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static double of(Number val) {
        return NumUtil.of(val);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    public static class BlockTopInfoDTO {

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