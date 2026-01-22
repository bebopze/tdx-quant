package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.util.SimpleFileMatcher;
import com.bebopze.tdx.quant.parser.writer.TdxBlockNewReaderWriter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;
import static com.bebopze.tdx.quant.parser.tdxdata.BlockReportParser.readLinesWithEncoding;


/**
 * 通达信   报表导出（ETF基金[TDX系统自带板块]   =>   行情报价[细分行业]）   -   解析
 * -
 * -   刷新港美 扩展市场行情（必须！-> 否则[细分行业] 加载不全）  ->   ETF基金（行情报价 面板 [TDX系统自带板块]）  ->   34（数据导出）  ->   格式文本文件（所有数据[显示的栏目]）
 *
 *
 *
 * -   行情报价（代码	名称	涨幅%	现价	开盘%	最高%	最低%	振幅%	换手%	开盘金额	总金额	一二级行业	[细分行业]	昨涨幅%	3日涨幅%	5日涨幅%	10日涨幅%	20日涨幅%	60日涨幅%	年初至今%	连涨天	）
 *
 *
 *
 * -
 *
 * @author: bebopze
 * @date: 2026/1/6
 */
@Slf4j
public class HyETFReportParser {


    /**
     * 行情报价（代码	名称	涨幅%	现价	开盘%	最高%	最低%	振幅%	换手%	开盘金额	总金额	一二级行业	[细分行业]	昨涨幅%	3日涨幅%	5日涨幅%	10日涨幅%	20日涨幅%	60日涨幅%	年初至今%	连涨天	）
     */
    private static final String filePath_ETF = TDX_PATH + "/T0002/export/ETF基金20260107.txt";


    private static final String basePath = TDX_PATH + "/T0002/export";
    private static final String fileName = "ETF基金";


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 1、解析 全量 [ETF基金] TXT报表
     * 2、根据 ETF -> [细分行业+name] 去重     =>     同一[细分行业+name]  ->  保留最大 AMO 的ETF
     *
     * @return
     */
    public static List<TdxETFDTO> parseAndDistinct() {


        // 解析 + 去重（细分行业）
        List<TdxETFDTO> exportTxtDTOList = parseAndDistinct_0();


        // 写回 tdx自定义板块   ->   行业ETF（HYETF）
        write2Tdx__distinct_HyETF(exportTxtDTOList);


        return exportTxtDTOList;
    }


    /**
     * 1、解析 全量 [ETF基金] TXT报表
     * 2、根据 ETF -> [细分行业/name] 去重     =>     同一[细分行业/name]  ->  保留最大 AMO 的ETF
     *
     * @return
     */
    private static List<TdxETFDTO> parseAndDistinct_0() {

        // 通达信 - ETF文件（行情报价 面板列表导出txt）
        File file_ETF = SimpleFileMatcher.getLastETFFile(basePath, fileName);


        // 解析（全量ETF   =>   系统板块[ETF基金]）
        List<TdxETFDTO> tdx_etf__rowList = parseByFilePath(file_ETF);


        // 去重1（细分行业）
        Map<String, TdxETFDTO> hy__rowMap = Maps.newHashMap();
        tdx_etf__rowList.stream()
                        .filter(e -> StringUtils.isNotBlank(e.细分行业))
                        .forEach(e -> {

                            String 细分行业 = e.细分行业;

                            TdxETFDTO old_row = hy__rowMap.computeIfAbsent(细分行业, k -> e);
                            // 同一[细分行业]   ->   筛选保留 最大金额ETF
                            if (e.getAmount() > old_row.getAmount()) {
                                log.info("同一[细分行业:{}] -> 筛选保留 最大金额ETF     >>>     old : {} , new : {}", 细分行业, old_row, e);
                                hy__rowMap.put(细分行业, e);
                            }
                        });

        // 去重2（name）
        Map<String, TdxETFDTO> name__rowMap = Maps.newHashMap();
        hy__rowMap.values().forEach(e -> {


            // 卫星ETF / 卫星ETF
            // 1、name -> 完全相同
            String name = e.name;


            // 2、name -> 不完全相同（模糊匹配）
            // 卫星ETF/卫星产业ETF、人工智能ETF/人工智能ETF易方达、信用债ETF基金/信用债ETF广发
            name = name.split("ETF")[0];
            // 光伏ETF / 光伏龙头ETF广发
            name = name.replace("龙头", "");
            // TODO   ->   有色ETF基金 / 有色金属ETF基金


            TdxETFDTO old_row = name__rowMap.computeIfAbsent(name, k -> e);
            // 同一[name]   ->   筛选保留 最大金额ETF
            if (e.getAmount() > old_row.getAmount()) {
                log.info("同一[name:{}] -> 筛选保留 最大金额ETF     >>>     old : {} , new : {}", name, old_row, e);
                name__rowMap.put(name, e);
            }
        });


        return name__rowMap.values()
                           .stream()
                           .sorted(Comparator.comparing(TdxETFDTO::getCode))   // code 正序
                           .collect(Collectors.toList());
    }


    private static void write2Tdx__distinct_HyETF(List<TdxETFDTO> exportTxtDTOList) {
        // ETF code列表（按照[细分行业] 去重后的）
        Set<String> ETF_codeSet = exportTxtDTOList.stream().map(TdxETFDTO::getCode).collect(Collectors.toSet());

        // 写回 tdx自定义板块   ->   行业ETF（HYETF）
        TdxBlockNewReaderWriter.write("HYETF", ETF_codeSet);
    }


    /**
     * tdx ETF 报表txt（ETF基金20260107.txt）   -   解析器
     *
     * @param file_ETF ETF文件     -    /new_tdx/T0002/export/ETF基金20260107.txt
     * @return
     */
    private static List<TdxETFDTO> parseByFilePath(File file_ETF) {


        List<TdxETFDTO> dtoList = Lists.newArrayList();


        String code = null;
        try {

            List<String> lines = readLinesWithEncoding(file_ETF, "GBK");
            if (CollectionUtils.isEmpty(lines) || lines.size() < 2) {
                return dtoList;
            }


            // 第1行   ->   标题
            String title = lines.get(0);
            String[] titleArr = title.trim().replaceAll(" ", "").split("\t");

            int length = titleArr.length;


            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim().replaceAll(" ", "");


                // 处理每一行
                if (StringUtils.isNoneBlank(line)) {


                    String[] strArr = line.trim().split("\t");

                    // 一二级行业、细分行业   可能为空
                    if (strArr.length < length - 2) {
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


//                        if (StringUtils.isBlank(v)) {
//                            fullData = false;
//                            break;
//                        }

                        row.put(k, v);
                    }


                    if (fullData) {
                        TdxETFDTO dto = convert2DTO(row);
                        dtoList.add(dto);

                        code = dto.getCode();
                    }
                }
            }


        } catch (Exception e) {
            log.error("err     >>>     code : {} , exMsg : {}", code, e.getMessage(), e);
        }


        return dtoList;
    }


    private static TdxETFDTO convert2DTO(JSONObject row) {
        TdxETFDTO dto = new TdxETFDTO();


        dto.setCode(row.getString("代码"));
        dto.setName(row.getString("名称"));


        dto.setChangePct(convertDoubleVal(row, "涨幅%"));
        dto.setClose(convertDoubleVal(row, "现价"));
        dto.setOpenPct(convertDoubleVal(row, "开盘%"));
        dto.setHighPct(convertDoubleVal(row, "最高%"));
        dto.setLowPct(convertDoubleVal(row, "最低%"));
        dto.setRangePct(convertDoubleVal(row, "振幅%"));
        dto.setTurnoverPct(convertDoubleVal(row, "换手%"));


        dto.setOpenAmount(convertDoubleVal(row, "开盘金额"));
        dto.setAmount(convertDoubleVal(row, "总金额"));


        dto.setHy__lv1_lv2(row.getString("一二级行业"));
        dto.set细分行业(row.getString("细分行业"));


        dto.set昨涨幅(convertDoubleVal(row, "昨涨幅%"));
        dto.setN3日涨幅(convertDoubleVal(row, "3日涨幅%"));
        dto.setN5日涨幅(convertDoubleVal(row, "5日涨幅%"));
        dto.setN10日涨幅(convertDoubleVal(row, "10日涨幅%"));
        dto.setN20日涨幅(convertDoubleVal(row, "20日涨幅%"));
        dto.setN60日涨幅(convertDoubleVal(row, "60日涨幅%"));
        dto.set年初至今(convertDoubleVal(row, "年初至今%"));
        dto.set连涨天((int) convertDoubleVal(row, "连涨天"));


        return dto;
    }

    private static double convertDoubleVal(JSONObject row, String key) {
        return Objects.equals(row.getString(key), "--") ? 0.0 : row.getDouble(key);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdxETFDTO implements Serializable {


        // 行情报价（代码	名称	涨幅%	现价	开盘%	最高%	最低%	振幅%	换手%	开盘金额	总金额	一二级行业	[细分行业]	昨涨幅%	3日涨幅%	5日涨幅%	10日涨幅%	20日涨幅%	60日涨幅%	年初至今%	连涨天	）


        // 代码
        private String code;
        // 名称
        private String name;

        // 涨幅%
        private double changePct;

        // 现价
        private double close;

        // 开盘%
        private double openPct;
        // 最高%
        private double highPct;
        // 最低%
        private double lowPct;

        // 振幅%
        private double rangePct;
        // 换手%
        private double turnoverPct;

        // 开盘金额
        private double openAmount;
        // 总金额
        private double amount;


        // 一二级行业
        private String hy__lv1_lv2;
        // 细分行业
        private String 细分行业;


        // 昨涨幅%
        private double 昨涨幅;

        // 3日涨幅%
        private double N3日涨幅;
        // 5日涨幅%
        private double N5日涨幅;
        // 10日涨幅%
        private double N10日涨幅;
        // 20日涨幅%
        private double N20日涨幅;
        // 60日涨幅%
        private double N60日涨幅;
        // 年初至今%
        private double 年初至今;
        // 连涨天
        private Integer 连涨天;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        // 通达信 - ETF [细分行业]
        List<TdxETFDTO> tdx__rowList = parseAndDistinct_0();


        System.out.println("\nsize : " + tdx__rowList.size() + "\n");


        tdx__rowList.forEach(row -> System.out.println(row.细分行业 + " : " + row + "\n"));
    }


}