package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
 * 通达信 tdxzs3.cfg   -   解析
 * -
 * -   /T0002/hq_cache/tdxzs3.cfg               全部板块（含 板块-父子关系）     ->     code - name   -   bk_type
 *
 * @author: bebopze
 * @date: 2025/5/6
 */
@Slf4j
public class Tdxzs3Parser {


    private static final String filePath = TDX_PATH + "/T0002/hq_cache/tdxzs3.cfg";


    public static void main(String[] args) {

        parse();
    }


    /**
     * tdx 板块数据
     * -
     * - /new_tdx/T0002/hq_cache/tdxzs3.cfg
     *
     * @return
     */
    public static List<Tdxzs3DTO> parse() {

        List<Tdxzs3DTO> dtoList = Lists.newArrayList();
        Map<String, String> TXCode_code_map = Maps.newHashMap();


        try {
            List<String> lines = FileUtils.readLines(new File(filePath), "GB2312");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);


                // 处理每一行
                if (StringUtils.hasText(line)) {


                    // -------------------------------------------------------------------------------------------------


                    // TDX 能源|880981|2|1|0|T01                            2（普通行业-二级分类/细分行业）   -   T
                    //     煤炭|880301|2|1|0|T0101
                    //  煤炭开采|880302|2|1|1|T010101

                    //    黑龙江|880201|3|1|0|1                             3（地区板块）   -   无父子关系

                    // 机器人概念|880904|4|2|0|智能机器                       4（概念板块）   -   无父子关系

                    //   轮动趋势|880081|5|2|0|轮动趋势                       5（风格板块）   -   无父子关系


                    //      煤炭|881001|12|1|0|X10                         12（研究行业-一级/二级/三级分类）   -   X
                    //   煤炭开采|881002|12|1|0|X1001
                    //     动力煤|881003|12|1|1|X100101


                    // -------------------------------------------------------------------------------------------------


                    String[] strArr = line.trim().split("\\|");
                    if (strArr.length < 6) {
                        log.error("parseTdxStock err     >>>     filePath : {} , 行数 : {} , line : {} ", filePath, i, line);
                        continue;
                    }


                    // 板块名称
                    String name = strArr[0];
                    // 板块代码
                    String code = strArr[1];


                    // 板块类型：2-普通行业 / 3-地区板块 / 4-概念板块 / 5-风格板块 / 12-研究行业
                    String block_type = strArr[2];

                    // 1/2（含义未知）
                    String xx_type = strArr[3];

                    // 是否 最后一级： 0-否（有子级）； 1-是（无子级）；
                    String end_level = strArr[4];


                    // T010101 / X100101   /   概念板块-别名
                    String TXCode = strArr[5];


                    // ------------------------------------------- 父子关系


                    TXCode_code_map.put(TXCode, code);


                    // 父-code
                    String p_TXCode = null;
                    String p_code = null;

                    int level = 1;
                    if ("2".equals(block_type) || "12".equals(block_type)) {

                        if (TXCode.length() == 3) {
                            level = 1;
                            p_TXCode = null;
                        } else if (TXCode.length() == 5) {
                            level = 2;
                            p_TXCode = TXCode.substring(0, 3);
                        } else if (TXCode.length() == 7) {
                            level = 3;
                            p_TXCode = TXCode.substring(0, 5);
                        }


                    } else {
                        // 仅 行业板块 有该字段   （概念/风格/指数 - 无父子关系）
                        end_level = "1";
                    }


                    Tdxzs3DTO dto = new Tdxzs3DTO();
                    dto.setName(name);
                    dto.setCode(code);
                    dto.setBlockType(Integer.valueOf(block_type));
                    dto.setEndLevel(Integer.valueOf(end_level));
                    dto.setLevel(level);
                    dto.setTXCode(TXCode);
                    dto.setPTXCode(p_TXCode);
                    dto.setPCode(p_code);

                    dtoList.add(dto);


                    System.out.println(JSON.toJSONString(strArr));
                }
            }

        } catch (IOException e) {

            log.error("parseTdxSector err     >>>     filePath : {},   errMsg : {}", filePath, e.getMessage(), e);
        }


        // 信息补充
        fillInfo(dtoList, TXCode_code_map);


        return dtoList;
    }


    private static void fillInfo(List<Tdxzs3DTO> dtoList,
                                 Map<String, String> TXCode_code_map) {

        dtoList.forEach(e -> {
            e.setPCode(TXCode_code_map.get(e.getPTXCode()));
        });
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * tdxzs3.cfg   -   行数据
     */
    @Data
    public static class Tdxzs3DTO {


        // 白酒|880381|2|1|1|T030501
        // 云计算|880545|4|2|0|云计算


        /**
         * 板块name
         */
        String name;
        /**
         * 板块code
         */
        String code;
        /**
         * 板块类型：2/3/4/5/12
         */
        Integer blockType;

        /**
         * - 未知
         * - 暂时 无用字段
         */
        String t1;


        /**
         * 是否 最后一级： 0-否（有子级）； 1-是（无子级）；
         */
        Integer endLevel;

        /**
         * 关联CODE  / 概念bk-别名
         * -
         * - 白酒|880381|2|1|1|T030501
         * - 云计算|880545|4|2|0|云计算
         */
        String TXCode;


        // -----------------

        /**
         * 板块level（行业板块）
         */
        Integer level;


        /**
         * 父级 - pTXCode
         */
        String pTXCode;

        /**
         * 父级 - 板块code
         */
        String pCode;
    }

}