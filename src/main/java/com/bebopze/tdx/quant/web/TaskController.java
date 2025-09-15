package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.kline.DataInfoDTO;
import com.bebopze.tdx.quant.service.DataService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.task.TdxScript;
import com.bebopze.tdx.quant.task.TdxTask;
import com.bebopze.tdx.quant.task.progress.TaskProgress;
import com.bebopze.tdx.quant.task.progress.TaskProgressManager;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Task
 *
 * @author: bebopze
 * @date: 2025/5/6
 */
@RestController
@RequestMapping("/api/task")
public class TaskController {


    @Autowired
    private TdxTask tdxTask;

    @Autowired
    private DataService dataService;

    @Autowired
    private InitDataService initDataService;

    @Autowired
    private TaskProgressManager taskProgressManager;


    /**
     * refreshAll   -   盘中 -> 增量更新
     *
     * @return
     */
    @Operation(summary = "refreshAll - 增量更新", description = "refreshAll - 盘中 -> 增量更新")
    @GetMapping(value = "/refreshAll__lataDay")
    public Result<String> refreshAll__lataDay() {
        String taskId = tdxTask.execTask__refreshAll__lataDay();
        return Result.SUC(taskId);
    }


    /**
     * refreshAll   -   盘后 -> 全量更新
     *
     * @return
     */
    @Operation(summary = "refreshAll - 全量更新", description = "refreshAll - 盘后 -> 全量更新")
    @GetMapping(value = "/refreshAll")
    public Result<String> refreshAll() {
        String taskId = tdxTask.execTask__refreshAll();
        return Result.SUC(taskId);
    }


    @Operation(summary = "最新数据", description = "最新数据")
    @GetMapping(value = "/dataInfo")
    public Result<DataInfoDTO> dataInfo() {
        return Result.SUC(dataService.dataInfo());
    }


    @Operation(summary = "东方财富 - 刷新登录信息", description = "东方财富 - 刷新登录信息")
    @GetMapping(value = "/eastmoney/refreshSession")
    public Result<Void> eastmoneyRefreshSession(@RequestParam String validatekey,
                                                @RequestParam String cookie) {
        dataService.eastmoneyRefreshSession(validatekey, cookie);
        return Result.SUC();
    }


    @Operation(summary = "BacktestCache（回测 - 全量行情Cache） -  refresh", description = "BacktestCache（回测 - 全量行情Cache） -  refresh")
    @GetMapping(value = "/initData/refresh")
    public Result<Void> refreshCache(@RequestParam(defaultValue = "true") Boolean refresh) {
        initDataService.initData(null, null, refresh);
        return Result.SUC();
    }


    /**
     * task_933
     *
     * @return
     */
    @GetMapping(value = "/933")
    public Result<Void> task_933() {
        TdxScript.task_933();
        return Result.SUC();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Operation(summary = "获取任务进度", description = "获取任务进度")
    @GetMapping(value = "/progress/{taskId}")
    public Result<TaskProgress> getTaskProgress(@PathVariable String taskId) {
        return Result.SUC(taskProgressManager.getProgress(taskId));
    }


    @Operation(summary = "获取所有活跃任务", description = "获取所有活跃任务")
    @GetMapping(value = "/activeTasks")
    public Result<List<TaskProgress>> getActiveTasks() {
        return Result.SUC(taskProgressManager.getAllActiveTasks());
    }


    @Operation(summary = "获取任务历史", description = "获取任务历史")
    @GetMapping(value = "/taskHistory")
    public Result<List<TaskProgress>> getTaskHistory() {
        return Result.SUC(taskProgressManager.getTaskHistory());
    }

}