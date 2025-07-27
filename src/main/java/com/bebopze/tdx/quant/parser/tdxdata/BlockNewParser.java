package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;
import static com.bebopze.tdx.quant.common.util.DateTimeUtil.millis2Time;


/**
 * 通达信   自定义板块   xxx.blk     -     读 + 写
 * -
 * -   /T0002/blocknew/xxx.blk               自定义 - 板块 （公式选股 - 股票池子）
 *
 * @author: bebopze
 * @date: 2025/5/9
 */
@Slf4j
public class BlockNewParser {


    private static final String baseFilePath = TDX_PATH + "/T0002/blocknew/";

    private static final String filePath = baseFilePath + "IDEA-test.blk";


    /**
     * 自定义板块 - 行业ETF
     */
    private static final String ETF_filePath = baseFilePath + "HYETF.blk";


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        // parseAll();


        // parse(filePath);


        List<BlockNewDTO> dtoList = parse_ETF();
        dtoList.forEach(e -> System.out.println(JSON.toJSONString(e)));


        System.out.println();
        System.out.println("---------------------------------------");
        System.out.println();


//        write(Lists.newArrayList("0000001", "0000002", "0000007"));
//
//
//        parse(filePath);
    }


    public static void parseAll() {


        File dir = new File(baseFilePath);
        File[] blkFiles = dir.listFiles((d, name) -> name.endsWith(".blk"));


        for (File blkFile : blkFiles) {

            String absolutePath = blkFile.getAbsolutePath();
            long lastModified = blkFile.lastModified();
            LocalDateTime time = millis2Time(lastModified);


            List<BlockNewDTO> dtoList = parse(blkFile.getAbsolutePath());


            System.out.println(String.format("%s   -   %s",
                                             absolutePath.replace("baseFilePath", ""),
                                             JSON.toJSONString(dtoList)));
        }

    }


    /**
     * 自定义板块 - 行业ETF
     *
     * @return
     */
    public static List<BlockNewDTO> parse_ETF() {
        return parse(ETF_filePath);
    }


    /**
     * 股票池子   -   read
     * -
     * - /T0002/blocknew/xxx.blk
     *
     * @return
     */
    public static List<BlockNewDTO> parse(String filePath) {

        List<BlockNewDTO> dtoList = Lists.newArrayList();


        try {
            List<String> lines = FileUtils.readLines(new File(filePath), "UTF-8");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);


                // 1603985
                // 0000631
                // 0000605


                // 处理每一行
                if (StringUtils.hasText(line) && line.length() == 7) {


                    try {
                        // 0-深；1-沪；2-北；
                        Integer marketType = Integer.valueOf(line.substring(0, 1));
                        // 个股code
                        String stockCode = line.substring(1, 7);


                        BlockNewDTO dto = new BlockNewDTO(marketType, stockCode);
                        dtoList.add(dto);


                    } catch (Exception e) {
                        log.error("parse ex     >>>     filePath : {} , i : {} , line : {} , exMsg : {}",
                                  filePath.split("blocknew")[1], i, line, e.getMessage(), e);
                    }


                } else {

                    log.error("parse ex     >>>     filePath : {} , i : {} , line : {}",
                              filePath.split("blocknew")[1], i, line);
                }
            }


            return dtoList;


        } catch (IOException e) {

            log.error("BlockNewParser#parse   err     >>>     filePath : {},   errMsg : {}", filePath, e.getMessage(), e);

            return null;
        }
    }


    /**
     * 股票池子   -   write
     *
     * @param stockCodeList
     */
    public static void write(List<Object> stockCodeList) {

        try {
            FileUtils.writeLines(new File(filePath), "UTF-8", stockCodeList, true);

        } catch (IOException e) {

            log.error("BlockNewParser#write   err     >>>     filePath : {},   errMsg : {}", filePath, e.getMessage(), e);
        }
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class BlockNewDTO {
        private Integer tdxMarketType;
        private String stockCode;
    }

}