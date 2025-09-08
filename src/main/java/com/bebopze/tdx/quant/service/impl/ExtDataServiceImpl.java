package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtDataFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
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

    @Autowired
    private InitDataService initDataService;


    @TotalTime
    @Override
    public void refreshExtDataAll(Integer N) {
        calcBlockExtData(null);
        calcStockExtData(N);
    }


    @TotalTime
    @Override
    public void calcStockExtData(Integer N) {


        N = StockUtil.N(N);


        // -------------------------------------------------------------------------------------------------------------

        // 预加载 全量行情
        // 从本地DB   加载全部（5000+支） 个股的   收盘价序列/日期序列/ code-id


        // 预加载 -> 解析数据
        DataDTO data = loadAllStockKline(N);


        // -------------------------------------------------------------------------------------------------------------


        // RPS
        stockTask__RPS(data);

        // 扩展数据
        stockTask__extData(data, N);


        // -------------------------------------------------------------------------------------------------------------


        // del Cache
        initDataService.deleteCache();
    }


    @TotalTime
    @Override
    public void calcBlockExtData(Integer N) {


        // -------------------------------------------------------------------------------------------------------------

        // 从本地DB   加载全部（380+支） 板块的   收盘价序列/日期序列/ code-id


        // 预加载 -> 解析数据
        DataDTO data = loadAllBlockKline();


        // -------------------------------------------------------------------------------------------------------------


        // RPS
        blockTask__RPS(data);

        // 扩展数据
        blockTask__extData(data);


        // -------------------------------------------------------------------------------------------------------------


        // del Cache
        initDataService.deleteCache();
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 从本地DB   加载全部（5000+支）个股的 收盘价序列
     *
     * @param N
     * @return stock - close_arr
     */
    private DataDTO loadAllStockKline(Integer N) {
        DataDTO data = new DataDTO();


        data.stockDOList = baseStockService.listAllKline();
        data.stockDOList = data.stockDOList.parallelStream().filter(e -> StringUtils.isNotBlank(e.getKlineHis())).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // 增量更新     =>     kline_his（计算参数 -> 需截取） / ext_data_his（被计算 -> 无需截取）    =>     截取 最后N条
        if (StockUtil.incrUpdate(N)) {


            data.stockDOList.parallelStream().forEach(e -> {


                // klineHis   ->   最后N条
                List<KlineDTO> klineDTOList = ListUtil.lastN(e.getKlineDTOList(), N);

                e.setKlineHis(ConvertStockKline.dtoList2JsonStr(klineDTOList));


                // -----------------------------------------------------------------------------


//                // extDataHis -> 无需同步 截取（数据对齐）            ==>       此处 仅需用 kline_his  ->  计算 rps + ext_data
//
//
//                // extDataHis   ->   最后N条
//                List<ExtDataDTO> extDataDTOList = ListUtil.lastN(e.getExtDataDTOList(), N); // TODO   如需截取 也应该用 date 过滤
//
//                e.setExtDataHis(ConvertStockExtData.dtoList2JsonStr(extDataDTOList));
            });
        }


        // -------------------------------------------------------------------------------------------------------------


        // 空行情 过滤（时间段内 -> 未上市）
        // data.stockDOList = data.stockDOList.parallelStream().filter(e -> !Objects.equals("[]", e.getKlineHis())).collect(Collectors.toList());


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
            KlineArrDTO klineArrDTO = ConvertStock.kline__dtoList2Arr(klineDTOList);


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


        log.info("computeRPS - 个股     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start));


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
     * @param N
     */
    private void stockTask__extData(DataDTO data, Integer N) {
        AtomicInteger count = new AtomicInteger(0);
        long start = System.currentTimeMillis();


        // -----------------------------------------------------------------------------------------------


        data.stockDOList.parallelStream().forEach(stockDO -> {
            try {

                execCalcStockExtData(data, N, stockDO, count, start);

            } catch (Exception ex) {
                log.error("execCalcStockExtData - err     >>>     stockCode : {} , stockDO : {} , errMsg : {}",
                          stockDO.getCode(), JSON.toJSONString(stockDO), ex.getMessage(), ex);
            }
        });
    }

    private void execCalcStockExtData(DataDTO data, Integer N, BaseStockDO stockDO, AtomicInteger count, long start) {


        String code = stockDO.getCode();
        List<ExtDataDTO> new_extDataDTOList = data.extDataMap.get(code);     // 暂时 只计算了 RPS


        // --------------------------------------------------------


        // old
        List<ExtDataDTO> old_extDataDTOList = stockDO.getExtDataDTOList();


        // fill -> new_RPS（后续计算 RPS相关指标）
        stockDO.setExtDataHis(ConvertStockExtData.dtoList2JsonStr(new_extDataDTOList));


        // -------------------------------------------------------------------------------------------------------------
        long stock_start = System.currentTimeMillis();


        // 1、计算ExtData（序列值）    ->     2、convert（序列   ->   列表）
        calcExtData(new StockFun(code, stockDO), new_extDataDTOList, 85);


        log.info("stockFun 指标计算 - 个股 suc     >>>     code : {} , count : {} , stockTime : {} , totalTime : {}",
                 code, count.incrementAndGet(), DateTimeUtil.formatNow2Hms(stock_start), DateTimeUtil.formatNow2Hms(start));
        // -------------------------------------------------------------------------------------------------------------


        // ------------------------------------------------------------------------ 更新 -> DB


        // 比较新旧     ==>     old_list   =>   当日 已存在->覆盖     不存在->add
        compare__old_new__extData(old_extDataDTOList, new_extDataDTOList, N);


        // -------------------------


        BaseStockDO entity = new BaseStockDO();
        entity.setId(data.codeIdMap.get(code));

        // entity.setExtDataHis(JSON.toJSONString(ConvertStockExtData.dtoList2StrList(new_extDataDTOList))); // 覆盖更新
        entity.setExtDataHis(JSON.toJSONString(ConvertStockExtData.dtoList2StrList(old_extDataDTOList)));    // 增量更新


        baseStockService.updateById(entity);
    }

    /**
     * 1、计算ExtData（序列值）    ->     2、convert（序列   ->   列表）
     *
     * @param fun
     * @param extDataDTOList
     * @param RPS            个股-85/90/95          板块-87/92/97
     */
    private void calcExtData(StockFun fun, List<ExtDataDTO> extDataDTOList, int RPS) {


        // ---------------------------- 1、计算ExtData（序列值）


        // 计算 -> 指标
        // StockFun fun = new StockFun(stockDO.getCode(), stockDO);


        double[] SSF = fun.SSF();


        double[] 中期涨幅 = fun.中期涨幅N(20);
        int[] 趋势支撑线 = fun.趋势支撑线();
        double[] C_SSF_偏离率 = fun.C_SSF_偏离率();


        boolean[] 高位爆量上影大阴 = fun.高位爆量上影大阴();


        boolean[] MA20多 = fun.MA多(20);
        boolean[] MA20空 = fun.MA空(20);
        boolean[] SSF多 = fun.SSF多();
        boolean[] SSF空 = fun.SSF空();


        boolean[] N60日新高 = fun.N日新高(60);
        boolean[] N100日新高 = fun.N日新高(100);
        boolean[] 历史新高 = fun.历史新高();


        boolean[] 月多 = fun.月多();
        boolean[] 均线预萌出 = fun.均线预萌出();
        boolean[] 均线萌出 = fun.均线萌出();
        boolean[] 大均线多头 = fun.大均线多头();
        boolean[] 均线大多头 = fun.均线大多头();
        boolean[] 均线极多头 = fun.均线极多头();


//        boolean[] RPS红 = fun.RPS红(85);
//        boolean[] RPS一线红 = fun.RPS一线红(95);
//        boolean[] RPS双线红 = fun.RPS双线红(90);
//        boolean[] RPS三线红 = fun.RPS三线红(85);


        // 个股-85/90/95          板块-87/92/97
        boolean[] RPS红 = fun.RPS红(RPS);
        boolean[] RPS一线红 = fun.RPS一线红(RPS + 10);
        boolean[] RPS双线红 = fun.RPS双线红(RPS + 5);
        boolean[] RPS三线红 = fun.RPS三线红(RPS);


        // ---------------------------- 2、convert（序列   ->   列表）


        for (int i = 0; i < extDataDTOList.size(); i++) {
            ExtDataDTO dto = extDataDTOList.get(i);


            dto.setSSF(of(SSF[i], 3));


            dto.set中期涨幅(of(中期涨幅[i], 3));
            dto.set趋势支撑线(趋势支撑线[i]);
            dto.setC_SSF_偏离率(C_SSF_偏离率[i]);


            dto.set高位爆量上影大阴(高位爆量上影大阴[i]);


            dto.setMA20多(MA20多[i]);
            dto.setMA20空(MA20空[i]);
            dto.setSSF多(SSF多[i]);
            dto.setSSF空(SSF空[i]);


            dto.setN60日新高(N60日新高[i]);
            dto.setN100日新高(N100日新高[i]);
            dto.set历史新高(历史新高[i]);


            dto.set月多(月多[i]);
            dto.set均线预萌出(均线预萌出[i]);
            dto.set均线萌出(均线萌出[i]);
            dto.set大均线多头(大均线多头[i]);
            dto.set均线大多头(均线大多头[i]);
            dto.set均线极多头(均线极多头[i]);


            dto.setRPS红(RPS红[i]);
            dto.setRPS一线红(RPS一线红[i]);
            dto.setRPS双线红(RPS双线红[i]);
            dto.setRPS三线红(RPS三线红[i]);
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


            // （从 250日 开始） 遍历 new_extDataList   ->   逐日判断 是否已存在

            for (int i = 250; i < new_extDataList.size(); i++) {
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


        for (int i = 0; i < old__extDataDTOList.size(); i++) {
            ExtDataDTO dto = old__extDataDTOList.get(i);

            old_dateIndexMap.put(dto.getDate(), i);
        }


        return old_dateIndexMap;
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


            // ---------------------------------------------------------------------------------------------------------
            long block_start = System.currentTimeMillis();


            // 1、计算ExtData（序列值）    ->     2、convert（序列   ->   列表）
            calcExtData(new BlockFun(code, blockDO), dtoList, 87);


            log.info("blockFun 指标计算 - 板块 suc     >>>     code : {} , count : {} , blockTime : {} , totalTime : {}",
                     code, count.incrementAndGet(), DateTimeUtil.formatNow2Hms(block_start), DateTimeUtil.formatNow2Hms(start));
            // ---------------------------------------------------------------------------------------------------------


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


        // -----------------------------------------------------------------


        @Override
        public String toString() {
            // toString  ->  OOM
            return "DataDTO{" +
                    "extDataMap=" + extDataMap.size() +
                    ", stockDOList=" + stockDOList.size() +
                    ", blockDOList=" + blockDOList.size() +
                    ", codePriceMap=" + codePriceMap.size() +
                    ", codeDateMap=" + codeDateMap.size() +
                    ", codeCloseMap=" + codeCloseMap.size() +
                    ", codeIdMap=" + codeIdMap.size() +
                    ", codeEntityMap=" + codeEntityMap.size() +
                    '}';
        }
    }


}
