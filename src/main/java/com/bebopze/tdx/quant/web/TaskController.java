package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.task.TdxScript;
import org.springframework.web.bind.annotation.*;


/**
 * @author: bebopze
 * @date: 2025/5/6
 */
@RestController
@RequestMapping("/api/v1/task")
public class TaskController {


    /**
     * task_933
     *
     * @return
     */
    @GetMapping(value = "/933")
    public Result task_933() {
        TdxScript.task_933();
        return Result.SUC();
    }

}