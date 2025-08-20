package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestAnalysisDTO;
import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import com.bebopze.tdx.quant.service.BacktestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 策略 - 回测
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
@RestController
@RequestMapping("/api/backtest")
@Tag(name = "回测", description = "交易策略 -> 回测")
public class BacktestController {


    @Autowired
    private BacktestService backTestService;


    @Operation(summary = "回测", description = "回测task")
    @GetMapping("/exec")
    public Result<Long> backtest(@Schema(description = "回测-开始时间", example = "2022-01-01")
                                 @RequestParam(defaultValue = "2022-01-01") LocalDate startDate,

                                 @Schema(description = "回测-结束时间", example = "2025-07-01")
                                 @RequestParam(defaultValue = "2025-07-01") LocalDate endDate,

                                 @Schema(description = "回测-是否支持 中断恢复（true：接着上次处理进度，继续执行； false：重开一局 ）", example = "true")
                                 @RequestParam(defaultValue = "true") boolean resume,

                                 @Schema(description = "任务批次号（resume=true 生效）", example = "1")
                                 @RequestParam(required = false) Integer batchNo) {


        return Result.SUC(backTestService.backtest(startDate, endDate, resume, batchNo));
    }


    @Operation(summary = "check - 回测数据", description = "逐日 交叉check - 回测数据")
    @GetMapping("/check")
    public Result<Void> checkBacktest(@RequestParam(defaultValue = "1") Long taskId) {
        backTestService.checkBacktest(taskId);
        return Result.SUC();
    }


    @Operation(summary = "回测 - task列表", description = "回测 - task列表")
    @GetMapping("/task/list")
    public Result<List<BtTaskDO>> listTask(@RequestParam(required = false) Long taskId,
                                           @RequestParam(required = false, defaultValue = "") String batchNoList,
                                           @RequestParam(required = false) LocalDateTime startCreateTime,
                                           @RequestParam(required = false) LocalDateTime endCreateTime) {


        List<Integer> _batchNoList = Arrays.stream(batchNoList.split(","))
                                           .filter(StringUtils::isNotBlank)
                                           .map(Integer::valueOf)
                                           .collect(Collectors.toList());

        return Result.SUC(backTestService.listTask(taskId, _batchNoList, startCreateTime, endCreateTime));
    }


    @Operation(summary = "回测 - 结果分析", description = "回测 - 结果分析")
    @GetMapping("/analysis")
    public Result<BacktestAnalysisDTO> analysis(@RequestParam(defaultValue = "1") Long taskId) {
        return Result.SUC(backTestService.analysis(taskId));
    }


    @Operation(summary = "回测 - 异常task删除（by任务批次号）", description = "回测 - 异常task删除（by任务批次号）")
    @GetMapping("/task/delErrTaskByBatchNo")
    public Result<Integer> delErrTaskByBatchNo(@Schema(description = "任务批次号", example = "12")
                                               @RequestParam Integer batchNo) {

        return Result.SUC(backTestService.delErrTaskByBatchNo(batchNo));
    }


    @Operation(summary = "回测 - 批量删除", description = "回测 - 批量删除 异常task")
    @GetMapping("/task/delete")
    public Result<Integer> deleteByTaskIds(@Schema(description = "taskId列表（逗号分隔）", example = "1,2,3")
                                           @RequestParam String taskIdList) {


        List<Long> taskIds = Arrays.stream(taskIdList.split(","))
                                   .map(Long::valueOf)
                                   .collect(Collectors.toList());

        return Result.SUC(backTestService.deleteByTaskIds(taskIds));
    }


    @Operation(summary = "回测 - 个股交易记录", description = "回测 - 个股交易记录")
    @GetMapping("/tradeRecord/stock")
    public Result<List<BtTradeRecordDO>> stockTradeRecordList(@RequestParam(defaultValue = "1") Long taskId,
                                                              @RequestParam(defaultValue = "300587") String stockCode) {
        return Result.SUC(backTestService.stockTradeRecordList(taskId, stockCode));
    }


    @Deprecated
    @GetMapping("/holdingStockRule")
    public Result<Void> test(@RequestParam String stockCode) {
        backTestService.holdingStockRule(stockCode);
        return Result.SUC();
    }

}