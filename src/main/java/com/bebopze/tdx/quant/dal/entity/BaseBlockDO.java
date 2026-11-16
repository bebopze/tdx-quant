package com.bebopze.tdx.quant.dal.entity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bebopze.tdx.quant.common.constant.BlockTypeEnum;
import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.util.CompressUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 板块/指数-实时行情（以 tdx 为准）
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
@Getter
@Setter
@ToString
@TableName("base_block")
@Schema(name = "BaseBlockDO", description = "板块/指数-实时行情（以 tdx 为准）")
public class BaseBlockDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 板块代码
     */
    @TableField("code")
    @Schema(description = "板块代码")
    private String code;

    /**
     * 板块名称
     */
    @TableField("name")
    @Schema(description = "板块名称")
    private String name;

    /**
     * 板块代码-path
     */
    @TableField("code_path")
    @Schema(description = "板块代码-path")
    private String codePath;

    /**
     * 板块名称-path
     */
    @TableField("name_path")
    @Schema(description = "板块名称-path")
    private String namePath;

    /**
     * 父-ID（行业板块）
     */
    @TableField("parent_id")
    @Schema(description = "父-ID（行业板块）")
    private Long parentId;

    /**
     * tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；
     */
    @TableField("type")
    @Schema(description = "tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；")
    private Integer type;

    /**
     * 行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；
     */
    @TableField("level")
    @Schema(description = "行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；")
    private Integer level;

    /**
     * 是否最后一级：0-否；1-是；
     */
    @TableField("end_level")
    @Schema(description = "是否最后一级：0-否；1-是；")
    private Integer endLevel;

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


    // -------------------------- 近N日（默认：近500日） ->  每日 更新1次（盘中 -> 可 每分钟 更新1次）


    /**
     * 近N日K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）
     */
    @JsonIgnore
    @TableField(value = "kline_nd")
    @Schema(description = "近N日K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）")
    private byte[] klineNd;

    /**
     * 近N日K线数据-JSON（byte[]）压缩前长度（解压算法参数）
     */
    @TableField(value = "kline_nd_raw_len")
    @Schema(description = "近N日K线数据-JSON（byte[]）压缩前长度（解压算法参数）")
    private Integer klineNdRawLen;

    /**
     * 近N日扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）
     */
    @JsonIgnore
    @TableField(value = "ext_data_nd")
    @Schema(description = "近N日扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）")
    private byte[] extDataNd;

    /**
     * 近N日扩展数据-JSON（byte[]）压缩前长度（解压算法参数）
     */
    @TableField(value = "ext_data_nd_raw_len")
    @Schema(description = "近N日扩展数据-JSON（byte[]）压缩前长度（解压算法参数）")
    private Integer extDataNdRawLen;


    // -------------------------- 全量  ->  最长可 1年 更新1次


    /**
     * 全量K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）
     */
    @JsonIgnore
    @TableField(value = "kline_his"/*, typeHandler = KlineListTypeHandler.class*/)
    @Schema(description = "全量K线数据-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）")
    private byte[] klineHis;

    /**
     * 全量K线数据-JSON（byte[]）压缩前长度（解压算法参数）
     */
    @TableField(value = "kline_his_raw_len")
    @Schema(description = "全量K线数据-JSON（byte[]）压缩前长度（解压算法参数）")
    private Integer klineHisRawLen;

    /**
     * 全量扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）
     */
    @JsonIgnore
    @TableField(value = "ext_data_his")
    @Schema(description = "全量扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）")
    private byte[] extDataHis;

    /**
     * 全量扩展数据-JSON（byte[]）压缩前长度（解压算法参数）
     */
    @TableField(value = "ext_data_his_raw_len")
    @Schema(description = "全量扩展数据-JSON（byte[]）压缩前长度（解压算法参数）")
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


    public void setKlineStr(List<String> klineStrList, UpdateTypeEnum updateTypeEnum) {

        if (Objects.equals(updateTypeEnum, UpdateTypeEnum.N_DAY)) {

            setKlineNdStr(JSON.toJSONString(klineStrList));

        } else if (Objects.equals(updateTypeEnum, UpdateTypeEnum.HIS)) {

            setKlineHisStr(JSON.toJSONString(klineStrList));

            // HIS 更新时   ->   附带 同步更新1次 N_DAY
            List<String> nd_klineStrList = ListUtil.lastN(klineStrList, UpdateTypeEnum.N_DAY.days);
            setExtDataNdStr(JSON.toJSONString(nd_klineStrList));
        }
    }


    public void setExtDataStr(List<String> extDataStrList, UpdateTypeEnum updateTypeEnum) {

        if (Objects.equals(updateTypeEnum, UpdateTypeEnum.N_DAY)) {

            setExtDataNdStr(JSON.toJSONString(extDataStrList));

        } else if (Objects.equals(updateTypeEnum, UpdateTypeEnum.HIS)) {

            setExtDataHisStr(JSON.toJSONString(extDataStrList));

            // HIS 更新时   ->   附带 同步更新1次 N_DAY
            List<String> nd_extDataStrList = ListUtil.lastN(extDataStrList, UpdateTypeEnum.N_DAY.days);
            setExtDataNdStr(JSON.toJSONString(nd_extDataStrList));
        }
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 自动压缩 K线数据（String  ->  byte[]）
     *
     * @param klineNdStr K线数据-JSON字符串
     */
    public void setKlineNdStr(String klineNdStr) {
        byte[] raw = klineNdStr == null ? null : klineNdStr.getBytes(StandardCharsets.UTF_8);
        this.klineNd = CompressUtil.zstdCompress(raw);
        this.klineNdRawLen = klineNdStr == null ? null : raw.length;
    }

    /**
     * 自动解压 K线数据（byte[]  ->  String）
     *
     * @return K线数据-JSON字符串
     */
    public String getKlineNdStr() {
        byte[] decompressed = CompressUtil.zstdDecompress(klineNd, klineNdRawLen);
        return decompressed == null ? null : new String(decompressed, StandardCharsets.UTF_8);
    }


    /**
     * 自动压缩 扩展数据（String  ->  byte[]）
     *
     * @param extDataNdStr 扩展数据-JSON字符串
     */
    public void setExtDataNdStr(String extDataNdStr) {
        byte[] raw = extDataNdStr == null ? null : extDataNdStr.getBytes(StandardCharsets.UTF_8);
        this.extDataNd = CompressUtil.zstdCompress(raw);
        this.extDataNdRawLen = extDataNdStr == null ? null : raw.length;
    }

    /**
     * 自动解压 扩展数据（byte[]  ->  String）
     *
     * @return 扩展数据-JSON字符串
     */
    public String getExtDataNdStr() {
        byte[] decompressed = CompressUtil.zstdDecompress(extDataNd, extDataNdRawLen);
        return decompressed == null ? null : new String(decompressed, StandardCharsets.UTF_8);
    }


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
            if (klineDTOList == null && (klineHis != null || klineNd != null)) {


                if (all) {

                    List<KlineDTO> klineDTOS_his = ConvertStockKline.str2DTOList(getKlineHisStr());
                    List<KlineDTO> klineDTOS_nd = ConvertStockKline.str2DTOList(getKlineNdStr());


                    // 去重 + add
                    if (CollectionUtils.isNotEmpty(klineDTOS_his)) {

                        LocalDate his_end_date = klineDTOS_his.getLast().getDate();
                        for (KlineDTO nd : klineDTOS_nd) {
                            if (nd.getDate().isAfter(his_end_date)) {
                                klineDTOS_his.add(nd);
                            }
                        }

                    } else {
                        klineDTOS_his = klineDTOS_nd;
                    }


                    // 只转换1次
                    klineDTOList = klineDTOS_his;


                } else {

                    // 只转换1次
                    klineDTOList = ConvertStockKline.str2DTOList(getKlineNdStr());
                }


                // -------------------------


                // Str -> null（否则，会同时存在2份【klineHis + klineDTOList】 ->  OOM）
                klineNd = null;
                klineNdRawLen = null;

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
            if (extDataDTOList == null && (extDataHis != null || extDataNd != null)) {


                if (all) {

                    List<ExtDataDTO> extDataDTOS_his = ConvertStockExtData.extDataHis2DTOList(getExtDataHisStr());
                    List<ExtDataDTO> extDataDTOS_nd = ConvertStockExtData.extDataHis2DTOList(getExtDataNdStr());


                    // 去重 + add
                    if (CollectionUtils.isNotEmpty(extDataDTOS_his)) {

                        LocalDate his_end_date = extDataDTOS_his.getLast().getDate();
                        for (ExtDataDTO nd : extDataDTOS_nd) {
                            if (nd.getDate().isAfter(his_end_date)) {
                                extDataDTOS_his.add(nd);
                            }
                        }

                    } else {
                        extDataDTOS_his = extDataDTOS_nd;
                    }


                    // 只转换1次
                    extDataDTOList = extDataDTOS_his;


                } else {

                    // 只转换1次
                    extDataDTOList = ConvertStockExtData.extDataHis2DTOList(getExtDataNdStr());
                }


                // -------------------------


                // Str -> null（否则，会同时存在2份【extDataHis + extDataDTOList】 ->  OOM）
                extDataNd = null;
                extDataNdRawLen = null;

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
        return klineHis != null || klineNd != null || klineDTOList != null;
    }


    /**
     * 是否含扩展数据（不触发 全历史 DTO 惰性转换）
     */
    public boolean hasExtData() {
        return extDataHis != null || extDataNd != null || extDataDTOList != null;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public String getTypeDesc() {
        return BlockTypeEnum.getDescByType(type);
    }

}