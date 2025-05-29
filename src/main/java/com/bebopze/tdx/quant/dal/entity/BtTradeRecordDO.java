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
 * 回测-BS交易记录
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Getter
@Setter
@ToString
@TableName("bt_trade_record")
@Schema(name = "BtTradeRecordDO", description = "回测-BS交易记录")
public class BtTradeRecordDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 回测任务ID
     */
    @TableField("task_id")
    @Schema(description = "回测任务ID")
    private Long taskId;

    /**
     * 交易类型：1-买入；2-卖出；
     */
    @TableField("trade_type")
    @Schema(description = "交易类型：1-买入；2-卖出；")
    private Integer tradeType;

    /**
     * 股票ID
     */
    @TableField("stock_id")
    @Schema(description = "股票ID")
    private Long stockId;

    /**
     * 股票代码
     */
    @TableField("stock_code")
    @Schema(description = "股票代码")
    private String stockCode;

    /**
     * 股票名称
     */
    @TableField("stock_name")
    @Schema(description = "股票名称")
    private String stockName;

    /**
     * 交易日期
     */
    @TableField("trade_date")
    @Schema(description = "交易日期")
    private LocalDate tradeDate;

    /**
     * 交易价格
     */
    @TableField("price")
    @Schema(description = "交易价格")
    private BigDecimal price;

    /**
     * 交易数量
     */
    @TableField("quantity")
    @Schema(description = "交易数量")
    private Integer quantity;

    /**
     * 交易金额
     */
    @TableField("amount")
    @Schema(description = "交易金额")
    private BigDecimal amount;

    /**
     * 交易费用
     */
    @TableField("fee")
    @Schema(description = "交易费用")
    private BigDecimal fee;

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
