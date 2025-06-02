package com.bebopze.tdx.quant.parser.check;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * Java指标（TdxFun）  -   check       ==>       TdxFun指标  -  通达信指标          计算结果对比
 *
 *
 *
 * -   [通达信指标] 计算结果导出          =>          通达信   ->   自定义指标（副图公式）  ->   34[数据导出]
 *
 *
 * -   export目录：/new_tdx/T0002/export/
 *
 * @author: bebopze
 * @date: 2025/6/2
 */
@Slf4j
public class TdxFunCheck {


    public static void main(String[] args) {


        String stockCode_sz = "000001";
        String stockCode_sh = "600519";
        String stockCode_bj = "833171";

        String stockCode_bk = "880904";

        String stockCode_zs_sz = "399106";
        String stockCode_zs_sh = "880003";


        List<TdxFunResultDTO> stockDataList = parseByStockCode("300059");
        for (TdxFunResultDTO row : stockDataList) {
            // String[] item = {e.getCode(), String.valueOf(e.getTradeDate()), String.format("%.2f", e.getOpen()), String.format("%.2f", e.getHigh()), String.format("%.2f", e.getLow()), String.format("%.2f", e.getClose()), String.valueOf(e.getAmount()), String.valueOf(e.getVol()), String.format("%.2f", e.getChangePct())};
            // System.out.println(JSON.toJSONString(row));
        }


        // ----------


        System.out.println("---------------------------------- code：" + stockDataList.get(0).code + "     总数：" + stockDataList.size());
    }


    /**
     * tdx 盘后数据（xx.day）   -   解析器
     *
     * @param stockCode 证券代码
     * @return
     */
    @SneakyThrows
    public static List<TdxFunResultDTO> parseByStockCode(String stockCode) {


        String filePath = TDX_PATH + String.format("/T0002/export/%s.txt", stockCode);


        try {

            // 通达信 - 指标result
            List<TdxFunResultDTO> tdx__rowList = parseByFilePath(filePath);
            // Java - 指标result
            List<TdxFunResultDTO> java__rowList = calcByJava(stockCode);


            // check
            check(tdx__rowList, java__rowList);


            return tdx__rowList;


        } catch (Exception e) {
            log.error("parseByFilePath   err     >>>     stockCode : {} , filePath : {} , exMsg : {}",
                      stockCode, filePath, e.getMessage(), e);
        }


        return Lists.newArrayList();
    }


    private static void check(List<TdxFunResultDTO> tdx__rowList, List<TdxFunResultDTO> java__rowList) {

        // 起始日期
        LocalDate dateLine = tdx__rowList.get(0).getDate();
        java__rowList = java__rowList.stream().filter(e -> !e.getDate().isBefore(dateLine)).collect(Collectors.toList());


        // ----------


        Set<String> sucSet = Sets.newLinkedHashSet(Lists.newArrayList("close", "vol", "MA", "MACD", "SAR", "MA多", "MA空", "SSF", "SSF多", "SSF空", "N日新高", "均线预萌出", "均线萌出", "大均线多头", "月多", "RPS三线红"));
        Map<String, Integer> failCountMap = Maps.newHashMap();


        for (int i = 0; i < tdx__rowList.size(); i++) {
            TdxFunResultDTO dto1 = tdx__rowList.get(i);
            TdxFunResultDTO dto2 = java__rowList.get(i);


            LocalDate date = dto1.getDate();


            String jsonStr1 = JSON.toJSONString(dto1);
            String jsonStr2 = JSON.toJSONString(dto2);

            if (!StringUtils.equals(jsonStr1, jsonStr2)) {

                JSONObject json1 = JSON.parseObject(jsonStr1);
                JSONObject json2 = JSON.parseObject(jsonStr2);

                JSONObject diffFields = getDiffFields(json1, json2);
                log.error("check err     >>>     stockCode : {} , idx : {} , date : {} , diffFields : {}",
                          tdx__rowList.get(0).code, i, date, diffFields.toJSONString());

            } else {

                log.debug("check suc     >>>     stockCode : {} , idx : {} , date : {}",
                          tdx__rowList.get(0).code, i, date);
            }


            // ------------------------------------------------------ 固定：TDX 系统指标


            // C     ->     SUC
            if (!(equals(dto1.getClose(), dto2.getClose()) && equals(dto1.getVol(), dto2.getVol()))) {
                failCountMap.put("close", failCountMap.getOrDefault("close", 0) + 1);
                failCountMap.put("vol", failCountMap.getOrDefault("vol", 0) + 1);
            }


            // -------------------------------- 基础指标（系统）


            // MA     ->     SUC
            if (!(equals(dto1.getMA5(), dto2.getMA5(), 0.001) && equals(dto1.getMA10(), dto2.getMA10(), 0.001)
                    && equals(dto1.getMA20(), dto2.getMA20(), 0.001) && equals(dto1.getMA50(), dto2.getMA50(), 0.001)
                    && equals(dto1.getMA100(), dto2.getMA100(), 0.001) && equals(dto1.getMA200(), dto2.getMA200(), 0.001))) {

                failCountMap.put("MA", failCountMap.getOrDefault("MA", 0) + 1);
            }


            // MACD     ->     SUC
            if (!(equals(dto1.getMACD(), dto2.getMACD(), 0.015)
                    && equals(dto1.getDIF(), dto2.getDIF())
                    && equals(dto1.getDEA(), dto2.getDEA()))) {

                failCountMap.put("MACD", failCountMap.getOrDefault("MACD", 0) + 1);
            }


            // SAR     ->     FAIL
            if (equals(dto1.getSAR(), dto2.getSAR())) {
                log.info("check     >>>     SAR   suc");
            } else {
                failCountMap.put("SAR", failCountMap.getOrDefault("SAR", 0) + 1);
            }


            // -------------------------------- 简单指标


            // MA多     ->     SUC
            if (!equals(dto1.getMA20多(), dto2.getMA20多())) {
                failCountMap.put("MA多", failCountMap.getOrDefault("MA多", 0) + 1);
            }
            // MA空     ->     SUC
            if (!equals(dto1.getMA20空(), dto2.getMA20空())) {
                failCountMap.put("MA空", failCountMap.getOrDefault("MA空", 0) + 1);
            }


            // SSF     ->     SUC
            if (!equals(dto1.getSSF(), dto2.getSSF())) {
                failCountMap.put("SSF", failCountMap.getOrDefault("SSF", 0) + 1);
            }


            // SSF多     ->     SUC
            if (!equals(dto1.getSSF多(), dto2.getSSF多())) {
                failCountMap.put("SSF多", failCountMap.getOrDefault("SSF多", 0) + 1);
            }
            // SSF空     ->     SUC
            if (!equals(dto1.getSSF空(), dto2.getSSF空())) {
                failCountMap.put("SSF空", failCountMap.getOrDefault("SSF空", 0) + 1);
            }


            // -------------------------------- 高级指标


            // N日新高     ->     SUC
            if (!equals(dto1.get_60日新高(), dto2.get_60日新高())) {
                failCountMap.put("N日新高", failCountMap.getOrDefault("N日新高", 0) + 1);
            }


            // 均线预萌出     ->     SUC
            if (!equals(dto1.get均线预萌出(), dto2.get均线预萌出())) {
                failCountMap.put("均线预萌出", failCountMap.getOrDefault("均线预萌出", 0) + 1);
            }


            // 均线萌出     ->     SUC
            if (!equals(dto1.get均线萌出(), dto2.get均线萌出())) {
                failCountMap.put("均线萌出", failCountMap.getOrDefault("均线萌出", 0) + 1);
            }


            // 大均线多头     ->     SUC
            if (!equals(dto1.get大均线多头(), dto2.get大均线多头())) {
                failCountMap.put("大均线多头", failCountMap.getOrDefault("大均线多头", 0) + 1);
            }


            // -------------------------------- 复杂指标


            // 月多     ->     FAIL
            if (equals(dto1.get月多(), dto2.get月多())) {
                log.info("check suc     >>>     月多");
            } else {
                failCountMap.put("月多", failCountMap.getOrDefault("月多", 0) + 1);
            }


            // RPS三线红     ->     FAIL
            if (equals(dto1.getRPS三线红(), dto2.getRPS三线红())) {
                log.info("check suc     >>>     RPS三线红");
            } else {
                failCountMap.put("RPS三线红", failCountMap.getOrDefault("RPS三线红", 0) + 1);
            }
        }


        // ---------------------------------------------------------------


        Map<String, String> failPctMap = Maps.newHashMap();
        Set<String> failSet = Sets.newHashSet(failCountMap.keySet());


        failCountMap.forEach((k, count) -> {

            // 百分比（%）
            double failPct = (double) count / tdx__rowList.size() * 100;
            failPctMap.put(k, of(failPct) + "%");


            // 失败率 > 1%     =>     fail
            if (failPct > 1) {
                sucSet.remove(k);
            }
        });
        failSet.removeAll(sucSet);


        log.info("check suc      >>>     {}", JSON.toJSONString(sucSet));
        log.error("check fail     >>>     {}", JSON.toJSONString(failSet));

        System.out.println();

        log.error("check fail   -   count     >>>     total : {} , failCountMap : {}", tdx__rowList.size(), JSON.toJSONString(failCountMap));
        log.error("check fail   -   pct       >>>     failPctMap : {}", JSON.toJSONString(failPctMap));
    }


    private static JSONObject getDiffFields(JSONObject json1, JSONObject json2) {
        JSONObject result = new JSONObject();


        for (String key : json1.keySet()) {

            Object v1 = json1.get(key);
            Object v2 = json2.get(key);


            if (v2 instanceof Number) {

                BigDecimal _v1 = new BigDecimal(String.valueOf(v1));
                BigDecimal _v2 = new BigDecimal(String.valueOf(v2));


                if (!equals(_v1, _v2)) {

                    if (key.equals("MA100")) {
                        boolean equals = equals(_v1, _v2);
                        System.out.println();
                    }
                    JSONObject diff = new JSONObject();
                    diff.put("v1", v1);
                    diff.put("v2", v2);
                    result.put(key, diff);
                }


            } else {

                if (!Objects.equals(v1, v2)) {
                    JSONObject diff = new JSONObject();
                    diff.put("v1", v1);
                    diff.put("v2", v2);
                    result.put(key, diff);
                }
            }
        }


        return result;
    }

    private static boolean equals(Number a, Number b) {
        return equals(a, b, 0.0005);     // ±0.05% 误差
    }

    private static boolean equals(Number a, Number b, double precision) {
        if (Objects.equals(a, b)) {
            return true;
        }


        if (a == null || b == null) {
            // return Objects.equals(a, b);
            return false;
        }


        // 差值
        double diffVal = a.doubleValue() - b.doubleValue();
        boolean equal1 = NumUtil.between(diffVal, -0.001001, 0.001001);


        if (b.doubleValue() == 0) {
            return equal1;
        }


        // 百分比
        double val = a.doubleValue() / b.doubleValue();
        boolean equal2 = NumUtil.between(val, 1 - precision, 1 + precision);

        return equal1 || equal2;
    }


    // -----------------------------------------------------------------------------------------------------------------


    private static List<TdxFunResultDTO> calcByJava(String stockCode) {
        List<TdxFunResultDTO> dtoList = Lists.newArrayList();


        BaseStockMapper mapper = MybatisPlusUtil.getMapper(BaseStockMapper.class);
        BaseStockDO stockDO = mapper.getByCode(stockCode);


        StockFun fun = new StockFun(stockCode, stockDO);
        String[] date_arr = fun.getDate_arr();
        double[] close_arr = fun.getClose_arr();
        double[] ssf_arr = fun.getSsf_arr();


        double[] open_arr = fun.getOpen_arr();
        double[] high_arr = fun.getHigh_arr();
        double[] low_arr = fun.getLow_arr();
        long[] vol_arr = fun.getVol_arr();


        double[] MA5 = TdxFun.MA(close_arr, 5);
        double[] MA10 = TdxFun.MA(close_arr, 10);
        double[] MA20 = TdxFun.MA(close_arr, 20);
        double[] MA50 = TdxFun.MA(close_arr, 50);
        double[] MA100 = TdxFun.MA(close_arr, 100);
        double[] MA200 = TdxFun.MA(close_arr, 200);


        double[][] macd = TdxFun.MACD(close_arr);
        double[] DIF = macd[0];
        double[] DEA = macd[1];
        double[] MACD = macd[2];


        double[] SAR = null; //TdxFun.SAR();


        boolean[] MA20多 = fun.MA多(20);
        boolean[] MA20空 = fun.MA空(20);

        boolean[] SSF多 = fun.SSF多();
        boolean[] SSF空 = fun.SSF空();


        boolean[] _60日新高_arr = fun.N日新高(60);
        boolean[] 均线预萌出_arr = fun.均线预萌出();
        boolean[] 均线萌出_arr = fun.均线萌出();
        boolean[] 大均线多头_arr = fun.大均线多头();


        boolean[] 月多_arr = fun.月多();
        boolean[] RPS三线红_arr = fun.RPS三线红(80);


        // -------------------------------------------------------------------------------------------------------------


        for (int i = 0; i < date_arr.length; i++) {
            String dateStr = date_arr[i];


            TdxFunResultDTO dto = new TdxFunResultDTO();
            dto.setCode(stockCode);


            dto.setDate(DateTimeUtil.parseDate_yyyy_MM_dd(dateStr));
            dto.setOpen(open_arr[i]);
            dto.setHigh(high_arr[i]);
            dto.setLow(low_arr[i]);
            dto.setClose(close_arr[i]);
            dto.setVol(vol_arr[i]);


            // -------------------------------- 基础指标（系统）

            dto.setMA5(of(MA5[i]));
            dto.setMA10(of(MA10[i]));
            dto.setMA20(of(MA20[i]));
            dto.setMA50(of(MA50[i]));
            dto.setMA100(of(MA100[i]));
            dto.setMA200(of(MA200[i]));


            dto.setMACD(MACD[i]);
            dto.setDIF(DIF[i]);
            dto.setDEA(DEA[i]);


            // dto.setSAR(SAR[i]);


            // -------------------------------- 简单指标


            dto.setMA20多(bool2Int(MA20多[i]));
            dto.setMA20空(bool2Int(MA20空[i]));

            dto.setSSF(of(ssf_arr[i]));
            dto.setSSF多(bool2Int(SSF多[i]));
            dto.setSSF空(bool2Int(SSF空[i]));


            // -------------------------------- 高级指标


            dto.set_60日新高(bool2Int(_60日新高_arr[i]));
            dto.set均线预萌出(bool2Int(均线预萌出_arr[i]));
            dto.set均线萌出(bool2Int(均线萌出_arr[i]));


            dto.set大均线多头(bool2Int(大均线多头_arr[i]));


            // -------------------------------- 复杂指标


            // dto.set月多(bool2Int(月多_arr[i]));
            // dto.setRPS三线红(bool2Int(RPS三线红_arr[i]));


            dtoList.add(dto);
        }


        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * tdx 盘后数据（xx.day）   -   解析器
     *
     * @param filePath 文件路径     -    /new_tdx/T0002/export/
     * @return
     */
    private static List<TdxFunResultDTO> parseByFilePath(String filePath) {


        // 股票代码
        String code = parseCode(filePath);


        List<TdxFunResultDTO> dtoList = Lists.newArrayList();


        LocalDate date = null;
        try {

            List<String> lines = FileUtils.readLines(new File(filePath), "GB2312");
            if (CollectionUtils.isEmpty(lines) || lines.size() < 3) {
                return dtoList;
            }


            // 第3行   ->   标题
            String title = lines.get(2);
            String[] titleArr = title.trim().replaceAll(" ", "").replaceAll("指标CHECK.", "").split("\t");

            int length = titleArr.length;


            for (int i = 4; i < lines.size(); i++) {
                String line = lines.get(i).trim().replaceAll(" ", "");


                // 处理每一行
                if (StringUtils.isNoneBlank(line)) {


                    String[] strArr = line.trim().split("\t");

                    if (strArr.length < length) {
                        log.warn("line : {}", line);
                        continue;
                    }


                    // ----------------------------------- 自定义 指标


                    JSONObject row = new JSONObject();
                    // 完整 行数据
                    boolean fullData = true;


                    for (int j = 0; j < strArr.length; j++) {
                        String k = titleArr[j];
                        String v = strArr[j];


                        if (StringUtils.isBlank(v)) {
                            fullData = false;
                            break;
                        }

                        row.put(k, v);
                    }


                    if (fullData) {
                        TdxFunResultDTO dto = convert2DTO(code, row);
                        dtoList.add(dto);
                    }
                }
            }


        } catch (Exception e) {
            log.error("err     >>>     code : {} , date : {} , exMsg : {}", code, date, e.getMessage(), e);
        }


        return dtoList;
    }


    private static TdxFunResultDTO convert2DTO(String code, JSONObject row) {
        TdxFunResultDTO dto = new TdxFunResultDTO();


        // ------------------------------------------------ 固定：TDX 系统指标


        dto.setCode(code);


        // 时间	    开盘	    最高	    最低	    收盘	         成交量
        dto.setDate(DateTimeUtil.parseDate_yyyyMMdd__slash(row.getString("时间")));
        dto.setOpen(row.getDouble("开盘"));
        dto.setHigh(row.getDouble("最高"));
        dto.setLow(row.getDouble("最低"));
        dto.setClose(row.getDouble("收盘"));
        dto.setVol(row.getLong("成交量"));


        // ------------------------------------------------ 基础指标（系统）


        // ------- MA
        dto.setMA5(row.getDouble("MA5"));
        dto.setMA10(row.getDouble("MA10"));
        dto.setMA20(row.getDouble("MA20"));
        dto.setMA50(row.getDouble("MA50"));
        dto.setMA100(row.getDouble("MA100"));
        dto.setMA200(row.getDouble("MA200"));


        // ------- MACD
        dto.setMACD(row.getDouble("MACD"));
        dto.setDIF(row.getDouble("DIF"));
        dto.setDEA(row.getDouble("DEA"));


        // ------- SAR
        dto.setSAR(row.getDouble("_SAR"));


        // ------------------------------------------------ 简单指标


        // ------- MA20 多/空
        dto.setMA20多(row.getInteger("MA20多"));
        dto.setMA20空(row.getInteger("MA20空"));


        // ------- SSF
        dto.setSSF(row.getDouble("SSF"));
        dto.setSSF多(row.getInteger("SSF多"));
        dto.setSSF空(row.getInteger("SSF空"));


        // ------------------------------------------------ 高级指标


        dto.set_60日新高(row.getInteger("_60日新高"));
        dto.set均线预萌出(row.getInteger("均线预萌出"));
        dto.set均线萌出(row.getInteger("均线萌出"));
        dto.set大均线多头(row.getInteger("大均线多头"));


        // ------------------------------------------------ 复杂指标


        dto.set月多(row.getInteger("月多"));
        dto.setRPS三线红(row.getInteger("RPS三线红"));


        return dto;
    }


    /**
     * 解析   股票代码
     *
     * @param filePath 文件路径
     * @return 股票代码
     */
    private static String parseCode(String filePath) {
        //   .../export/000001.txt
        String[] arr = filePath.split("/");
        return arr[arr.length - 1].split("\\.")[0];
    }


    private static double of(Number val) {
        if (null == val || (val instanceof Double && Double.isNaN((Double) val))) return Double.NaN;
        return new BigDecimal(String.valueOf(val)).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private static Integer bool2Int(boolean bool) {
        return bool ? 1 : 0;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdxFunResultDTO implements Serializable {

        private String code;


        // ------------------------------------------------------ 固定：TDX 系统指标


        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        private double open;
        private double high;
        private double low;
        private double close;

        private long vol;


        // ------------------------------------------------------ 自定义 指标


        // -------------------------------- 基础指标（系统）


        // MA
        private Double MA5;
        private Double MA10;
        private Double MA20;
        private Double MA50;
        private Double MA100;
        private Double MA200;


        // MACD
        private Double MACD;
        private Double DIF;
        private Double DEA;


        // SAR
        private Double SAR;


        // -------------------------------- 简单指标


        // MA20 - 多/空
        private Integer MA20多;
        private Integer MA20空;


        // SSF
        private Double SSF;
        private Integer SSF多;
        private Integer SSF空;


        // -------------------------------- 高级指标


        // N日新高
        private Integer _60日新高;


        // 均线预萌出
        private Integer 均线预萌出;


        // 均线萌出
        private Integer 均线萌出;


        // 大均线多头
        private Integer 大均线多头;


        // -------------------------------- 复杂指标


        // 月多
        private Integer 月多;


        // RPS三线红
        private Integer RPS三线红;
    }


}
