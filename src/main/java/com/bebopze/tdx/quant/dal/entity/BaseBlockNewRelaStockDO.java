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
import java.time.LocalDateTime;

/**
 * <p>
 * 股票-自定义板块 关联
 * </p>
 *
 * @author bebopze
 * @since 2025-05-11
 */
@Getter
@Setter
@ToString
@TableName("base_block_new_rela_stock")
@Schema(name = "BaseBlockNewRelaStockDO", description = "自定义板块 - 股票/板块/指数  关联")
public class BaseBlockNewRelaStockDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联ID：股票ID/板块ID/指数ID
     */
    @TableField("stock_id")
    @Schema(description = "关联ID：股票ID/板块ID/指数ID")
    private Long stockId;

    /**
     * 关联ID类型：1-个股；2-板块；3-指数；
     */
    @TableField("type")
    @Schema(description = "关联ID类型：1-个股；2-板块；3-指数；")
    private Integer type;

    /**
     * 自定义板块ID
     */
    @TableField("block_new_id")
    @Schema(description = "自定义板块ID")
    private Long blockNewId;

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
