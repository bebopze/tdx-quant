package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.mapper.BaseStockMapper;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 自定义指标  -  check
 *
 *
 * -   通达信  ->  自定义指标  ->  34[数据导出]
 *
 *
 * -   行情数据   export目录：/new_tdx/T0002/export/
 *
 * @author: bebopze
 * @date: 2025/6/2
 */
@Slf4j
public class TdxFunCheckReportParser {


    public static void main(String[] args) {


        // ----------


        String stockCode_sz = "000001";
        String stockCode_sh = "600519";
        String stockCode_bj = "833171";

        String stockCode_bk = "880904";

        String stockCode_zs_sz = "399106";
        String stockCode_zs_sh = "880003";


        List<TdxFunResultDTO> stockDataList = parseByStockCode("300059");
        for (TdxFunResultDTO row : stockDataList) {
            // String[] item = {e.getCode(), String.valueOf(e.getTradeDate()), String.format("%.2f", e.getOpen()), String.format("%.2f", e.getHigh()), String.format("%.2f", e.getLow()), String.format("%.2f", e.getClose()), String.valueOf(e.getAmount()), String.valueOf(e.getVol()), String.format("%.2f", e.getChangePct())};
            System.out.println(JSON.toJSONString(row));
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
            List<TdxFunResultDTO> java__rowList = calcFromJava(stockCode);


            // check
            check(tdx__rowList, java__rowList);


            return tdx__rowList;


        } catch (Exception e) {
            log.error("parseByFilePath   err     >>>     stockCode : {} , filePath : {} , exMsg : {}",
                      stockCode, filePath, e.getMessage(), e);
        }


        return Lists.newArrayList();
    }


    private static List<TdxFunResultDTO> calcFromJava(String stockCode) {
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

            dto.setDate(DateTimeUtil.parseDate_yyyy_MM_dd(dateStr));
            dto.setOpen(open_arr[i]);
            dto.setHigh(high_arr[i]);
            dto.setLow(low_arr[i]);
            dto.setClose(close_arr[i]);
            dto.setVol(vol_arr[i]);


            // -------------------------------- 基础指标（系统）

            dto.setMA5(MA5[i]);
            dto.setMA10(MA10[i]);
            dto.setMA20(MA20[i]);
            dto.setMA50(MA50[i]);
            dto.setMA100(MA100[i]);
            dto.setMA200(MA200[i]);


            dto.setMACD(MACD[i]);
            dto.setDIF(DIF[i]);
            dto.setDEA(DEA[i]);


            // dto.setSAR(SAR[i]);


            // -------------------------------- 简单指标


            dto.setMA20多(bool2Int(MA20多[i]));
            dto.setMA20空(bool2Int(MA20空[i]));

            dto.setSSF(ssf_arr[i]);
            dto.setSSF多(bool2Int(SSF多[i]));
            dto.setSSF空(bool2Int(SSF空[i]));


            // -------------------------------- 高级指标


            dto.set_60日新高(bool2Int(_60日新高_arr[i]));
            dto.set均线预萌出(bool2Int(均线预萌出_arr[i]));
            dto.set均线萌出(bool2Int(均线萌出_arr[i]));


            // dto.set大均线多头(bool2Int(大均线多头_arr[i]));


            // -------------------------------- 复杂指标


            // dto.set月多(bool2Int(月多_arr[i]));
            // dto.setRPS三线红(bool2Int(RPS三线红_arr[i]));


            dtoList.add(dto);
        }


        return dtoList;
    }


    private static void check(List<TdxFunResultDTO> tdx__rowList, List<TdxFunResultDTO> java__rowList) {


        TdxFunResultDTO dto = new TdxFunResultDTO();

    }


    /**
     * tdx 盘后数据（xx.day）   -   解析器
     *
     * @param filePath 文件路径     -    /new_tdx/T0002/export/
     * @return
     */
    public static List<TdxFunResultDTO> parseByFilePath(String filePath) {


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
                if (StringUtils.hasText(line)) {


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


                        if (!StringUtils.hasText(v)) {
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


    private static BigDecimal of(Number val) {
        return new BigDecimal(String.valueOf(val)).setScale(2, RoundingMode.HALF_UP);
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


        //


    }


}
