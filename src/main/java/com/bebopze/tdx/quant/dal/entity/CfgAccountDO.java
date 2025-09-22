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
import java.time.LocalDateTime;

/**
 * <p>
 * 配置-持仓账户
 * </p>
 *
 * @author bebopze
 * @since 2025-09-23
 */
@Getter
@Setter
@ToString
@TableName("cfg_account")
@Schema(name = "CfgAccountDO", description = "配置-持仓账户")
public class CfgAccountDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 是否可融资（0-否；1-是；）
     */
    @TableField("rz_account")
    @Schema(description = "是否可融资（0-否；1-是；）")
    private Integer rzAccount;

    /**
     * 最大融资比例（1~2.5）
     */
    @TableField("max_rz_pct")
    @Schema(description = "最大融资比例（1~2.5）")
    private BigDecimal maxRzPct;

    /**
     * 账户-持仓上限（%）
     */
    @TableField("pos_limit_pct")
    @Schema(description = "账户-持仓上限（%）")
    private BigDecimal posLimitPct;

    /**
     * 单只个股-持仓上限（%）
     */
    @TableField("stock_pos_limit_pct")
    @Schema(description = "单只个股-持仓上限（%）")
    private BigDecimal stockPosLimitPct;

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
