package com.bebopze.tdx.quant.task.progress;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;


/**
 * task - 进度
 *
 * @author: bebopze
 * @date: 2025/9/15
 */
@Data
public class TaskProgress {


    private String taskId;
    private String taskName;
    private TaskStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // 总耗时(毫秒)
    private Long totalTime;
    private List<SubTaskProgress> subTasks;
    private String currentSubTask;
    private int progress;
    private String message;


    // 计算总耗时（如果未设置）
    public Long getTotalTime() {
        if (totalTime == null && endTime != null && startTime != null) {
            return Duration.between(startTime, endTime).toMillis();
        }
        return totalTime;
    }

    // 获取运行时间（正在运行的任务）
    public Long getRunningTime() {
        if (status == TaskStatus.RUNNING && startTime != null) {
            return Duration.between(startTime, LocalDateTime.now()).toMillis();
        }
        return null;
    }
}


/**
 * 子任务 - 进度
 */
@Data
class SubTaskProgress {


    private String name;
    private TaskStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // 耗时(毫秒)
    private Long duration;
    private String message;


    // 计算耗时
    public Long getDuration() {
        if (duration == null && endTime != null && startTime != null) {
            return Duration.between(startTime, endTime).toMillis();
        }
        return duration;
    }

    // 获取运行时间（正在运行的子任务）
    public Long getRunningTime() {
        if (status == TaskStatus.RUNNING && startTime != null) {
            return Duration.between(startTime, LocalDateTime.now()).toMillis();
        }
        return null;
    }
}


/**
 * 任务 状态
 */
enum TaskStatus {
    PENDING, RUNNING, COMPLETED, FAILED
}
