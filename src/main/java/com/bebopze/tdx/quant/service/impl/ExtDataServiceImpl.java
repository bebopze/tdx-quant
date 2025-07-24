package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtDataFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.ExtDataService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * 扩展数据 - 计算
 *
 * @author: bebopze
 * @date: 2025/5/24
 */
@Slf4j
@Service
public class ExtDataServiceImpl implements ExtDataService {


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;


    @Override
    public void refreshExtDataAll() {
        calcBlockExtData();
        calcStockExtData();
    }


    @Override
    public void calcStockExtData() {


        // -------------------------------------------------------------------------------------------------------------

        // 预加载 全量行情
        // 从本地DB   加载全部（5000+支） 个股的   收盘价序列/日期序列/ code-id


        // 预加载 -> 解析数据
        DataDTO data = loadAllStockKline();


        // -------------------------------------------------------------------------------------------------------------


        // RPS
        stockTask__RPS(data);

        // 扩展数据
        stockTask__extData(data);
    }


    @Override
    public void calcBlockExtData() {


        // -------------------------------------------------------------------------------------------------------------

        // 从本地DB   加载全部（380+支） 板块的   收盘价序列/日期序列/ code-id


        // 预加载 -> 解析数据
        DataDTO data = loadAllBlockKline();


        // -------------------------------------------------------------------------------------------------------------


        // RPS
        blockTask__RPS(data);

        // 扩展数据
        blockTask__extData(data);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 从本地DB   加载全部（5000+支）个股的 收盘价序列
     *
     * @return stock - close_arr
     */
    private DataDTO loadAllStockKline() {
        DataDTO data = new DataDTO();


        data.stockDOList = baseStockService.listAllKline();
        data.stockDOList = data.stockDOList.stream().filter(e -> StringUtils.isNotBlank(e.getKlineHis())).collect(Collectors.toList());


        data.stockDOList.parallelStream().forEach(e -> {

            Long id = e.getId();
            String code = e.getCode();


            List<KlineDTO> klineDTOList = e.getKlineDTOList();
            KlineArrDTO klineArrDTO = ConvertStock.dtoList2Arr(klineDTOList);


            LocalDate[] date_arr = klineArrDTO.date;
            double[] close_arr = klineArrDTO.close;


            TreeMap<LocalDate, Double> dateCloseMap = klineArrDTO.getDateCloseMap();


            data.codeDateMap.put(code, date_arr);
            data.codeCloseMap.put(code, close_arr);

            data.codeIdMap.put(code, id);


            data.codePriceMap.put(code, dateCloseMap);
        });


        return data;
    }


    /**
     * 从本地DB   加载全部（380+支）板块的 收盘价序列
     *
     * @return stock - close_arr
     */
    private DataDTO loadAllBlockKline() {
        DataDTO data = new DataDTO();


        data.blockDOList = baseBlockService.listAllRpsKline();
        data.blockDOList = data.blockDOList.stream().filter(e -> StringUtils.isNotBlank(e.getKlineHis())).collect(Collectors.toList());


        data.blockDOList.parallelStream().forEach(e -> {

            Long id = e.getId();
            String code = e.getCode();


            List<KlineDTO> klineDTOList = e.getKlineDTOList();
            KlineArrDTO klineArrDTO = ConvertStock.dtoList2Arr(klineDTOList);


            LocalDate[] date_arr = klineArrDTO.date;
            double[] close_arr = klineArrDTO.close;


            TreeMap<LocalDate, Double> dateCloseMap = klineArrDTO.getDateCloseMap();


            data.codeCloseMap.put(code, close_arr);
            data.codeDateMap.put(code, date_arr);

            data.codeIdMap.put(code, id);


            data.codePriceMap.put(code, dateCloseMap);
        });


        return data;
    }


    /**
     * 个股 - RPS计算
     *
     * @param data
     */
    private void stockTask__RPS(DataDTO data) {
        long start = System.currentTimeMillis();


        // 计算 -> RPS
        Map<String, double[]> RPS10 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 10);
        Map<String, double[]> RPS20 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 20);
        Map<String, double[]> RPS50 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 50);
        Map<String, double[]> RPS120 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 120); // 120 -> 100
        Map<String, double[]> RPS250 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 250); // 250 -> 200


        log.info("computeRPS - 个股     >>>     totalTime : {}", DateTimeUtil.format2Hms(System.currentTimeMillis() - start));


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        data.codeDateMap.keySet().parallelStream().forEach(code -> {
            LocalDate[] date_arr = data.codeDateMap.get(code);

            int length = date_arr.length;


            double[] rps10 = RPS10.get(code);
            double[] rps20 = RPS20.get(code);
            double[] rps50 = RPS50.get(code);
            double[] rps120 = RPS120.get(code);
            double[] rps250 = RPS250.get(code);


            List<ExtDataDTO> dtoList = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {

                ExtDataDTO dto = new ExtDataDTO();
                dto.setDate(date_arr[i]);
                dto.setRps10(of(rps10[i]));
                dto.setRps20(of(rps20[i]));
                dto.setRps50(of(rps50[i]));
                dto.setRps120(of(rps120[i]));
                dto.setRps250(of(rps250[i]));

                dtoList.add(dto);
            }


            data.extDataMap.put(code, dtoList);
        });
    }


    /**
     * 个股 - 扩展数据 计算
     *
     * @param data
     */
    private void stockTask__extData(DataDTO data) {
        AtomicInteger count = new AtomicInteger(0);
        long start = System.currentTimeMillis();


        data.stockDOList.parallelStream().forEach(stockDO -> {

            String code = stockDO.getCode();
            List<ExtDataDTO> dtoList = data.extDataMap.get(code);


            // fill -> RPS
            stockDO.setExtDataHis(ConvertStockExtData.dtoList2JsonStr(dtoList));


            // --------------------------------------------------------


            long stock_start = System.currentTimeMillis();


            // --------------------------------------------------------


            // 计算 -> 指标
            StockFun fun = new StockFun(code, stockDO);


            double[] SSF = fun.SSF();

            double[] 中期涨幅 = fun.中期涨幅N(20);
            boolean[] 高位爆量上影大阴 = fun.高位爆量上影大阴();


            boolean[] MA20多 = fun.MA多(20);
            boolean[] MA20空 = fun.MA空(20);
            boolean[] SSF多 = fun.SSF多();
            boolean[] SSF空 = fun.SSF空();


            boolean[] N日新高 = fun.N日新高(60);
            boolean[] 均线预萌出 = fun.均线预萌出();
            boolean[] 均线萌出 = fun.均线萌出();
            boolean[] 大均线多头 = fun.大均线多头();


            boolean[] 月多 = fun.月多();
            boolean[] RPS三线红 = fun.RPS三线红(80);


            // --------------------------------------------------------


            long end = System.currentTimeMillis();
            log.info("stockFun 指标计算 - 个股     >>>     code : {} , count : {} , stockTime : {} , totalTime : {} ",
                     code, count.incrementAndGet(), DateTimeUtil.format2Hms(end - stock_start), DateTimeUtil.format2Hms(end - start));


            // --------------------------------------------------------


            for (int i = 0; i < dtoList.size(); i++) {
                ExtDataDTO dto = dtoList.get(i);


                dto.setSSF(of(SSF[i], 3));


                try {
                    dto.set中期涨幅(of(中期涨幅[i], 3));
                } catch (Exception e) {
                    log.error("code : {} , stockDO : {} , exMsg : {}", code, JSON.toJSONString(stockDO), e.getMessage(), e);
                }
                dto.set高位爆量上影大阴(高位爆量上影大阴[i]);


                dto.setMA20多(MA20多[i]);
                dto.setMA20空(MA20空[i]);
                dto.setSSF多(SSF多[i]);
                dto.setSSF空(SSF空[i]);


                dto.setN日新高(N日新高[i]);
                dto.set均线预萌出(均线预萌出[i]);
                dto.set均线萌出(均线萌出[i]);
                dto.set大均线多头(大均线多头[i]);


                dto.set月多(月多[i]);
                dto.setRPS三线红(RPS三线红[i]);
            }


            List<String> extDataList = ConvertStockExtData.dtoList2StrList(dtoList);


            BaseStockDO entity = new BaseStockDO();
            entity.setId(data.codeIdMap.get(code));
            entity.setExtDataHis(JSON.toJSONString(extDataList));

            baseStockService.updateById(entity);
        });
    }


    /**
     * 板块 - RPS计算
     *
     * @param data
     */
    private void blockTask__RPS(DataDTO data) {


        // 计算 -> RPS
        Map<String, double[]> RPS5 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 5);
        Map<String, double[]> RPS10 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 10);
        Map<String, double[]> RPS15 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 15);
        Map<String, double[]> RPS20 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 20);
        Map<String, double[]> RPS50 = TdxExtDataFun.computeRPS(data.codeDateMap, data.codeCloseMap, 50);


        // -------------------------------------------------------------------------------------------------------------


        data.codeDateMap.keySet().parallelStream().forEach(code -> {
            LocalDate[] date_arr = data.codeDateMap.get(code);

            int length = date_arr.length;


            double[] rps10_arr = RPS5.get(code);
            double[] rps20_arr = RPS10.get(code);
            double[] rps50_arr = RPS15.get(code);
            double[] rps120_arr = RPS20.get(code);
            double[] rps250_arr = RPS50.get(code);


            List<ExtDataDTO> dtoList = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {

                ExtDataDTO dto = new ExtDataDTO();
                dto.setDate(date_arr[i]);
                dto.setRps10(of(rps10_arr[i]));
                dto.setRps20(of(rps20_arr[i]));
                dto.setRps50(of(rps50_arr[i]));
                dto.setRps120(of(rps120_arr[i]));
                dto.setRps250(of(rps250_arr[i]));

                dtoList.add(dto);
            }


            data.extDataMap.put(code, dtoList);
        });
    }


    /**
     * 板块 - 扩展数据 计算
     *
     * @param data
     */
    private void blockTask__extData(DataDTO data) {
        AtomicInteger count = new AtomicInteger(0);
        long start = System.currentTimeMillis();


        data.blockDOList.parallelStream().forEach(blockDO -> {

            String code = blockDO.getCode();
            List<ExtDataDTO> dtoList = data.extDataMap.get(code);


            // fill -> RPS
            blockDO.setExtDataHis(ConvertStockExtData.dtoList2JsonStr(dtoList));


            // --------------------------------------------------------


            long block_start = System.currentTimeMillis();


            // --------------------------------------------------------


            // 计算 -> 指标
            BlockFun fun = new BlockFun(code, blockDO);


            double[] SSF = fun.SSF();

            double[] 中期涨幅 = fun.中期涨幅N(20);
            boolean[] 高位爆量上影大阴 = fun.高位爆量上影大阴();


            boolean[] MA20多 = fun.MA多(20);
            boolean[] MA20空 = fun.MA空(20);
            boolean[] SSF多 = fun.SSF多();
            boolean[] SSF空 = fun.SSF空();


            boolean[] N日新高 = fun.N日新高(60);
            boolean[] 均线预萌出 = fun.均线预萌出();
            boolean[] 均线萌出 = fun.均线萌出();
            boolean[] 大均线多头 = fun.大均线多头();


            boolean[] 月多 = fun.月多();
            boolean[] RPS三线红 = fun.RPS三线红(80);


            // --------------------------------------------------------


            long end = System.currentTimeMillis();
            log.info("blockFun 指标计算 - 板块     >>>     code : {} , count : {} , blockTime : {} , totalTime : {} ",
                     code, count.incrementAndGet(), DateTimeUtil.format2Hms(end - block_start), DateTimeUtil.format2Hms(end - start));


            // --------------------------------------------------------


            for (int i = 0; i < dtoList.size(); i++) {
                ExtDataDTO dto = dtoList.get(i);


                dto.setSSF(of(SSF[i], 3));

                dto.set中期涨幅(of(中期涨幅[i]));
                dto.set高位爆量上影大阴(高位爆量上影大阴[i]);


                dto.setMA20多(MA20多[i]);
                dto.setMA20空(MA20空[i]);
                dto.setSSF多(SSF多[i]);
                dto.setSSF空(SSF空[i]);


                dto.setN日新高(N日新高[i]);
                dto.set均线预萌出(均线预萌出[i]);
                dto.set均线萌出(均线萌出[i]);
                dto.set大均线多头(大均线多头[i]);


                dto.set月多(月多[i]);
                dto.setRPS三线红(RPS三线红[i]);
            }


            List<String> extDataList = ConvertStockExtData.dtoList2StrList(dtoList);


            BaseBlockDO entity = new BaseBlockDO();
            entity.setId(data.codeIdMap.get(code));
            entity.setExtDataHis(JSON.toJSONString(extDataList));

            baseBlockService.updateById(entity);
        });
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 填充最近N日的数据，如果 原始数据 不足N日，则不足的天数 补0（NaN）
     *
     * @param arr 原始数据
     * @param N   填充N日
     * @return
     */
    public static double[] fillNaN(double[] arr, int N) {
        double[] new_arr = new double[N];


        int M = arr.length;

        if (N >= M) {
            // 前 N-M 项补 NaN
            for (int i = 0; i < N - M; i++) {
                new_arr[i] = Double.NaN;
            }
            // 后 M 项直接拷贝 arr[0..M-1]
            System.arraycopy(arr, 0, new_arr, N - M, M);
        } else {
            // N < M 时，只拷贝最近 N 天的数据（arr 的后 N 项）
            // arr[M-N .. M-1]
            System.arraycopy(arr, M - N, new_arr, 0, N);
        }

        return new_arr;
    }


    private static double of(Number val, int setScale) {
        if (Double.isNaN(val.doubleValue())) {
            return Double.NaN;
        }
        return new BigDecimal(String.valueOf(val)).setScale(setScale, RoundingMode.HALF_UP).doubleValue();
    }


    private static double of(BigDecimal val) {
        if (null == val) return Double.NaN;
        return val.setScale(3, RoundingMode.HALF_UP).doubleValue();
    }


    private static double of(double val) {
        return of(val, 2);
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


        List<BaseStockDO> stockDOList = Lists.newArrayList();
        List<BaseBlockDO> blockDOList = Lists.newArrayList();


        Map<String, TreeMap<LocalDate, Double>> codePriceMap = Maps.newConcurrentMap();


        // code - date_arr
        Map<String, LocalDate[]> codeDateMap = Maps.newConcurrentMap();
        // code - close_arr
        Map<String, double[]> codeCloseMap = Maps.newConcurrentMap();


        // code - id
        Map<String, Long> codeIdMap = Maps.newConcurrentMap();
        Map<String, BaseBlockDO> codeEntityMap = Maps.newConcurrentMap();
    }

}
