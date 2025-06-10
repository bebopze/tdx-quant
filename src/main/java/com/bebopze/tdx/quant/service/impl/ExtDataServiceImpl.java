package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
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


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;


    @Override
    public void calcStockRps() {


        // -------------------------------------------------------------------------------------------------------------

        // 预加载 行情数据


        // -------------------------------------------------------------------------------------------------------------

        // 从本地DB   加载全部（5000+支） 个股的   收盘价序列/日期序列/ code-id


        // 加载 -> 解析数据
        DataDTO dataDTO = loadAllStockKline();


        // -------------------------------------------------------------------------------------------------------------


        task__RPS(dataDTO);


        task__stockFun(dataDTO);
    }


    @Override
    public void calcBlockRps() {


        // -------------------------------------------------------------------------------------------------------------

        // 从本地DB   加载全部（380+支） 板块的   收盘价序列/日期序列/ code-id


        // 加载 -> 解析数据
        DataDTO data = loadAllBlockKline();


        // -------------------------------------------------------------------------------------------------------------


        blockTask__RPS(data);


        blockTask__BlockFun(data);
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
            List<ExtDataDTO> extDataDTOList = e.getExtDataDTOList();


            KlineArrDTO klineArrDTO = ConvertStock.dtoList2Arr(klineDTOList);
            ExtDataArrDTO extDataArrDTO = ConvertStock.dtoList2Arr2(extDataDTOList);


            LocalDate[] date_arr = klineArrDTO.date;
            double[] close_arr = klineArrDTO.close;


            // TreeMap<LocalDate, Double> dateCloseMap = ConvertStockKline.fieldDatePriceMap(klineDTOList, "close");
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


        // 加载  最近500日   行情数据
        int DAY_LIMIT = 500;


        data.blockDOList = baseBlockService.listAllKline();
        data.blockDOList = data.blockDOList.stream().filter(e -> StringUtils.isNotBlank(e.getKlineHis())).collect(Collectors.toList());


        data.blockDOList.parallelStream().forEach(e -> {

            Long id = e.getId();
            String code = e.getCode();

            List<KlineDTO> klineDTOList = e.getKlineDTOList();


            LocalDate[] date_arr = ConvertStockKline.dateFieldValArr(klineDTOList, "date");
            double[] close_arr = ConvertStockKline.fieldValArr(klineDTOList, "close");


            TreeMap<LocalDate, Double> dateCloseMap = ConvertStockKline.fieldDatePriceMap(klineDTOList, "close");


            data.codeCloseMap.put(code, close_arr);
            data.codeDateMap.put(code, date_arr);

            data.codeIdMap.put(code, id);


            data.codePriceMap.put(code, dateCloseMap);
        });


        return data;
    }


    private void task__RPS(DataDTO data) {


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


            double[] rps10_arr = RPS10.get(code);
            double[] rps20_arr = RPS20.get(code);
            double[] rps50_arr = RPS50.get(code);
            double[] rps120_arr = RPS120.get(code);
            double[] rps250_arr = RPS250.get(code);


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


    private void task__stockFun(DataDTO data) {


        data.stockDOList.parallelStream().forEach(stockDO -> {

            String code = stockDO.getCode();
            List<ExtDataDTO> dtoList = data.extDataMap.get(code);


            // fill -> RPS
            stockDO.setExtDataHis(ConvertStockExtData.dtoList2JsonStr(dtoList));


            // --------------------------------------------------------


            long start = System.currentTimeMillis();


            // 计算 -> 指标
            StockFun fun = new StockFun(code, stockDO);


            double[] SSF = fun.SSF();


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


            log.info("stockFun 指标计算 - 个股     >>>     code : {} , totalTime : {}", code, DateTimeUtil.format2Hms(System.currentTimeMillis() - start));


            // --------------------------------------------------------


            for (int i = 0; i < dtoList.size(); i++) {
                ExtDataDTO dto = dtoList.get(i);


                dto.setSSF(of(SSF[i], 3));


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


            List<String> extDatas = ConvertStockExtData.dtoList2StrList(dtoList);


            BaseStockDO entity = new BaseStockDO();
            entity.setId(data.codeIdMap.get(code));
            entity.setExtDataHis(JSON.toJSONString(extDatas));

            baseStockService.updateById(entity);
        });
    }


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

    private void blockTask__BlockFun(DataDTO data) {


        data.blockDOList.parallelStream().forEach(blockDO -> {

            String code = blockDO.getCode();
            List<ExtDataDTO> dtoList = data.extDataMap.get(code);


            // fill -> RPS
            blockDO.setExtDataHis(ConvertStockExtData.dtoList2JsonStr(dtoList));


            // --------------------------------------------------------


            // 计算 -> 指标
            BlockFun fun = new BlockFun(code, blockDO);


            double[] SSF = fun.SSF();


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


            for (int i = 0; i < dtoList.size(); i++) {
                ExtDataDTO dto = dtoList.get(i);


                dto.setSSF(of(SSF[i], 3));


                dto.setMA20多(MA20多[i]);
                dto.setMA20空(MA20空[i]);
                dto.setSSF多(SSF多[i]);
                dto.setSSF空(SSF空[i]);


                dto.setN日新高(N日新高[i]);
                dto.set均线预萌出(均线预萌出[i]);
                dto.set均线萌出(均线萌出[i]);
                dto.set大均线多头(大均线多头[i]);


                dto.set月多(月多[i]);
                try {
                    dto.setRPS三线红(RPS三线红[i]);
                } catch (Exception e) {
                    log.error("code : {} , blockDO : {} , exMsg : {}", code, blockDO, e.getMessage(), e);
                }
            }


            List<String> extDatas = ConvertStockExtData.dtoList2StrList(dtoList);


            BaseBlockDO entity = new BaseBlockDO();
            entity.setId(data.codeIdMap.get(code));
            entity.setExtDataHis(JSON.toJSONString(extDatas));

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
        }
        return BigDecimal.valueOf(val).setScale(setScale, RoundingMode.HALF_UP).doubleValue();
    }


}
