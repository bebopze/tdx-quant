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
 * 期权BS回测-运行日志
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
@Getter
@Setter
@ToString
@TableName("obt_task_log")
@Schema(name = "ObtTaskLogDO", description = "期权BS回测-运行日志")
public class ObtTaskLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @Schema(description = "任务ID")
    @TableField("task_id")
    private Long taskId;

    /**
     * 日志时间
     */
    @Schema(description = "日志时间")
    @TableField("log_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime logTime;

    /**
     * 级别
     */
    @Schema(description = "级别")
    @TableField("level")
    private String level;

    /**
     * logger
     */
    @Schema(description = "logger")
    @TableField("logger")
    private String logger;

    /**
     * 消息
     */
    @Schema(description = "消息")
    @TableField("message")
    private String message;

}
