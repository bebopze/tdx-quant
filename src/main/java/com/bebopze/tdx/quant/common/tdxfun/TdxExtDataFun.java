package com.bebopze.tdx.quant.common.tdxfun;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.FastJson2Config;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.parser.tdxdata.LdayParser;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.bebopze.tdx.quant.service.impl.ExtDataServiceImpl.fillNaN;


/**
 * 通达信 - 扩展数据                           Java实现
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@Slf4j
public class TdxExtDataFun {


    public static void test1() {

        double[] arr = {3.0, 2.0, 1.0};


        double[] doubles1 = fillNaN(arr, 0);
        double[] doubles2 = fillNaN(arr, 1);
        double[] doubles3 = fillNaN(arr, 2);
        double[] doubles4 = fillNaN(arr, 3);
        double[] doubles5 = fillNaN(arr, 10);
        // double[] doubles6 = fillNaN(arr, -1);


        System.out.println(JSON.toJSONString(doubles1));
        System.out.println(JSON.toJSONString(doubles2));
        System.out.println(JSON.toJSONString(doubles3));
        System.out.println(JSON.toJSONString(doubles4));
        System.out.println(JSON.toJSONString(doubles5));


        double[] fillNaN_arr = doubles5;
        if (Double.isNaN(fillNaN_arr[0])) {
            double v = fillNaN_arr[0];

            System.out.println("fillNaN     >>>     v : " + v);

            System.out.println(Objects.equals(v, Double.NaN));
        }
    }

    public static void main(String[] args) {
        FastJson2Config fastJson2Config = new FastJson2Config();


        test1();


        // 从本地DB   加载5000支个股的收盘价序列
        AllStockKlineDTO dto = loadAllStockKline();
        Map<String, String[]> stockDateArrMap = dto.stockDateArrMap;
        Map<String, double[]> stockCloseArrMap = dto.stockCloseArrMap;
        Map<String, Long> codeIdMap = dto.codeIdMap;


        Map<String, double[]> RPS_N = computeRPS(stockDateArrMap, stockCloseArrMap, 50);
        System.out.println(JSON.toJSONString(RPS_N));


        // 打印某只股票的最近几日 RPS
        String stockCode = "300059";

        double[] rps_val = RPS_N.get(stockCode);
        System.out.println("rps_val : " + JSON.toJSONString(rps_val));


        System.out.println("最近10日 " + stockCode + " RPS: " +
                                   Arrays.toString(Arrays.copyOfRange(RPS_N.get(stockCode), RPS_N.get(stockCode).length - 10, RPS_N.get(stockCode).length))
        );
    }


    /**
     * 计算 RPS   ->   save2DB
     */
    public static void calcRps() {

        // 从本地DB   加载5000支个股的收盘价序列
        AllStockKlineDTO dto = loadAllStockKline();
        Map<String, String[]> stockDateArrMap = dto.stockDateArrMap;
        Map<String, double[]> stockCloseArrMap = dto.stockCloseArrMap;
        // Map<String, Long> codeIdMap = dto.codeIdMap;

        Map<String, double[]> RPS50 = computeRPS(stockDateArrMap, stockCloseArrMap, 50);
        Map<String, double[]> RPS120 = computeRPS(stockDateArrMap, stockCloseArrMap, 120); // 120 -> 100
        Map<String, double[]> RPS250 = computeRPS(stockDateArrMap, stockCloseArrMap, 250); // 250 -> 200


        // save -> DB
        IBaseStockService baseStockService = MybatisPlusUtil.getBaseStockService();
        Map<String, Long> codeIdMap = baseStockService.codeIdMap();


        List<BaseStockDO> baseStockDOList = Lists.newArrayList();
        RPS50.forEach((stockCode, v) -> {

            BaseStockDO baseStockDO = new BaseStockDO();
            baseStockDO.setId(codeIdMap.get(stockCode));
            // baseStockDO.setRps();

            baseStockDOList.add(baseStockDO);
        });
        baseStockService.updateBatchById(baseStockDOList, 500);


        // TODO   refresh cache
    }


    @Data
    @AllArgsConstructor
    public static class AllStockKlineDTO {
        // code - date_arr
        Map<String, String[]> stockDateArrMap;
        // code - close_arr
        Map<String, double[]> stockCloseArrMap;
        // code - id
        Map<String, Long> codeIdMap;
    }

    /**
     * 从本地DB   加载全部（5000+支）个股的 收盘价序列
     *
     * @return stock - close_arr
     */
    public static AllStockKlineDTO loadAllStockKline() {
        // code - date_arr
        Map<String, String[]> stockDateArrMap = Maps.newHashMap();
        // code - close_arr
        Map<String, double[]> stockCloseArrMap = Maps.newHashMap();
        // code - id
        Map<String, Long> codeIdMap = Maps.newHashMap();


        // 加载  最近500日   行情数据
        int DAY_LIMIT = 500;


        BaseStockMapper mapper = MybatisPlusUtil.getMapper(BaseStockMapper.class);


        List<BaseStockDO> baseStockDOList = mapper.listAllKline();
        baseStockDOList.forEach(e -> {

            String stockCode = e.getCode();
            String[] date_arr = ConvertStockKline.strFieldValArr(e.getKlineDTOList(), "date");
            double[] close_arr = ConvertStockKline.fieldValArr(e.getKlineDTOList(), "close");


            // 上市1年
            if (close_arr.length > 200) {
                stockCloseArrMap.put(stockCode, fillNaN(close_arr, DAY_LIMIT));


                double[] fillNaN_arr = stockCloseArrMap.get(stockCode);
                if (Double.isNaN(fillNaN_arr[0])) {
                    log.debug("fillNaN     >>>     stockCode : {}", stockCode);
                }
            }
        });


        return new AllStockKlineDTO(stockDateArrMap, stockCloseArrMap, codeIdMap);
    }


    private List<double[]> closr_arr__list;


    public void initData() {

        List<LdayParser.LdayDTO> ldayDTOS = LdayParser.parseByStockCode("");


        // From DB     ->     klines
        this.closr_arr__list = null;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  基础指标
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 计算  全市场个股   N日RPS   序列     （RPS = N日涨幅 -> 总排名百分位）            calculateNDayRPS
     *
     *
     * 计算  N日 RPS：
     *
     * 1、计算  全市场个股（假设共5000只） 的   N日涨幅
     * 2、全部个股 N日涨幅   从小到大排序
     * 3、个股N日RPS : 个股N日涨幅 在总排名 中的百分比（0–100）
     *
     *
     * -
     *
     * @param stockCloseArrMap 全市场收盘价，key=股票代码，value=按时间顺序的收盘价数组
     * @param N                计算涨幅的周期（天数）
     * @return key=股票代码，value=该股按时间序列的 N日 RPS（0–100）
     */

    /**
     * 计算全市场个股的 N 日 RPS 序列。
     *
     * @param N                N 日涨幅周期
     * @param stockDateArrMap  Map<股票代码, String[] 日期序列>，日期按升序排列
     * @param stockCloseArrMap Map<股票代码, double[] 收盘价序列>，与日期数组一一对应
     * @return Map<股票代码, double [ ]>     double[] 与该股日期序列长度一致，若无 RPS 则为 NaN
     */
    public static Map<String, double[]> computeRPS(Map<String, String[]> stockDateArrMap,
                                                   Map<String, double[]> stockCloseArrMap,
                                                   int N) {


//        Map<String, TreeMap<String, Double>> stockPriceMap = Maps.newHashMap();


        // 1. 为每个股票构建：Map<日期, N日涨幅>，并同时收集所有“有效涨幅”的日期
        Map<String, TreeMap<String, Double>> returnsMap = new HashMap<>();
        Set<String> allDates = new TreeSet<>();  // 按自然升序保存所有出现过的涨幅日期


//        TreeMap<String, Double> datePctMap = new TreeMap<>();
//
//
//        stockPriceMap.forEach((code, dateCloseMap) -> {
//
//
//            int size = dateCloseMap.size();
//            double prev = Double.NaN;
//            if (size > N) {
//
//                for (Map.Entry<String, Double> entry : dateCloseMap.entrySet()) {
//                    String date = entry.getKey();
//                    Double close = entry.getValue();
//
//
//                    if (!Double.isNaN(prev)) {
//                        double pct = (close / prev - 1.0) * 100.0;
//                        datePctMap.put(date, pct);
//                        allDates.add(date);
//                    }
//                    prev = close;
//                    returnsMap.put(code, datePctMap);
//                }
//            }
//        });

        for (String code : stockDateArrMap.keySet()) {
            String[] dates = stockDateArrMap.get(code);
            double[] closes = stockCloseArrMap.get(code);
            TreeMap<String, Double> codeReturns = new TreeMap<>();

            if (dates.length > N) {
                for (int i = N; i < dates.length; i++) {
                    double prev = closes[i - N];
                    if (prev != 0) {
                        double pct = (closes[i] / prev - 1.0) * 100.0;
                        String dt = dates[i];
                        codeReturns.put(dt, pct);
                        allDates.add(dt);
                    }
                }
            }
            returnsMap.put(code, codeReturns);
        }

        // 2. 为每个股票预分配 RPS 结果数组，长度与其日期序列一致，填充 NaN
        Map<String, double[]> rpsResult = new HashMap<>();
        Map<String, Map<String, Integer>> dateIndexMap = new HashMap<>();

        for (String code : stockDateArrMap.keySet()) {
            String[] dates = stockDateArrMap.get(code);
            int len = dates.length;
            double[] rpsArr = new double[len];
            Arrays.fill(rpsArr, Double.NaN);
            rpsResult.put(code, rpsArr);

            // 构建 日期->索引 映射，便于后续快速定位
            Map<String, Integer> idxMap = new HashMap<>();
            for (int i = 0; i < len; i++) {
                idxMap.put(dates[i], i);
            }
            dateIndexMap.put(code, idxMap);
        }

        // 3. 构建“按日期聚合所有股票涨幅”的结构：Map<日期, List< (code, pct) >>
        Map<String, List<Map.Entry<String, Double>>> dateToList = new TreeMap<>();
        for (String date : allDates) {
            dateToList.put(date, new ArrayList<>());
        }
        for (String code : returnsMap.keySet()) {
            TreeMap<String, Double> codeReturns = returnsMap.get(code);
            for (Map.Entry<String, Double> e : codeReturns.entrySet()) {
                String date = e.getKey();
                double pct = e.getValue();
                dateToList.get(date).add(new AbstractMap.SimpleEntry<>(code, pct));
            }
        }

        // 4. 对每个日期，按涨幅升序排序，计算 RPS 并写入对应股票的结果数组
        for (String date : dateToList.keySet()) {
            List<Map.Entry<String, Double>> list = dateToList.get(date);
            int m = list.size();
            if (m == 0) continue;

            list.sort(Comparator.comparingDouble(Map.Entry::getValue));

            if (m == 1) {
                String code = list.get(0).getKey();
                int idx = dateIndexMap.get(code).get(date);
                rpsResult.get(code)[idx] = 100.0;
            } else {
                for (int i = 0; i < m; i++) {
                    String code = list.get(i).getKey();
                    double rps = (i * 1.0 / (m - 1)) * 100.0;
                    int idx = dateIndexMap.get(code).get(date);
                    rpsResult.get(code)[idx] = rps;
                }
            }
        }

        return rpsResult;
    }


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  高级指标
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                                  复杂指标
    // -----------------------------------------------------------------------------------------------------------------


}