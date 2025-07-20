package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.BacktestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;


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
                                 @RequestParam(defaultValue = "2025-07-01") LocalDate endDate) {

        return Result.SUC(backTestService.backtest(startDate, endDate));
    }


    @Operation(summary = "check - 回测数据", description = "逐日 交叉check - 回测数据")
    @GetMapping("/check")
    public Result<Void> checkBacktest(@RequestParam(defaultValue = "1") Long taskId) {
        backTestService.checkBacktest(taskId);
        return Result.SUC();
    }


    @Operation(summary = "回测 - 结果分析", description = "回测 - 结果分析")
    @GetMapping("/analysis")
    public Result<Map> analysis(@RequestParam(defaultValue = "1") Long taskId) {
        return Result.SUC(backTestService.analysis(taskId));
    }


    @GetMapping("/holdingStockRule")
    public Result<Void> test(@RequestParam String stockCode) {
        backTestService.holdingStockRule(stockCode);
        return Result.SUC();
    }

}