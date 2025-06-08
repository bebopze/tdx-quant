package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtDataFun;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.ExtDataService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    public void calcStockRps() {


        // -------------------------------------------------------------------------------------------------------------

        // 预加载 行情数据


        Map<String, TreeMap<String, Double>> codePriceMap = Maps.newHashMap();

        // code - date_arr
        Map<String, String[]> codeDateMap = Maps.newHashMap();
        // code - close_arr
        Map<String, double[]> codeCloseMap = Maps.newHashMap();
        // code - id
        Map<String, Long> codeIdMap = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------

        // 从本地DB   加载全部（5000+支） 个股的   收盘价序列/日期序列/ code-id


        // 加载 -> 解析数据
        loadAllStockKline(codeDateMap, codeCloseMap, codePriceMap, codeIdMap);


        // -------------------------------------------------------------------------------------------------------------


        // 计算RPS
        Map<String, double[]> RPS10 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 10);
        Map<String, double[]> RPS20 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 20);
        Map<String, double[]> RPS50 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 50);
        Map<String, double[]> RPS120 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 120);// 120 -> 100
        Map<String, double[]> RPS250 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 250);// 250 -> 200


        // -------------------------------------------------------------------------------------------------------------


        // save -> DB


        codeDateMap.keySet().parallelStream().forEach(code -> {
            String[] date_arr = codeDateMap.get(code);


            double[] rps10_arr = RPS10.get(code);
            double[] rps20_arr = RPS20.get(code);
            double[] rps50_arr = RPS50.get(code);
            double[] rps120_arr = RPS120.get(code);
            double[] rps250_arr = RPS250.get(code);


            List<String> extDatas = Lists.newArrayList();
            for (int i = 0; i < date_arr.length; i++) {

                // 2025-05-13,91,92,93,94,95
                // 日期,RPS10,RPS20,RPS50,RPS120,RPS250

                // 扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）
                List<Object> extData = Lists.newArrayList(date_arr[i], of(rps10_arr[i]), of(rps20_arr[i]), of(rps50_arr[i]), of(rps120_arr[i]), of(rps250_arr[i]));


                String extDataStr = extData.stream().map(obj -> obj != null ? obj.toString() : "").collect(Collectors.joining(","));
                extDatas.add(extDataStr);
            }


            BaseStockDO stockDO = new BaseStockDO();
            stockDO.setId(codeIdMap.get(code));
            stockDO.setExtDataHis(JSON.toJSONString(extDatas));

            baseStockService.updateById(stockDO);
        });
    }


    @Override
    public void calcBlockRps() {


        // -------------------------------------------------------------------------------------------------------------

        // 预加载 行情数据


        Map<String, TreeMap<String, Double>> codePriceMap = Maps.newHashMap();

        // code - date_arr
        Map<String, String[]> codeDateMap = Maps.newHashMap();
        // code - close_arr
        Map<String, double[]> codeCloseMap = Maps.newHashMap();
        // code - id
        Map<String, Long> codeIdMap = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------

        // 从本地DB   加载全部（5000+支） 个股的   收盘价序列/日期序列/ code-id


        // 加载 -> 解析数据
        loadAllBlockKline(codeDateMap, codeCloseMap, codePriceMap, codeIdMap);


        // -------------------------------------------------------------------------------------------------------------


        // 计算RPS
        Map<String, double[]> RPS5 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 5);
        Map<String, double[]> RPS10 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 10);
        Map<String, double[]> RPS15 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 15);
        Map<String, double[]> RPS20 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 20);
        Map<String, double[]> RPS50 = TdxExtDataFun.computeRPS(codeDateMap, codeCloseMap, 50);


        // -------------------------------------------------------------------------------------------------------------


        // save -> DB


        codeDateMap.keySet().parallelStream().forEach(code -> {
            String[] date_arr = codeDateMap.get(code);


            double[] rps10_arr = RPS5.get(code);
            double[] rps20_arr = RPS10.get(code);
            double[] rps50_arr = RPS15.get(code);
            double[] rps120_arr = RPS20.get(code);
            double[] rps250_arr = RPS50.get(code);


            List<String> extDatas = Lists.newArrayList();
            for (int i = 0; i < date_arr.length; i++) {

                // 2025-05-13,91,92,93,94,95
                // 日期,RPS5,RPS10,RPS15,RPS20,RPS50

                // 扩展数据-JSON（[日期,RPS5,RPS10,RPS15,RPS20,RPS50]）
                List<Object> extData = Lists.newArrayList(date_arr[i], of(rps10_arr[i]), of(rps20_arr[i]), of(rps50_arr[i]), of(rps120_arr[i]), of(rps250_arr[i]));


                String extDataStr = extData.stream().map(obj -> obj != null ? obj.toString() : "").collect(Collectors.joining(","));
                extDatas.add(extDataStr);
            }


            BaseBlockDO blockDO = new BaseBlockDO();
            blockDO.setId(codeIdMap.get(code));
            blockDO.setExtDataHis(JSON.toJSONString(extDatas));

            baseBlockService.updateById(blockDO);
        });

    }


    /**
     * 从本地DB   加载全部（5000+支）个股的 收盘价序列
     *
     * @return stock - close_arr
     */
    private void loadAllStockKline(Map<String, String[]> stockDateArrMap,
                                   Map<String, double[]> stockCloseArrMap,
                                   Map<String, TreeMap<String, Double>> codePriceMap,
                                   Map<String, Long> codeIdMap) {


        List<BaseStockDO> baseStockDOList = baseStockService.listAllKline();
        baseStockDOList.forEach(e -> {

            Long id = e.getId();
            String code = e.getCode();


            String[] date_arr = ConvertStockKline.strFieldValArr(e.getKlineDTOList(), "date");
            double[] close_arr = ConvertStockKline.fieldValArr(e.getKlineDTOList(), "close");


            TreeMap<String, Double> dateCloseMap = ConvertStockKline.fieldDatePriceMap(e.getKlineDTOList(), "close");


            // 上市50天
            if (close_arr.length >= 50) {

                stockCloseArrMap.put(code, close_arr);
                stockDateArrMap.put(code, date_arr);

                codeIdMap.put(code, id);


                codePriceMap.put(code, dateCloseMap);
            }
        });
    }


    /**
     * 从本地DB   加载全部（380+支）板块的 收盘价序列
     *
     * @return stock - close_arr
     */
    private void loadAllBlockKline(Map<String, String[]> codeDateMap,
                                   Map<String, double[]> codeCloseMap,
                                   Map<String, TreeMap<String, Double>> codePriceMap,
                                   Map<String, Long> codeIdMap) {


        // 加载  最近500日   行情数据
        int DAY_LIMIT = 500;


        List<BaseBlockDO> blockDOList = baseBlockService.listAllKline();
        blockDOList.forEach(e -> {

            Long id = e.getId();
            String code = e.getCode();

            String[] date_arr = ConvertStockKline.strFieldValArr(e.getKlineDTOList(), "date");
            double[] close_arr = ConvertStockKline.fieldValArr(e.getKlineDTOList(), "close");


            TreeMap<String, Double> dateCloseMap = ConvertStockKline.fieldDatePriceMap(e.getKlineDTOList(), "close");


            // 上市50天
            if (close_arr.length >= 50) {

                codeCloseMap.put(code, close_arr);
                codeDateMap.put(code, date_arr);

                codeIdMap.put(code, id);


                codePriceMap.put(code, dateCloseMap);
            }
        });
    }


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


    private static Number of(Number val) {
        if (Double.isNaN(val.doubleValue())) {
            return val;
        }
        return new BigDecimal(String.valueOf(val)).setScale(2, RoundingMode.HALF_UP);
    }

}