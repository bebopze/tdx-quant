package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bebopze.tdx.quant.service.impl.InitDataServiceImpl;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

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

//    /**
//     * 交易信号
//     */
//    @TableField("trade_signal")
//    @Schema(description = "交易信号")
//    private String tradeSignal;

    /**
     * 交易信号-类型
     */
    @TableField("trade_signal_type")
    @Schema(description = "交易信号-类型")
    private Integer tradeSignalType;

    /**
     * 交易信号-描述
     */
    @TableField("trade_signal_desc")
    @Schema(description = "交易信号-描述")
    private String tradeSignalDesc;

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
     * 仓位占比（%）
     */
    @Schema(description = "仓位占比（%）")
    @TableField("position_pct")
    private BigDecimal positionPct;

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


//    @TableField(exist = false)
//    private String blockCodePath;
//
//    @TableField(exist = false)
//    private String blockNamePath;


    public String getBlockCodePath() {
        return Optional.ofNullable(InitDataServiceImpl.data.getBlock(stockCode, 2, 2)).map(BaseBlockDO::getCodePath)
                       .orElse(null);
    }

    public String getBlockNamePath() {
        return Optional.ofNullable(InitDataServiceImpl.data.getBlock(stockCode, 2, 2)).map(BaseBlockDO::getNamePath)
                       .orElse(null);
    }


}