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
import java.util.Arrays;
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * ETF - 概念板块/细分行业  关联数据 解析器
 *
 * - 格式为：
 * -        ETF代码-ETF名称|ETF细分行业|板块code-板块name,板块code-板块name,...
 * -        159206-卫星ETF|卫星通信|880546-卫星导航,880548-商业航天
 *
 * @author: bebopze
 * @date: 2026/1/21
 */
@Slf4j
public class EtfRelaBlockParser {


    private static final String filePath = TDX_PATH + "/T0002/export/概念-ETF.txt";


    /**
     * ETF - 板块（概念板块 + 细分行业）
     *
     * @return
     */
    public static List<EtfRelaBlockDTO> parse() {
        List<EtfRelaBlockDTO> dtoList = Lists.newArrayList();


        try {
            List<String> lines = FileUtils.readLines(new File(filePath), "UTF-8");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);


                // ETF代码-ETF名称|ETF细分行业|板块code-板块name,板块code-板块name,...


                // 处理每一行
                if (StringUtils.hasText(line)) {


                    // 159206-卫星ETF|卫星通信|880546-卫星导航,880548-商业航天


                    String[] strArr = line.trim().split("\\|");
                    if (strArr.length != 3) {
                        log.error("GnRelaEtfParser#parse  -  err     >>>     filePath : {} , 行数 : {} , line : {}", filePath, i, line);
                        continue;
                    }


                    // 159206-卫星ETF
                    String[] etfArr = strArr[0].split("-");
                    // 卫星通信
                    String etf_blockName = strArr[1];
                    // 880546-卫星导航,880548-商业航天
                    String[] blockArr = strArr[2].split(",");


                    // ETF代码
                    String etfCode = etfArr[0];
                    // ETF名称
                    String etfName = etfArr[1];


                    // 板块code-板块name
                    int finalI = i;
                    Arrays.stream(blockArr).map(block_codeName -> block_codeName.split("-")).forEach(arr -> {
                        if (arr.length != 2) {
                            log.error("GnRelaEtfParser#parse  -  err     >>>     filePath : {} , 行数 : {} , line : {} , block_codeName : {}", filePath, finalI, line, blockArr);
                            return;
                        }


                        String blockCode = arr[0];
                        String blockName = arr[1];


                        EtfRelaBlockDTO dto = new EtfRelaBlockDTO(etfCode, etfName, etf_blockName, blockCode, blockName);
                        dtoList.add(dto);


                        System.out.println(JSON.toJSONString(dto));
                    });
                }
            }

        } catch (IOException e) {

            log.error("GnRelaEtfParser#parse  -  err     >>>     filePath : {} , errMsg : {}", filePath, e.getMessage(), e);
        }


        log.info("GnRelaEtfParser#parse  -  suc     >>>     filePath : {} , dtoList.size : {}", filePath, dtoList.size());
        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class EtfRelaBlockDTO {
        // ETF
        private String etfCode;
        private String etfName;


        // ETF细分行业
        private String etfBlockName;


        // 板块
        private String blockCode;
        private String blockName;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        List<EtfRelaBlockDTO> dtoList = parse();
        System.out.println(JSON.toJSONString(dtoList));
    }


}