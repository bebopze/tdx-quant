package com.bebopze.tdx.quant.common.domain.trade.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


/**
 * 实时行情：买5 / 卖5
 *
 * @author: bebopze
 * @date: 2025/5/11
 */
@Data
public class SHSZQuoteSnapshotResp implements Serializable {


    //   {
    //       code: "300059",
    //       name: "东方财富",
    //       sname: "东方财富",
    //       flag: 0,
    //       transMarket: 0,
    //       transType: 80,
    //       topprice: "25.46",
    //       bottomprice: "16.98",
    //       nt: "25.00",
    //       nb: "16.66",
    //       status: 0,
    //       tradeperiod: 11,
    //       fivequote: {
    //            yesClosePrice: "21.22",
    //            yesSettlePrice: "0.00",
    //            yesClosePriceE: null,
    //            openPrice: "21.21",
    //            sale1: "20.83",
    //            sale2: "20.84",
    //            sale3: "20.85",
    //            sale4: "20.86",
    //            sale5: "20.87",
    //            buy1: "20.82",
    //            buy2: "20.81",
    //            buy3: "20.80",
    //            buy4: "20.79",
    //            buy5: "20.78",
    //            sale1_count: 7368,
    //            sale2_count: 4125,
    //            sale3_count: 4639,
    //            sale4_count: 3051,
    //            sale5_count: 3036,
    //            buy1_count: 7529,
    //            buy2_count: 9263,
    //            buy3_count: 18581,
    //            buy4_count: 3306,
    //            buy5_count: 5282
    //       },
    //       realtimequote: {
    //            open: "21.21",
    //            high: "21.27",
    //            low: "20.77",
    //            avg: "20.91",
    //            zd: "-0.39",
    //            zdf: "-1.84%",
    //            turnover: "1.65%",
    //            currentPrice: "20.83",
    //            settlePrice: "209.07",
    //            volume: "2203757",
    //            amount: "4607385600",
    //            wp: "875529",
    //            np: "1328228",
    //            time: "15:34:39",
    //            date: "20250509",
    //            openE: null,
    //            highE: null,
    //            lowE: null,
    //            zdE: null,
    //            currentPriceE: null
    //       },
    //       pricelimit: {
    //            upper: "21.25",
    //            lower: "20.41"
    //       }
    //   }


    // -------------------------------------------


    //       code: "300059",
    //       name: "东方财富",
    //       sname: "东方财富",
    //
    //       flag: 0,
    //       transMarket: 0,
    //       transType: 80,
    //
    //       topprice: "25.46",
    //       bottomprice: "16.98",
    //       nt: "25.00",
    //       nb: "16.66",
    //
    //       status: 0,
    //       tradeperiod: 11,


    // 证券代码
    private String code;
    // 证券名称
    private String name;
    // 简称？
    private String sname;


    // -
    private String flag;
    private String transMarket;
    private String transType;


    // 涨停价（昨日收盘价 计算 - 今日有效）
    private BigDecimal topprice;
    // 跌停价（昨日收盘价 计算 - 今日有效）
    private BigDecimal bottomprice;

    // 预估-涨停价（今日收盘价 计算 - 明日有效）
    private BigDecimal nt;
    // 预估-跌停价（今日收盘价 计算 - 明日有效）
    private BigDecimal nb;


    // -
    private String status;
    private String tradeperiod;


    // ------------------------------------------- 买5 / 卖5
    private FivequoteDTO fivequote;


    //       fivequote: {
    //            yesClosePrice: "21.22",
    //            yesSettlePrice: "0.00",
    //            yesClosePriceE: null,
    //            openPrice: "21.21",
    //            sale1: "20.83",
    //            sale2: "20.84",
    //            sale3: "20.85",
    //            sale4: "20.86",
    //            sale5: "20.87",
    //            buy1: "20.82",
    //            buy2: "20.81",
    //            buy3: "20.80",
    //            buy4: "20.79",
    //            buy5: "20.78",
    //            sale1_count: 7368,
    //            sale2_count: 4125,
    //            sale3_count: 4639,
    //            sale4_count: 3051,
    //            sale5_count: 3036,
    //            buy1_count: 7529,
    //            buy2_count: 9263,
    //            buy3_count: 18581,
    //            buy4_count: 3306,
    //            buy5_count: 5282
    //       },


    /**
     * `买5 / 卖5
     */
    @Data
    public static class FivequoteDTO implements Serializable {


        // 昨日-收盘价
        private BigDecimal yesClosePrice;
        // -
        private BigDecimal yesSettlePrice;
        // -
        private BigDecimal yesClosePriceE;
        // 今日-开盘价
        private BigDecimal openPrice;


        // 卖1->卖5       小 -> 大

        // 11
        private BigDecimal sale1;
        private BigDecimal sale2;
        private BigDecimal sale3;
        private BigDecimal sale4;
        // 15（报价最高）
        private BigDecimal sale5;   // 一键买入


        // 买1->买5       大 -> 小

        // 15
        private BigDecimal buy1;
        private BigDecimal buy2;
        private BigDecimal buy3;
        private BigDecimal buy4;
        // 11（出价最低）
        private BigDecimal buy5;   // 一键卖出


        // S量
        private Integer sale1_count;
        private Integer sale2_count;
        private Integer sale3_count;
        private Integer sale4_count;
        private Integer sale5_count;

        // B量
        private Integer buy1_count;
        private Integer buy2_count;
        private Integer buy3_count;
        private Integer buy4_count;
        private Integer buy5_count;
    }


    // ------------------------------------------- 实时报价
    private RealtimequoteDTO realtimequote;


    //       realtimequote: {
    //            open: "21.21",
    //            high: "21.27",
    //            low: "20.77",
    //            avg: "20.91",
    //            zd: "-0.39",
    //            zdf: "-1.84%",
    //            turnover: "1.65%",
    //            currentPrice: "20.83",
    //            settlePrice: "209.07",
    //            volume: "2203757",
    //            amount: "4607385600",
    //            wp: "875529",
    //            np: "1328228",
    //            time: "15:34:39",
    //            date: "20250509",
    //
    //            openE: null,
    //            highE: null,
    //            lowE: null,
    //            zdE: null,
    //            currentPriceE: null
    //       },


    /**
     * 实时报价
     */
    @Data
    public static class RealtimequoteDTO implements Serializable {


        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        // 均价
        private BigDecimal avg;


        // 涨跌额（-0.39）
        private BigDecimal zd;
        // 涨跌幅（-1.84%）
        private String zdf;
        // 换手率（1.65%）
        private String turnover;


        // 当前价格（实时）
        private BigDecimal currentPrice;


        //
        private BigDecimal settlePrice;


        // 成交量（2203757）
        private Integer volume;
        // 成交额（4607385600 - 46亿）
        private BigDecimal amount;


        // 875529
        private String wp;
        // 1328228
        private String np;


        // 时间（15:34:39）
        private String time;
        // 日期（20250509）
        private String date;


        // ----- 自定义 字段
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime dateTime;


        // --------------------------- 已废弃（全 null）
        private String openE;
        private String highE;
        private String lowE;
        private String zdE;
        private String currentPriceE;


        // ---------------------------------------- convert
        public BigDecimal getZdf() {
            // -1.84%   ->   -1.84
            return new BigDecimal(zdf.split("%")[0]);
        }

        public BigDecimal getTurnover() {
            // 1.65%   ->   1.65
            return new BigDecimal(turnover.split("%")[0]);
        }


        public LocalDateTime getDateTime() {

            LocalDate _date = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
            LocalTime _time = LocalTime.parse(time);

            LocalDateTime dateTime = LocalDateTime.of(_date, _time);
            return dateTime;
        }
    }


    // ------------------------------------------- 价格笼子（挂单价不能超过2%）
    private PricelimitDTO pricelimit;

    //   pricelimit: {
    //       upper: 21.25,
    //       lower: 20.41
    //   }


    @Data
    public static class PricelimitDTO implements Serializable {


        // 价格上限   （ C x 1.02 ）
        private BigDecimal upper;
        // 价格下限   （ C x 0.98 ）
        private BigDecimal lower;
    }

}