package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 期权-实时行情
 * </p>
 *
 * @author bebopze
 * @since 2026-11-16
 */
@Getter
@Setter
@ToString
@TableName("base_option")
@Schema(name = "BaseOptionDO", description = "期权-实时行情")
public class BaseOptionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 期权代码
     */
    @TableField("code")
    @Schema(description = "期权代码")
    private String code;

    /**
     * 期权名称
     */
    @TableField("name")
    @Schema(description = "期权名称")
    private String name;

    /**
     * 期权类型：1-ETF期权；2-商品期权；
     */
    @TableField("type")
    @Schema(description = "期权类型：1-ETF期权；2-商品期权；")
    private Integer type;

    /**
     * 通达信-市场类型：0-深交所；1-上交所；2-北交所；12-美股指数；27-香港指数；31-香港主板；48-香港创业板；74-美股；
     */
    @TableField("tdx_market_type")
    @Schema(description = "通达信-市场类型：0-深交所；1-上交所；2-北交所；12-美股指数；27-香港指数；31-香港主板；48-香港创业板；74-美股；")
    private Integer tdxMarketType;

    /**
     * 细分行业（仅ETF导入后 关联概念板块 用）
     */
    @TableField("block_name")
    @Schema(description = "细分行业（仅ETF导入后 关联概念板块 用）")
    private String blockName;

    /**
     * 交易日期
     */
    @TableField("trade_date")
    @Schema(description = "交易日期")
    private LocalDate tradeDate;

    /**
     * 开盘价
     */
    @TableField("open")
    @Schema(description = "开盘价")
    private BigDecimal open;

    /**
     * 最高价
     */
    @TableField("high")
    @Schema(description = "最高价")
    private BigDecimal high;

    /**
     * 最低价
     */
    @TableField("low")
    @Schema(description = "最低价")
    private BigDecimal low;

    /**
     * 收盘价
     */
    @TableField("close")
    @Schema(description = "收盘价")
    private BigDecimal close;

    /**
     * 昨日收盘价
     */
    @TableField("prev_close")
    @Schema(description = "昨日收盘价")
    private BigDecimal prevClose;

    /**
     * 成交量
     */
    @TableField("volume")
    @Schema(description = "成交量")
    private Long volume;

    /**
     * 成交额
     */
    @TableField("amount")
    @Schema(description = "成交额")
    private BigDecimal amount;

    /**
     * 振幅（%）
     */
    @TableField("range_pct")
    @Schema(description = "振幅（%）")
    private BigDecimal rangePct;

    /**
     * 涨跌幅（%）
     */
    @TableField("change_pct")
    @Schema(description = "涨跌幅（%）")
    private BigDecimal changePct;

    /**
     * 涨跌额
     */
    @TableField("change_price")
    @Schema(description = "涨跌额")
    private BigDecimal changePrice;

    /**
     * 换手率（%）
     */
    @TableField("turnover_pct")
    @Schema(description = "换手率（%）")
    private BigDecimal turnoverPct;

    /**
     * 历史K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）
     */
    @TableField("kline_his")
    @Schema(description = "历史K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）")
    private byte[] klineHis;

    /**
     * 历史K线数据-JSON（byte[]） 压缩前长度（解压算法参数）
     */
    @TableField("kline_his_raw_len")
    @Schema(description = "历史K线数据-JSON（byte[]） 压缩前长度（解压算法参数）")
    private Integer klineHisRawLen;

    /**
     * 历史扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）
     */
    @TableField("ext_data_his")
    @Schema(description = "历史扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）")
    private byte[] extDataHis;

    /**
     * 历史扩展数据-JSON（byte[]） 压缩前长度（解压算法参数）
     */
    @TableField("ext_data_his_raw_len")
    @Schema(description = "历史扩展数据-JSON（byte[]） 压缩前长度（解压算法参数）")
    private Integer extDataHisRawLen;

    /**
     * 创建时间
     */
    @TableField("gmt_create")
    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField("gmt_modify")
    @Schema(description = "更新时间")
    private LocalDateTime gmtModify;


}