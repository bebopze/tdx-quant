package com.bebopze.tdx.quant.dal.entity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
     * 复权后收盘价（可选）
     */
    @TableField("adj_close")
    @Schema(description = "复权后收盘价（可选）")
    private BigDecimal adjClose;

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
     * 历史行情-JSON（[日期,O,C,H,L,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）
     */
    @TableField(value = "kline_his", select = false)
    @Schema(description = "历史行情-JSON（[日期,O,C,H,L,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）")
    private String klineHis;

    /**
     * 扩展数据 指标-JSON（[]）
     */
    @TableField(value = "ext_data_his", select = false)
    @Schema(description = "扩展数据 指标-JSON（[]）")
    private String extDataHis;

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


    public List<ExtDataDTO> getExtDataHis() {
        return ConvertStockExtData.extDataHis2DTOList(extDataHis);
    }


    public List<KlineDTO> getKLineHis() {
        return ConvertStockKline.str2DTOList(klineHis);
    }


    @JsonIgnore
    public String getKLineHisOriginal() {
        return klineHis;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {
        getRps2();
    }

    public static ExtDataDTO getRps2() {
        String rps = "{\n" +
                "  \"rps10\": [\n" +
                "    31.04426,\n" +
                "    31.13828,\n" +
                "    31.2699\n" +
                "  ],\n" +
                "  \"rps20\": [\n" +
                "    31.04426,\n" +
                "    31.13828,\n" +
                "    31.2699\n" +
                "  ],\n" +
                "  \"rps50\": [\n" +
                "    31.04426,\n" +
                "    31.13828,\n" +
                "    31.2699\n" +
                "  ],\n" +
                "  \"rps120\": [\n" +
                "    31.04426,\n" +
                "    31.13828,\n" +
                "    31.2699\n" +
                "  ],\n" +
                "  \"rps250\": [\n" +
                "    31.04426,\n" +
                "    31.13828,\n" +
                "    31.2699\n" +
                "  ]\n" +
                "}";


        ExtDataDTO rpsDTO = JSON.parseObject(rps, ExtDataDTO.class);

        Object[] date = ConvertStockExtData.objFieldValArr(rps, "date");
        double[] rps10 = ConvertStockExtData.fieldValArr(rps, "rps10");
        double[] rps20 = ConvertStockExtData.fieldValArr(rps, "rps20");
        double[] rps50 = ConvertStockExtData.fieldValArr(rps, "rps50");
        double[] rps120 = ConvertStockExtData.fieldValArr(rps, "rps120");
        double[] rps250 = ConvertStockExtData.fieldValArr(rps, "rps250");


        System.out.println(JSON.toJSONString(rpsDTO));
        return rpsDTO;
    }


}