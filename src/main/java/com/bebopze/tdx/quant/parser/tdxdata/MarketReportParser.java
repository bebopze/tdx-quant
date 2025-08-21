package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 通达信   报表导出（大盘指标   =>   880003 - 平均股价）   -   解析
 * -
 * -   34（数据导出）   ->   大盘指标EXCEL   ->   格式文本文件
 *
 *
 *
 * -   大盘指标（MA50占比 / 月多占比 / 板块-月多占比 / 新高-新低 / 板块-BS占比（左侧试仓/左侧买/右侧买/强势卖出/左侧卖/右侧卖） / ... / 大盘顶底）
 *
 *
 *
 * -
 *
 * @author: bebopze
 * @date: 2025/5/7
 */
@Slf4j
public class MarketReportParser {


    /**
     * 大盘指标（MA50占比 / 月多占比 / 板块-月多占比 / 新高-新低 / 板块-BS占比（左侧试仓/左侧买/右侧买/强势卖出/左侧卖/右侧卖） / ... / 大盘顶底）
     */
    private static final String filePath = TDX_PATH + "/T0002/export/880003.txt";


    public static void main(String[] args) {

        // 通达信 - 指标result
        List<TdxFunResultDTO> tdx__rowList = parse();


        System.out.println(JSON.toJSONString(tdx__rowList));
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static List<TdxFunResultDTO> parse() {
        // 通达信 - 指标result
        return parseByFilePath(filePath);
    }


    /**
     * tdx 报表txt（880003.txt）   -   解析器
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
            String[] titleArr = title.trim().replaceAll(" ", "").replaceAll("大盘指标EXCEL.", "").split("\t");

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


        dto.setCode(code);


        // ------------------------------------------------------ 固定：TDX 系统指标


        // 时间   开盘   最高   最低   收盘   成交量
        dto.setDate(DateTimeUtil.parseDate_yyyyMMdd__slash(row.getString("时间")));
        dto.setOpen(row.getDouble("开盘"));
        dto.setHigh(row.getDouble("最高"));
        dto.setLow(row.getDouble("最低"));
        dto.setClose(row.getDouble("收盘"));
        dto.setVol(row.getLong("成交量"));


        // ------------------------------------------------------ 自定义 指标


        // MA50占比   个股月多占比   板块月多占比
        dto.setMA50占比(row.getBigDecimal("MA50占比"));
        dto.set个股月多占比(row.getBigDecimal("个股月多占比"));
        dto.set板块月多占比(row.getBigDecimal("板块月多占比"));


        // ------------------------------------------------


        // 新高   新低   全A   差值
        dto.set新高(row.getInteger("新高"));
        dto.set新低(row.getInteger("新低"));
        dto.set全A(row.getInteger("全A"));
        dto.set差值(row.getInteger("差值"));


        // ------------------------------------------------


        // 左侧试仓_占比   左侧买_占比   右侧买_占比   强势卖出_占比   左侧卖_占比   右侧卖_占比   右侧B_占比   右侧S_占比
        dto.set左侧试仓_占比(row.getBigDecimal("左侧试仓_占比"));
        dto.set左侧买_占比(row.getBigDecimal("左侧买_占比"));
        dto.set右侧买_占比(row.getBigDecimal("右侧买_占比"));
        dto.set强势卖出_占比(row.getBigDecimal("强势卖出_占比"));
        dto.set左侧卖_占比(row.getBigDecimal("左侧卖_占比"));
        dto.set右侧卖_占比(row.getBigDecimal("右侧卖_占比"));
        dto.set右侧B_占比(row.getBigDecimal("右侧B_占比"));
        dto.set右侧S_占比(row.getBigDecimal("右侧S_占比"));


        // ------------------------------------------------


        // 大盘顶底_STATUS   底   顶   底_DAY   顶_DAY
        dto.set大盘顶底_STATUS(row.getInteger("大盘顶底_STATUS"));
        dto.set底(row.getInteger("底"));
        dto.set顶(row.getInteger("顶"));
        dto.set底_DAY(row.getInteger("底_DAY"));
        dto.set顶_DAY(row.getInteger("顶_DAY"));


        // ------------------------------------------------


        // 大盘牛熊
        dto.set牛市(row.getInteger("牛市"));
        dto.set熊市(row.getInteger("熊市"));


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


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TdxFunResultDTO implements Serializable {

        private String code;


        // ------------------------------------------------------ 固定：TDX 系统指标（行情数据）


        // -------------------------------- 日K


        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;

        private double open;
        private double high;
        private double low;
        private double close;

        private long vol;


        // ------------------------------------------------------ 自定义 指标


        // MA50占比   个股月多占比   板块月多占比
        private BigDecimal MA50占比;
        private BigDecimal 个股月多占比;
        private BigDecimal 板块月多占比;


        // 新高   新低   全A   差值
        private Integer 新高;
        private Integer 新低;
        private Integer 全A;
        private Integer 差值;


        // 左侧试仓_占比   左侧买_占比   右侧买_占比   强势卖出_占比   左侧卖_占比   右侧卖_占比   右侧B_占比   右侧S_占比
        private BigDecimal 左侧试仓_占比;
        private BigDecimal 左侧买_占比;
        private BigDecimal 右侧买_占比;
        private BigDecimal 强势卖出_占比;
        private BigDecimal 左侧卖_占比;
        private BigDecimal 右侧卖_占比;

        private BigDecimal 右侧B_占比;
        private BigDecimal 右侧S_占比;


        // 大盘顶底_STATUS   底   顶   底_DAY   顶_DAY
        private Integer 大盘顶底_STATUS;
        private Integer 底;
        private Integer 顶;
        private Integer 底_DAY;
        private Integer 顶_DAY;


        // 大盘牛熊
        private Integer 牛市;
        private Integer 熊市;
    }

}