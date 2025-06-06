package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtDataFun;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
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
import java.util.stream.Collectors;


/**
 * @author: bebopze
 * @date: 2025/5/24
 */
@Slf4j
@Service
public class ExtDataServiceImpl implements ExtDataService {


    @Autowired
    private IBaseStockService baseStockService;


    @Override
    public void calcStockRps() {


        // -------------------------------------------------------------------------------------------------------------

        // 预加载 行情数据


        // code - date_arr
        Map<String, String[]> stockDateArrMap = Maps.newHashMap();
        // code - close_arr
        Map<String, double[]> stockCloseArrMap = Maps.newHashMap();
        // code - id
        Map<String, Long> codeIdMap = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------

        // 从本地DB   加载全部（5000+支） 个股的   收盘价序列/日期序列/ code-id


        // 加载 -> 解析数据
        loadAllStockKline(stockCloseArrMap, stockDateArrMap, codeIdMap);


        // -------------------------------------------------------------------------------------------------------------


        // 计算RPS
        Map<String, double[]> RPS10 = TdxExtDataFun.computeRPS(stockDateArrMap, stockCloseArrMap, 10);
        Map<String, double[]> RPS20 = TdxExtDataFun.computeRPS(stockDateArrMap, stockCloseArrMap, 20);
        Map<String, double[]> RPS50 = TdxExtDataFun.computeRPS(stockDateArrMap, stockCloseArrMap, 50);
        Map<String, double[]> RPS120 = TdxExtDataFun.computeRPS(stockDateArrMap, stockCloseArrMap, 120);// 120 -> 100
        Map<String, double[]> RPS250 = TdxExtDataFun.computeRPS(stockDateArrMap, stockCloseArrMap, 250);// 250 -> 200


        // -------------------------------------------------------------------------------------------------------------


        // save -> DB


        //


        stockDateArrMap.keySet().parallelStream().forEach(stockCode -> {
            String[] date_arr = stockDateArrMap.get(stockCode);


            double[] rps10_arr = RPS10.get(stockCode);
            double[] rps20_arr = RPS20.get(stockCode);
            double[] rps50_arr = RPS50.get(stockCode);
            double[] rps120_arr = RPS120.get(stockCode);
            double[] rps250_arr = RPS250.get(stockCode);


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
            stockDO.setId(codeIdMap.get(stockCode));
            stockDO.setExtDataHis(JSON.toJSONString(extDatas));

            try {
                baseStockService.updateById(stockDO);
                System.out.println();
            } catch (Exception e) {
                log.error("stockDO : {} , exMsg : {}", JSON.toJSONString(stockDO), e.getMessage(), e);
                System.out.println();
            }
        });
    }


    @Override
    public void calcBlockRps() {


    }


    /**
     * 从本地DB   加载全部（5000+支）个股的 收盘价序列
     *
     * @return stock - close_arr
     */
    private void loadAllStockKline(Map<String, double[]> stockCloseArrMap,
                                   Map<String, String[]> stockDateArrMap,
                                   Map<String, Long> codeIdMap) {


        // Map<String, double[]> stockCloseArrMap = Maps.newHashMap();


        // 加载  最近500日   行情数据
        int DAY_LIMIT = 500;


        List<BaseStockDO> baseStockDOList = baseStockService.listAllKline();
        baseStockDOList.forEach(e -> {

            Long stockId = e.getId();
            String stockCode = e.getCode();

            double[] close_arr = ConvertStockKline.fieldValArr(e.getKlineDTOList(), "close");
            String[] date_arr = ConvertStockKline.strFieldValArr(e.getKlineDTOList(), "date");


            // 上市1年
            if (close_arr.length > 200) {


                stockCloseArrMap.put(stockCode, close_arr);
                stockDateArrMap.put(stockCode, date_arr);

                codeIdMap.put(stockCode, stockId);
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