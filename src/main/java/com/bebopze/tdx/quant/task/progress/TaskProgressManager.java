package com.bebopze.tdx.quant.task.progress;

import com.google.common.collect.Lists;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * 任务进度
 *
 * @author: bebopze
 * @date: 2025/9/15
 */
@Slf4j
@Component
public class TaskProgressManager {


    // 任务进度存储 - 保持最近的任务状态
    private final ConcurrentHashMap<String, TaskProgress> taskProgressMap = new ConcurrentHashMap<>();

    // 任务历史记录 - 保持已完成的任务（可选，用于历史查询）
    private final ConcurrentHashMap<String, TaskProgress> taskHistoryMap = new ConcurrentHashMap<>();


    // 注入 WebSocket Handler
    @Setter
    private TaskProgressWebSocketHandler webSocketHandler;


    /**
     * 创建 主任务
     *
     * @param taskId
     * @param taskName
     * @return
     */
    public TaskProgress createTask(String taskId, String taskName) {
        TaskProgress progress = new TaskProgress();
        progress.setTaskId(taskId);
        progress.setTaskName(taskName);
        progress.setStatus(TaskStatus.PENDING);
        progress.setStartTime(LocalDateTime.now());
        progress.setProgress(0);
        progress.setSubTasks(new ArrayList<>());
        taskProgressMap.put(taskId, progress);
        return progress;
    }

    /**
     * 开启 主任务
     *
     * @param taskId
     */
    public void startTask(String taskId) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress != null) {
            progress.setStatus(TaskStatus.RUNNING);
            progress.setStartTime(LocalDateTime.now());
            broadcastProgress(taskId);
        }
    }


    /**
     * 更新 主任务进度、当前关联 子任务
     *
     * @param taskId         主任务ID
     * @param progress       主任务 进度
     * @param currentSubTask 当前 子任务
     */
    public void updateProgress(String taskId, int progress, String currentSubTask) {


        // 创建 子任务
        addSubTask(taskId, currentSubTask);


        // 主任务
        TaskProgress taskProgress = taskProgressMap.get(taskId);
        if (taskProgress == null) {
            log.error("主任务不存在：{}", taskId);
            return;
        }


        // old != new     =>     当前为 new子任务   =>   old子任务 -> [已完成]
        String old_currentSubTask = taskProgress.getCurrentSubTask();
        if (old_currentSubTask != null && !old_currentSubTask.equals(currentSubTask)) {
            completeSubTask(taskId, old_currentSubTask, "SUC");
        }


        // 更新当前   主任务  ->  关联 子任务（new）
        taskProgress.setCurrentSubTask(currentSubTask);
        // 更新 主任务进度
        taskProgress.setProgress(progress);


        broadcastProgress(taskId);
    }

    /**
     * 创建 子任务
     *
     * @param taskId
     * @param subTaskName
     */
    public void addSubTask(String taskId, String subTaskName) {
        TaskProgress taskProgress = taskProgressMap.get(taskId);
        if (taskProgress != null) {
            SubTaskProgress subTask = new SubTaskProgress();
            subTask.setName(subTaskName);
            subTask.setStatus(TaskStatus.RUNNING);
            subTask.setStartTime(LocalDateTime.now());
            taskProgress.getSubTasks().add(subTask);
            broadcastProgress(taskId);
        }
    }


    public void completeSubTask(String taskId, String subTaskName, String message) {
        completeSubTask(taskId, subTaskName, message, TaskStatus.COMPLETED);
    }

    public void completeSubTask(String taskId, String subTaskName, String message, TaskStatus status) {

        TaskProgress taskProgress = taskProgressMap.get(taskId);
        if (taskProgress != null) {

            taskProgress.getSubTasks().stream()
                        .filter(sub -> sub.getName().equals(subTaskName))
                        .findFirst()
                        .ifPresent(sub -> {
                            sub.setStatus(status);
                            sub.setEndTime(LocalDateTime.now());
                            if (sub.getStartTime() != null && sub.getEndTime() != null) {
                                sub.setDuration(Duration.between(sub.getStartTime(), sub.getEndTime()).toMillis());
                            }
                            sub.setMessage(message);
                        });


            broadcastProgress(taskId);
        }
    }


    public void completeTask(String taskId, String message) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress != null) {
            progress.setStatus(TaskStatus.COMPLETED);
            progress.setEndTime(LocalDateTime.now());
            if (progress.getStartTime() != null && progress.getEndTime() != null) {
                progress.setTotalTime(Duration.between(progress.getStartTime(), progress.getEndTime()).toMillis());
            }
            progress.setMessage(message);
            progress.setProgress(100);

            // 将完成的任务移到历史记录中
            taskHistoryMap.put(taskId, progress);
            broadcastProgress(taskId);
        }
    }


    public void failTask(String taskId, String message) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress != null) {
            // 主任务
            progress.setStatus(TaskStatus.FAILED);
            progress.setEndTime(LocalDateTime.now());
            progress.setMessage(message);

            // 子任务
            completeSubTask(taskId, progress.getCurrentSubTask(), message, TaskStatus.FAILED);

            // 将失败的任务移到历史记录中
            taskHistoryMap.put(taskId, progress);
            broadcastProgress(taskId);
        }
    }


    public TaskProgress getProgress(String taskId) {
        // 先从当前任务中查找，再从历史任务中查找
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            progress = taskHistoryMap.get(taskId);
        }
        return progress;
    }


    // 获取所有活跃任务
    public List<TaskProgress> getAllActiveTasks() {
        return Lists.newArrayList(taskProgressMap.values())
                    .stream()
                    .sorted(Comparator.comparing(TaskProgress::getStartTime).reversed())
                    .collect(Collectors.toList());
    }


    // 获取任务历史
    public List<TaskProgress> getTaskHistory() {
        return Lists.newArrayList(taskHistoryMap.values())
                    .stream()
                    .sorted(Comparator.comparing(TaskProgress::getStartTime).reversed())
                    .collect(Collectors.toList());
    }


    // 广播📢  ->   当然任务进度
    private void broadcastProgress(String taskId) {
        if (webSocketHandler != null) {
            webSocketHandler.broadcastProgress(taskId);
        }
    }


}