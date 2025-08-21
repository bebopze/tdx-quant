package com.bebopze.tdx.quant.parser.writer;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 读取/写回     通达信 - 自定义板块（股票池/板块池）
 *
 * @author: bebopze
 * @date: 2025/5/25
 */
@Slf4j
public class TdxBlockNewReaderWriter {


    private static final String baseFilePath = TDX_PATH + "/T0002/blocknew/";


    public static void main(String[] args) {


        write("IDEA-test", Lists.newArrayList("300059", "002594", "300353"));
        List<String> codeList = read("IDEA-test");


        System.out.println(codeList);


        // TODO   清理缓存文件     ->     .IDEA-test.blk（隐藏文件）
        // delCacheHideFile("IDEA-test");
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 股票池子/板块池子（策略结果）   ->   readFrom 通达信
     *
     * @param blockNewCode 自定义板块（板块池/个股池）- code
     */
    public static List<String> read(String blockNewCode) {

        //   /T0002/blocknew/IDEA-test.blk
        String filePath = baseFilePath + blockNewCode + ".blk";


        List<String> codeList = Lists.newArrayList();

        try {

            List<String> lines = FileUtils.readLines(new File(filePath), "UTF-8");
            if (CollectionUtils.isEmpty(lines)) {
                return Collections.emptyList();
            }


            for (String s : lines) {
                String line = s.trim().replaceAll(" ", "");


                // 处理每一行
                if (StringUtils.isNotBlank(line)) {


                    // 0300059     -7位->     交易所：012   +   个股/板块 code
                    // 如果是 板块   ->   全部：1-沪市


                    if (line.length() < 7) {
                        log.warn("line : {}", line);
                        continue;
                    }


                    // -----------------------------------


                    String code = line.substring(1);
                    codeList.add(code);
                }
            }


            log.info("read   suc     >>>     blockNewCode : {} , filePath : {},   stockOrBlockCodeList : {}",
                     blockNewCode, filePath, JSON.toJSONString(codeList));


        } catch (Exception e) {

            log.error("read   err     >>>     blockNewCode : {} , filePath : {}, stockOrBlockCodeList : {} , errMsg : {}",
                      blockNewCode, filePath, JSON.toJSONString(codeList), e.getMessage(), e);
        }


        return codeList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 股票池子/板块池子（策略结果）   ->   写回 通达信
     *
     * @param blockNewCode         自定义板块（板块池/个股池）- code
     * @param stockOrBlockCodeList 个股/板块 - code列表
     */
    public static void write(String blockNewCode, Collection<String> stockOrBlockCodeList) {

        //   /T0002/blocknew/IDEA-test.blk
        String filePath = baseFilePath + blockNewCode + ".blk";


        List<String> codeList = stockOrBlockCodeList.stream().map(code -> {
            // 0300059     -7位->     交易所：012   +   个股/板块 code

            // 如果是 板块   ->   全部：1-沪市
            return StockMarketEnum.getTdxMarketType(code) + code;
        }).collect(Collectors.toList());


        try {
            // 覆盖写入
            FileUtils.writeLines(new File(filePath), "UTF-8", codeList, false);

            log.info("write   suc     >>>     blockNewCode : {} , filePath : {},   stockOrBlockCodeList : {}",
                     blockNewCode, filePath, JSON.toJSONString(stockOrBlockCodeList));


        } catch (IOException e) {

            log.error("write   err     >>>     blockNewCode : {} , filePath : {},   errMsg : {}",
                      blockNewCode, filePath, e.getMessage(), e);
        }
    }


}