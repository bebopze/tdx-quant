package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.util.List;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 解析 通达信-盘后数据 获取历史日线数据   -   https://blog.csdn.net/weixin_57522153/article/details/119992838
 * -
 * - 盘后数据目录：/new_tdx/vipdoc/
 *
 *
 *
 *
 * - 废弃   ==>   不可用
 *
 * -   xx.day文件   -   行情数据 未知bug     ->     90% 行情数据    有偏差       // 仅近1年内 数据基本准确
 *
 *
 * - 替代方案：  @see  KlineReportParser          // 通达信   -  （行情）数据导出   ->   解析
 *
 * @author: bebopze
 * @date: 2024/10/9
 * @see KlineReportParser
 */
@Slf4j
@Deprecated
public class LdayParser {


    public static void main(String[] args) {


//        String filePath = "/DEL/hsjday (3)/sz/lday/" + "sz300059.day";
//        List<LdayDTO> ldayDTOS = parseByFilePath(filePath);
//        System.out.println();


        // C:/soft/通达信/v_2024/跑数据专用/new_tdx/vipdoc/sh/lday/sh000001.day
        // C:/soft/通达信/v_2024/跑数据专用/new_tdx/vipdoc/ds/lday/31#00700.day
        // C:/soft/通达信/v_2024/跑数据专用/new_tdx/vipdoc/ds/lday/74#SPY.day


        // A股          sz-深圳；sh-上海；bj-北京
        String filePath_a = TDX_PATH + "/vipdoc/sh/lday/sh600519.day";
        // ds - 港美
        String filePath_hk = TDX_PATH + "/vipdoc/ds/lday/31#00700.day";
        String filePath_us = TDX_PATH + "/vipdoc/ds/lday/74#SPY.day";


        // 板块
        String filePath_bk = TDX_PATH + "/vipdoc/sh/lday/sh880904.day";
        String filePath_bk2 = TDX_PATH + "/vipdoc/sh/lday/sh880948.day";


        // 指数
        String filePath_zs = TDX_PATH + "/vipdoc/sh/lday/sh880003.day";
        String filePath_zs2 = TDX_PATH + "/vipdoc/sz/lday/sz399106.day";


        // ----------


        String stockCode_sz = "000001";
        String stockCode_sh = "600519";
        String stockCode_bj = "833171";

        String stockCode_bk = "880904";

        String stockCode_zs_sz = "399106";
        String stockCode_zs_sh = "880003";


        List<LdayDTO> stockDataList = parseByStockCode(stockCode_sz);
        for (LdayDTO e : stockDataList) {
            String[] item = {e.code, String.valueOf(e.tradeDate), String.format("%.2f", e.open), String.format("%.2f", e.high), String.format("%.2f", e.low), String.format("%.2f", e.close), String.valueOf(e.amount), String.valueOf(e.vol), String.format("%.2f", e.changePct)};
            System.out.println(JSON.toJSONString(item));
        }


        // ----------


//        List<LdayDTO> stockDataList = parseByFilePath(filePath_zs);
//        for (LdayDTO e : stockDataList) {
//            String[] item = {e.code, String.valueOf(e.tradeDate), String.format("%.2f", e.open), String.format("%.2f", e.high), String.format("%.2f", e.low), String.format("%.2f", e.close), String.valueOf(e.amount), String.valueOf(e.vol), String.format("%.2f", e.changePct)};
//            System.out.println(JSON.toJSONString(item));
//        }


        System.out.println("---------------------------------- code：" + stockDataList.get(0).code + "     总数：" + stockDataList.size());
    }


    /**
     * tdx 盘后数据（xx.day）   -   解析器
     *
     * @param stockCode 证券代码
     * @return
     */
    @SneakyThrows
    public static List<LdayDTO> parseByStockCode(String stockCode) {

        // sz/sh/bj
        String market = StockMarketEnum.getMarketSymbol(stockCode);
        // 兼容   ->   板块（全部sh） / 指数（sh/sz）
        market = market == null ? "sh" : market;


        // String filePath_a = TDX_PATH + "/vipdoc/sh/lday/sh600519.day";
        String filePath = TDX_PATH + String.format("/vipdoc/%s/lday/%s%s.day", market, market, stockCode);


        // 指数：上证 / 深证
        if (!new File(filePath).exists()) {
            market = "sz";
            filePath = TDX_PATH + String.format("/vipdoc/%s/lday/%s%s.day", market, market, stockCode);
        }


        try {
            return parseByFilePath(filePath);
        } catch (Exception e) {
            log.error("parseByFilePath   err     >>>     stockCode : {} , filePath : {} , exMsg : {}",
                      stockCode, filePath, e.getMessage(), e);
        }


        return Lists.newArrayList();
    }


    /**
     * tdx 盘后数据（xx.day）   -   解析器
     *
     * @param filePath 文件路径     -    /new_tdx/vipdoc/
     * @return
     */
    @SneakyThrows
    public static List<LdayDTO> parseByFilePath(String filePath) {

        // 股票代码
        String code = parseCode(filePath);


        FileInputStream fileInputStream = new FileInputStream(filePath.trim());
        byte[] buffer = new byte[fileInputStream.available()];
        fileInputStream.read(buffer);
        fileInputStream.close();

        int num = buffer.length;
        int no = num / 32;
        int b = 0, e = 32;


        List<LdayDTO> dtoList = Lists.newArrayList();
        float preClose = 0.0f;


        for (int i = 0; i < no; i++) {
            byte[] slice = new byte[32];
            System.arraycopy(buffer, b, slice, 0, 32);


            b += 32;
            // e += 32;


            // ---------------------------------------------------------------------------------------------------------


            // 在大多数公开资料和业内实践中，通达信个股日线数据的记录一般采用如下结构（每条记录共32字节，每个字段均按小端模式存储）：

            // 日期（int，4字节）
            // 存储格式为形如 YYYYMMDD 的整数。例如 20250408 表示 2025 年 4 月 8 日。


            // 开盘价（float，4字节）
            // 最高价（float，4字节）
            // 最低价（float，4字节）
            // 收盘价（float，4字节）
            //
            // 上述价格通常为浮点数（单精度），表示当日的价格。注意：在不同版本中可能存在“倍率”或“缩放因子”的情况，解析时需要结合实际的数值单位检查。


            // 成交额（float，4字节）
            // 表示当天的成交金额


            // 成交量（int，4字节）
            // 表示当天的成交股数（A港：手[x100]   /   美：股）


            // 保留字段/备用字段（int，4字节）
            // 用于预留或对一些扩展信息存放。部分版本中可能用于存放其他数据（如持仓量等），但多数情况下该字段为保留字段。


            // 这样，总计 4 + 4×4 + 4 + 4 + 4 = 32 字节


            // ---------------------------------------------------------------------------------------------------------


            ByteBuffer byteBuffer = ByteBuffer.wrap(slice).order(ByteOrder.LITTLE_ENDIAN);


            // 日期
            int date = byteBuffer.getInt();
            // 开盘价
            float open = (float) byteBuffer.getInt() / 100;
            // 最高价
            float high = (float) byteBuffer.getInt() / 100;
            // 最低价
            float low = (float) byteBuffer.getInt() / 100;
            // 收盘价
            float close = (float) byteBuffer.getInt() / 100;
            // 成交额（元）
            BigDecimal amount = BigDecimal.valueOf(byteBuffer.getFloat());
            // 成交量
            int vol = byteBuffer.getInt();
            // 保留字段
            int unUsed = byteBuffer.getInt();


            int year = date / 10000;
            int month = (date % 10000) / 100;
            int day = date % 100;
            LocalDate tradeDate;
            try {
                tradeDate = LocalDate.of(year, month, day);
            } catch (Exception ex) {
                log.error("code : {} , date : {}", code, date);
                continue;
            }


            // 只记录   2010-01-01   以后的数据
            if (tradeDate.isBefore(LocalDate.of(2010, 1, 1))) {
                continue;
            }

//            if (tradeDate.isEqual(LocalDate.of(2025, 1, 2))) {
//                System.out.println("--------");
//            }


            if (i == 0) {
                preClose = close;
            }

            float changePct = Math.round((close - preClose) / preClose * 100 * 100.0f) / 100.0f;
            float changePrice = close - preClose;
            preClose = close;


            // 振幅       H/L   x100-100
            float rangePct = high / low * 100 - 100;


            // String[] item = {code, String.valueOf(tradeDate), String.format("%.2f", open), String.format("%.2f", high), String.format("%.2f", low), String.format("%.2f", close), amount.toPlainString(), String.valueOf(vol), String.format("%.2f", changePct)};
            // System.out.println(JSON.toJSONString(item));


            LdayDTO dto = new LdayDTO(code, tradeDate, of(open), of(high), of(low), of(close), amount, vol, of(changePct), of(changePrice), of(rangePct), null);


            dtoList.add(dto);
        }


        return dtoList;
    }


    /**
     * 解析   股票代码
     *
     * @param filePath 文件路径
     * @return 股票代码
     */
    private static String parseCode(String filePath) {
        return filePath.split("lday")[1].split(".day")[0].split("sh|sz|bj|#")[1];
    }


    private static BigDecimal of(Number val) {
        return new BigDecimal(String.valueOf(val)).setScale(2, RoundingMode.HALF_UP);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class LdayDTO implements Serializable {

        private String code;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate tradeDate;

        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal amount;
        private Integer vol;
        private BigDecimal changePct;
        // 涨跌额       C - pre_C          |          今日收盘价 × 涨跌幅 / (1+涨跌幅)
        private BigDecimal changePrice;
        // 振幅       H/L   x100-100
        private BigDecimal rangePct;

        // ----------------------------------- 自动计算 字段


        // 换手率
        private BigDecimal turnoverPct;
    }


}
