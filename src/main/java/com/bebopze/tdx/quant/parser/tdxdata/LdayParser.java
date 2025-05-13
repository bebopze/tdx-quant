package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
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
 * @author: bebopze
 * @date: 2024/10/9
 */
@Slf4j
public class LdayParser {


    public static void main(String[] args) {

        // C:\soft\通达信\v_2024\跑数据专用\new_tdx\vipdoc\sh\lday\sh000001.day
        // C:\soft\通达信\v_2024\跑数据专用\new_tdx\vipdoc\ds\lday\31#00700.day
        // C:\soft\通达信\v_2024\跑数据专用\new_tdx\vipdoc\ds\lday\74#SPY.day


        String filePath_a = TDX_PATH + "/vipdoc/sh/lday/sh600519.day";
        String filePath_hk = TDX_PATH + "/vipdoc/ds/lday/31#00700.day";
        String filePath_us = TDX_PATH + "/vipdoc/ds/lday/74#SPY.day";

        String filePath_bk = TDX_PATH + "/vipdoc/sh/lday/sh880904.day";
        String filePath_bk2 = TDX_PATH + "/vipdoc/sh/lday/sh880948.day";
        String filePath_zs = TDX_PATH + "/vipdoc/sh/lday/sh880003.day";
        String filePath_zs2 = TDX_PATH + "/vipdoc/sh/lday/sz399106.day";


        List<LdayDTO> stockDataList = parse(filePath_zs);
        for (LdayDTO e : stockDataList) {
            String[] item = {e.code, String.valueOf(e.tradeDate), String.format("%.2f", e.open), String.format("%.2f", e.high), String.format("%.2f", e.low), String.format("%.2f", e.close), String.valueOf(e.amount), String.valueOf(e.vol), String.format("%.2f", e.changePct)};
            System.out.println(JSON.toJSONString(item));
        }


        System.out.println("---------------------------------- code：" + stockDataList.get(0).code + "     总数：" + stockDataList.size());
    }


    /**
     * tdx 盘后数据（xx.day）   -   解析器
     *
     * @param filePath 文件路径     -    /new_tdx/vipdoc/
     * @return
     */
    @SneakyThrows
    public static List<LdayDTO> parse(String filePath) {

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
            BigDecimal amount = new BigDecimal(byteBuffer.getFloat());
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


            if (i == 0) {
                preClose = close;
            }

            float changePct = Math.round((close - preClose) / preClose * 100 * 100.0f) / 100.0f;
            preClose = close;


            // String[] item = {code, String.valueOf(tradeDate), String.format("%.2f", open), String.format("%.2f", high), String.format("%.2f", low), String.format("%.2f", close), amount.toPlainString(), String.valueOf(vol), String.format("%.2f", changePct)};
            // System.out.println(JSON.toJSONString(item));


            LdayDTO dto = new LdayDTO(code, tradeDate, of(open), of(high), of(low), of(close), amount, vol, of(changePct));


            dtoList.add(dto);


            b += 32;
            e += 32;
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
        return new BigDecimal(String.valueOf(val));
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
    }

}
