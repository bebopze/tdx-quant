package com.bebopze.tdx.quant.parser.tdxdata;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.StockTypeUtil;
import com.bebopze.tdx.quant.parser.check.TdxFunCheck;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;
import static com.bebopze.tdx.quant.parser.tdxdata.KlineTxtExportParser.TDX_SHIT_BUG___REPEAT_KLINE;


/**
 * 解析 通达信-盘后数据 获取历史日线数据   -   https://blog.csdn.net/weixin_57522153/article/details/119992838
 * -
 * - 盘后数据目录：/new_tdx/vipdoc/                           // 不复权
 *
 *
 *
 * - 废弃   ==>   不可用
 *
 * -   xx.day文件   -   行情数据 未知bug     ->     90% 行情数据    有偏差       // 仅 近半年内   数据基本准确
 *
 * -      bug原因：    经验证   xx.day 数据   均为   ->   【不复权】 数据        // 板块/指数 - 不存在 复权  =>  无影响
 *
 *
 *
 * - 替代方案：  @see  KlineReportParser          // 通达信   -  （行情）数据导出   ->   解析
 *
 *
 * - 终极方案：  东方财富/同花顺/雪球   -   行情API   // 缺点：限流  ->  封IP
 *
 *
 * -
 *
 * @author: bebopze
 * @date: 2024/10/9
 * @see KlineTxtExportParser
 */
@Slf4j
public class LdayParser {


    /**
     * A股 起始时间
     */
    public static final LocalDate MARKET_START_DATE = LocalDate.of(1990, 1, 1);


    /**
     * 只记录   2017-01-01   以后的数据
     */
    public static final LocalDate KLINE_START_DATE = LocalDate.of(2017, 1, 1);


    /**
     * tdx 盘后数据（xx.day）  -   解析器
     *
     *
     * - 往期数据  ->  报表
     * - 短期数据  ->  xx.day
     *
     * @param stockCode 证券代码
     * @return
     */
    @SneakyThrows
    public static List<LdayDTO> parseByStockCode(String stockCode) {

        // A股（sz/sh/bj）
        String market = StockMarketEnum.getMarketSymbol(stockCode);
        // 兼容   ->   板块（全部sh） / 指数（sh/sz）
        market = market == null ? "sh" : market;


        // String filePath_a = TDX_PATH + "/vipdoc/sh/lday/sh600519.day";
        String filePath = TDX_PATH + String.format("/vipdoc/%s/lday/%s%s.day", market, market, stockCode);


        // 港美股（ds）
        if (StockTypeEnum.isHkUsStock(stockCode)) {
            Integer tdxMarketType = StockMarketEnum.getTdxMarketType(stockCode);

            // String filePath_a = TDX_PATH + "/vipdoc/ds/lday/31#00700.day";
            // String filePath_a = TDX_PATH + "/vipdoc/ds/lday/74#SPY.day";
            filePath = TDX_PATH + String.format("/vipdoc/ds/lday/%s#%s.day", tdxMarketType, stockCode);
        }


        // 指数：上证 / 深证
        if (!new File(filePath).exists()) {
            market = "sz";
            filePath = TDX_PATH + String.format("/vipdoc/%s/lday/%s%s.day", market, market, stockCode);
        }


        // ---------------------------------------------------------------


        // 往期数据  ->  报表
        List<LdayDTO> klineTxtReport__ldayDTOList = Lists.newArrayList();
        // 短期数据  ->  xx.day
        List<LdayDTO> lday__ldayDTOList = Lists.newArrayList();


        try {


            // 往期数据  ->  报表
            // klineTxtReport__ldayDTOList = Lists.newArrayList();

            // 个股   ->   行情数据   复权bug          // 板块/指数 - 不存在 复权
            klineTxtReport__ldayDTOList = KlineTxtExportParser.parseTxtByStockCode(stockCode);


            // 报表导出  ->  最后一天数据 有bug（盘中导出  ->  最后一日价格 全部为 昨日收盘价          盘后导出 -> 正常）
            checkReport__lastKline(klineTxtReport__ldayDTOList);


            // -----------------------


//            // 往期数据  ->  报表
//            // klineTxtReport__ldayDTOList = Lists.newArrayList();
//            if (StockMarketEnum.getMarketSymbol(stockCode) != null) {
//
//                // 个股   ->   行情数据   复权bug          // 板块/指数 - 不存在 复权
//                klineTxtReport__ldayDTOList = KlineReportParser.parseByStockCode(stockCode);
//
//
//                // 报表导出  ->  最后一天数据 有bug（盘中导出  ->  最后一日价格 全部为 昨日收盘价          盘后导出 -> 正常）
//                checkReport__lastKline(klineTxtReport__ldayDTOList);
//            }


            // 短期数据  ->  xx.day
            lday__ldayDTOList = parseLdayByFilePath(filePath);


            // ---------------------------------------------------------------------


            check(klineTxtReport__ldayDTOList, lday__ldayDTOList);


            return merge(klineTxtReport__ldayDTOList, lday__ldayDTOList);


        } catch (Exception e) {


            if (size(klineTxtReport__ldayDTOList) == 0 && size(lday__ldayDTOList) == 0) {
                // 准 新股  ->  未上市/上市失败                    688688 蚂蚁金服
                log.warn("parseLdayByFilePath   err  -  准新股 -> 未上市/上市失败     >>>     stockCode : {} , filePath : {} , klineTxtReport__ldayDTOList : {} , lday__ldayDTOList : {} , exMsg : {}",
                         stockCode, filePath, size(klineTxtReport__ldayDTOList), size(lday__ldayDTOList), e.getMessage(), e);
            } else {
                log.error("parseLdayByFilePath   err     >>>     stockCode : {} , filePath : {} , klineTxtReport__ldayDTOList : {} , lday__ldayDTOList : {} , exMsg : {}",
                          stockCode, filePath, size(klineTxtReport__ldayDTOList), size(lday__ldayDTOList), e.getMessage(), e);
            }
        }


        return Lists.newArrayList();
    }


    /**
     * 报表导出  ->  最后一天数据 有bug（盘中导出  ->  最后一日     价格全部为 昨日收盘价 + VOL=0          盘后导出 -> 正常）
     *
     * @param klineReport__ldayDTOList
     * @return
     */
    private static void checkReport__lastKline(List<LdayDTO> klineReport__ldayDTOList) {

        int size = size(klineReport__ldayDTOList);


        // 盘中导出  ->  最后一日     价格 全部为 昨日收盘价     +     VOL=0
        if (size > 0 && klineReport__ldayDTOList.get(size - 1).getVol() == 0) {

            klineReport__ldayDTOList.remove(size - 1);
        }
    }


    /**
     * tdx 盘后数据（xx.day）   -   解析器
     *
     * @param filePath 文件路径     -    /new_tdx/vipdoc/
     * @return
     */
    @SneakyThrows
    public static List<LdayDTO> parseLdayByFilePath(String filePath) {

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
        float preClose = Float.NaN;


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


            // ------------------------- 价格精度


            // 股票价格 精度     ->     A股-2位小数；ETF-3位小数；
            int priceScale = StockTypeUtil.stockPriceScale(code);
            // 2位精度 -> /100
            // 3位精度 -> /1000
            int priceDivisor = priceScale == 3 ? 1_000 : 1_00;


            // -------------------------


            // 日期
            int date = byteBuffer.getInt();
            // 开盘价
            // float open = (float) byteBuffer.getInt() / 100;
            float open = (float) byteBuffer.getInt() / priceDivisor;
            // 最高价
            float high = (float) byteBuffer.getInt() / priceDivisor;
            // 最低价
            float low = (float) byteBuffer.getInt() / priceDivisor;
            // 收盘价
            float close = (float) byteBuffer.getInt() / priceDivisor;
            // 成交额（元）
            BigDecimal amount = BigDecimal.valueOf(byteBuffer.getFloat());


            // 成交量（A股：N股 -> 非N手）
            // 成交量（ETF bug：N股[99.9%]   ->   N手[0.1%]）         stockCode : 513120 , idx : 378 , date : 2024-01-26 , diffFields : {"vol":{"v1":4550469600,"v2":45504696}}
            long vol = byteBuffer.getInt();     // 负数bug + 复权bug
            if (vol < 0) {
                long signedVol = vol;

                // vol &= 0xFFFFFFFFL;
                vol = Integer.toUnsignedLong((int) vol);
                log.debug("{}   -   {}   {} -> {}", code, date, signedVol, vol);

            } else {

                // 成交量（ETF bug：N股[99.9%]   ->   N手[0.1%]）         stockCode : 513120 , idx : 378 , date : 2024-01-26 , diffFields : {"vol":{"v1":4550469600,"v2":45504696}}
                double calcVol = amount.doubleValue() / close;
                if (calcVol / vol > 95) {
                    vol *= 100;
                }
            }


            // 保留字段
            int unUsed = byteBuffer.getInt();
//            if (unUsed != 0) {
//                List<Integer> byteList = Lists.newArrayList();
//                for (byte x : byteBuffer.array()) {
//                    byteList.add((int) x);
//                }
//
//                List<Integer> unUsedList = byteList.subList(28, 32);
//                log.debug("保留字段     >>>     unUsed : {} , unUsedList : {}", unUsed, unUsedList);
//            }


            int year = date / 10000;
            int month = (date % 10000) / 100;
            int day = date % 100;
            LocalDate tradeDate;
            try {
                tradeDate = LocalDate.of(year, month, day);

                // 1990-1-1  ~  now()
                Assert.isTrue(DateTimeUtil.between(tradeDate, MARKET_START_DATE, LocalDate.now()), String.format("tradeDate=[%s]超出有效范围", tradeDate));

            } catch (Exception ex) {
                log.error("parseLdayByFilePath - 解析[tradeDate]异常     >>>     code : {} , date : {} , yyyy-mm-dd : {}-{}-{}",
                          code, date, year, month, day);
                continue;
            }


            // ---------------------------------------------------------------------------------------------------------


            // 只记录   2017-01-01   以后的数据
            if (tradeDate.isBefore(KLINE_START_DATE)) {
                continue;
            }


            if (Float.isNaN(preClose)) {
                preClose = close;
            }

            float changePct = Math.round((close - preClose) / preClose * 100 * 100.0f) / 100.0f;
            float changePrice = close - preClose;
            preClose = close;


            // 振幅       (H/L - 1) x 100%
            float rangePct = (high / low - 1) * 100;


            LdayDTO dto = new LdayDTO(code, tradeDate, of(open), of(high), of(low), of(close), of(amount), vol, of(changePct), of(changePrice), of(rangePct), null);


            // ---------------------------------------------------------------------------------------------------------


            // 成交额为0（停牌）
            if (amount.doubleValue() == 0) {
                log.error("parseLdayByFilePath - 成交额为0（停牌）    >>>     code : {} , date : {} , dto : {}", code, date, JSON.toJSONString(dto));
                continue;
            }


            // ---------------------------------------------------------------------------------------------------------


            dtoList.add(dto);
        }


        // tdx 日K（导出/xx.day）    ->     竟然出现重复   🐶💩
        TDX_SHIT_BUG___REPEAT_KLINE(dtoList);


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
        // 个股价格 - 2位小数          ETF价格 - 3位小数
        return new BigDecimal(String.valueOf(val)).setScale(3, RoundingMode.HALF_UP);
    }


    private static void check(List<LdayDTO> klineReport__ldayDTOList, List<LdayDTO> lday__ldayDTOList) {

        String stockCode = lday__ldayDTOList.get(0).code;


        // ---------------------------------------------


        int size1 = klineReport__ldayDTOList.size();
        int size2 = lday__ldayDTOList.size();

        int size = Math.min(size1, size2);


        if (size1 >= size2) {
            if (size1 - size2 <= 1) {
                // 839680（*ST广道）
                log.warn("check err - 退市股     >>>     [{}] , klineReport__ldayDTOList size : {} , lday__ldayDTOList size : {}", stockCode, size1, size2);
            } else {
                log.error("check err     >>>     [{}] , klineReport__ldayDTOList size : {} , lday__ldayDTOList size : {}", stockCode, size1, size2);
            }
        }


        // ---------------------------------------------


        for (int i = 0; i < size; i++) {

            LdayDTO dto1 = klineReport__ldayDTOList.get(i);
            LdayDTO dto2 = lday__ldayDTOList.get(i);


            String dto1_str = JSON.toJSONString(dto1);
            String dto2_str = JSON.toJSONString(dto2);


            if (!StringUtils.equals(dto1_str, dto2_str)) {

                JSONObject json1 = JSON.parseObject(dto1_str);
                JSONObject json2 = JSON.parseObject(dto2_str);


                JSONObject diffFields = getDiffFields(json1, json2);
                if (MapUtils.isNotEmpty(diffFields)) {


                    // 复权/不复权   ->   不变量：vol、amo     =>     vol/amo 不等 -> 数据异常     |     vol/amo 相等 -> 复权 bug
                    if (diffFields.containsKey("vol")) {

                        // vol/amo 不等 -> 数据异常
                        log.error("check err     >>>     stockCode : {} , idx : {} , date : {} , diffFields : {}",
                                  stockCode, i, dto1.tradeDate, diffFields.toJSONString());
                    } else {

                        // vol/amo 相等 -> 复权 bug
                        log.debug("check err     >>>     stockCode : {} , idx : {} , date : {} , diffFields : {}",
                                  stockCode, i, dto1.tradeDate, diffFields.toJSONString());
                    }


                    // if (diffFields.containsKey("vol")) {
                    //     log.error("vol - err     >>>     v1={}, v2={}", diffFields.getJSONObject("vol").getLong("v1"), diffFields.getJSONObject("vol").getLong("v2"));
                    // }
                }


            } else {

                log.debug("check suc     >>>     stockCode : {} , idx : {} , date : {}",
                          stockCode, i, dto1.tradeDate);
            }
        }
    }


    private static JSONObject getDiffFields(JSONObject json1, JSONObject json2) {
        JSONObject result = new JSONObject();


        for (String key : json1.keySet()) {

            Object v1 = json1.get(key);
            Object v2 = json2.get(key);


            if (v1 instanceof Number) {

                BigDecimal _v1 = new BigDecimal(v1.toString());
                BigDecimal _v2 = v2 == null ? null : new BigDecimal(v2.toString());

                if (!TdxFunCheck.equals(_v1, _v2)) {
                    JSONObject diff = new JSONObject();
                    diff.put("v1", _v1);
                    diff.put("v2", _v2);
                    result.put(key, diff);
                }


            } else {

                if (!Objects.equals(v1, v2)) {
                    JSONObject diff = new JSONObject();
                    diff.put("v1", v1);
                    diff.put("v2", v2);
                    result.put(key, diff);
                }
            }


            // ------------------------------------- DEBUG -------------------------------------


            if ("vol".equals(key)) {
                if (!v1.equals(v2)) {
                    log.error("vol - err     >>>     v1={}, v2={}", v1, v2);
                } else {
                    log.debug("vol - suc     >>>     v1={}, v2={}", v1, v2);
                }
            }


            // ------------------------------------- DEBUG -------------------------------------
        }

        return result;
    }


    private static List<LdayDTO> merge(List<LdayDTO> klineReport__ldayDTOList,
                                       List<LdayDTO> lday__ldayDTOList) {


        int size1 = size(klineReport__ldayDTOList);
        int size2 = size(lday__ldayDTOList);


        // -------------------------------------------------------------------------------------------------------------


        LocalTime now = LocalTime.now();


        // 盘中   ->   导出过  行情数据
        // 盘后   ->   已下载  [盘后数据]     ->     以 xx.day 为准


        // 盘中   ->   导出过  行情数据
        if (size1 == size2
                // 盘后（>=16点）   ->   已下载  [盘后数据]
                && now.isAfter(LocalTime.of(16, 00))) {


            // 盘后   ->   已下载 [盘后数据]     ->     以 xx.day 为准（舍弃 盘中导出 行情数据）
            klineReport__ldayDTOList.removeLast();
            size1--;
        }


        // -------------------------------------------------------------------------------------------------------------


        List<LdayDTO> dtoList = Lists.newArrayList(klineReport__ldayDTOList);


        if (size1 < size2) {
            List<LdayDTO> subList = lday__ldayDTOList.subList(size1, size2);
            dtoList.addAll(subList);
        }

        return dtoList;
    }


    private static int size(Collection list) {
        return list == null ? 0 : list.size();
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
        private Long vol;
        private BigDecimal changePct;
        // 涨跌额       C - pre_C          |          今日收盘价 × 涨跌幅 / (1+涨跌幅)
        private BigDecimal changePrice;
        // 振幅       H/L   x100-100
        private BigDecimal rangePct;

        // ----------------------------------- 自动计算 字段


        // 换手率
        private BigDecimal turnoverPct;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


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


        // stockCode : 002364 , idx : 3612 , date : 2025-03-13 , diffFields : {"vol":{"v1":"88832504","v2":"2911100"}}
        // stockCode : 002518 , idx : 3466 , date : 2025-03-13 , diffFields : {"vol":{"v1":"46390892","v2":"16293000"}}
        // stockCode : 601988 , idx : 1303 , date : 2015-06-09 , diffFields : {"vol":{"v1":"4795353100","v2":"47953531"}}
        // stockCode : 601988 , idx : 1323 , date : 2015-07-08 , diffFields : {"vol":{"v1":"5109897400","v2":"51098974"}}
        // stockCode : 920249 , idx : 2250 , date : 2025-02-13 , diffFields : {"vol":{"v1":"26171562","v2":"9244100"}}


        List<LdayDTO> stockDataList = parseByStockCode("SPY");
//        List<LdayDTO> stockDataList = parseByStockCode("00700");
//        List<LdayDTO> stockDataList = parseByStockCode("513120");
        for (LdayDTO e : stockDataList) {
            String[] item = {e.code, String.valueOf(e.tradeDate), String.format("%.2f", e.open), String.format("%.2f", e.high), String.format("%.2f", e.low), String.format("%.2f", e.close), e.amount.toPlainString(), String.valueOf(e.vol), String.format("%.2f", e.changePct)};
            System.out.println(JSON.toJSONString(item));
        }


        // ----------


//        List<LdayDTO> stockDataList = parseLdayByFilePath(filePath_zs);
//        for (LdayDTO e : stockDataList) {
//            String[] item = {e.code, String.valueOf(e.tradeDate), String.format("%.2f", e.open), String.format("%.2f", e.high), String.format("%.2f", e.low), String.format("%.2f", e.close), String.valueOf(e.amount), String.valueOf(e.vol), String.format("%.2f", e.changePct)};
//            System.out.println(JSON.toJSONString(item));
//        }


        System.out.println("---------------------------------- code：" + stockDataList.get(0).code + "     总数：" + stockDataList.size());
    }


}