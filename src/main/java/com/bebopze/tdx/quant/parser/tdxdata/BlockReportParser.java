package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.constant.BlockTypeEnum;
import com.bebopze.tdx.quant.common.util.PinYinUtil;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 通达信   报表导出（板块成份）   -   解析
 * -
 * -   34（数据导出）   ->   板块成份导出   ->   逗号分隔
 * -
 *
 * @author: bebopze
 * @date: 2025/5/7
 */
@Slf4j
public class BlockReportParser {


    private static final String filePath_hy = TDX_PATH + "/T0002/export/行业板块.txt";
    private static final String filePath_gn = TDX_PATH + "/T0002/export/概念板块.txt";
    private static final String filePath_fg = TDX_PATH + "/T0002/export/风格板块.txt";
    private static final String filePath_dq = TDX_PATH + "/T0002/export/地区板块.txt";
    private static final String filePath_zs = TDX_PATH + "/T0002/export/指数板块.txt";
    private static final String filePath_zdy = TDX_PATH + "/T0002/export/自定义板块.txt";


    public static void main(String[] args) {

        List<ExportBlockDTO> dtoList = parseAllTdxBlock();


        System.out.println(JSON.toJSONString(dtoList));
    }


    /**
     * tdx - 系统自带 板块     -     all
     *
     * @return
     */
    public static List<ExportBlockDTO> parseAllTdxBlock() {


        // 880302,煤炭开采,000552,甘肃能化

        List<ExportBlockDTO> hy_pt_list = parse_hy_pt();
        List<ExportBlockDTO> hy_yj_list = parse_hy_yj();

        List<ExportBlockDTO> gn_list = parse_gn();
        List<ExportBlockDTO> fg_list = parse_fg();
        List<ExportBlockDTO> dq_list = parse_dq();
        List<ExportBlockDTO> zs_list = parse_zs();


        // -----------------------


        List<ExportBlockDTO> allDTOList = Lists.newArrayList();

        allDTOList.addAll(hy_pt_list);
        allDTOList.addAll(hy_yj_list);

        allDTOList.addAll(gn_list);

        // allDTOList.addAll(fg_list);
        // allDTOList.addAll(dq_list);
        // allDTOList.addAll(zs_list);


        return allDTOList;
    }


    public static List<ExportBlockDTO> parse_hy_pt() {
        // 普通行业   ->   880302,煤炭开采,000552,甘肃能化
        return parse(filePath_hy, BlockTypeEnum.HY_PT);
    }

    public static List<ExportBlockDTO> parse_hy_yj() {
        // 研究行业   ->   881002,煤炭开采,000552,甘肃能化
        return parse(filePath_hy, BlockTypeEnum.HY_YJ);
    }

    public static List<ExportBlockDTO> parse_gn() {
        return parse(filePath_gn, BlockTypeEnum.GN);
    }

    public static List<ExportBlockDTO> parse_fg() {
        return parse(filePath_fg, BlockTypeEnum.FG);
    }

    public static List<ExportBlockDTO> parse_dq() {
        return parse(filePath_dq, BlockTypeEnum.DQ);
    }

    public static List<ExportBlockDTO> parse_zs() {
        return parse(filePath_zs, BlockTypeEnum.ZS);
    }


    /**
     * 自定义 板块
     *
     * @return
     */
    public static List<ExportBlockDTO> parse_zdy() {


        // ----------------------- 自定义板块

        // 51（快捷键51-59，后面为空）,上市1年（自定义板块）,600203（个股code）,福日电子（个股name）
        //
        // ,板块902,880515,通达信88


        List<ExportBlockDTO> dtoList = parse(filePath_zdy, BlockTypeEnum.ZDY);


        for (ExportBlockDTO dto : dtoList) {
            // 板块名称 -> 板块code（拼音 首字母）
            // 60日新高 -> 60RXG
            dto.setBlockCode(PinYinUtil.toFirstLetters(dto.getBlockName()));
        }

        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 个股-所属板块   [报表]       解析
     * -
     * - /new_tdx/T0002/export/
     *
     * @return
     */
    public static List<ExportBlockDTO> parse(String filePath, BlockTypeEnum blockTypeEnum) {

        List<ExportBlockDTO> dtoList = Lists.newArrayList();


        try {
            List<String> lines = FileUtils.readLines(new File(filePath), "GB2312");
            for (String line : lines) {

                // 处理每一行
                if (StringUtils.hasText(line)) {


                    // 880515,通达信88,000100,TCL科技


                    String[] strArr = line.trim().split(",");

                    if (strArr.length < 4) {
                        continue;
                    }


                    // 板块code
                    String blockCode = strArr[0];
                    // 板块name
                    String blockName = strArr[1];


                    // 个股code
                    String stockCode = strArr[2];
                    // 个股name
                    String stockName = strArr[3];


                    // -------------------------------------------


                    ExportBlockDTO dto = new ExportBlockDTO();
                    dto.setBlockCode(blockCode);
                    dto.setBlockName(blockName);
                    dto.setStockCode(stockCode);
                    dto.setStockName(stockName);

                    dtoList.add(dto);
                }
            }


            return dtoList;


        } catch (IOException e) {

            log.error("ExportBlockParser#parse err     >>>     filePath : {} , blockType : {} , errMsg : {}", filePath, blockTypeEnum, e.getMessage(), e);
        }


        log.info("ExportBlockParser#parse suc     >>>     filePath : {} , blockType : {} , totalNum : {} , dtoList : {}", filePath, blockTypeEnum, dtoList.size(), JSON.toJSONString(dtoList));
        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    public static class ExportBlockDTO {
        /**
         * 板块code
         */
        private String blockCode;
        /**
         * 板块name
         */
        private String blockName;

        /**
         * 个股code
         */
        private String stockCode;
        /**
         * 个股name
         */
        private String stockName;


        // ------------------------------------------------------------- 自定义板块（存在 个股/板块/指数/...）


        /**
         * 1-个股；2-板块；
         * -
         * - 简略方案：只存储 个股 关系，非个股（直接pass）
         */
        private Integer type = 1;
    }

}