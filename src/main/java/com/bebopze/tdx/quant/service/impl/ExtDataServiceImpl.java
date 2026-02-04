package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.ThreadPoolType;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.domain.dto.fun.MidAdjustResult;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtDataFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.ParallelCalcUtil;
import com.bebopze.tdx.quant.common.util.StockUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.ExtDataService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.INDEX_BLOCK;


/**
 * 扩展数据 - 计算
 *
 * @author: bebopze
 * @date: 2025/5/24
 */
@Primary
@Slf4j
@Service
public class ExtDataServiceImpl implements ExtDataService {


    private static final int lastN = 251;


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private InitDataService initDataService;


    @TotalTime
    @Override
    public void refreshExtDataAll(Integer N) {
        // 板块
        calcBlockExtData(N);
        // ETF
        calcStockExtData(N, StockTypeEnum.ETF.type);
        // A股
        calcStockExtData(N, StockTypeEnum.A_STOCK.type);
    }


    @TotalTime
    @Override
    public void calcStockExtData(Integer N, Integer stockType) {


        N = StockUtil.N(N);


        // -------------------------------------------------------------------------------------------------------------

        // 预加载 全量行情
        // 从本地DB   加载全部（5000+支） 个股的   收盘价序列/日期序列/ code-id


        // 预加载 -> 解析数据
        DataDTO data = loadAllStockKline(N, stockType);


        // -------------------------------------------------------------------------------------------------------------


        // RPS
        stockTask__RPS(data, N, stockType);

        // 扩展数据
        stockTask__extData(data, N, stockType);


        // -------------------------------------------------------------------------------------------------------------


        // del Cache
        initDataService.deleteDiskCache();
    }


    @TotalTime
    @Override
    public void calcBlockExtData(Integer N) {


        N = StockUtil.N(N);


        // -------------------------------------------------------------------------------------------------------------

        // 从本地DB   加载全部（380+支） 板块的   收盘价序列/日期序列/ code-id


        // 预加载 -> 解析数据
        DataDTO data = loadAllBlockKline(N);


        // -------------------------------------------------------------------------------------------------------------


        // RPS
        blockTask__RPS(data, N);

        // 扩展数据
        blockTask__extData(data, N);


        // -------------------------------------------------------------------------------------------------------------


        // del Cache
        initDataService.deleteDiskCache();
    }


    //


    // -----------------------------------------------------------------------------------------------------------------
    // ---------------------------------------------------- 个股 --------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 从本地DB   加载全部（5000+支）个股的 收盘价序列
     *
     * @param N
     * @param stockType
     * @return stock - close_arr
     */
    private DataDTO loadAllStockKline(Integer N, Integer stockType) {
        DataDTO data = new DataDTO();


        data.stockDOList = baseStockService.listAllKline(stockType);
        data.stockDOList = data.stockDOList.parallelStream().filter(e -> CollectionUtils.isNotEmpty(e.getKlineDTOList())).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // 增量更新     =>     kline_his（计算参数 -> 需截取） / ext_data_his（被计算 -> 无需截取）    =>     截取 最后N条
        if (StockUtil.incrUpdate(N)) {
            data.stockDOList.parallelStream().forEach(e -> {
                // klineHis   ->   最后N条
                List<KlineDTO> klineDTOList = ListUtil.lastN(e.getKlineDTOList(), N + lastN);   // 为了计算 MA250、月多、...（需要至少 250日的K线数据）
                e.setKlineDTOList(klineDTOList);
            });
        }


        // -------------------------------------------------------------------------------------------------------------


        // 空行情 过滤（时间段内 -> 未上市）
        // // data.stockDOList = data.stockDOList.parallelStream().filter(e -> !Objects.equals("[]", e.getKlineHisStr())).collect(Collectors.toList());
        // data.stockDOList = data.stockDOList.parallelStream().filter(e -> CollectionUtils.isNotEmpty(e.getKlineDTOList())).collect(Collectors.toList());


        // -----------------------------------------------------------------------------


        data.stockDOList.parallelStream().forEach(e -> {

            Long id = e.getId();
            String code = e.getCode();


            List<KlineDTO> klineDTOList = e.getKlineDTOList();
            KlineArrDTO klineArrDTO = ConvertStock.kline__dtoList2Arr(klineDTOList);


            LocalDate[] date_arr = klineArrDTO.date;
            double[] close_arr = klineArrDTO.close;


            TreeMap<LocalDate, Double> dateCloseMap = klineArrDTO.getDateCloseMap();


            data.codeDateMap.put(code, date_arr);
            data.codeCloseMap.put(code, close_arr);

            data.codeIdMap.put(code, id);


            data.codePriceMap.put(code, dateCloseMap);


            // ---------------------------------------------------------------------------------------------------------


            // 300059[东方财富] / 600519[贵州茅台] / 601288[农业银行]     ->     总不可能 都同时 停牌！！！
            if ("300059".equals(code) || "600519".equals(code) || "601288".equals(code)
                    // 510300[沪深300ETF] / 159915[创业板ETF] / 588000[科创50ETF]
                    || "510300".equals(code) || "159915".equals(code) || "588000".equals(code)) {


                int len = date_arr.length;

                data.startDate = data.startDate == null ? date_arr[0] : DateTimeUtil.min(data.startDate, date_arr[0]);
                data.endDate = data.endDate == null ? date_arr[len - 1] : DateTimeUtil.max(data.endDate, date_arr[len - 1]);

                if (StockUtil.incrUpdate(N)) {
                    data.startDate = DateTimeUtil.min(data.startDate, date_arr[Math.max(0, len - N)]);
                    data.endDate = DateTimeUtil.max(data.endDate, date_arr[len - 1]);
                }
            }
        });


        // -------------------------------------------------------------------------------------------------------------


        for (int i = 0; i < data.stockDOList.size(); i++) {
            BaseStockDO stockDO = data.stockDOList.get(i);
            data.codeIdxMap.put(stockDO.getCode(), i);
        }


        // -------------------------------------------------------------------------------------------------------------


        return data;
    }


    /**
     * 个股 - RPS计算
     *
     * @param data
     * @param lastDays  仅计算最后 lastDays 天的数据（<=0 时计算全部）
     * @param stockType
     */
    private void stockTask__RPS(DataDTO data, int lastDays, Integer stockType) {
        long start = System.currentTimeMillis();


        // 个股
        int[] RPS_N_arr = new int[]{10, 20, 50, 120, 250};
        // ETF（等同于 板块）
        if (Objects.equals(stockType, StockTypeEnum.ETF.type)) {
            RPS_N_arr = new int[]{5, 10, 15, 20, 50};
        }


        // 计算 -> RPS
        Map<String, double[]> RPS10 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, RPS_N_arr[0], lastDays);
        Map<String, double[]> RPS20 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, RPS_N_arr[1], lastDays);
        Map<String, double[]> RPS50 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, RPS_N_arr[2], lastDays);
        Map<String, double[]> RPS120 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, RPS_N_arr[3], lastDays); // 120 -> 100
        Map<String, double[]> RPS250 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, RPS_N_arr[4], lastDays); // 250 -> 200


        log.info("computeRPS - {}     >>>     totalTime : {}", StockTypeEnum.getDescByType(stockType), DateTimeUtil.formatNow2Hms(start));


//        Map<String, double[]> RPS10_2 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 10);
//        Map<String, double[]> RPS20_2 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 20);
//        Map<String, double[]> RPS50_2 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 50);
//        Map<String, double[]> RPS120_2 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 120); // 120 -> 100
//        Map<String, double[]> RPS250_2 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 250); // 250 -> 200
//
//
//        log.info("computeRPS - 个股2     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start));
//
//
//        Map<String, double[]> RPS10_3 = TdxExtDataFun.computeRPS_2(data.codeDateMap, data.codeCloseMap, 10);
//        Map<String, double[]> RPS20_3 = TdxExtDataFun.computeRPS_2(data.codeDateMap, data.codeCloseMap, 20);
//        Map<String, double[]> RPS50_3 = TdxExtDataFun.computeRPS_2(data.codeDateMap, data.codeCloseMap, 50);
//        Map<String, double[]> RPS120_3 = TdxExtDataFun.computeRPS_2(data.codeDateMap, data.codeCloseMap, 120); // 120 -> 100
//        Map<String, double[]> RPS250_3 = TdxExtDataFun.computeRPS_2(data.codeDateMap, data.codeCloseMap, 250); // 250 -> 200
//
//
//        log.info("computeRPS_2 - 个股3     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start));
//
//
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        try {
//            RPS10.forEach((code, rps10_arr) -> {
//                double[] rps10_arr_2 = RPS10_2.get(code);
//                double[] rps10_arr_3 = RPS10_3.get(code);
//
//
//                LocalDate[] date_arr = data.codeDateMap.get(code);
//
//                if (null == rps10_arr) {
//                    return;
//                }
//
//
//                int rpsLen = rps10_arr.length;
//                int rpsLen_2 = rps10_arr_2.length;
//                int rpsLen_3 = rps10_arr_3.length;
//
//                int startIdx_2 = rpsLen_2 - rpsLen;
//                int startIdx_3 = rpsLen_3 - rpsLen;
//
//
//                for (int j = 0; j < rpsLen; j++) {
//                    double rps10_1 = rps10_arr[j];
//                    double rps10_2 = rps10_arr_2[startIdx_2 + j];
//                    double rps10_3 = rps10_arr_3[startIdx_3 + j];
//
//
//                    LocalDate date = date_arr[startIdx_2 + j];
//
//
//                    try {
//                        Assert.isTrue(Double.isNaN(rps10_1) || TdxFunCheck.equals(rps10_1, rps10_2, 0.02, 0.0001), String.format("code=[%s]  date=[%s]     RPS10_1=[%.4f] != RPS10_2=[%.4f]   ,   RPS10_3=[%.4f]", code, date, rps10_1, rps10_2, rps10_3));
//                        Assert.isTrue(TdxFunCheck.equals(rps10_2, rps10_3, 0.01, 0.0001), String.format("code=[%s]  date=[%s]     RPS10_2=[%.4f] != RPS10_3=[%.4f]   ,   RPS10_1=[%.4f]", code, date, rps10_2, rps10_3, rps10_1));
//                    } catch (Exception e) {
//                        log.error("Check - err   >>>   {}", e.getMessage());
//                    }
//                }
//            });
//
//        } catch (Exception e) {
//            log.error("computeRPS - 个股 err     >>>     errMsg : {}", e.getMessage(), e);
//        }


        // -------------------------------------------------------------------------------------------------------------


        fill_rpsArr(RPS10, data.codeCloseMap);
        fill_rpsArr(RPS20, data.codeCloseMap);
        fill_rpsArr(RPS50, data.codeCloseMap);
        fill_rpsArr(RPS120, data.codeCloseMap);
        fill_rpsArr(RPS250, data.codeCloseMap);


        // -------------------------------------------------------------------------------------------------------------


        data.codeDateMap.keySet().parallelStream().forEach(code -> {
            LocalDate[] date_arr = data.codeDateMap.get(code);


            int length = date_arr.length;


            double[] rps10 = RPS10.get(code);
            double[] rps20 = RPS20.get(code);
            double[] rps50 = RPS50.get(code);
            double[] rps120 = RPS120.get(code);
            double[] rps250 = RPS250.get(code);


            // 这里保持数据对齐，禁止截取！！！  =>   只在 save2DB 阶段（compare__old_new__extData） ->  才做最终截取！！！
            List<ExtDataDTO> new_extDataDTOList = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {


                ExtDataDTO dto = new ExtDataDTO();
                dto.setDate(date_arr[i]);
                dto.setRps10(of(rps10[i]));
                dto.setRps20(of(rps20[i]));
                dto.setRps50(of(rps50[i]));
                dto.setRps120(of(rps120[i]));
                dto.setRps250(of(rps250[i]));

                new_extDataDTOList.add(dto);


                data.extDataMap.put(code, new_extDataDTOList);
            }
        });
    }


    /**
     * 个股 - 扩展数据 计算
     *
     * @param data
     * @param N
     * @param stockType
     */
    private void stockTask__extData(DataDTO data, Integer N, Integer stockType) {
        AtomicInteger count = new AtomicInteger(0);
        long start = System.currentTimeMillis();


        // -------------------------------------------------------


        // TODO   从 TDX 实时读取（通达信 -> 开启 自定义板块  =>  每分钟刷新一次 -> 写入txt）


        // 1亿 + 月多 + IN主线 + RPS红（5500 -> 300）
//        data.stockDOList = data.stockDOList.stream().filter(e -> CollectionUtils.isNotEmpty(e.getKlineDTOList())).collect(Collectors.toList());


        // -------------------------------------------------------


        ParallelCalcUtil.forEach(data.stockDOList,
                                 stockDO -> execCalcStockExtData(data, N, stockType, stockDO, count, start),
                                 ThreadPoolType.CPU_INTENSIVE_2
        );
    }

    private void execCalcStockExtData(DataDTO data, Integer N, Integer stockType,
                                      BaseStockDO stockDO, AtomicInteger count, long start) {


        String code = stockDO.getCode();
        List<ExtDataDTO> new_extDataDTOList = data.extDataMap.get(code);     // 暂时 只计算了 RPS


        // --------------------------------------------------------


        // old
        List<ExtDataDTO> old_extDataDTOList = stockDO.getExtDataDTOList();


        // fill -> new_RPS（后续计算 RPS相关指标）
        stockDO.setExtDataDTOList(new_extDataDTOList);


        // -------------------------------------------------------------------------------------------------------------
        long stock_start = System.currentTimeMillis();


        // 1、计算ExtData（序列值）    ->     2、convert（序列   ->   列表）
        int RPS = Objects.equals(stockType, StockTypeEnum.A_STOCK.type) ? 85 : 87;
        calcExtData(new StockFun(stockDO), new_extDataDTOList, RPS);


        log.info("stockFun 指标计算 - 个股 suc     >>>     code : {} , count : {} , stockTime : {} , totalTime : {}",
                 code, count.incrementAndGet(), DateTimeUtil.formatNow2Hms(stock_start), DateTimeUtil.formatNow2Hms(start));
        // -------------------------------------------------------------------------------------------------------------


        // ------------------------------------------------------------------------ 更新 -> DB


        // 比较新旧     ==>     old_list   =>   当日 已存在->覆盖     不存在->add
        compare__old_new__extData(old_extDataDTOList, new_extDataDTOList, N);


        // -------------------------


        BaseStockDO entity = new BaseStockDO();
        entity.setId(data.codeIdMap.get(code));
        entity.setExtDataHisStr(JSON.toJSONString(ConvertStockExtData.dtoList2StrList(old_extDataDTOList)));    // 增量更新

        // 更新 -> DB
        baseStockService.updateById(entity);


        // -------------------------------------------------------------------------------------------------------------


        // OOM -> 缓存清理
        clearCodeCache(data, code);
    }


    // -----------------------------------------------------------------------------------------------------------------
    // ---------------------------------------------------- 板块 --------------------------------------------------------
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 从本地DB   加载全部（380+支）板块的 收盘价序列
     *
     * @return stock - close_arr
     */
    private DataDTO loadAllBlockKline(Integer N) {
        DataDTO data = new DataDTO();


        data.blockDOList = baseBlockService.listAllRpsKline();
        data.blockDOList = data.blockDOList.stream().filter(e -> CollectionUtils.isNotEmpty(e.getKlineDTOList())).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // 增量更新     =>     kline_his（计算参数 -> 需截取） / ext_data_his（被计算 -> 无需截取）    =>     截取 最后N条
        if (StockUtil.incrUpdate(N)) {

            data.blockDOList.parallelStream().forEach(e -> {
                // klineHis   ->   最后N条
                List<KlineDTO> klineDTOList = ListUtil.lastN(e.getKlineDTOList(), N + lastN);   // 为了计算 MA250、月多、...（需要至少 250日的K线数据）
                e.setKlineDTOList(klineDTOList);
            });
        }


        // -------------------------------------------------------------------------------------------------------------


        data.blockDOList.parallelStream().forEach(e -> {

            Long id = e.getId();
            String code = e.getCode();


            List<KlineDTO> klineDTOList = e.getKlineDTOList();
            KlineArrDTO klineArrDTO = ConvertStock.kline__dtoList2Arr(klineDTOList);


            LocalDate[] date_arr = klineArrDTO.date;
            double[] close_arr = klineArrDTO.close;


            TreeMap<LocalDate, Double> dateCloseMap = klineArrDTO.getDateCloseMap();


            data.codeCloseMap.put(code, close_arr);
            data.codeDateMap.put(code, date_arr);

            data.codeIdMap.put(code, id);


            data.codePriceMap.put(code, dateCloseMap);


            // ---------------------------------------------------------------------------------------------------------


            // 大盘/板块  ->  不会停牌
            if (INDEX_BLOCK.equals(code)) {
                int len = date_arr.length;

                data.startDate = data.startDate == null ? date_arr[0] : DateTimeUtil.min(data.startDate, date_arr[0]);
                data.endDate = data.endDate == null ? date_arr[len - 1] : DateTimeUtil.max(data.endDate, date_arr[len - 1]);

                if (StockUtil.incrUpdate(N)) {
                    data.startDate = DateTimeUtil.min(data.startDate, date_arr[Math.max(0, len - N)]);
                    data.endDate = DateTimeUtil.max(data.endDate, date_arr[len - 1]);
                }
            }
        });


        // -------------------------------------------------------------------------------------------------------------


        for (int i = 0; i < data.blockDOList.size(); i++) {
            BaseBlockDO blockDO = data.blockDOList.get(i);
            data.codeIdxMap.put(blockDO.getCode(), i);
        }


        // -------------------------------------------------------------------------------------------------------------


        return data;
    }


    /**
     * 板块 - RPS计算
     *
     * @param data
     */
    private void blockTask__RPS(DataDTO data, int lastDays) {
        long start = System.currentTimeMillis();


        // 计算 -> RPS
        Map<String, double[]> RPS5 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 5, lastDays);
        Map<String, double[]> RPS10 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 10, lastDays);
        Map<String, double[]> RPS15 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 15, lastDays);
        Map<String, double[]> RPS20 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 20, lastDays);
        Map<String, double[]> RPS50 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 50, lastDays);


        log.info("computeRPS - 板块     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start));


        // -------------------------------------------------------------------------------------------------------------


        fill_rpsArr(RPS5, data.codeCloseMap);
        fill_rpsArr(RPS10, data.codeCloseMap);
        fill_rpsArr(RPS15, data.codeCloseMap);
        fill_rpsArr(RPS20, data.codeCloseMap);
        fill_rpsArr(RPS50, data.codeCloseMap);


        // -------------------------------------------------------------------------------------------------------------


        data.codeDateMap.keySet().parallelStream().forEach(code -> {
            LocalDate[] date_arr = data.codeDateMap.get(code);

            int length = date_arr.length;


            double[] rps10 = RPS5.get(code);
            double[] rps20 = RPS10.get(code);
            double[] rps50 = RPS15.get(code);
            double[] rps120 = RPS20.get(code);
            double[] rps250 = RPS50.get(code);


            List<ExtDataDTO> new_extDataDTOList = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {

                // 大盘/板块  ->  不会停牌
                ExtDataDTO dto = new ExtDataDTO();
                dto.setDate(date_arr[i]);
                dto.setRps10(of(rps10[i]));
                dto.setRps20(of(rps20[i]));
                dto.setRps50(of(rps50[i]));
                dto.setRps120(of(rps120[i]));
                dto.setRps250(of(rps250[i]));

                new_extDataDTOList.add(dto);
            }


            data.extDataMap.put(code, new_extDataDTOList);
        });
    }


    /**
     * 板块 - 扩展数据 计算
     *
     * @param data
     * @param N
     */
    private void blockTask__extData(DataDTO data, Integer N) {
        AtomicInteger count = new AtomicInteger(0);
        long start = System.currentTimeMillis();


        // -------------------------------------------------------


        ParallelCalcUtil.forEach(data.blockDOList,
                                 blockDO -> execCalcBlockExtData(data, N, blockDO, count, start),
                                 ThreadPoolType.CPU_INTENSIVE_2
        );
    }


    private void execCalcBlockExtData(DataDTO data,
                                      Integer N,
                                      BaseBlockDO blockDO,
                                      AtomicInteger count,
                                      long start) {


        String code = blockDO.getCode();
        List<ExtDataDTO> new_extDataDTOList = data.extDataMap.get(code);     // 暂时 只计算了 RPS


        // --------------------------------------------------------


        // old（完整His  ->  load时，并未截取）
        List<ExtDataDTO> old_extDataDTOList = blockDO.getExtDataDTOList();


        // fill -> new_RPS（后续计算 RPS相关指标）
        blockDO.setExtDataDTOList(new_extDataDTOList);


        // -------------------------------------------------------------------------------------------------------------
        long stock_start = System.currentTimeMillis();


        // 1、计算ExtData（序列值）    ->     2、convert（序列   ->   列表）
        calcExtData(new BlockFun(blockDO), new_extDataDTOList, 87);


        log.info("blockFun 指标计算 - 板块 suc     >>>     [{}-{}] , count : {} , stockTime : {} , totalTime : {}",
                 code, blockDO.getName(), count.incrementAndGet(), DateTimeUtil.formatNow2Hms(stock_start), DateTimeUtil.formatNow2Hms(start));
        // -------------------------------------------------------------------------------------------------------------


        // ------------------------------------------------------------------------ 更新 -> DB


        // 比较新旧     ==>     old_list   =>   当日 已存在->覆盖     不存在->add
        compare__old_new__extData(old_extDataDTOList, new_extDataDTOList, N);


        // -------------------------


        BaseBlockDO entity = new BaseBlockDO();
        entity.setId(data.codeIdMap.get(code));
        entity.setExtDataHisStr(JSON.toJSONString(ConvertStockExtData.dtoList2StrList(old_extDataDTOList)));    // 增量更新


        // 更新 -> DB
        baseBlockService.updateById(entity);


        // -------------------------------------------------------------------------------------------------------------


        // OOM -> 缓存清理
        clearCodeCache(data, code);
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 1、计算ExtData（序列值）    ->     2、convert（序列   ->   列表）
     *
     * @param fun
     * @param extDataDTOList
     * @param RPS            个股-85/90/95          板块-87/92/97
     */
    public static void calcExtData(StockFun fun, List<ExtDataDTO> extDataDTOList, int RPS) {


        // ---------------------------- 1、计算ExtData（序列值）


        // 计算 -> 指标
        // StockFun fun = new StockFun(stockDO.getCode(), stockDO);


        // ---------------------------------------------------


        double[] MA5 = fun.MA(5);
        double[] MA10 = fun.MA(10);
        double[] MA20 = fun.MA(20);
        double[] MA30 = fun.MA(30);
        double[] MA50 = fun.MA(50);
        double[] MA60 = fun.MA(60);
        double[] MA100 = fun.MA(100);
        double[] MA120 = fun.MA(120);
        double[] MA150 = fun.MA(150);
        double[] MA200 = fun.MA(200);
        double[] MA250 = fun.MA(250);


        // ---------------------------------------------------


        double[] SSF = fun.SSF();
        double[] SAR = fun.SAR();


        // ---------------------------------------------------


        double[] RPS三线和 = fun.RPS三线和();
        double[] RPS五线和 = fun.RPS五线和();


        // ---------------------------------------------------


        double[] 中期涨幅N5 = fun.中期涨幅N(5);
        double[] 中期涨幅N10 = fun.中期涨幅N(10);
        double[] 中期涨幅N20 = fun.中期涨幅N(20);
        double[] 中期涨幅N30 = fun.中期涨幅N(30);
        double[] 中期涨幅N50 = fun.中期涨幅N(50);
        double[] 中期涨幅N60 = fun.中期涨幅N(60);
        double[] 中期涨幅N100 = fun.中期涨幅N(100);
        double[] 中期涨幅N120 = fun.中期涨幅N(120);
        double[] 中期涨幅N150 = fun.中期涨幅N(150);
        double[] 中期涨幅N200 = fun.中期涨幅N(200);
        double[] 中期涨幅N250 = fun.中期涨幅N(250);


        // ---------------------------------------------------


        double[] N3日涨幅 = fun.N日涨幅(3);
        double[] N5日涨幅 = fun.N日涨幅(5);
        double[] N10日涨幅 = fun.N日涨幅(10);
        double[] N20日涨幅 = fun.N日涨幅(20);
        double[] N30日涨幅 = fun.N日涨幅(30);
        double[] N50日涨幅 = fun.N日涨幅(50);
        double[] N60日涨幅 = fun.N日涨幅(60);
        double[] N100日涨幅 = fun.N日涨幅(100);
        double[] N120日涨幅 = fun.N日涨幅(120);
        double[] N150日涨幅 = fun.N日涨幅(150);
        double[] N200日涨幅 = fun.N日涨幅(200);
        double[] N250日涨幅 = fun.N日涨幅(250);


        // ---------------------------------------------------


        MidAdjustResult 中期调整 = fun.中期调整();

        double[] 中期调整幅度 = 中期调整.adjustPct1;
        int[] 中期调整天数 = 中期调整.adjustDays1;

        double[] 中期调整幅度2 = 中期调整.adjustPct2;
        int[] 中期调整天数2 = 中期调整.adjustDays2;


        // ---------------------------------------------------


        int[] 短期趋势支撑线 = fun.短期趋势支撑线();
        int[] 中期趋势支撑线 = fun.中期趋势支撑线(短期趋势支撑线);
        int[] 长期趋势支撑线 = fun.长期趋势支撑线(中期趋势支撑线);


        // ---------------------------------------------------


        double[] C_SSF_偏离率 = fun.C_SSF_偏离率();
        double[] H_SSF_偏离率 = fun.H_SSF_偏离率();


        // --------------------------------------------------


        double[] C_MA5_偏离率 = fun.C_MA_偏离率(5);
        double[] H_MA5_偏离率 = fun.H_MA_偏离率(5);

        double[] C_MA10_偏离率 = fun.C_MA_偏离率(10);
        double[] H_MA10_偏离率 = fun.H_MA_偏离率(10);

        double[] C_MA20_偏离率 = fun.C_MA_偏离率(20);
        double[] H_MA20_偏离率 = fun.H_MA_偏离率(20);

        double[] C_MA30_偏离率 = fun.C_MA_偏离率(30);
        double[] H_MA30_偏离率 = fun.H_MA_偏离率(30);

        double[] C_MA50_偏离率 = fun.C_MA_偏离率(50);
        double[] H_MA50_偏离率 = fun.H_MA_偏离率(50);

        double[] C_MA60_偏离率 = fun.C_MA_偏离率(60);
        double[] H_MA60_偏离率 = fun.H_MA_偏离率(60);

        double[] C_MA100_偏离率 = fun.C_MA_偏离率(100);
        double[] H_MA100_偏离率 = fun.H_MA_偏离率(100);

        double[] C_MA120_偏离率 = fun.C_MA_偏离率(120);
        double[] H_MA120_偏离率 = fun.H_MA_偏离率(120);

        double[] C_MA150_偏离率 = fun.C_MA_偏离率(150);
        double[] H_MA150_偏离率 = fun.H_MA_偏离率(150);

        double[] C_MA200_偏离率 = fun.C_MA_偏离率(200);
        double[] H_MA200_偏离率 = fun.H_MA_偏离率(200);

        double[] C_MA250_偏离率 = fun.C_MA_偏离率(250);
        double[] H_MA250_偏离率 = fun.H_MA_偏离率(250);


        // --------------------------------------------------


        boolean[] 上影大阴 = fun.上影大阴();
        boolean[] 高位爆量上影大阴 = fun.高位爆量上影大阴();


        boolean[] 涨停 = fun.涨停();
        boolean[] 跌停 = fun.跌停();


        // ---------------------------------------------------


        boolean[] XZZB = fun.XZZB();
        boolean[] BSQJ = fun.BSQJ();


        // ---------------------------------------------------


        boolean[] MA5多 = fun.MA多(5);
        boolean[] MA5空 = fun.MA空(5);
        boolean[] MA10多 = fun.MA多(10);
        boolean[] MA10空 = fun.MA空(10);
        boolean[] MA20多 = fun.MA多(20);
        boolean[] MA20空 = fun.MA空(20);
        boolean[] SSF多 = fun.SSF多();
        boolean[] SSF空 = fun.SSF空();


        boolean[] 上MA20 = fun.上MA(20);
        boolean[] 下MA20 = fun.下MA(20);
        boolean[] 上SSF = fun.上SSF();
        boolean[] 下SSF = fun.下SSF();


        // ---------------------------------------------------


        boolean[] N60日新高 = fun.N日新高(60);
        boolean[] N100日新高 = fun.N日新高(100);
        boolean[] 历史新高 = fun.历史新高();


        boolean[] 百日新高 = fun.百日新高(100);


        // ---------------------------------------------------


        boolean[] 月多 = fun.月多();
        boolean[] 均线预萌出 = fun.均线预萌出();
        boolean[] 均线萌出 = fun.均线萌出();
        boolean[] 小均线多头 = fun.小均线多头();
        boolean[] 大均线多头 = fun.大均线多头();
        boolean[] 均线大多头 = fun.均线大多头();
        boolean[] 均线极多头 = fun.均线极多头();


        // ---------------------------------------------------


        // 个股-85/90/95          板块-87/92/97
        boolean[] RPS红 = fun.RPS红(RPS);
        boolean[] RPS一线红 = fun.RPS一线红(RPS + 10);
        boolean[] RPS双线红 = fun.RPS双线红(RPS + 5);
        boolean[] RPS三线红 = fun.RPS三线红(RPS);


        // ---------------------------------------------------


        boolean[] 首次三线红 = fun.首次三线红(RPS);
        boolean[] 口袋支点 = fun.口袋支点(MA10, MA20, MA50, MA100, MA120, MA200, MA250, SAR, 中期调整, RPS红, 均线预萌出, N60日新高, 中期涨幅N20, 上影大阴);


        // ---------------------------------------------------


        int[] klineType = fun.klineType();


        // ---------------------------------------------------


        // ---------------------------- 2、convert（序列   ->   列表）


        for (int i = 0; i < extDataDTOList.size(); i++) {
            ExtDataDTO dto = extDataDTOList.get(i);


            // ---------------------------------------------------


            dto.setMA5(of(MA5[i]));
            dto.setMA10(of(MA10[i]));
            dto.setMA20(of(MA20[i]));
            dto.setMA30(of(MA30[i]));
            dto.setMA50(of(MA50[i]));
            dto.setMA60(of(MA60[i]));
            dto.setMA100(of(MA100[i]));
            dto.setMA120(of(MA120[i]));
            dto.setMA150(of(MA150[i]));
            dto.setMA200(of(MA200[i]));
            dto.setMA250(of(MA250[i]));


            // ---------------------------------------------------


            dto.setSSF(of(SSF[i]));
            dto.setSAR(of(SAR[i]));


            // ---------------------------------------------------


            dto.setRPS三线和(of(RPS三线和[i]));
            dto.setRPS五线和(of(RPS五线和[i]));


            // ---------------------------------------------------


            dto.set中期涨幅N5(of(中期涨幅N5[i]));
            dto.set中期涨幅N10(of(中期涨幅N10[i]));
            dto.set中期涨幅N20(of(中期涨幅N20[i]));
            dto.set中期涨幅N30(of(中期涨幅N30[i]));
            dto.set中期涨幅N50(of(中期涨幅N50[i]));
            dto.set中期涨幅N60(of(中期涨幅N60[i]));
            dto.set中期涨幅N100(of(中期涨幅N100[i]));
            dto.set中期涨幅N120(of(中期涨幅N120[i]));
            dto.set中期涨幅N150(of(中期涨幅N150[i]));
            dto.set中期涨幅N200(of(中期涨幅N200[i]));
            dto.set中期涨幅N250(of(中期涨幅N250[i]));


            // ---------------------------------------------------


            dto.setN3日涨幅(of(N3日涨幅[i]));
            dto.setN5日涨幅(of(N5日涨幅[i]));
            dto.setN10日涨幅(of(N10日涨幅[i]));
            dto.setN20日涨幅(of(N20日涨幅[i]));
            dto.setN30日涨幅(of(N30日涨幅[i]));
            dto.setN50日涨幅(of(N50日涨幅[i]));
            dto.setN60日涨幅(of(N60日涨幅[i]));
            dto.setN100日涨幅(of(N100日涨幅[i]));
            dto.setN120日涨幅(of(N120日涨幅[i]));
            dto.setN150日涨幅(of(N150日涨幅[i]));
            dto.setN200日涨幅(of(N200日涨幅[i]));
            dto.setN250日涨幅(of(N250日涨幅[i]));


            // ---------------------------------------------------


            dto.set中期调整幅度(of(中期调整幅度[i]));
            dto.set中期调整天数(of(中期调整天数[i]));
            dto.set中期调整幅度2(of(中期调整幅度2[i]));
            dto.set中期调整天数2(of(中期调整天数2[i]));


            // ---------------------------------------------------


            dto.set短期支撑线(短期趋势支撑线[i]);
            dto.set中期支撑线(中期趋势支撑线[i]);
            dto.set长期支撑线(长期趋势支撑线[i]);


            // ---------------------------------------------------


            dto.setC_SSF_偏离率(C_SSF_偏离率[i]);
            dto.setH_SSF_偏离率(H_SSF_偏离率[i]);


            // ---------------------------------------------------


            dto.setC_MA5_偏离率(C_MA5_偏离率[i]);
            dto.setH_MA5_偏离率(H_MA5_偏离率[i]);

            dto.setC_MA10_偏离率(C_MA10_偏离率[i]);
            dto.setH_MA10_偏离率(H_MA10_偏离率[i]);

            dto.setC_MA20_偏离率(C_MA20_偏离率[i]);
            dto.setH_MA20_偏离率(H_MA20_偏离率[i]);

            dto.setC_MA30_偏离率(C_MA30_偏离率[i]);
            dto.setH_MA30_偏离率(H_MA30_偏离率[i]);

            dto.setC_MA50_偏离率(C_MA50_偏离率[i]);
            dto.setH_MA50_偏离率(H_MA50_偏离率[i]);

            dto.setC_MA60_偏离率(C_MA60_偏离率[i]);
            dto.setH_MA60_偏离率(H_MA60_偏离率[i]);

            dto.setC_MA100_偏离率(C_MA100_偏离率[i]);
            dto.setH_MA100_偏离率(H_MA100_偏离率[i]);

            dto.setC_MA120_偏离率(C_MA120_偏离率[i]);
            dto.setH_MA120_偏离率(H_MA120_偏离率[i]);

            dto.setC_MA150_偏离率(C_MA150_偏离率[i]);
            dto.setH_MA150_偏离率(H_MA150_偏离率[i]);

            dto.setC_MA200_偏离率(C_MA200_偏离率[i]);
            dto.setH_MA200_偏离率(H_MA200_偏离率[i]);

            dto.setC_MA250_偏离率(C_MA250_偏离率[i]);
            dto.setH_MA250_偏离率(H_MA250_偏离率[i]);


            // ---------------------------------------------------


            dto.set上影大阴(上影大阴[i]);
            dto.set高位爆量上影大阴(高位爆量上影大阴[i]);


            dto.set涨停(涨停[i]);
            dto.set跌停(跌停[i]);


            // ---------------------------------------------------


            dto.setXZZB(XZZB[i]);
            dto.setBSQJ(BSQJ[i]);


            // ---------------------------------------------------

            dto.setMA5多(MA5多[i]);
            dto.setMA5空(MA5空[i]);
            dto.setMA10多(MA10多[i]);
            dto.setMA10空(MA10空[i]);
            dto.setMA20多(MA20多[i]);
            dto.setMA20空(MA20空[i]);
            dto.setSSF多(SSF多[i]);
            dto.setSSF空(SSF空[i]);


            dto.set上MA20(上MA20[i]);
            dto.set下MA20(下MA20[i]);
            dto.set上SSF(上SSF[i]);
            dto.set下SSF(下SSF[i]);


            // ---------------------------------------------------


            dto.setN60日新高(N60日新高[i]);
            dto.setN100日新高(N100日新高[i]);
            dto.set历史新高(历史新高[i]);


            dto.set百日新高(百日新高[i]);


            // ---------------------------------------------------


            dto.set月多(月多[i]);
            dto.set均线预萌出(均线预萌出[i]);
            dto.set均线萌出(均线萌出[i]);
            dto.set小均线多头(小均线多头[i]);
            dto.set大均线多头(大均线多头[i]);
            dto.set均线大多头(均线大多头[i]);
            dto.set均线极多头(均线极多头[i]);


            // ---------------------------------------------------


            dto.setRPS红(RPS红[i]);
            dto.setRPS一线红(RPS一线红[i]);
            dto.setRPS双线红(RPS双线红[i]);
            dto.setRPS三线红(RPS三线红[i]);


            // ---------------------------------------------------


            dto.set首次三线红(首次三线红[i]);
            dto.set口袋支点(口袋支点[i]);


            // ---------------------------------------------------


            dto.setKlineType(klineType[i]);


            // ---------------------------------------------------
        }
    }


    /**
     * 增量更新     =>     比较新旧   ->   当日 已存在->覆盖     不存在->add
     *
     * @param old_extDataList
     * @param new_extDataList
     * @param N
     */
    private void compare__old_new__extData(List<ExtDataDTO> old_extDataList,
                                           List<ExtDataDTO> new_extDataList,
                                           Integer N) {


        // 增量更新
        if (StockUtil.incrUpdate(N, old_extDataList.size())) {


            // ------------------- old       date - idx


            Map<LocalDate, Integer> old_dateIndexMap = old_dateIndexMap(old_extDataList);


            // -------------------------------------------------------


            // （从 251日 开始） 遍历 new_extDataList   ->   逐日判断 是否已存在
            for (int i = lastN; i < new_extDataList.size(); i++) {
                ExtDataDTO new_dto = new_extDataList.get(i);


                // 当前extData  ->  交易日                1个交易日   ->   1条记录（ExtDataDTO）
                LocalDate date = new_dto.getDate();


                // ------------------------------- 当日 扩展数据     =>     在 old 中  ->  是否已存在


                Integer idx = old_dateIndexMap.get(date);

                // 已存在 -> 覆盖
                if (idx != null) {
                    old_extDataList.set(idx, new_dto);
                }
                // 不存在 -> 新插入
                else {
                    old_extDataList.add(new_dto);
                }
            }


        } else {

            // 全量更新
            old_extDataList.clear();
            old_extDataList.addAll(new_extDataList);
        }
    }


    private Map<LocalDate, Integer> old_dateIndexMap(List<ExtDataDTO> old__extDataDTOList) {
        Map<LocalDate, Integer> old_dateIndexMap = Maps.newHashMap();


        // 从0开始（非251）
        for (int i = 0; i < old__extDataDTOList.size(); i++) {
            ExtDataDTO dto = old__extDataDTOList.get(i);

            old_dateIndexMap.put(dto.getDate(), i);
        }


        return old_dateIndexMap;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    private void fill_rpsArr(Map<String, double[]> rpsMap, Map<String, double[]> codeCloseMap) {
        rpsMap.replaceAll((code, rpsArrOld) -> {
            double[] closeArr = codeCloseMap.get(code);
            if (closeArr == null) return rpsArrOld;

            int newLen = closeArr.length;
            int oldLen = rpsArrOld.length;

            double[] rpsArrNew = new double[newLen];
            Arrays.fill(rpsArrNew, Double.NaN);

            // 使用 System.arraycopy 快速拷贝
            // 源数组, 源起始位, 目标数组, 目标起始位, 长度
            System.arraycopy(rpsArrOld, 0, rpsArrNew, newLen - oldLen, oldLen);

            return rpsArrNew; // replaceAll 会自动更新 Map
        });
    }


    private static double of(double val) {
        return of(val, 3);
    }

    private static double of(double val, int setScale) {
        if (Double.isNaN(val)) {
            return val;
        } else if (Double.isInfinite(val)) {
            return 0;
        }

        return BigDecimal.valueOf(val).setScale(setScale, RoundingMode.HALF_UP).doubleValue();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    public static class DataDTO {
        Map<String, List<ExtDataDTO>> extDataMap = Maps.newConcurrentMap();


        LocalDate startDate;
        LocalDate endDate;


        List<BaseStockDO> stockDOList = Lists.newArrayList();
        List<BaseBlockDO> blockDOList = Lists.newArrayList();


        Map<String, TreeMap<LocalDate, Double>> codePriceMap = Maps.newConcurrentMap();


        // code - date_arr
        Map<String, LocalDate[]> codeDateMap = Maps.newConcurrentMap();
        // code - close_arr
        Map<String, double[]> codeCloseMap = Maps.newConcurrentMap();


        // code - id
        Map<String, Long> codeIdMap = Maps.newConcurrentMap();


        // code - idx
        Map<String, Integer> codeIdxMap = Maps.newConcurrentMap();


        // -----------------------------------------------------------------


        @Override
        public String toString() {
            return "DataDTO{" +
                    "extDataMap=" + extDataMap +
                    ", startDate=" + startDate +
                    ", endDate=" + endDate +
                    ", stockDOList=" + stockDOList.size() +
                    ", blockDOList=" + blockDOList.size() +
                    ", codePriceMap=" + codePriceMap.size() +
                    ", codeDateMap=" + codeDateMap.size() +
                    ", codeCloseMap=" + codeCloseMap.size() +
                    ", codeIdMap=" + codeIdMap.size() +
                    ", codeIdxMap=" + codeIdxMap.size() +
                    '}';
        }
    }


    /**
     * 提前 清理缓存（OOM）       // 内存 < 64GB     =>     手动GC
     *
     * 注意：本方法会修改 data 对象内的集合，外部若只读请先做防御性拷贝
     *
     * @param data 全局数据容器
     * @param code 待移除的 股票/板块code
     */
    private static void clearCodeCache(DataDTO data, String code) {

        List<ExtDataDTO> remove1 = data.extDataMap.remove(code);
        TreeMap<LocalDate, Double> remove2 = data.codePriceMap.remove(code);
        LocalDate[] remove3 = data.codeDateMap.remove(code);
        double[] remove4 = data.codeCloseMap.remove(code);
        Long remove5 = data.codeIdMap.remove(code);
        log.info("clearCodeCache  ->  [Map] - suc     >>>     remove1_size : {} , remove2_size : {} , remove3_size : {} , remove4_size : {} , remove5_size : {}",
                 remove1.size(), remove2.size(), remove3.length, remove4.length, remove5 != null ? 1 : 0);


        try {
            Integer idx = data.codeIdxMap.get(code);
            if (null == idx) {
                log.error("clearCodeCache  ->  [Entity] - fail     >>>     code : {} , idx : {}", code, idx);
                return;
            }


            BaseBlockDO blockDO = StockTypeEnum.isBlock(code) ? data.blockDOList.get(idx) : null;
            if (null != blockDO && Objects.equals(blockDO.getCode(), code)) {

                blockDO.setKlineHis(null);
                blockDO.setKlineHisStr(null);
                blockDO.setKlineDTOList(null);

                blockDO.setExtDataHis(null);
                blockDO.setExtDataHisStr(null);
                blockDO.setExtDataDTOList(null);

                data.blockDOList.set(idx, null);
                log.info("clearCodeCache  ->  [blockDO] - suc     >>>     code : {} , blockDO : {}", code, JSON.toJSONString(data.blockDOList.get(idx)));
            }


            BaseStockDO stockDO = StockTypeEnum.isStock_ETF(code) ? data.stockDOList.get(idx) : null;
            if (null != stockDO && Objects.equals(stockDO.getCode(), code)) {
                earlyClearStockCache__OOM(stockDO);

                data.stockDOList.set(idx, null);
                log.info("clearCodeCache  ->  [stockDO] - suc     >>>     code : {} , stockDO : {}", code, JSON.toJSONString(data.stockDOList.get(idx)));
            }

        } catch (Exception e) {
            log.error("clearCodeCache - error", e);
        }
    }


    /**
     * 提前 清理缓存（OOM）       // 内存 < 64GB     =>     手动GC
     *
     * @param stockDO 待清理的股票
     */
    public static void earlyClearStockCache__OOM(BaseStockDO stockDO) {
        stockDO.setKlineHis(null);
        stockDO.setKlineHisStr(null);
        stockDO.setKlineDTOList(null);

        stockDO.setExtDataHis(null);
        stockDO.setExtDataHisStr(null);
        stockDO.setExtDataDTOList(null);
    }


}