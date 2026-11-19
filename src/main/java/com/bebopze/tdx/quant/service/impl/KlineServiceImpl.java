package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.TdxTqKlineAPI;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.parser.tdxdata.LdayParser;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.KlineService;
import com.google.common.collect.Lists;
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


        task1.get();
        task2.get();
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
    }

    public void fillBlockKline(String blockCode, Long blockId, UpdateTypeEnum updateTypeEnum) {

        BaseBlockDO baseBlockDO = new BaseBlockDO();
        baseBlockDO.setCode(blockCode);


        // 板块 - K线数据
        List<LdayParser.LdayDTO> ldayDTOList = LdayParser.parseByStockCode(blockCode, updateTypeEnum.days);


        List<String> klines = Lists.newArrayList();

        ldayDTOList.forEach(x -> {


            // K线数据-JSON（日期：[O,H,L,C,VOL,AMO,涨幅,振幅,换手率]）
            // List<Number> kline = Lists.newArrayList(x.getOpen(), x.getHigh(), x.getLow(), x.getClose(), x.getVol(), x.getAmount(), x.getChangePct(), null, null);


            // 2025-05-13,21.06,21.97,20.89,21.45,8455131,18181107751.03,5.18,2.98,0.62,6.33
            // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率,流通股本,总股本

            // K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率,流通股本,总股本]）
            List<Object> kline = Lists.newArrayList(String.valueOf(x.getTradeDate()), x.getOpen(), x.getHigh(), x.getLow(), x.getClose(), x.getVol(), x.getAmount(),
                                                    x.getRangePct(), x.getChangePct(), x.getChangePrice(), null, x.getLtgb(), x.getZgb());


            String klineStr = kline.stream().map(obj -> obj != null ? obj.toString() : "").collect(Collectors.joining(","));
            klines.add(klineStr);
        });


        // kline
        baseBlockDO.setKlineStr(klines, updateTypeEnum);


        if (CollectionUtils.isEmpty(ldayDTOList)) {
            return;
        }


        // 昨日
        if (ldayDTOList.size() >= 2) {
            LdayParser.LdayDTO prev = ldayDTOList.get(ldayDTOList.size() - 2);
            baseBlockDO.setPrevClose(prev.getClose());
        }


        // 板块 - 实时行情
        LdayParser.LdayDTO last = ldayDTOList.get(ldayDTOList.size() - 1);

        baseBlockDO.setTradeDate(last.getTradeDate());
        baseBlockDO.setOpen(last.getOpen());
        baseBlockDO.setHigh(last.getHigh());
        baseBlockDO.setLow(last.getLow());
        baseBlockDO.setClose(last.getClose());
        baseBlockDO.setVolume(last.getVol());
        baseBlockDO.setAmount(last.getAmount());
        baseBlockDO.setRangePct(last.getRangePct());
        baseBlockDO.setChangePct(last.getChangePct());
        baseBlockDO.setChangePrice(last.getChangePrice());
        baseBlockDO.setTurnoverPct(last.getTurnoverPct());


        if (blockId != null) {
            baseBlockDO.setId(blockId);
            baseBlockService.updateById(baseBlockDO);
        } else {
            blockId = baseBlockService.getIdByCode(blockCode);
            baseBlockDO.setId(blockId);

            baseBlockService.saveOrUpdate(baseBlockDO);
        }


        log.info("fillBlockKline suc     >>>     blockCode : {}", blockCode);
    }


    @TotalTime
    @Override
    public void fillBlockKlineAll(UpdateTypeEnum updateTypeEnum) {
        long[] start = {System.currentTimeMillis()};

        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger suc_count = new AtomicInteger(0);
        AtomicInteger err_count = new AtomicInteger(0);


        // -------------------------------------------------------------------------------------------------------------


        Map<String, Long> codeIdMap = baseBlockService.codeIdMap();


        codeIdMap.keySet().parallelStream().forEach((blockCode) -> {
            Long blockId = codeIdMap.get(blockCode);
            int countVal = count.incrementAndGet();


            try {
                fillBlockKline(blockCode, blockId, updateTypeEnum);


                // ------------------------------------------- 计时（频率）   26ms/次   x 881     ->     总耗时：23s


                long time = System.currentTimeMillis() - start[0];


                long r1 = time / countVal;
                long r2 = countVal * 1000L / time;
                String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


                // blockCode : 880444, blockId : 94 , count : 881 , r1 : 26ms/次 , r2 : 37次/s , r3 : 881次 - 23s
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


    public void fillBlockKline__lastDay(String blockCode, Long blockId, KlineDTO snapshot__klineDTO) {


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


        codeIdMap.keySet().parallelStream().forEach(stockCode -> {
            Long stockId = codeIdMap.get(stockCode);
            int countVal = count.incrementAndGet();


            try {
                fillStockKline(stockCode, stockId, updateTypeEnum);


                // ------------------------------------------- 计时（频率）   29ms/次   x 5500     ->     总耗时：161s


                long time = System.currentTimeMillis() - start[0];


                long r1 = time / countVal;
                long r2 = countVal * 1000 / time;
                String r3 = String.format("%s次 - %s", countVal, DateTimeUtil.format2Hms(time));


                // stockCode : 300154, stockId : 1630 , count : 5424 , r1 : 29ms/次 , r2 : 33次/s , r3 : 5424次 - 161s
                log.info("update__fillStockKlineAll - suc     >>>     stockCode : {}, stockId : {} , count : {} , suc : {} , err : {} , r1 : {}ms/次 , r2 : {}次/s , r3 : {}",
                         stockCode, stockId, countVal, suc_count.incrementAndGet(), err_count.get(), r1, r2, r3);


            } catch (Exception ex) {

                // 不要影响 其他正常个股
                log.error("update__fillStockKlineAll - err     >>>     stockCode : {}, stockId : {} , count : {} , suc : {} , err : {}",
                          stockCode, stockId, countVal, suc_count.get(), err_count.incrementAndGet(), ex);
            }
        });
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


        // ---------------------  拉取数据     ->     通达信-本地读取   /   东方财富 API


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


}