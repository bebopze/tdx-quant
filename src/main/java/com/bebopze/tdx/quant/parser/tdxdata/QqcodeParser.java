package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.FileUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 通达信   ggqqcode.txt / szqqcode.txt       -       期权解析
 * -
 * -
 * -   /T0002/hq_cache/ggqqcode.txt               上海期权
 * -   /T0002/hq_cache/szqqcode.txt               深圳期权
 * -
 *
 * @author: bebopze
 * @date: 2026/11/23
 */
@Slf4j
public class QqcodeParser {


    /**
     * 上海期权
     */
    private static final String SHO_filePath = TDX_PATH + "/T0002/hq_cache/ggqqcode.txt";
    /**
     * 深圳期权
     */
    private static final String SZO_filePath = TDX_PATH + "/T0002/hq_cache/szqqcode.txt";


    public static void main(String[] args) {
        List<QqcodeDTO> dtoList = parse();
        System.out.println("size : " + dtoList.size());
    }


    //  ----------------------------------------------------------------------------------------------------------------


    public static List<QqcodeDTO> parse() {
        List<QqcodeDTO> sh__dtoList = parse(SHO_filePath);
        List<QqcodeDTO> sz__dtoList = parse(SZO_filePath);
        sh__dtoList.addAll(sz__dtoList);

        return sh__dtoList;
    }


    /**
     * tdx 板块数据   ->     个股 - 行业BK（研究行业 + 普通/细分行业）
     * -
     * - /new_tdx/T0002/hq_cache/tdxhy.cfg
     *
     * @return
     */
    public static List<QqcodeDTO> parse(String filePath) {
        List<QqcodeDTO> dtoList = Lists.newArrayList();


        int i = 0;
        String line = null;


        try {
            List<String> lines = FileUtil.readLines(new File(filePath), "GBK");

            for (; i < lines.size(); i++) {
                line = lines.get(i);


                // 处理每一行
                if (StringUtils.hasText(line)) {


                    // 10011565,588000CCM01900,科创50购12月1900,588000,科创50ETF华夏,2612,1.3440,10000,1.9000,20260507,20261223,20261223,20261223,EC0N0000E0  ,959.80,12.00,7.00,1,1,50,1,10


                    String[] strArr = line.trim().split(",");
                    if (strArr.length < 22) {
                        log.error("QqcodeParser#parse - err     >>>     filePath : {} , 行数 : {} , line : {} ", filePath, i, line);
                        continue;
                    }


                    // 期权编码（10011565）
                    String code = strArr[0];
                    // 期权代码（588000CCM01900）
                    String symbol = strArr[1];
                    // 科创50购12月1900
                    String name = strArr[2];


                    // 期权类型（C/P）
                    String callPutType = name.contains("购") ? "C" : "P";
                    // 期权标的类型：1-ETF；2-指数；3-商品；4-期货；
                    int underlyingType = 1;


                    // 期权标的code（588000）
                    String underlyingCode = strArr[3];
                    // 期权标的name（科创50ETF华夏）
                    String underlyingName = strArr[4];


                    // 合约年月（2612  ->  202612）
                    int contractMonth = Integer.parseInt("20" + strArr[5]);
                    // 标的证券实时价格（1.3440）
                    BigDecimal underlyingPrice = NumUtil.ofDecimal(strArr[6]);
                    // 合约乘数（10000）
                    int multiplier = Integer.parseInt(strArr[7]);
                    // 行权价格（1.9000）
                    BigDecimal strikePrice = NumUtil.ofDecimal(strArr[8]);


                    // 合约开始日期（20260507）
                    LocalDate startDate = DateTimeUtil.parseDate_yyyyMMdd(strArr[9]);
                    // 合约结束日期（20261223）
                    LocalDate endDate = DateTimeUtil.parseDate_yyyyMMdd(strArr[10]);


                    // 20261223
                    String x_11 = strArr[11];
                    // 20261223
                    String x_12 = strArr[12];


                    // EC0N0000E0
                    String x_13 = strArr[13];
                    // 959.80
                    String x_14 = strArr[14];
                    // 12.00
                    String x_15 = strArr[15];
                    // 7.00
                    String x_16 = strArr[16];

                    // 1
                    String x_17 = strArr[17];
                    // 1
                    String x_18 = strArr[18];
                    // 50
                    String x_19 = strArr[19];
                    // 1
                    String x_20 = strArr[20];
                    // 10
                    String x_21 = strArr[21];


                    QqcodeDTO dto = new QqcodeDTO(code, symbol, name, callPutType, underlyingType, underlyingCode, underlyingName, contractMonth, underlyingPrice, multiplier, strikePrice, startDate, endDate);
                    dtoList.add(dto);


                    System.out.println(JSON.toJSONString(dto));
                }
            }


        } catch (Exception e) {
            log.error("QqcodeParser#parse - err     >>>     filePath : {} , 行数 : {} , line : {} , errMsg : {}", filePath, i, line, e.getMessage(), e);
        }


        return dtoList;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class QqcodeDTO {


        // 期权编码（10011565）
        private String code;
        // 期权代码（588000CCM01900）
        private String symbol;
        // 科创50购12月1900
        private String name;


        // 期权类型（C/P）
        private String callPutType;
        // 期权标的类型：1-ETF；2-指数；3-商品；4-期货；
        private int underlyingType;


        // 期权标的code（588000）
        private String underlyingCode;
        // 期权标的name（科创50ETF华夏）
        private String underlyingName;


        // 合约年月（202612）
        private int contractMonth;
        // 标的证券实时价格（1.3440）
        private BigDecimal underlyingPrice;
        // 合约乘数（10000）
        private int multiplier;
        // 行权价格（1.9000）
        private BigDecimal strikePrice;


        // 合约开始日期（20260507）
        private LocalDate startDate;
        // 合约结束日期（20261223）
        private LocalDate endDate;
    }


}