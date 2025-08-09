package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.task.TdxScript;
import com.bebopze.tdx.quant.task.TdxTask;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * Task
 *
 * @author: bebopze
 * @date: 2025/5/6
 */
@RestController
@RequestMapping("/api/v1/task")
public class TaskController {


    @Autowired
    private TdxTask tdxTask;


    /**
     * refreshAll   -   盘中 -> 增量更新
     *
     * @return
     */
    @Operation(summary = "refreshAll - 增量更新", description = "refreshAll - 盘中 -> 增量更新")
    @GetMapping(value = "/refreshAll__lataDay")
    public Result<Void> refreshAll__lataDay() {
        tdxTask.execTask__refreshAll__lataDay();
        return Result.SUC();
    }


    /**
     * refreshAll   -   盘后 -> 全量更新
     *
     * @return
     */
    @Operation(summary = "refreshAll - 全量更新", description = "refreshAll - 盘后 -> 全量更新")
    @GetMapping(value = "/refreshAll")
    public Result<Void> refreshAll() {
        tdxTask.execTask__refreshAll();
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


}