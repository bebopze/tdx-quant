package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.StrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 策略
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
@RestController
@RequestMapping("/api/strategy")
@Tag(name = "策略", description = "策略")
public class StrategyController {


    @Autowired
    private StrategyService strategyService;


    @Operation(summary = "持仓策略", description = "持仓策略")
    @GetMapping(value = "/holdingStockRule")
    public Result<Object> holdingStockRule(@RequestParam String stockCode) {
        strategyService.holdingStockRule(stockCode);
        return Result.SUC();
    }


    @Operation(summary = "破位卖出", description = "破位卖出")
    @GetMapping(value = "/breakSell")
    public Result<Object> breakSell(@RequestParam String stockCode) {
        strategyService.breakSell();
        return Result.SUC();
    }

}
