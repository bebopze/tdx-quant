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
     * 父-ID（行业板块）
     */
    @TableField("parent_id")
    @Schema(description = "父-ID（行业板块）")
    private Long parentId;

    /**
     * 行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；
     */
    @TableField("level")
    @Schema(description = "行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；")
    private Integer level;

    /**
     * tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；
     */
    @TableField("type")
    @Schema(description = "tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；")
    private Integer type;

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
     * 历史行情-JSON（日期：[O,H,L,C,VOL,AMO,涨跌幅,振幅,换手率]）
     */
    @TableField("kline_his")
    @Schema(description = "历史行情-JSON（日期：[O,H,L,C,VOL,AMO,涨跌幅,振幅,换手率]）")
    private String klineHis;

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


//    // -----------------------------------------------------------------------------------------------------------------
//
//
//    /**
//     * 父级 - 板块代码
//     */
//    @TableField(value = "p_code", exist = false)
//    @Schema(description = "父级 - 板块代码")
//    private transient String pCode;
}
