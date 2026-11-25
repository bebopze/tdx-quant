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
import java.time.LocalDateTime;

/**
 * <p>
 * 期权BS回测-参数模板
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
@Getter
@Setter
@ToString
@TableName("obt_template")
@Schema(name = "ObtTemplateDO", description = "期权BS回测-参数模板")
public class ObtTemplateDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模板号
     */
    @Schema(description = "模板号")
    @TableField("template_no")
    private String templateNo;

    /**
     * 名称
     */
    @Schema(description = "名称")
    @TableField("name")
    private String name;

    /**
     * 描述
     */
    @Schema(description = "描述")
    @TableField("description")
    private String description;

    /**
     * 参数JSON
     */
    @Schema(description = "参数JSON")
    @TableField("params_json")
    private String paramsJson;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    @TableField("creator")
    private String creator;

    /**
     * 公开：0否 1是
     */
    @Schema(description = "公开：0否 1是")
    @TableField("public_flag")
    private Integer publicFlag;

    /**
     * 使用次数
     */
    @Schema(description = "使用次数")
    @TableField("use_count")
    private Integer useCount;

    /**
     * 逻辑删除
     */
    @Schema(description = "逻辑删除")
    @TableField("deleted_flag")
    private Integer deletedFlag;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField("gmt_create")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField("gmt_modify")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtModify;

}
