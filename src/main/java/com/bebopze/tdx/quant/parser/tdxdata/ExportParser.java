package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 通达信   报表导出（自定义 报表）   -   解析
 *
 * @author: bebopze
 * @date: 2025/5/7
 */
@Slf4j
public class ExportParser {


    public static void main(String[] args) {

        // GB2312
        String filePath_export = TDX_PATH + "/T0002/export/全部Ａ股_20250430_1.txt";
        // GB2312
        String filePath_export2 = TDX_PATH + "/T0002/export/全部Ａ股_20250430_1.xls";
        // UTF-8
        String filePath_export3 = TDX_PATH + "/T0002/export/全部Ａ股_20250430_1.csv";


        List<String> strings = parse(filePath_export);
    }


    /**
     * 个股-所属板块   [报表]       解析
     * -
     * -
     *
     * @param filePath /new_tdx/T0002/export/全部Ａ股_20250430_1.txt
     * @return
     */
    public static List<String> parse(String filePath) {

        List<String> items = Lists.newArrayList();


        try {
            List<String> lines = FileUtils.readLines(new File(filePath), "GB2312");
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();


                // 历史行情.指标排序 全部Ａ股 周期:日线 日期:2025-04-30 三 指标:所属板块  前复权
                // 代码   	名称	 涨幅%	 收盘	总金额	一级研究行业	行业CODE	行业NAME	细分行业	概念板块	深沪京_012	融资融券	_20CM_1234	上市日期	上市天数	股东人数
                if (i < 2) {
                    continue;
                }


                // 处理每一行
                if (StringUtils.hasText(line)) {
                    items.add(line);


                    // String[] strArr = line.trim().replaceAll("\\t", ",").split(",");
                    String[] strArr = line.trim().split("\\t");


                    // 920068	天工股份
                    if (strArr.length < 3) {
                        log.warn("ExportParser#parse   err     >>>     行号 : {} , line : {}", i, line);
                        continue;
                    }


                    // 代码
                    String stockCode = strArr[0];
                    // 名称
                    String stockName = strArr[1];


                    // 涨幅%
                    String zf = strArr[2];
                    // 收盘
                    String closePrice = strArr[3];
                    // 总金额
                    String amount = strArr[4];


                    // 一级研究行业
                    String level1HyName = strArr[5];
                    // 行业CODE
                    String hyCode = strArr[6];
                    // 行业NAME
                    String hyName = strArr[7];
                    // 细分行业 - name（3级 - 研究/细分）
                    String xfhyName = strArr[8];


                    // 概念板块
                    String gnBlockName = strArr[9];


                    // 深沪京_012
                    String hsj = strArr[10];
                    // 融资融券
                    String rzrq = strArr[11];
                    // _20CM_1234
                    String _20cm = strArr[12];
                    // 上市日期
                    String ss_date = strArr[13];
                    // 上市天数
                    String ss_day_num = strArr[14];
                    // 股东人数
                    String gg_num = strArr[15];


                    System.out.println(JSON.toJSONString(strArr));
                }
            }


        } catch (IOException e) {

            log.error("ExportParser#parse err     >>>     filePath : {},   errMsg : {}", filePath, e.getMessage(), e);
        }


        log.error("ExportParser#parse suc     >>>     filePath : {},   totalNum : {}", filePath, items.size());
        return items;
    }

}