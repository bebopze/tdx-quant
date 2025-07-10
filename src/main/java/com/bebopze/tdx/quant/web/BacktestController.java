package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.BacktestService;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


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


    @GetMapping("/exec")
    public Result<Void> backtest(@Schema(description = "回测-开始时间", example = "2022-01-01")
                                 @RequestParam(defaultValue = "2022-01-01") LocalDate startDate,

                                 @Schema(description = "回测-结束时间", example = "2025-07-01")
                                 @RequestParam(defaultValue = "2025-07-01") LocalDate endDate) {

        backTestService.backtest(startDate, endDate);
        return Result.SUC();
    }

    @GetMapping("/holdingStockRule")
    public Result<Void> test(@RequestParam String stockCode) {
        backTestService.holdingStockRule(stockCode);
        return Result.SUC();
    }

}