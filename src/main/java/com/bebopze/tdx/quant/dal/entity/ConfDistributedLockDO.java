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
 * 分布式锁
 * </p>
 *
 * @author bebopze
 * @since 2026-01-05
 */
@Getter
@Setter
@ToString
@TableName("conf_distributed_lock")
@Schema(name = "ConfDistributedLockDO", description = "分布式锁")
public class ConfDistributedLockDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 锁唯一key
     */
    @TableField("lock_key")
    @Schema(description = "锁唯一key")
    private String lockKey;

    /**
     * 锁value
     */
    @TableField("lock_value")
    @Schema(description = "锁value")
    private String lockValue;

    /**
     * 锁过期时间（s）
     */
    @TableField("expire")
    @Schema(description = "锁过期时间（s）")
    private Long expire;

    /**
     * 锁过期时间戳
     */
    @Schema(description = "锁过期时间戳")
    @TableField("expire_timestamp")
    private Long expireTimestamp;

    /**
     * 本机唯一标识
     */
    @TableField("machine_unique_id")
    @Schema(description = "本机唯一标识")
    private String machineUniqueId;

    @TableField("gmt_create")
    private LocalDateTime gmtCreate;

    @TableField("gmt_modify")
    private LocalDateTime gmtModify;
}
