package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bebopze.tdx.quant.common.constant.StockLimitEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
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
     * 涨跌幅
     */
    @TableField("change_pct")
    @Schema(description = "涨跌幅")
    private BigDecimal changePct;

    /**
     * 振幅
     */
    @TableField("range_pct")
    @Schema(description = "振幅")
    private BigDecimal rangePct;

    /**
     * 换手率
     */
    @TableField("turnover_pct")
    @Schema(description = "换手率")
    private BigDecimal turnoverPct;

    /**
     * 历史行情-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）
     */
    @JsonIgnore
    @TableField(value = "kline_his")
    @Schema(description = "历史行情-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）")
    private String klineHis;

    /**
     * 扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）
     */
    @JsonIgnore
    @TableField(value = "ext_data_his")
    @Schema(description = "扩展数据-JSON（[日期,RPS10,RPS20,RPS50,RPS120,RPS250]）")
    private String extDataHis;

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


    public List<KlineDTO> getKlineDTOList() {
        if (klineHis == null) {
            return Lists.newArrayList();
        }
        return ConvertStockKline.str2DTOList(klineHis);
    }


    public List<ExtDataDTO> getExtDataDTOList() {
        if (extDataHis == null) {
            return Lists.newArrayList();
        }
        return ConvertStockExtData.extDataHis2DTOList(extDataHis);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 涨跌幅 限制
     *
     * @return
     */
    public int getChangePctLimit() {
        return StockLimitEnum.getChangePctLimit(code, name);
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