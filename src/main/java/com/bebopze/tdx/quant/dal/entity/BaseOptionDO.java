package com.bebopze.tdx.quant.dal.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.util.CompressUtil;
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
 * 期权-实时行情
 * </p>
 *
 * @author bebopze
 * @since 2026-11-22
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
     * 期权合约编码
     */
    @TableField("code")
    @Schema(description = "期权合约编码")
    private String code;

    /**
     * 期权合约代码
     */
    @TableField("symbol")
    @Schema(description = "期权合约代码")
    private String symbol;

    /**
     * 期权合约名称
     */
    @TableField("name")
    @Schema(description = "期权合约名称")
    private String name;

    /**
     * 合约类型：C-认购；P-认沽；
     */
    @TableField("call_put_type")
    @Schema(description = "合约类型：C-认购；P-认沽；")
    private String callPutType;

    /**
     * 行权价格
     */
    @TableField("strike_price")
    @Schema(description = "行权价格")
    private BigDecimal strikePrice;

    /**
     * 合约乘数（ETF期权：10000）
     */
    @TableField("multiplier")
    @Schema(description = "合约乘数（ETF期权：10000）")
    private Integer multiplier;

    /**
     * 期权标的类型：1-ETF；2-指数；3-商品；4-期货；
     */
    @TableField("underlying_type")
    @Schema(description = "期权标的类型：1-ETF；2-指数；3-商品；4-期货；")
    private Integer underlyingType;

    /**
     * 标的证券code
     */
    @TableField("underlying_code")
    @Schema(description = "标的证券code")
    private String underlyingCode;

    /**
     * 标的证券name
     */
    @TableField("underlying_name")
    @Schema(description = "标的证券name")
    private String underlyingName;

    /**
     * 合约开始日期
     */
    @TableField("start_date")
    @Schema(description = "合约开始日期")
    private LocalDate startDate;

    /**
     * 合约结束日期
     */
    @TableField("end_date")
    @Schema(description = "合约结束日期")
    private LocalDate endDate;

    /**
     * 通达信-市场类型：0-深交所；1-上交所；2-北交所；12-美股指数；27-香港指数；31-香港主板；48-香港创业板；74-美股；
     */
    @TableField("tdx_market_type")
    @Schema(description = "通达信-市场类型：0-深交所；1-上交所；2-北交所；12-美股指数；27-香港指数；31-香港主板；48-香港创业板；74-美股；")
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
    @JSONField(serialize = false)
    private List<KlineDTO> klineDTOList;

    @TableField(exist = false)
    @JSONField(serialize = false)
    private List<ExtDataDTO> extDataDTOList;


    public List<KlineDTO> getKlineDTOList() {
        return getKlineDTOList(true);
    }

    public List<ExtDataDTO> getExtDataDTOList() {
        return getExtDataDTOList(true);
    }


    /**
     * K线数据-JSON字符串  ->  K线数据-DTO列表         （延迟加载）
     *
     * @param all 是否全历史数据：true-全历史（his + nd），false-近N日（nd）
     * @return K线数据-DTO列表
     */
    @Schema(description = "K线数据-JSON字符串  ->  K线数据-DTO列表")
    public List<KlineDTO> getKlineDTOList(boolean all) {
        // 手动set过的，直接返回（只转换1次）
        if (klineDTOList != null) {
            return klineDTOList;
        }


        synchronized (this) {
            // 双重检测
            if (klineDTOList == null && klineHis != null) {


                // 只转换1次
                klineDTOList = ConvertStockKline.str2DTOList(getKlineHisStr());


                // -------------------------


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
     * @param all 是否全历史数据：true-全历史（his + nd），false-近N日（nd）
     * @return 扩展数据-DTO列表
     */
    @Schema(description = "扩展数据-JSON字符串  -> 扩展数据-DTO列表")
    public List<ExtDataDTO> getExtDataDTOList(boolean all) {
        // 手动set过的，直接返回（只转换1次）
        if (extDataDTOList != null) {
            return extDataDTOList;
        }


        synchronized (this) {
            // 双重检测
            if (extDataDTOList == null && extDataHis != null) {


                // 只转换1次
                extDataDTOList = ConvertStockExtData.extDataHis2DTOList(getExtDataHisStr());


                // -------------------------


                // Str -> null（否则，会同时存在2份【extDataHis + extDataDTOList】 ->  OOM）
                extDataHis = null;
                extDataHisRawLen = null;
            }


            return extDataDTOList != null ? extDataDTOList : Lists.newArrayList();
        }
    }


    /**
     * 是否含 K 线数据（不触发 全历史 DTO 惰性转换）
     *
     * <p>
     * 用于 {@code InitDataServiceImpl} 的空数据过滤：原先用 {@code CollectionUtils.isNotEmpty(e.getKlineDTOList())}
     * 判空，会把 5370 只股票的 全历史 对象图（约 9.6 GB）全部建出来 —— 即使随后只截取 1 年窗口。
     */
    public boolean hasKlineData() {
        return klineHis != null || klineDTOList != null;
    }


    /**
     * 是否含扩展数据（不触发 全历史 DTO 惰性转换）
     */
    public boolean hasExtData() {
        return extDataHis != null || extDataDTOList != null;
    }


}