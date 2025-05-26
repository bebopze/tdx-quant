package com.bebopze.tdx.quant.parser.writer;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 写回   通达信 - 自定义板块（股票池/板块池）
 *
 * @author: bebopze
 * @date: 2025/5/25
 */
@Slf4j
public class TdxBlockNewWriter {


    private static final String baseFilePath = TDX_PATH + "/T0002/blocknew/";


    public static void main(String[] args) {


        write("IDEA-test", Lists.newArrayList("300059", "002594", "300353"));


        // TODO   清理缓存文件     ->     .IDEA-test.blk（隐藏文件）
        // delCacheHideFile("IDEA-test");
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 股票池子/板块池子（策略结果）   ->   写回 通达信
     *
     * @param blockNewCode         自定义板块（板块池/个股池）- code
     * @param stockOrBlockCodeList 个股/板块 - code列表
     */
    public static void write(String blockNewCode, List<String> stockOrBlockCodeList) {

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
