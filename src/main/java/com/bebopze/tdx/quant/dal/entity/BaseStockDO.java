package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.bebopze.tdx.quant.common.constant.StockLimitEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.util.CompressUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 股票-实时行情
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Getter
@Setter
@ToString
@TableName("base_stock")
@Schema(name = "BaseStockDO", description = "股票-实时行情")
public class BaseStockDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 股票代码
     */
    @TableField("code")
    @Schema(description = "股票代码")
    private String code;

    /**
     * 股票名称
     */
    @TableField("name")
    @Schema(description = "股票名称")
    private String name;

    /**
     * 股票类型：1-A股；2-ETF；
     */
    @TableField("type")
    @Schema(description = "股票类型：1-A股；2-ETF；")
    private Integer type;

    /**
     * 通达信-市场类型：0-深交所；1-上交所；2-北交所；
     */
    @TableField("tdx_market_type")
    @Schema(description = "通达信-市场类型：0-深交所；1-上交所；2-北交所；")
    private Integer tdxMarketType;

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
     * K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）
     */
    @JsonIgnore
    @TableField(value = "kline_his"/*, typeHandler = KlineListTypeHandler.class*/)
    @Schema(description = "K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）")
    private byte[] klineHis;
    // private String klineHis;
    // private List<KlineDTO> klineHis;

    /**
     * K线数据-JSON（byte[]）压缩前长度（解压算法参数）
     */
    @TableField(value = "kline_his_raw_len")
    @Schema(description = "K线数据-JSON（byte[]）压缩前长度（解压算法参数）")
    private Integer klineHisRawLen;

    /**
     * 扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）
     */
    @JsonIgnore
    @TableField(value = "ext_data_his"/*, typeHandler = KlineListTypeHandler.class*/)
    @Schema(description = "扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）")
    private byte[] extDataHis;
    // private String extDataHis;
    // private List<ExtDataDTO> extDataHis;

    /**
     * 扩展数据-JSON（byte[]）压缩前长度（解压算法参数）
     */
    @TableField(value = "ext_data_his_raw_len")
    @Schema(description = "扩展数据-JSON（byte[]）压缩前长度（解压算法参数）")
    private Integer extDataHisRawLen;

    /**
     * 创建时间
     */
    @TableField("gmt_create")
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField("gmt_modify")
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtModify;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 自动压缩 K线数据（String  ->  byte[]）
     *
     * @param klineHisStr K线数据-JSON字符串
     */
    public void setKlineHisStr(String klineHisStr) {
        byte[] raw = klineHisStr == null ? null : klineHisStr.getBytes(StandardCharsets.UTF_8);
        this.klineHis = CompressUtil.zstdCompress(raw);
        this.klineHisRawLen = klineHisStr == null ? null : raw.length;
    }

    /**
     * 自动解压 K线数据（byte[]  ->  String）
     *
     * @return K线数据-JSON字符串
     */
    public String getKlineHisStr() {
        byte[] decompressed = CompressUtil.zstdDecompress(klineHis, klineHisRawLen);
        return decompressed == null ? null : new String(decompressed, StandardCharsets.UTF_8);
    }


    /**
     * 自动压缩 扩展数据（String  ->  byte[]）
     *
     * @param extDataHisStr 扩展数据-JSON字符串
     */
    public void setExtDataHisStr(String extDataHisStr) {
        byte[] raw = extDataHisStr == null ? null : extDataHisStr.getBytes(StandardCharsets.UTF_8);
        this.extDataHis = CompressUtil.zstdCompress(raw);
        this.extDataHisRawLen = extDataHisStr == null ? null : raw.length;
    }

    /**
     * 自动解压 扩展数据（byte[]  ->  String）
     *
     * @return 扩展数据-JSON字符串
     */
    public String getExtDataHisStr() {
        byte[] decompressed = CompressUtil.zstdDecompress(extDataHis, extDataHisRawLen);
        return decompressed == null ? null : new String(decompressed, StandardCharsets.UTF_8);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TableField(exist = false)
    private List<KlineDTO> klineDTOList;

    @TableField(exist = false)
    private List<ExtDataDTO> extDataDTOList;


    /**
     * K线数据-JSON字符串  ->  K线数据-DTO列表         （延迟加载）
     *
     * @return K线数据-DTO列表
     */
    @Schema(description = "K线数据-JSON字符串  ->  K线数据-DTO列表")
    public List<KlineDTO> getKlineDTOList() {
        // 手动set过的，直接返回（只转换1次）
        if (klineDTOList != null) {
            return klineDTOList;
        }


        synchronized (this) {
            // 双重检测
            if (klineDTOList == null && klineHis != null) {

                // 只转换1次
                klineDTOList = ConvertStockKline.str2DTOList(getKlineHisStr());

                // Str -> null（否则，会同时存在2份【klineHis + klineDTOList】 ->  OOM）
                klineHis = null;
                klineHisRawLen = null;
            }


            return klineDTOList != null ? klineDTOList : Lists.newArrayList();
        }
    }


    /**
     * 扩展数据-JSON字符串  ->  扩展数据-DTO列表         （延迟加载）
     *
     * @return 扩展数据-DTO列表
     */
    @Schema(description = "扩展数据-JSON字符串  -> 扩展数据-DTO列表")
    public List<ExtDataDTO> getExtDataDTOList() {
        // 手动set过的，直接返回（只转换1次）
        if (extDataDTOList != null) {
            return extDataDTOList;
        }


        synchronized (this) {
            // 双重检测
            if (extDataDTOList == null && extDataHis != null) {

                // 只转换1次
                extDataDTOList = ConvertStockExtData.extDataHis2DTOList(getExtDataHisStr());

                // Str -> null（否则，会同时存在2份【extDataHis + extDataDTOList】 ->  OOM）
                extDataHis = null;
                extDataHisRawLen = null;
            }


            return extDataDTOList != null ? extDataDTOList : Lists.newArrayList();
        }
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 涨跌幅 限制
     *
     * @return
     */
    public int getChangePctLimit() {
        return StockLimitEnum.getChgPctLimit(code, name);
    }

    /**
     * 是否 20CM（含30CM）
     *
     * @return
     */
    public boolean is20CM() {
        return StockLimitEnum.is20CM(code, name);
    }

}