package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.TdxTqAPI;
import com.bebopze.tdx.quant.client.TdxTqKlineAPI;
import com.bebopze.tdx.quant.common.config.anno.DistributedLock;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.BlockTypeEnum;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.tq.GetStockInfoDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.parser.tdxdata.LdayParser;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.KlineService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * kline 行情数据 拉取
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
@Slf4j
@Service
public class KlineServiceImpl implements KlineService {


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseOptionService baseOptionService;


    @Autowired
    private InitDataService initDataService;


    /**
     * 板块+个股 - 行情（kline_his）     一键刷新
     */
    @TotalTime
    @SneakyThrows
    @Override
    public void refreshKlineAll(UpdateTypeEnum updateTypeEnum) {

        // 行情-板块
        Future<?> task1 = Executors.newSingleThreadExecutor().submit(() -> {
            // refresh  ->  block kline
            fillBlockKlineAll(updateTypeEnum);
        });


        // 行情-个股/ETF
        Future<?> task2 = Executors.newSingleThreadExecutor().submit(() -> {
            // refresh  ->  stock/ETF kline
            fillStockKlineAll(updateTypeEnum);
        });


        // 行情-期权
        Future<?> task3 = Executors.newSingleThreadExecutor().submit(() -> {
            // refresh  ->  option kline
            fillOptionKlineAll();
        });


        task1.get();
        task2.get();
        task3.get();
    }

    @TotalTime
    @SneakyThrows
    @Override
    public void refreshKlineAll__lastDay() {

        // 行情-板块
        Future<?> task1 = Executors.newSingleThreadExecutor().submit(() -> {
            // refresh  ->  block kline
            fillBlockKlineAll__lastDay();
        });


        // 行情-个股/ETF
        Future<?> task2 = Executors.newSingleThreadExecutor().submit(() -> {
            // refresh  ->  stock/ETF kline
            fillStockKlineAll__lastDay();
        });


        task1.get();
        task2.get();
    }


    @TotalTime
    @Override
    public void fillBlockKline(String blockCode, UpdateTypeEnum updateTypeEnum) {
        fillBlockKline(blockCode, null, updateTypeEnum);
        log.info("fillBlockKline suc     >>>     blockCode : {}", blockCode);
    }

    private void fillBlockKline(String blockCode, Long blockId, UpdateTypeEnum updateTypeEnum) {


        // --------------------- ID


        blockId = blockId == null ? baseBlockService.getIdByCode(blockCode) : blockId;
        Assert.notNull(blockId, "板块信息不存在：" + blockCode);


        // ---------------------  拉取数据     ->     通达信-本地读取   /   通达信 TQ


        // 通达信   本地读取
        List<String> klines = klinesFromTdx(blockCode, updateTypeEnum);


        // -------------------------------------------------------------------------------------------------------------


        // --------------------- entity -> 实时行情

        if (CollectionUtils.isEmpty(klines)) {
            return;
        }


        // 实时行情
        BaseBlockDO entity = convertBlockDO(blockId, null, klines, updateTypeEnum);


        // --------------------- DB
        baseBlockService.updateById(entity);
    }


    @TotalTime
    @Override
    public void fillBlockKlineAll(UpdateTypeEnum updateTypeEnum) {
        long[] start = {System.currentTimeMillis()};

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger suc_count = new AtomicInteger(0);
        AtomicInteger err_count = new AtomicInteger(0);


        // -------------------------------------------------------------------------------------------------------------


        // del ALL   ->   kline_ext_data
        if (updateTypeEnum == UpdateTypeEnum.HIS) {
            baseBlockService.delAllKlineExtData();
        }


        // -------------------------------------------------------------------------------------------------------------


        // insert ALL   ->   kline_ext_data


        Map<String, Long> codeIdMap = baseBlockService.codeIdMap();


        codeIdMap.keySet().parallelStream().forEach((blockCode) -> {
            Long blockId = codeIdMap.get(blockCode);
            int countVal = count.incrementAndGet();


            try {
                fillBlockKline(blockCode, blockId, updateTypeEnum);


                // ------------------------------------------- 计时（频率）   187ms/次   x 884     ->     总耗时：2min 45s


                long time = System.currentTimeMillis() - start[0];


                long r1 = time / countVal;
                long r2 = countVal * 1000L / time;
                String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


                // blockCode : 880367, blockId : 43 , count : 884 , suc : 881 , err : 3 , r1 : 187ms/次 , r2 : 5次/s , r3 : 884次 - 2min 45s
                log.info("fillBlockKlineAll - suc     >>>     blockCode : {}, blockId : {} , count : {} , suc : {} , err : {} , r1 : {}ms/次 , r2 : {}次/s , r3 : {}",
                         blockCode, blockId, countVal, suc_count.incrementAndGet(), err_count.get(), r1, r2, r3);


            } catch (Exception ex) {
                log.error("fillBlockKlineAll - err     >>>     blockCode : {}, blockId : {} , count : {} , suc : {} , err : {}",
                          blockCode, blockId, countVal, suc_count.get(), err_count.incrementAndGet(), ex);
            }
        });


        initDataService.deleteDiskCache();
    }


    @TotalTime
    @Override
    public void fillBlockKlineAll__lastDay() {
        long[] start = {System.currentTimeMillis()};

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger suc_count = new AtomicInteger(0);
        AtomicInteger err_count = new AtomicInteger(0);


        // -------------------------------------------------------------------------------------------------------------


        // 全部板块   codeIdMap
        Map<String, Long> codeIdMap = baseBlockService.codeIdMap();


        // 拉取全部   板块 - K线数据（实时行情）
        List<StockSnapshotKlineDTO> allBlock__snapshotKline = TdxTqKlineAPI.pullAllBlockSnapshotKline();
        Map<String, StockSnapshotKlineDTO> blockCode_snapshotKline__Map = allBlock__snapshotKline.stream()
                                                                                                 .collect(Collectors.toMap(StockSnapshotKlineDTO::getStockCode, e -> e));


        // -------------------------------------------------------------------------------------------------------------


        codeIdMap.keySet().parallelStream().forEach((blockCode) -> {
            Long blockId = codeIdMap.get(blockCode);
            int countVal = count.incrementAndGet();


            StockSnapshotKlineDTO snapshot_klineDTO = blockCode_snapshotKline__Map.get(blockCode);
            if (snapshot_klineDTO == null) {
                return;
            }


            try {
                fillBlockKline__lastDay(blockCode, blockId, snapshot_klineDTO.toKlineDTO());


                // ------------------------------------------- 计时（频率）   26ms/次   x 881     ->     总耗时：23s


                long time = System.currentTimeMillis() - start[0];


                long r1 = time / countVal;
                long r2 = countVal * 1000L / time;
                String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


                // blockCode : 880444, blockId : 94 , count : 881 , r1 : 26ms/次 , r2 : 37次/s , r3 : 881次 - 23s
                log.info("fillBlockKlineAll__lastDay - suc     >>>     blockCode : {}, blockId : {} , count : {} , suc : {} , err : {} , r1 : {}ms/次 , r2 : {}次/s , r3 : {}",
                         blockCode, blockId, countVal, suc_count.incrementAndGet(), err_count.get(), r1, r2, r3);


            } catch (Exception ex) {

                // 不要影响 其他正常板块
                log.error("fillBlockKlineAll__lastDay - err     >>>     blockCode : {}, blockId : {} , count : {} , suc : {} , err : {}",
                          blockCode, blockId, countVal, suc_count.get(), err_count.incrementAndGet(), ex);
            }
        });


        // -------------------------------------------------------------------------------------------------------------


        initDataService.deleteDiskCache();
    }


    private void fillBlockKline__lastDay(String blockCode, Long blockId, KlineDTO snapshot__klineDTO) {


        // 板块 - K线数据
        BaseBlockDO dn_entity = baseBlockService.getByCodeAndUpdateType(blockCode, UpdateTypeEnum.N_DAY.type);
        List<KlineDTO> nd_klineDTOList = dn_entity.getKlineDTOList();


        Assert.notEmpty(nd_klineDTOList, String.format("[%s] 的 nd_kline 数据为空", blockCode));


        // ----------------------------------------------------------


        KlineDTO last__nd_klineDTO = nd_klineDTOList.getLast();


        // 更新 K线数据
        if (snapshot__klineDTO.getDate().isAfter(last__nd_klineDTO.getDate())) {
            // 最新日期 不存在  ->  添加
            nd_klineDTOList.add(snapshot__klineDTO);
        } else if (snapshot__klineDTO.getDate().equals(last__nd_klineDTO.getDate())) {
            // 最新日期 已存在  ->  替换
            nd_klineDTOList.set(nd_klineDTOList.size() - 1, snapshot__klineDTO);
        }


        // ----------------------------------------------------------


        BaseBlockDO entity = new BaseBlockDO();
        entity.setCode(blockCode);


        // kline
        entity.setKlineNdStr(JSON.toJSONString(ConvertStockKline.dtoList2StrList(nd_klineDTOList)));


        // 昨日
        if (nd_klineDTOList.size() >= 2) {
            KlineDTO prev = nd_klineDTOList.get(nd_klineDTOList.size() - 2);
            entity.setPrevClose(NumUtil.double2Decimal(prev.getClose()));
        }


        // 板块 - 实时行情
        entity.setTradeDate(snapshot__klineDTO.getDate());
        entity.setOpen(NumUtil.double2Decimal(snapshot__klineDTO.getOpen()));
        entity.setHigh(NumUtil.double2Decimal(snapshot__klineDTO.getHigh()));
        entity.setLow(NumUtil.double2Decimal(snapshot__klineDTO.getLow()));
        entity.setClose(NumUtil.double2Decimal(snapshot__klineDTO.getClose()));
        entity.setVolume(snapshot__klineDTO.getVol());
        entity.setAmount(NumUtil.double2Decimal(snapshot__klineDTO.getAmo()));
        entity.setRangePct(NumUtil.double2Decimal(snapshot__klineDTO.getRangePct()));
        entity.setChangePct(NumUtil.double2Decimal(snapshot__klineDTO.getChangePct()));
        entity.setChangePrice(NumUtil.double2Decimal(snapshot__klineDTO.getChangePrice()));
        entity.setTurnoverPct(NumUtil.double2Decimal(snapshot__klineDTO.getTurnoverPct()));


        if (blockId != null) {
            entity.setId(blockId);
            baseBlockService.updateById(entity);
        } else {
            blockId = baseBlockService.getIdByCode(blockCode);
            entity.setId(blockId);

            baseBlockService.saveOrUpdate(entity);
        }


        log.info("fillBlockKline__lastDay suc     >>>     blockCode : {}", blockCode);
    }


    @TotalTime
    @Override
    public void fillStockKline(String stockCode, UpdateTypeEnum updateTypeEnum) {
        fillStockKline(stockCode, null, updateTypeEnum);
        log.info("fillStockKline suc     >>>     stockCode : {}", stockCode);
    }

    @TotalTime
    @Override
    public void fillStockKlineAll(UpdateTypeEnum updateTypeEnum) {


        // 全量股票   codeIdMap
        Map<String, Long> codeIdMap = baseStockService.codeIdMap((Integer) null);


        // kline 更新
        update__fillStockKlineAll(codeIdMap, updateTypeEnum);


        initDataService.deleteDiskCache();
    }


    /**
     * kline 更新
     */
    @TotalTime
    @Override
    public void fillStockKlineAll__lastDay() {
        long[] start = {System.currentTimeMillis()};

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger suc_count = new AtomicInteger(0);
        AtomicInteger err_count = new AtomicInteger(0);


        // -------------------------------------------------------------------------------------------------------------


        // 全量股票（全A+ETF）   codeIdMap
        Map<String, Long> codeIdMap = baseStockService.codeIdMap(StockTypeEnum.A_STOCK.type);
        codeIdMap.putAll(baseStockService.codeIdMap(StockTypeEnum.ETF.type));


        // 拉取全部   全A+ETF - K线数据（实时行情）
        List<StockSnapshotKlineDTO> stockSnapshotKlineDTOS = TdxTqKlineAPI.pullAllStockETFSnapshotKline();
        Map<String, StockSnapshotKlineDTO> stockCode_snapshotKline__Map = stockSnapshotKlineDTOS.stream()
                                                                                                .collect(Collectors.toMap(StockSnapshotKlineDTO::getStockCode, e -> e));


        // -------------------------------------------------------------------------------------------------------------


        codeIdMap.keySet().parallelStream().forEach(stockCode -> {
            Long stockId = codeIdMap.get(stockCode);
            int countVal = count.incrementAndGet();


            StockSnapshotKlineDTO snapshot__klineDTO = stockCode_snapshotKline__Map.get(stockCode);
            if (snapshot__klineDTO == null) {
                return;
            }


            try {
                fillStockKline__lastDay(stockCode, stockId, snapshot__klineDTO.toKlineDTO());


                // ------------------------------------------- 计时（频率）   29ms/次   x 5500     ->     总耗时：161s


                long time = System.currentTimeMillis() - start[0];


                long r1 = time / countVal;
                long r2 = countVal * 1000 / time;
                String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


                // stockCode : 300154, stockId : 1630 , count : 5424 , r1 : 29ms/次 , r2 : 33次/s , r3 : 5424次 - 161s
                log.info("fillStockKlineAll__lastDay - suc     >>>     stockCode : {}, stockId : {} , count : {} , suc : {} , err : {} , r1 : {}ms/次 , r2 : {}次/s , r3 : {}",
                         stockCode, stockId, countVal, suc_count.incrementAndGet(), err_count.get(), r1, r2, r3);


            } catch (Exception ex) {

                // 不要影响 其他正常个股
                log.error("fillStockKlineAll__lastDay - err     >>>     stockCode : {}, stockId : {} , count : {} , suc : {} , err : {}",
                          stockCode, stockId, countVal, suc_count.get(), err_count.incrementAndGet(), ex);
            }
        });


        // -------------------------------------------------------------------------------------------------------------


        initDataService.deleteDiskCache();
    }


    @Override
    public void fillOptionKline(String optionCode) {
        fillOptionKline(optionCode, null);
        log.info("fillOptionKline suc     >>>     optionCode : {}", optionCode);
    }


    /**
     * 期权行情 拉取 -> DB
     *
     * @param optionCode
     * @param optionId
     */
    private void fillOptionKline(String optionCode, Long optionId) {


//        // --------------------- ID
//
//
//        optionId = optionId == null ? baseOptionService.getIdByCode(optionCode) : optionId;
//        Assert.notNull(optionId, "期权信息不存在：" + optionCode);


        // ---------------------  拉取数据     ->     通达信-本地读取   /   通达信 TQ


        // 通达信   本地读取
        List<String> klines = klinesFromTdx(optionCode, UpdateTypeEnum.HIS);


        // -------------------------------------------------------------------------------------------------------------


        // --------------------- entity -> 实时行情

        if (CollectionUtils.isEmpty(klines)) {
            return;
        }


        // 实时行情
        BaseOptionDO entity = convertOptionDO(optionId, optionCode, null, klines);


        // --------------------- DB


        if (optionId != null) {
            baseOptionService.updateById(entity);
        } else {
            optionId = baseOptionService.getIdByCode(optionCode);
            entity.setId(optionId);

            baseOptionService.saveOrUpdate(entity);
        }
    }


    @TotalTime
    @Override
    public void fillOptionKlineAll() {


        long[] start = {System.currentTimeMillis()};

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger suc_count = new AtomicInteger(0);
        AtomicInteger err_count = new AtomicInteger(0);


        // -------------------------------------------------------------------------------------------------------------


        // del ALL   ->   kline_ext_data
        baseOptionService.delAllKlineExtData();


        // -------------------------------------------------------------------------------------------------------------


        // insert ALL   ->   kline_ext_data


        Map<String, Long> codeIdMap = baseOptionService.codeIdMap();


        codeIdMap.keySet().parallelStream().forEach((optionCode) -> {
            Long optionId = codeIdMap.get(optionCode);
            int countVal = count.incrementAndGet();


            try {
                fillOptionKline(optionCode, optionId);


                // ------------------------------------------- 计时（频率）   12ms/次   x 1386     ->     总耗时：17.1s


                long time = System.currentTimeMillis() - start[0];


                long r1 = time / countVal;
                long r2 = countVal * 1000L / time;
                String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


                // optionCode : 90007890, optionId : 894 , count : 1386 , suc : 1386 , err : 0 , r1 : 12ms/次 , r2 : 80次/s , r3 : 1386次 - 17.1s
                log.info("fillOptionKlineAll - suc     >>>     optionCode : {}, optionId : {} , count : {} , suc : {} , err : {} , r1 : {}ms/次 , r2 : {}次/s , r3 : {}",
                         optionCode, optionId, countVal, suc_count.incrementAndGet(), err_count.get(), r1, r2, r3);


            } catch (Exception ex) {
                log.error("fillOptionKlineAll - err     >>>     optionCode : {}, optionId : {} , count : {} , suc : {} , err : {}",
                          optionCode, optionId, countVal, suc_count.get(), err_count.incrementAndGet(), ex);
            }
        });


        initDataService.deleteDiskCache();
    }


    @DistributedLock(value = 60, autoRenew = true, renewInterval = 10, keyPrefix = "importAll_check")
    @Override
    public Map<String, List<String>> importAll_check() {


        // 个股   ->   kline_extData 数据check
        Map<String, List<String>> stock___code_errInfoList__Map = check__Kline_ExtData___Stock();
        // 板块   ->   kline_extData 数据check
        Map<String, List<String>> block___code_errInfoList__Map = check__Kline_ExtData___Block();


        // 合并结果
        stock___code_errInfoList__Map.putAll(block___code_errInfoList__Map);
        return stock___code_errInfoList__Map;
    }


    /**
     * 个股行情 拉取 -> DB
     *
     * @param stockCode
     * @param stockId
     * @param snapshot__klineDTO
     */
    private void fillStockKline__lastDay(String stockCode, Long stockId, KlineDTO snapshot__klineDTO) {


        // 板块 - K线数据
        BaseStockDO db_entity = baseStockService.getByCodeAndUpdateType(stockCode, UpdateTypeEnum.N_DAY.type);
        List<KlineDTO> nd_klineDTOList = db_entity.getKlineDTOList();


        Assert.notEmpty(nd_klineDTOList, String.format("[%s] 的 nd_kline 数据为空", stockCode));


        // ----------------------------------------------------------


        KlineDTO last__nd_klineDTO = nd_klineDTOList.getLast();


        // 更新 K线数据
        if (snapshot__klineDTO.getDate().isAfter(last__nd_klineDTO.getDate())) {
            // 最新日期 不存在  ->  添加
            nd_klineDTOList.add(snapshot__klineDTO);
        } else if (snapshot__klineDTO.getDate().equals(last__nd_klineDTO.getDate())) {
            // 最新日期 已存在  ->  替换
            nd_klineDTOList.set(nd_klineDTOList.size() - 1, snapshot__klineDTO);
        }


        // ----------------------------------------------------------


        BaseStockDO entity = new BaseStockDO();
        entity.setCode(stockCode);


        // kline
        entity.setKlineNdStr(JSON.toJSONString(ConvertStockKline.dtoList2StrList(nd_klineDTOList)));


        // 昨日
        if (nd_klineDTOList.size() >= 2) {
            KlineDTO prev = nd_klineDTOList.get(nd_klineDTOList.size() - 2);
            entity.setPrevClose(NumUtil.double2Decimal(prev.getClose()));
        }


        // 板块 - 实时行情
        entity.setTradeDate(snapshot__klineDTO.getDate());
        entity.setOpen(NumUtil.double2Decimal(snapshot__klineDTO.getOpen()));
        entity.setHigh(NumUtil.double2Decimal(snapshot__klineDTO.getHigh()));
        entity.setLow(NumUtil.double2Decimal(snapshot__klineDTO.getLow()));
        entity.setClose(NumUtil.double2Decimal(snapshot__klineDTO.getClose()));
        entity.setVolume(snapshot__klineDTO.getVol());
        entity.setAmount(NumUtil.double2Decimal(snapshot__klineDTO.getAmo()));
        entity.setRangePct(NumUtil.double2Decimal(snapshot__klineDTO.getRangePct()));
        entity.setChangePct(NumUtil.double2Decimal(snapshot__klineDTO.getChangePct()));
        entity.setChangePrice(NumUtil.double2Decimal(snapshot__klineDTO.getChangePrice()));
        entity.setTurnoverPct(NumUtil.double2Decimal(snapshot__klineDTO.getTurnoverPct()));


        if (stockId != null) {
            entity.setId(stockId);
            baseStockService.updateById(entity);
        } else {
            stockId = baseStockService.getIdByCode(stockCode);
            entity.setId(stockId);

            baseStockService.saveOrUpdate(entity);
        }
    }


    /**
     * kline 更新
     *
     * @param codeIdMap
     * @param updateTypeEnum 历史更新 / 近N日更新
     */
    private void update__fillStockKlineAll(Map<String, Long> codeIdMap, UpdateTypeEnum updateTypeEnum) {
        long[] start = {System.currentTimeMillis()};

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger suc_count = new AtomicInteger(0);
        AtomicInteger err_count = new AtomicInteger(0);


        // -------------------------------------------------------------------------------------------------------------


        // del ALL   ->   kline_ext_data
        if (updateTypeEnum == UpdateTypeEnum.HIS) {
            baseStockService.delAllKlineExtData();
        }


        // -------------------------------------------------------------------------------------------------------------


        // insert ALL   ->   kline_ext_data


        codeIdMap.keySet().parallelStream().forEach(stockCode -> {
            Long stockId = codeIdMap.get(stockCode);
            int countVal = count.incrementAndGet();


            try {
                fillStockKline(stockCode, stockId, updateTypeEnum);


                // ------------------------------------------- 计时（频率）   65ms/次   x 7171     ->     总耗时：7min 52s


                long time = System.currentTimeMillis() - start[0];


                long r1 = time / countVal;
                long r2 = countVal * 1000 / time;
                String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


                // stockCode : 300033, stockId : 1523 , count : 7171 , suc : 7144 , err : 27 , r1 : 65ms/次 , r2 : 15次/s , r3 : 7171次 - 7min 52s
                log.info("update__fillStockKlineAll - suc     >>>     stockCode : {}, stockId : {} , count : {} , suc : {} , err : {} , r1 : {}ms/次 , r2 : {}次/s , r3 : {}",
                         stockCode, stockId, countVal, suc_count.incrementAndGet(), err_count.get(), r1, r2, r3);


            } catch (Exception ex) {

                // 不要影响 其他正常个股
                log.error("update__fillStockKlineAll - err     >>>     stockCode : {}, stockId : {} , count : {} , suc : {} , err : {}",
                          stockCode, stockId, countVal, suc_count.get(), err_count.incrementAndGet(), ex);
            }
        });
    }


    private BaseBlockDO convertBlockDO(Long blockId,
                                       String blockName,
                                       List<String> klines,
                                       UpdateTypeEnum updateTypeEnum) {


        BaseBlockDO entity = new BaseBlockDO();
        entity.setId(blockId);
        entity.setName(blockName);


        // 实时行情   -   last kline
        KlineDTO lastKlineDTO = ConvertStockKline.kline2DTO(ListUtil.last(klines));


        // --------------------------------------------------------------------------------


        // K线数据
        entity.setKlineStr(klines, updateTypeEnum);


        // --------------------------------------------------------------------------------


        // 昨日
        if (klines.size() >= 2) {
            KlineDTO prevKlineDTO = ConvertStockKline.kline2DTO(klines.get(klines.size() - 2));
            entity.setPrevClose(of(prevKlineDTO.getClose()));
        }


        // 实时行情   -   last kline
        entity.setTradeDate(lastKlineDTO.getDate());

        entity.setOpen(of(lastKlineDTO.getOpen()));
        entity.setHigh(of(lastKlineDTO.getHigh()));
        entity.setLow(of(lastKlineDTO.getLow()));
        entity.setClose(of(lastKlineDTO.getClose()));

        entity.setVolume(lastKlineDTO.getVol());
        entity.setAmount(of(lastKlineDTO.getAmo()));

        entity.setRangePct(of(lastKlineDTO.getRangePct()));
        entity.setChangePct(of(lastKlineDTO.getChangePct()));
        entity.setChangePrice(of(lastKlineDTO.getChangePrice()));
        entity.setTurnoverPct(of(lastKlineDTO.getTurnoverPct()));


        return entity;
    }


    private BaseOptionDO convertOptionDO(Long optionId,
                                         String optionCode,
                                         String optionName,
                                         List<String> klines) {


        BaseOptionDO entity = new BaseOptionDO();
        entity.setId(optionId);
        entity.setCode(optionCode);
        entity.setName(optionName);
        entity.setTdxMarketType(StockMarketEnum.getTdxMarketType(optionCode));


        // --------------------------------------------------------------------------------


        // K线数据
        entity.setKlineHisStr(JSON.toJSONString(klines));


        // --------------------------------------------------------------------------------


        // 昨日
        if (klines.size() >= 2) {
            KlineDTO prevKlineDTO = ConvertStockKline.kline2DTO(klines.get(klines.size() - 2));
            entity.setPrevClose(of(prevKlineDTO.getClose()));
        }


        // --------------------------------------------------------------------------------


        // 实时行情   -   last kline
        KlineDTO lastKlineDTO = ConvertStockKline.kline2DTO(ListUtil.last(klines));


        // --------------------------------------------------------------------------------


        // 期权 合约信息
        GetStockInfoDTO stockInfo = TdxTqAPI.get_stock_info(optionCode);


        if (stockInfo != null) {


            // 科创50购12月1900
            String name = stockInfo.getName();


            String[] name_arr = name.split("购");
            String callPutType = "C";
            String underlyingName = name_arr[0];

            if (name_arr.length != 2) {

                name_arr = name.split("沽");
                callPutType = "P";
                underlyingName = name_arr[0];
            }


            entity.setSymbol(null);
            entity.setName(name);
            entity.setCallPutType(callPutType);
            entity.setStrikePrice(stockInfo.getJ_mgjzc2());
            entity.setMultiplier(stockInfo.getQHVolBaseRate());
            entity.setUnderlyingType(1);
            entity.setUnderlyingCode(stockInfo.getUnderly_code());
            entity.setUnderlyingName(underlyingName);
            entity.setStartDate(DateTimeUtil.parseDate_yyyyMMdd(stockInfo.getJ_start()));
            // ⚠️ 不要用「最后一根K线日期」覆写 end_date（合约到期日）：
            // 否则 存续合约 的 end_date 会变成「数据更新日」，导致 `date <= end_date` 的存续判定失效
            // （base_option.end_date 语义 = 合约到期日，由 importOption / 历史合约导入 维护）
            // entity.setEndDate(lastKlineDTO.getDate());
        }


        // --------------------------------------------------------------------------------


        // 实时行情   -   last kline
        entity.setTradeDate(lastKlineDTO.getDate());

        entity.setOpen(of(lastKlineDTO.getOpen()));
        entity.setHigh(of(lastKlineDTO.getHigh()));
        entity.setLow(of(lastKlineDTO.getLow()));
        entity.setClose(of(lastKlineDTO.getClose()));

        entity.setVolume(lastKlineDTO.getVol());
        entity.setAmount(of(lastKlineDTO.getAmo()));

        entity.setRangePct(of(lastKlineDTO.getRangePct()));
        entity.setChangePct(of(lastKlineDTO.getChangePct()));
        entity.setChangePrice(of(lastKlineDTO.getChangePrice()));
        entity.setTurnoverPct(of(lastKlineDTO.getTurnoverPct()));


        return entity;
    }

    private BaseStockDO convertStockDO(Long stockId,
                                       String stockName,
                                       List<String> klines,
                                       UpdateTypeEnum updateTypeEnum) {


        BaseStockDO entity = new BaseStockDO();
        entity.setId(stockId);
        entity.setName(stockName);


        // 实时行情   -   last kline
        KlineDTO lastKlineDTO = ConvertStockKline.kline2DTO(ListUtil.last(klines));


        // --------------------------------------------------------------------------------


        // K线数据
        entity.setKlineStr(klines, updateTypeEnum);


        // --------------------------------------------------------------------------------


        // 昨日
        if (klines.size() >= 2) {
            KlineDTO prevKlineDTO = ConvertStockKline.kline2DTO(klines.get(klines.size() - 2));
            entity.setPrevClose(of(prevKlineDTO.getClose()));
        }


        // 实时行情   -   last kline
        entity.setTradeDate(lastKlineDTO.getDate());

        entity.setOpen(of(lastKlineDTO.getOpen()));
        entity.setHigh(of(lastKlineDTO.getHigh()));
        entity.setLow(of(lastKlineDTO.getLow()));
        entity.setClose(of(lastKlineDTO.getClose()));

        entity.setVolume(lastKlineDTO.getVol());
        entity.setAmount(of(lastKlineDTO.getAmo()));

        entity.setRangePct(of(lastKlineDTO.getRangePct()));
        entity.setChangePct(of(lastKlineDTO.getChangePct()));
        entity.setChangePrice(of(lastKlineDTO.getChangePrice()));
        entity.setTurnoverPct(of(lastKlineDTO.getTurnoverPct()));


        return entity;
    }


    /**
     * 个股行情 拉取 -> DB
     *
     * @param stockCode
     * @param stockId
     * @param updateTypeEnum
     */
    private void fillStockKline(String stockCode, Long stockId, UpdateTypeEnum updateTypeEnum) {


        // --------------------- ID


        stockId = stockId == null ? baseStockService.getIdByCode(stockCode) : stockId;
        Assert.notNull(stockId, "个股信息不存在：" + stockCode);


        // ---------------------  拉取数据     ->     通达信-本地读取   /   通达信 TQ


        // 通达信   本地读取
        List<String> klines = klinesFromTdx(stockCode, updateTypeEnum);


        // -------------------------------------------------------------------------------------------------------------


        // --------------------- entity -> 实时行情

        if (CollectionUtils.isEmpty(klines)) {
            return;
        }


        // 实时行情
        BaseStockDO entity = convertStockDO(stockId, null, klines, updateTypeEnum);


        // --------------------- DB
        baseStockService.updateById(entity);
    }


    private List<String> klinesFromTdx(String stockCode, UpdateTypeEnum updateTypeEnum) {

        // 个股 - K线数据
        List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parseByStockCode(stockCode, updateTypeEnum.days);


        List<String> klines = Lists.newArrayList();
        ldayDTOList.forEach(e -> {


            // 2025-05-13,21.06,21.97,20.89,21.45,8455131,18181107751.03,5.18,2.98,0.62,6.33
            // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率,流通股本,总股本

            // K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率,流通股本,总股本]）

            List<Object> kline = Lists.newArrayList(String.valueOf(e.getTradeDate()), e.getOpen(), e.getHigh(), e.getLow(), e.getClose(), e.getVol(), e.getAmount(),
                                                    e.getRangePct(), e.getChangePct(), e.getChangePrice(), e.getTurnoverPct(), e.getLtgb(), e.getZgb());


            String klineStr = kline.stream().map(obj -> obj != null ? obj.toString() : "").collect(Collectors.joining(","));
            klines.add(klineStr);
        });


        return klines;
    }


    private BigDecimal of(Double val) {
        return NumUtil.double2Decimal(val);
    }


    // -----------------------------------------------------------------------------------------------------------------


    private Map<String, List<String>> check__Kline_ExtData___Stock() {


        Map<String, List<String>> code_errInfoList__Map = Maps.newConcurrentMap();


        // 全量股票   kline_extData 数据
        List<BaseStockDO> baseStockDOList = baseStockService.listAllKline(UpdateTypeEnum.ALL_RANGE, null, true);


        // ---------------------------------------------------------


        baseStockDOList.parallelStream()
                       .forEach(entity -> {


                           String stockCode = entity.getCode();


                           String klineNdStr = entity.getKlineNdStr();
                           String klineHisStr = entity.getKlineHisStr();

                           String extDataNdStr = entity.getExtDataNdStr();
                           String extDataHisStr = entity.getExtDataHisStr();


                           // -------------------------


                           List<String> nd_klines = JSON.parseArray(klineNdStr, String.class);
                           List<String> his_klines = JSON.parseArray(klineHisStr, String.class);

                           List<String> nd_extDatas = JSON.parseArray(extDataNdStr, String.class);
                           List<String> his_extDatas = JSON.parseArray(extDataHisStr, String.class);


                           // ------------------------------------------------------------------------------------------


                           // kline size  =  ext_data size


                           if (ListUtil.size(nd_klines) != ListUtil.size(nd_extDatas)) {
                               String errInfo = String.format("nd size - err     >>>     stockCode : %s , kline size : %s , extData size : %s",
                                                              stockCode, ListUtil.size(nd_klines), ListUtil.size(nd_extDatas));

                               log.error(errInfo);
                               errInfoList(code_errInfoList__Map, stockCode, errInfo);
                           }


                           if (ListUtil.size(his_klines) != ListUtil.size(his_extDatas)) {
                               String errInfo = String.format("his size  -  err     >>>     stockCode : %s , kline size : %s , extData size : %s",
                                                              stockCode, ListUtil.size(his_klines), ListUtil.size(his_extDatas));

                               log.error(errInfo);
                               errInfoList(code_errInfoList__Map, stockCode, errInfo);
                           }


                           // ------------------------------------------------------------------------------------------


                           // KlineDTO field size  =  kline length


                           int KlineDTO_field__length = KlineDTO.class.getDeclaredFields().length;


                           if (CollectionUtils.isNotEmpty(nd_klines)) {
                               for (String kline : nd_klines) {

                                   String[] kline_arr = kline.split(",", -1);
                                   if (kline_arr.length != KlineDTO_field__length) {

                                       String errInfo = String.format("kline_nd - err     >>>     stockCode : %s , KlineDTO 字段 length : %s , kline_arr length : %s",
                                                                      stockCode, KlineDTO_field__length, kline_arr.length);

                                       log.error(errInfo);
                                       errInfoList(code_errInfoList__Map, stockCode, errInfo);
                                   }
                               }
                           }


                           if (CollectionUtils.isNotEmpty(his_klines)) {
                               for (String kline : his_klines) {

                                   String[] kline_arr = kline.split(",", -1);
                                   if (kline_arr.length != KlineDTO_field__length) {

                                       String errInfo = String.format("kline_his - err     >>>     stockCode : %s , KlineDTO 字段 length : %s , kline_arr length : %s",
                                                                      stockCode, KlineDTO_field__length, kline_arr.length);

                                       log.error(errInfo);
                                       errInfoList(code_errInfoList__Map, stockCode, errInfo);
                                   }
                               }
                           }


                           // ------------------------------------------------------------------------------------------


                           // ExtDataDTO field size  =  ext_data length


                           int extDataDTO_field__length = ExtDataDTO.class.getDeclaredFields().length;


                           if (CollectionUtils.isNotEmpty(nd_extDatas)) {
                               for (String extData_str : nd_extDatas) {

                                   String[] extData_arr = extData_str.split(",", -1);
                                   if (extData_arr.length != extDataDTO_field__length) {

                                       String errInfo = String.format("ext_data_nd - err     >>>     stockCode : %s , ExtDataDTO 字段 length : %s , extData_arr length : %s",
                                                                      stockCode, extDataDTO_field__length, extData_arr.length);

                                       log.error(errInfo);
                                       errInfoList(code_errInfoList__Map, stockCode, errInfo);
                                   }
                               }
                           }


                           if (CollectionUtils.isNotEmpty(his_extDatas)) {
                               for (String extData_str : his_extDatas) {

                                   String[] extData_arr = extData_str.split(",", -1);
                                   if (extData_arr.length != extDataDTO_field__length) {

                                       String errInfo = String.format("ext_data_his - err     >>>     stockCode : %s , ExtDataDTO 字段 length : %s , extData_arr length : %s",
                                                                      stockCode, extDataDTO_field__length, extData_arr.length);

                                       log.error(errInfo);
                                       errInfoList(code_errInfoList__Map, stockCode, errInfo);
                                   }
                               }
                           }
                       });


        return code_errInfoList__Map;
    }


    private Map<String, List<String>> check__Kline_ExtData___Block() {


        Map<String, List<String>> code_errInfoList__Map = Maps.newConcurrentMap();


        // 全量板块   kline_extData 数据
        List<BaseBlockDO> baseBlockDOList = baseBlockService.listAllKline(true);


        // ---------------------------------------------------------


        baseBlockDOList.parallelStream()
                       .forEach(entity -> {


                           String blockCode = entity.getCode();


                           String klineNdStr = entity.getKlineNdStr();
                           String klineHisStr = entity.getKlineHisStr();

                           String extDataNdStr = entity.getExtDataNdStr();
                           String extDataHisStr = entity.getExtDataHisStr();


                           // -------------------------


                           List<String> nd_klines = JSON.parseArray(klineNdStr, String.class);
                           List<String> his_klines = JSON.parseArray(klineHisStr, String.class);

                           List<String> nd_extDatas = JSON.parseArray(extDataNdStr, String.class);
                           List<String> his_extDatas = JSON.parseArray(extDataHisStr, String.class);


                           // ------------------------------------------------------------------------------------------


                           // kline size  =  ext_data size


                           boolean isRpsBlock = BlockTypeEnum.isRpsBlock(entity.getType());


                           if (isRpsBlock && ListUtil.size(nd_klines) != ListUtil.size(nd_extDatas)) {
                               String errInfo = String.format("nd size - err     >>>     blockCode : %s , kline size : %s , extData size : %s",
                                                              blockCode, ListUtil.size(nd_klines), ListUtil.size(nd_extDatas));

                               log.error(errInfo);
                               errInfoList(code_errInfoList__Map, blockCode, errInfo);
                           }


                           if (isRpsBlock && ListUtil.size(his_klines) != ListUtil.size(his_extDatas)) {
                               String errInfo = String.format("his size  -  err     >>>     blockCode : %s , kline size : %s , extData size : %s",
                                                              blockCode, ListUtil.size(his_klines), ListUtil.size(his_extDatas));

                               log.error(errInfo);
                               errInfoList(code_errInfoList__Map, blockCode, errInfo);
                           }


                           // ------------------------------------------------------------------------------------------


                           // KlineDTO field size  =  kline length


                           int KlineDTO_field__length = KlineDTO.class.getDeclaredFields().length;


                           if (CollectionUtils.isNotEmpty(nd_klines)) {
                               for (String kline : nd_klines) {

                                   String[] kline_arr = kline.split(",", -1);
                                   if (kline_arr.length != KlineDTO_field__length) {

                                       String errInfo = String.format("kline_nd - err     >>>     blockCode : %s , KlineDTO 字段 length : %s , kline_arr length : %s",
                                                                      blockCode, KlineDTO_field__length, kline_arr.length);

                                       log.error(errInfo);
                                       errInfoList(code_errInfoList__Map, blockCode, errInfo);
                                   }
                               }
                           }


                           if (CollectionUtils.isNotEmpty(his_klines)) {
                               for (String kline : his_klines) {

                                   String[] kline_arr = kline.split(",", -1);
                                   if (kline_arr.length != KlineDTO_field__length) {

                                       String errInfo = String.format("kline_his - err     >>>     blockCode : %s , KlineDTO 字段 length : %s , kline_arr length : %s",
                                                                      blockCode, KlineDTO_field__length, kline_arr.length);

                                       log.error(errInfo);
                                       errInfoList(code_errInfoList__Map, blockCode, errInfo);
                                   }
                               }
                           }


                           // ------------------------------------------------------------------------------------------


                           // ExtDataDTO field size  =  ext_data length


                           int extDataDTO_field__length = ExtDataDTO.class.getDeclaredFields().length;


                           if (CollectionUtils.isNotEmpty(nd_extDatas)) {
                               for (String extData_str : nd_extDatas) {

                                   String[] extData_arr = extData_str.split(",", -1);
                                   if (extData_arr.length != extDataDTO_field__length) {

                                       String errInfo = String.format("ext_data_nd - err     >>>     blockCode : %s , ExtDataDTO 字段 length : %s , extData_arr length : %s",
                                                                      blockCode, extDataDTO_field__length, extData_arr.length);

                                       log.error(errInfo);
                                       errInfoList(code_errInfoList__Map, blockCode, errInfo);
                                   }
                               }
                           }


                           if (CollectionUtils.isNotEmpty(his_extDatas)) {
                               for (String extData_str : his_extDatas) {

                                   String[] extData_arr = extData_str.split(",", -1);
                                   if (extData_arr.length != extDataDTO_field__length) {

                                       String errInfo = String.format("ext_data_his - err     >>>     blockCode : %s , ExtDataDTO 字段 length : %s , extData_arr length : %s",
                                                                      blockCode, extDataDTO_field__length, extData_arr.length);

                                       log.error(errInfo);
                                       errInfoList(code_errInfoList__Map, blockCode, errInfo);
                                   }
                               }
                           }
                       });


        return code_errInfoList__Map;
    }


    private void errInfoList(Map<String, List<String>> code_errInfoList__Map, String code, String errInfo) {
        code_errInfoList__Map.computeIfAbsent(code, k -> Lists.newArrayList()).add(errInfo);
    }


}