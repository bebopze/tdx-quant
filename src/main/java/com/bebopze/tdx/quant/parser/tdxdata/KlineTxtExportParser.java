package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;
import static com.bebopze.tdx.quant.parser.tdxdata.LdayParser.KLINE_START_DATE;


/**
 * 导出 通达信-盘后数据（xx.txt）     ->     解析获取 历史日线数据
 *
 *
 * -   通达信  ->  34[数据导出]  ->  高级导出  ->  添加品种  ->  全部A股/板块指数               // 前复权
 *
 *
 * -   行情数据   export目录：/new_tdx/T0002/export/
 *
 * @author: bebopze
 * @date: 2025/6/1
 */
@Slf4j
public class KlineTxtExportParser {


    public static void main(String[] args) {


        // ----------


        String stockCode_sz = "000001";
        String stockCode_sh = "600519";
        String stockCode_bj = "833171";

        String stockCode_bk = "880904";

        String stockCode_zs_sz = "399106";
        String stockCode_zs_sh = "880003";


        List<LdayParser.LdayDTO> stockDataList = parseTxtByStockCode("300059");
        for (LdayParser.LdayDTO e : stockDataList) {
            String[] item = {e.getCode(), String.valueOf(e.getTradeDate()), String.format("%.2f", e.getOpen()), String.format("%.2f", e.getHigh()), String.format("%.2f", e.getLow()), String.format("%.2f", e.getClose()), String.valueOf(e.getAmount()), String.valueOf(e.getVol()), String.format("%.2f", e.getChangePct())};
            System.out.println(JSON.toJSONString(item));
        }


        // ----------


//        List<LdayDTO> stockDataList = parseByFilePath(filePath_zs);
//        for (LdayDTO e : stockDataList) {
//            String[] item = {e.code, String.valueOf(e.tradeDate), String.format("%.2f", e.open), String.format("%.2f", e.high), String.format("%.2f", e.low), String.format("%.2f", e.close), String.valueOf(e.amount), String.valueOf(e.vol), String.format("%.2f", e.changePct)};
//            System.out.println(JSON.toJSONString(item));
//        }


        System.out.println("---------------------------------- code：" + stockDataList.get(0).getCode() + "     总数：" + stockDataList.size());
    }


    /**
     * tdx 行情导出数据（xx.txt）  -   解析器
     *
     * @param stockCode 证券代码
     * @return
     */
    @SneakyThrows
    public static List<LdayParser.LdayDTO> parseTxtByStockCode(String stockCode) {

        // sz/sh/bj
        String market = StockMarketEnum.getMarketSymbol(stockCode);


        String filePath;
        if (market == null) {
            // 兼容   ->   板块（全部sh） / 指数（sh/sz）
            market = "sh";

            // String filePath_bk = TDX_PATH + "/T0002/export/板块/SH#880302.txt";
            // filePath = TDX_PATH + String.format("/T0002/export/板块/%s#%s.txt", market, stockCode);
            filePath = TDX_PATH + String.format("/T0002/export/A股/%s#%s.txt", market, stockCode);


        } else {

            // String filePath_a = TDX_PATH + "/T0002/export/A股/SZ#000001.txt";
            filePath = TDX_PATH + String.format("/T0002/export/A股/%s#%s.txt", market, stockCode);
        }


        // 指数：上证 / 深证
        if (!new File(filePath).exists()) {
            market = "sz";
            // filePath = TDX_PATH + String.format("/T0002/export/板块/%s#%s.txt", market, stockCode);
            filePath = TDX_PATH + String.format("/T0002/export/A股/%s#%s.txt", market, stockCode);
        }


//        try {
        return parseTxtByFilePath(filePath);
//        } catch (Exception e) {
//
//
//            // 非 RPS指标 板块
//            String filePath_RPS_BK = TDX_PATH + "/T0002/export/A股/SH#880515.txt";
//            boolean PRS_BK__exists = new File(filePath_RPS_BK).exists();
//            boolean BK__exists = StockTypeEnum.isBlock(stockCode) && new File(filePath).exists();
//
//
//            if (PRS_BK__exists && !BK__exists) {
//                // 忽略
//                log.warn("parseTxtByFilePath - 当前板块 非RPS板块（无txt行情导出数据）    >>>     stockCode : {} , filePath : {}", stockCode, filePath);
//            } else {
//                log.error("parseTxtByFilePath   err     >>>     stockCode : {} , filePath : {} , exMsg : {}",
//                          stockCode, filePath, e.getMessage(), e);
//            }
//        }
//
//
//        return Lists.newArrayList();
    }


    /**
     * tdx 盘后数据（xx.txt）   -   解析器
     *
     * @param filePath 文件路径     -    /new_tdx/T0002/export/
     * @return
     */
    public static List<LdayParser.LdayDTO> parseTxtByFilePath(String filePath) {

        // 股票代码
        String code = parseCode(filePath);


        List<LdayParser.LdayDTO> dtoList = Lists.newArrayList();
        double preClose = Double.NaN;


        LocalDate date = null;
        try {

            List<String> lines = FileUtils.readLines(new File(filePath), "GB2312");
            for (String line : lines) {


                // 处理每一行
                if (StringUtils.hasText(line)) {


                    // date,O,H,L,C,VOL,AMO
                    // 2023/05/09,16.07,17.32,15.86,16.47,720610432,12118721536.00


                    String[] strArr = line.trim().split(",");

                    if (strArr.length < 7) {
                        log.warn("line : {}", line);
                        continue;
                    }


                    // 日期
                    date = DateTimeUtil.parseDate_yyyyMMdd__slash(strArr[0]);
                    // 开盘价
                    double open = Double.parseDouble(strArr[1]);
                    // 最高价
                    double high = Double.parseDouble(strArr[2]);
                    // 最低价
                    double low = Double.parseDouble(strArr[3]);
                    // 收盘价
                    double close = Double.parseDouble(strArr[4]);
                    // 成交量（A股、ETF：N股 -> 非N手）
                    long vol = Long.parseLong(strArr[5]);
                    // 成交额（元）
                    BigDecimal amount = new BigDecimal(strArr[6]);


                    // 只记录   2017-01-01   以后的数据
                    if (date.isBefore(KLINE_START_DATE)) {
                        continue;
                    }


//                    if (date.isEqual(LocalDate.of(2024, 10, 9))) {
//                        System.out.println("-------- vol : " + vol);
//                    }


                    if (Double.isNaN(preClose)) {
                        preClose = close;
                    }

                    double changePct = Math.round((close - preClose) / preClose * 100 * 100.0f) / 100.0f;
                    double changePrice = close - preClose;
                    preClose = close;


                    // 振幅       (H/L - 1) x 100%
                    double rangePct = low == 0 ? 0 : (high / low - 1) * 100;


                    // String[] item = {code, String.valueOf(tradeDate), String.format("%.2f", open), String.format("%.2f", high), String.format("%.2f", low), String.format("%.2f", close), amount.toPlainString(), String.valueOf(vol), String.format("%.2f", changePct)};
                    // System.out.println(JSON.toJSONString(item));


                    LdayParser.LdayDTO dto = new LdayParser.LdayDTO(code, date, of(open), of(high), of(low), of(close), of(amount), vol, of(changePct), of(changePrice), of(rangePct), null);


                    dtoList.add(dto);
                }
            }


        } catch (FileNotFoundException e) {


            String filePath_RPS_BK = TDX_PATH + "/T0002/export/A股/SH#880515.txt";
            // 非 RPS指标 板块
            boolean PRS_BK__exists = new File(filePath_RPS_BK).exists();
            boolean BK__exists = StockTypeEnum.isBlock(code) && new File(filePath).exists();


            if (PRS_BK__exists && !BK__exists) {
                // 忽略
                log.warn("parseTxtByFilePath - 当前板块 非RPS板块（无txt行情导出数据）    >>>     code : {} , filePath : {}", code, filePath);
            } else {
                log.error("parseTxtByFilePath - err     >>>     code : {} , date : {} , exMsg : {}", code, date, e.getMessage(), e);
            }


        } catch (Exception e) {
            log.error("parseTxtByFilePath - err     >>>     code : {} , date : {} , exMsg : {}", code, date, e.getMessage(), e);
        }


        // tdx 日K（导出/xx.day）    ->     竟然出现重复   🐶💩
        TDX_SHIT_BUG___REPEAT_KLINE(dtoList);


        return dtoList;
    }


    /**
     * 通达信 日K数据（导出/xx.day）      ->       1、同一交易日 出现多条记录的问题（前复权 与 不复权 重复）      🐶💩
     * -                                       2、日期断层   ->   日期 顺序错乱
     *
     *
     * -       保留 前复权 数据
     *
     * @param dtoList
     */
    public static void TDX_SHIT_BUG___REPEAT_KLINE(List<LdayParser.LdayDTO> dtoList) {


        Map<LocalDate, LdayParser.LdayDTO> date_dto_map = Maps.newHashMapWithExpectedSize(dtoList.size());
        List<LocalDate> repeatList = Lists.newArrayList();


        // ------- 000063（中兴通讯）

        // 2025/03/28,33.98,34.30,33.68,34.06,64806292,2242244864.00          - 6477 行       前复权
        // 2025/03/28,34.60,34.92,34.30,34.68,64806292,2242244864.00          - 6551 行       不复权（异常重复数据  ->  丢弃）


        // tips：  前复权 price   <=   不复权 price   （前复权价格 考虑了 分红和送股 对股价的 稀释，而 不复权价格 则没有）


        // 暂时 懒得比较了             根据观察🔎 导出文件     直接取 第一条  ->  前复权


        dtoList.forEach(e -> {
            LdayParser.LdayDTO ldayDTO = date_dto_map.putIfAbsent(e.getTradeDate(), e);

            if (ldayDTO != null) {
                repeatList.add(e.getTradeDate());
            }
        });


        if (!repeatList.isEmpty()) {
            log.warn("TDX_SHIT_BUG___REPEAT_KLINE     >>>     去重前 : {} , 去重后 : {} , 去重 : {} , repeatList : {}",
                     dtoList.size(), date_dto_map.size(), repeatList.size(), JSON.toJSONString(repeatList));
        }


        // 高效sort
        List<LdayParser.LdayDTO> distinctList = Lists.newArrayList(date_dto_map.values());
        distinctList.sort(Comparator.comparing(LdayParser.LdayDTO::getTradeDate));


        dtoList.clear();
        dtoList.addAll(distinctList);
    }


    /**
     * 解析   股票代码
     *
     * @param filePath 文件路径
     * @return 股票代码
     */
    private static String parseCode(String filePath) {
        //   .../export/A股/SZ#000001.txt
        String[] arr = filePath.split("/");
        return arr[arr.length - 1].split("#")[1].split("\\.")[0];
    }


    private static BigDecimal of(Number val) {
        // 个股价格 - 2位小数          ETF价格 - 3位小数
        return new BigDecimal(String.valueOf(val)).setScale(3, RoundingMode.HALF_UP);
    }


}
