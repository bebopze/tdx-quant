package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 概念板块 - 2级-普通行业/1级-研究行业
 *
 * - 格式为：
 * -        概念板块code,概念板块name,普通行业code,普通行业name,研究行业code,研究行业name
 * -        880519,碳中和,880456,环境保护,881469,环保
 *
 * @author: bebopze
 * @date: 2025/7/19
 */
@Slf4j
public class GnRelaHyParser {


    private static final String filePath = TDX_PATH + "/T0002/export/概念-行业.txt";


    public static void main(String[] args) {
        List<GnRelaHyDTO> dtoList = parse();
        System.out.println(JSON.toJSONString(dtoList));
    }


    /**
     * tdx 板块数据   ->     个股 - 行业BK（研究行业 + 普通/细分行业）
     * -
     * - /new_tdx/T0002/hq_cache/tdxhy.cfg
     *
     * @return
     */
    public static List<GnRelaHyDTO> parse() {
        List<GnRelaHyDTO> dtoList = Lists.newArrayList();


        try {
            List<String> lines = FileUtils.readLines(new File(filePath), "UTF-8");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);


                // 概念板块code,概念板块name,普通行业code,普通行业name,研究行业code,研究行业name
                // 880519,碳中和|880456,环境保护|881469,环保


                // 处理每一行
                if (StringUtils.hasText(line)) {


                    // 880519,碳中和,880456,环境保护,881469,环保


                    String[] strArr = line.trim().split("\\,", -1);
                    if (strArr.length < 6) {

                        log.error("GnRelaHyParser#parse   err     >>>     filePath : {} , 行数 : {} , line : {}", filePath, i, line);
                        continue;
                    }


                    // 概念板块code
                    String gnCode = strArr[0];
                    // 概念板块name
                    String gnName = strArr[1];


                    // 2级-普通行业code
                    String lv2_pthy_code = strArr[2];
                    // 2级-普通行业name
                    String lv2_pthy_name = strArr[3];


                    // 1级-研究行业code
                    String lv1_yjhy_code = strArr[4];
                    // 1级-研究行业name
                    String lv1_yjhy_name = strArr[5];


                    GnRelaHyDTO dto = new GnRelaHyDTO(gnCode, gnName, lv2_pthy_code, lv2_pthy_name, lv1_yjhy_code, lv1_yjhy_name);
                    dtoList.add(dto);


                    System.out.println(JSON.toJSONString(dto));
                }
            }

        } catch (IOException e) {

            log.error("GnRelaHyParser#parse   err     >>>     filePath : {},   errMsg : {}", filePath, e.getMessage(), e);
        }


        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 概念code - 行业code          // 2级-普通行业
     *
     * @return
     */
    public static Map<String, String> gnCode__hyCode__map() {

        // 列表
        List<GnRelaHyParser.GnRelaHyDTO> gnRelaHyDTOList = parse();


        // 概念code - 普通行业code
        Map<String, String> gnCode__hyCode__map = Maps.newHashMap();

        gnRelaHyDTOList.stream().forEach(e -> {

            // 概念板块
            String gnCode = e.getGnCode();
            // 2级-普通行业
            String lv2_pthy_code = e.getLv2_pthy_code();

            gnCode__hyCode__map.put(gnCode, lv2_pthy_code);
        });


        return gnCode__hyCode__map;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class GnRelaHyDTO {
        // 概念
        private String gnCode;
        private String gnName;

        // 2级-普通行业
        private String lv2_pthy_code;
        private String lv2_pthy_name;

        // 1级-研究行业
        private String lv1_yjhy_code;
        private String lv1_yjhy_name;
    }

}