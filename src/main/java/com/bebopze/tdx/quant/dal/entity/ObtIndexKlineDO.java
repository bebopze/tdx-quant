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
 * 期权BS回测-指数K线缓存
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
@Getter
@Setter
@ToString
@TableName("obt_index_kline")
@Schema(name = "ObtIndexKlineDO", description = "期权BS回测-指数K线缓存")
public class ObtIndexKlineDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 代码
     */
    @Schema(description = "代码")
    @TableField("code")
    private String code;

    /**
     * 交易日
     */
    @Schema(description = "交易日")
    @TableField("trade_date")
    private LocalDate tradeDate;

    /**
     * 开盘
     */
    @Schema(description = "开盘")
    @TableField("open")
    private BigDecimal open;

    /**
     * 最高
     */
    @Schema(description = "最高")
    @TableField("high")
    private BigDecimal high;

    /**
     * 最低
     */
    @Schema(description = "最低")
    @TableField("low")
    private BigDecimal low;

    /**
     * 收盘
     */
    @Schema(description = "收盘")
    @TableField("close")
    private BigDecimal close;

    /**
     * 成交量
     */
    @Schema(description = "成交量")
    @TableField("volume")
    private Long volume;

    /**
     * 成交额
     */
    @Schema(description = "成交额")
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 来源
     */
    @Schema(description = "来源")
    @TableField("source")
    private String source;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField("gmt_create")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreate;

}
