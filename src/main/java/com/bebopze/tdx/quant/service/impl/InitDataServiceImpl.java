package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockRelaStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.INDEX_BLOCK;


/**
 * 全量行情       DB -> cache
 *
 * @author: bebopze
 * @date: 2025/7/11
 */
@Slf4j
@Service
public class InitDataServiceImpl implements InitDataService {


    boolean init = false;

    public static final BacktestCache data = new BacktestCache();


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public BacktestCache initData() {
        return initData(null, null, false);
    }

    @Synchronized
    @Override
    public BacktestCache initData(LocalDate startDate, LocalDate endDate, boolean refresh) {


        if (init && !refresh) {
            return data;
        }


        // 加载   全量行情数据 - 个股
        loadAllStockKline(startDate, endDate);


        // 加载   全量行情数据 - 板块
        loadAllBlockKline();


        // 板块-个股  /  个股-板块
        loadAllBlockRelaStock();


        init = true;


        // 异步  ->  write2Disk
        // asyncWrite2Disk();


        return data;
    }


    /**
     * 从本地DB   加载   全部个股（5000+）
     *
     * @return
     */
    private void loadAllStockKline(LocalDate startDate, LocalDate endDate) {


        // null -> 全量行情（近10年）
        startDate = startDate == null ? LocalDate.now().minusYears(10) : startDate;
        endDate = endDate == null ? LocalDate.now() : DateTimeUtil.min(endDate, LocalDate.now());

        log.info("loadAllStockKline     >>>     startDate : {}, endDate : {}", startDate, endDate);


        // -----------------------------------------------------------------------------


        // DB 数据加载
        data.stockDOList = baseStockService.listAllKline();
        // 空数据 过滤
        data.stockDOList = data.stockDOList.stream().filter(e -> StringUtils.isNotBlank(e.getName()) && StringUtils.isNotBlank(e.getKlineHis())
                // TODO   基金北向
                && e.getAmount().doubleValue() > 1 * 1_0000_0000).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // 行情起点（往前倒推 250日 -> 1年）
        LocalDate dateLine_start = startDate.minusYears(1);
        LocalDate dateLine_end = endDate;

        log.info("loadAllStockKline - dateLine 截取（内存爆炸）    >>>     startDate : {}, endDate : {}", startDate, endDate);


        // -----------------


        // kline_his   ->   dateLine 截取   （ 内存爆炸 ）
        data.stockDOList.parallelStream().forEach(e -> {


            // klineHis
            List<KlineDTO> klineDTOList = e.getKlineDTOList();
            klineDTOList = klineDTOList.parallelStream()
                                       .filter(k -> !k.getDate().isBefore(dateLine_start) && !k.getDate().isAfter(dateLine_end)
                                               // 过滤  ->  负价格（前复权）
                                               && k.getClose() > 0)
                                       .sorted(Comparator.comparing(KlineDTO::getDate))
                                       .collect(Collectors.toList());


            e.setKlineHis(ConvertStockKline.dtoList2JsonStr(klineDTOList));


            // -----------------------------------------------------------------------------


            // extDataHis -> 必须同步 截取（数据对齐）


            // klineHis   ->   过滤后的 dateSet（   HashSet  ->  set.contains，只要 O(1)   ）
            Set<LocalDate> dateSet = klineDTOList.stream()
                                                 .map(KlineDTO::getDate)
                                                 .collect(Collectors.toSet());


            // 同步对齐 dateSet   ->   扩展数据
            List<ExtDataDTO> extDataDTOList = e.getExtDataDTOList().stream()
                                               .filter(k -> dateSet.contains(k.getDate()))
                                               .sorted(Comparator.comparing(ExtDataDTO::getDate))
                                               .collect(Collectors.toList());

            e.setExtDataHis(ConvertStockExtData.dtoList2JsonStr(extDataDTOList));
        });


        // -------------------------------------------------------------------------------------------------------------


        // 空行情 过滤（时间段内 -> 未上市）
        data.stockDOList = data.stockDOList.stream().filter(e -> !Objects.equals("[]", e.getKlineHis())).collect(Collectors.toList());


        // -----------------------------------------------------------------------------


        data.stockDOList.forEach(e -> {


            String stockCode = e.getCode();
            List<KlineDTO> klineDTOList = e.getKlineDTOList();


            LocalDate[] date_arr = ConvertStockKline.dateFieldValArr(klineDTOList, "date");
            double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");


            // --------------------------------------------------------


            data.codeStockMap.put(stockCode, e);
            data.stock__idCodeMap.put(e.getId(), stockCode);
            data.stock__codeIdMap.put(stockCode, e.getId());
            data.stock__codeNameMap.put(stockCode, StringUtils.defaultString(e.getName()));


            Map<LocalDate, Double> dateCloseMap = Maps.newHashMap();
            for (int i = 0; i < date_arr.length; i++) {
                dateCloseMap.put(date_arr[i], close_arr[i]);
            }
            data.stock__dateCloseMap.put(stockCode, dateCloseMap);
        });
    }


    /**
     * 从本地DB   加载   全部板块（380+）
     *
     * @return
     */
    private void loadAllBlockKline() {


        data.blockDOList = baseBlockService.listAllKline();


        // -------


        data.blockDOList.forEach(e -> {

            String blockCode = e.getCode();
            List<KlineDTO> klineDTOList = e.getKlineDTOList();


            LocalDate[] date_arr = ConvertStockKline.dateFieldValArr(klineDTOList, "date");
            double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");


            // -----------------------------------------------------------


            // 交易日 基准     ->     基准板块（代替 -> 大盘指数）
            if (Objects.equals(blockCode, INDEX_BLOCK)) {
                for (int i = 0; i < date_arr.length; i++) {
                    data.dateIndexMap.put(date_arr[i], i);
                    data.dateList.add(date_arr[i]);
                }
            }


            // -----------------------------------------------------------


            data.codeBlockMap.put(blockCode, e);
            data.block__idCodeMap.put(e.getId(), blockCode);
            data.block__codeIdMap.put(blockCode, e.getId());
            data.block__codeNameMap.put(blockCode, e.getName());


            Map<LocalDate, Double> dateCloseMap = Maps.newHashMap();
            for (int i = 0; i < date_arr.length; i++) {
                dateCloseMap.put(date_arr[i], close_arr[i]);
            }
            data.block__dateCloseMap.put(blockCode, dateCloseMap);
        });
    }


    /**
     * 从本地DB   加载全部   板块-个股
     */
    private void loadAllBlockRelaStock() {


        // 板块-个股   =>   lv3级【end_level=1】   ->     3级-行业（普通/研究） + 概念板块
        List<BaseBlockRelaStockDO> relaList = baseBlockRelaStockService.listAll();


        for (BaseBlockRelaStockDO rela : relaList) {

            Long blockId = rela.getBlockId();
            Long stockId = rela.getStockId();
            String blockCode = data.block__idCodeMap.get(blockId);
            String stockCode = data.stock__idCodeMap.get(stockId);


            // Assert.notNull(blockCode, String.format("blockCode数据异常：rela : {} ,  [blockCode-%s] , [stockCode-%s]", JSON.toJSONString(rela), blockCode, stockCode));
            // Assert.notNull(stockCode, String.format("stockCode数据异常：rela : {} ,  [blockCode-%s] , [stockCode-%s]", JSON.toJSONString(rela), blockCode, stockCode));


            // data.blockId_stockIdList_Map.computeIfAbsent(blockId, k -> Lists.newArrayList()).add(stockId);
            // data.stockId_blockIdList_Map.computeIfAbsent(stockId, k -> Lists.newArrayList()).add(blockId);


//            if ("880340".equals(blockCode)) {
//                log.debug("blockCode : {} , stockCode : {}, count : {}", blockCode, stockCode, count++);
//            }


            if (null == stockCode) {
                // null   =>   基金北向 过滤
                log.debug("loadAllBlockRelaStock - null     >>>     blockCode : [{}-{}] , stockCode : [{}-{}]",
                          blockId, blockCode, stockId, stockCode);

                continue;
            }


            data.blockCode_stockCodeSet_Map.computeIfAbsent(blockCode, k -> Sets.newHashSet()).add(stockCode);
            data.stockCode_blockCodeSet_Map.computeIfAbsent(stockCode, k -> Sets.newHashSet()).add(blockCode);


            // ---------------------------------------------------------- lv1 / lv2   =>   根据 lv3 倒推计算


            // lv3 -> lv2 -> lv1


            BaseBlockDO blockDO = data.codeBlockMap.get(blockCode);


            // 过滤   4-概念板块（废止   概念-行业   关联   ==>   经回测 -> 板块数据 污染严重 -> 收益 严重下降↓）
            if (blockDO.getType() == 4) {
                continue;
            }


            // 880981-880305-880306          TDX 能源-电力-水力发电
            String codePath = blockDO.getCodePath();
            if (StringUtils.isNotBlank(codePath)) {

                String[] codePathArr = codePath.split("-");
                if (codePathArr.length > 1) {

                    for (int i = 0; i < codePathArr.length - 1; i++) {
                        String pCode = codePathArr[i];

                        data.blockCode_stockCodeSet_Map.computeIfAbsent(pCode, k -> Sets.newHashSet()).add(stockCode);
                        data.stockCode_blockCodeSet_Map.computeIfAbsent(stockCode, k -> Sets.newHashSet()).add(pCode);
                    }
                }
            }
        }


        // 数量 不对   =>   基金北向 过滤
        log.debug("loadAllBlockRelaStock - size     >>>     blockCode_stockCodeSet_Map.size : {}", data.blockCode_stockCodeSet_Map.size());
        log.debug("loadAllBlockRelaStock - size     >>>     stockCode_blockCodeSet_Map.size : {}", data.stockCode_blockCodeSet_Map.size());


        log.debug("loadAllBlockRelaStock - map     >>>     blockCode_stockCodeSet_Map.size : {}", JSON.toJSONString(data.blockCode_stockCodeSet_Map));
        log.debug("loadAllBlockRelaStock - map     >>>     stockCode_blockCodeSet_Map.size : {}", JSON.toJSONString(data.stockCode_blockCodeSet_Map));
    }


//    /**
//     * 异步  ->  write2Disk
//     */
//    public void asyncWrite2Disk() {
//        // Executors.newCachedThreadPool().execute(() -> {
//        long start = System.currentTimeMillis();
//
//
//        JsonFileWriterAndReader.writeStringToFile___dataCache(data);
//
//
//        log.info("asyncWrite2Disk - dataCache     >>>     totalTime : {}", DateTimeUtil.format2Hms(System.currentTimeMillis() - start));
//        // });
//    }
//
//    /**
//     * read From Disk
//     *
//     * @return
//     */
//    private BacktestCache readFromDisk() {
//
//        BacktestCache backtestCache = JsonFileWriterAndReader.readStringFromFile___dataCache();
//        BeanUtils.copyProperties(backtestCache, data);
//
//        return backtestCache;
//    }


}
