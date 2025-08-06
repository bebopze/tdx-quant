package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
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
    @TableField(value = "kline_his", select = false)
    @Schema(description = "历史行情-JSON（[日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率]）")
    private String klineHis;

    /**
     * 扩展数据 指标-JSON（[日期,RPS5,RPS10,RPS15,RPS20,RPS50]）
     */
    @JsonIgnore
    @TableField(value = "ext_data_his", select = false)
    @Schema(description = "扩展数据 指标-JSON（[日期,RPS5,RPS10,RPS15,RPS20,RPS50]）")
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


}
