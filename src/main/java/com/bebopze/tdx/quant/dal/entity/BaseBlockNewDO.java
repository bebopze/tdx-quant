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
 * tdx - 自定义板块
 * </p>
 *
 * @author bebopze
 * @since 2025-05-11
 */
@Getter
@Setter
@ToString
@TableName("base_block_new")
@Schema(name = "BaseBlockNewDO", description = "tdx - 自定义板块")
public class BaseBlockNewDO implements Serializable {

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
