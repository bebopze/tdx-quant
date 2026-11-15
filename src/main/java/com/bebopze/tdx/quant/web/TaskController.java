package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.config.aspect.MysqlLockUtils;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BSStrategyInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.DataInfoDTO;
import com.bebopze.tdx.quant.service.DataService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.task.script.TdxScript;
import com.bebopze.tdx.quant.task.TdxTask;
import com.bebopze.tdx.quant.task.progress.TaskProgress;
import com.bebopze.tdx.quant.task.progress.TaskProgressManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @Autowired
    private MysqlLockUtils mysqlLockUtils;


    /**
     * refreshAll   -   盘中 -> 增量更新
     *
     * @return
     */
    @Operation(summary = "refreshAll - 增量更新", description = "refreshAll - 盘中 -> 增量更新（默认只刷实时字段；withHistory=true 持久化K线历史）")
    @GetMapping(value = "/refreshAll__lastDay")
    public Result<Void> refreshAll__lastDay(@Schema(description = "是否持久化 K 线历史；false=只刷 base_stock 实时字段（15s 快链路）", example = "false")
                                            @RequestParam(required = false, defaultValue = "false") boolean withHistory) {
        if (withHistory) {
            tdxTask.execTask__refreshKline__lastNDays(1, true, false);
        } else {
            tdxTask.execTask__refreshKline__lastDay(false);
        }
        return Result.SUC();
    }


    /**
     * refreshAll   -   盘中 -> 增量更新最近 N 日
     *
     * @return
     */
    @Operation(summary = "refreshAll - 增量更新最近N日", description = "盘中增量：N=1 实时快照；N>1 TQ 日线 + 最后1日快照；ext_data/topBlock 可选")
    @GetMapping(value = "/refreshAll__lastNDays")
    public Result<Void> refreshAll__lastNDays(@Schema(description = "最近 N 个交易日（1~250）", example = "5")
                                              @RequestParam(required = false, defaultValue = "5") int days,

                                              @Schema(description = "是否计算 板块/ETF/个股 扩展数据", example = "true")
                                              @RequestParam(required = false, defaultValue = "true") boolean withExtData,

                                              @Schema(description = "是否执行 极耗时的 主线板块计算", example = "false")
                                              @RequestParam(required = false, defaultValue = "false") boolean withTopBlock) {


        tdxTask.execTask__refreshKline__lastNDays(days, withExtData, withTopBlock);
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


//    @Operation(summary = "释放本机 所有分布式锁", description = "释放本机 所有分布式锁")
//    @GetMapping(value = "/lock/cleanLocks")
//    public Result<Void> releaseLock() {
//        mysqlLockUtils.cleanLocks();
//        return Result.SUC();
//    }


    @Operation(summary = "BacktestCache（回测 - 全量行情Cache） -  refresh", description = "BacktestCache（回测 - 全量行情Cache） -  refresh")
    @GetMapping(value = "/initData/refresh")
    public Result<Void> refreshCache(@Schema(description = "开始时间", example = "2019-01-01")
                                     @RequestParam(required = false, defaultValue = "2019-01-01") LocalDate startDate,

                                     @Schema(description = "结束时间", example = "2026-01-01")
                                     @RequestParam(required = false) LocalDate endDate,

                                     @RequestParam(defaultValue = "false") Boolean refresh) {


        endDate = endDate == null ? LocalDate.now() : endDate;

        initDataService.initData(startDate, endDate, null, refresh);
        return Result.SUC();
    }


    // -----------------------------------------------------------------------------------------------------------------


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


    /**
     * refreshEastmoneyCookie
     *
     * @return
     */
    @GetMapping(value = "/refreshEastmoneyCookie")
    public Result<Void> refreshEastmoneyCookie(@Schema(description = "登录方案（1-API登录；2-Chrome登录；）", example = "1")
                                               @RequestParam(required = false, defaultValue = "1") Integer loginType) {

        tdxTask.refreshEastmoneyCookie(loginType);
        return Result.SUC();
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * bsStrategyTradeDaily   -   bsStrategy - 每日自动BS
     *
     * @return
     */
    @Operation(summary = "bsStrategy - 每日自动BS", description = "bsStrategy - 每日自动BS")
    @GetMapping(value = "bsStrategyTradeDaily")
    public Result<BSStrategyInfoDTO> bsStrategyTradeDaily() {
        return Result.SUC(tdxTask.bsStrategyTradeDaily___SSF多_月多__C_MA60_偏离率());
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