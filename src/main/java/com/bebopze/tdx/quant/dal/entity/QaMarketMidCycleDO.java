package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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
 * 量化分析 - 大盘中期顶底
 * </p>
 *
 * @author bebopze
 * @since 2025-07-21
 */
@Getter
@Setter
@ToString
@TableName("qa_market_mid_cycle")
@Schema(name = "QaMarketMidCycleDO", description = "量化分析 - 大盘中期顶底")
public class QaMarketMidCycleDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 日期
     */
    @TableField("date")
    @Schema(description = "日期")
    private LocalDate date;

    /**
     * 大盘-牛熊：1-牛市；2-熊市；
     */
    @TableField("market_bull_bear_status")
    @Schema(description = "大盘-牛熊：1-牛市；2-熊市；")
    private Integer marketBullBearStatus;

    /**
     * 大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
     */
    @TableField("market_mid_status")
    @Schema(description = "大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；")
    private Integer marketMidStatus;

    /**
     * 底_DAY
     */
    @TableField("market_low_day")
    @Schema(description = "底_DAY")
    private Integer marketLowDay;

    /**
     * 顶_DAY
     */
    @Schema(description = "顶_DAY")
    @TableField("market_high_day")
    private Integer marketHighDay;

    /**
     * MA50占比（%）
     */
    @TableField("ma50_pct")
    @Schema(description = "MA50占比（%）")
    private BigDecimal ma50Pct;

    /**
     * 仓位占比（%）
     */
    @TableField("position_pct")
    @Schema(description = "仓位占比（%）")
    private BigDecimal positionPct;

    /**
     * 个股月多-占比（%）
     */
    @Schema(description = "个股月多-占比（%）")
    @TableField("stock_month_bull_pct")
    private BigDecimal stockMonthBullPct;

    /**
     * 板块月多-占比（%）
     */
    @Schema(description = "板块月多-占比（%）")
    @TableField("block_month_bull_pct")
    private BigDecimal blockMonthBullPct;

    /**
     * 新高数量
     */
    @TableField("high_num")
    @Schema(description = "新高数量")
    private Integer highNum;

    /**
     * 新低数量
     */
    @TableField("low_num")
    @Schema(description = "新低数量")
    private Integer lowNum;

    /**
     * 全A数量
     */
    @TableField("all_stock_num")
    @Schema(description = "全A数量")
    private Integer allStockNum;

    /**
     * 差值
     */
    @Schema(description = "差值")
    @TableField("high_low_diff")
    private Integer highLowDiff;

    /**
     * 左侧试仓-占比（%）
     */
    @TableField("bs1_pct")
    @Schema(description = "左侧试仓-占比（%）")
    private BigDecimal bs1Pct;

    /**
     * 左侧买-占比（%）
     */
    @TableField("bs2_pct")
    @Schema(description = "左侧买-占比（%）")
    private BigDecimal bs2Pct;

    /**
     * 右侧买-占比（%）
     */
    @TableField("bs3_pct")
    @Schema(description = "右侧买-占比（%）")
    private BigDecimal bs3Pct;

    /**
     * 强势卖出-占比（%）
     */
    @TableField("bs4_pct")
    @Schema(description = "强势卖出-占比（%）")
    private BigDecimal bs4Pct;

    /**
     * 左侧卖-占比（%）
     */
    @TableField("bs5_pct")
    @Schema(description = "左侧卖-占比（%）")
    private BigDecimal bs5Pct;

    /**
     * 右侧卖-占比（%）
     */
    @TableField("bs6_pct")
    @Schema(description = "右侧卖-占比（%）")
    private BigDecimal bs6Pct;

    /**
     * 右侧B-占比（%）
     */
    @TableField("right_buy_pct")
    @Schema(description = "右侧B-占比（%）")
    private BigDecimal rightBuyPct;

    /**
     * 右侧S-占比（%）
     */
    @TableField("right_sell_pct")
    @Schema(description = "右侧S-占比（%）")
    private BigDecimal rightSellPct;

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
}
