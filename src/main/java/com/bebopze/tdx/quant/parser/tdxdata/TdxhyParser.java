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
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 通达信 tdxhy.cfg   -   解析
 * -
 * -   /T0002/hq_cache/tdxhy.cfg               个股 - 行业板块（研究行业 + 普通行业）
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
@Slf4j
public class TdxhyParser {


    private static final String filePath = TDX_PATH + "/T0002/hq_cache/tdxhy.cfg";


    public static void main(String[] args) {
        List<TdxhyDTO> dtoList = parse();
        System.out.println(JSON.toJSONString(dtoList));
    }


    /**
     * tdx 板块数据   ->     个股 - 行业BK（研究行业 + 普通/细分行业）
     * -
     * - /new_tdx/T0002/hq_cache/tdxhy.cfg
     *
     * @return
     */
    public static List<TdxhyDTO> parse() {
        List<TdxhyDTO> dtoList = Lists.newArrayList();


        try {
            List<String> lines = FileUtils.readLines(new File(filePath), "GB2312");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);


                // 0|000001|T1001|||X500102
                //
                // 1|900901||||


                // 处理每一行
                if (StringUtils.hasText(line)) {


                    // 0|000001|T1001|||X500102


                    String[] strArr = line.trim().split("\\|");
                    if (strArr.length < 6) {

                        // 1|900901||||
                        // 0|002710||||       ->       B股     无行业信息，直接忽略
                        log.error("TdxhyParser#parse   err     >>>     filePath : {} , 行数 : {} , line : {} ", filePath, i, line);
                        continue;
                    }


                    // 0-深A；1-沪A；2-京A；
                    Integer tdxMarketType = Integer.valueOf(strArr[0]);

                    // 000001
                    String stockCode = strArr[1];

                    // T1001    -   2-普通行业
                    String hyCode_T = strArr[2];


                    String x_3 = strArr[3];
                    String x_4 = strArr[4];


                    // X500102    -   12-研究行业
                    String hyCode_X = strArr[5];


                    TdxhyDTO dto = new TdxhyDTO(tdxMarketType, stockCode, hyCode_T, hyCode_X);
                    dtoList.add(dto);


                    System.out.println(JSON.toJSONString(dto));
                }
            }

        } catch (IOException e) {

            log.error("TdxhyParser#parse   err     >>>     filePath : {},   errMsg : {}", filePath, e.getMessage(), e);
        }


        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class TdxhyDTO {
        private Integer tdxMarketType;
        private String stockCode;
        private String hyCode_T;
        private String hyCode_X;
    }

}