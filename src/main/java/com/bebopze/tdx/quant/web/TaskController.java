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
     * refreshKlineAll   -   盘中 -> 增量更新
     *
     * @return
     */
    @Operation(summary = "refreshKlineAll - 增量更新", description = "refreshKlineAll - 盘中 -> 增量更新")
    @GetMapping(value = "/refreshKlineAll__lataDay")
    public Result<Void> refreshKlineAll__lataDay() {
        tdxTask.execTask__refreshKlineAll__lataDay();
        return Result.SUC();
    }


    /**
     * refreshKlineAll   -   盘后 -> 全量更新
     *
     * @return
     */
    @Operation(summary = "refreshKlineAll - 全量更新", description = "refreshKlineAll - 盘后 -> 全量更新")
    @GetMapping(value = "/refreshKlineAll")
    public Result<Void> refreshKlineAll() {
        tdxTask.execTask__refreshKlineAll();
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