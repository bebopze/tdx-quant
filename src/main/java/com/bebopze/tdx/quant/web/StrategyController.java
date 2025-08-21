package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BSStrategyInfoDTO;
import com.bebopze.tdx.quant.service.StrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


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


    @Operation(summary = "策略交易", description = "策略交易")
    @GetMapping(value = "/bsTrade")
    public Result<BSStrategyInfoDTO> bsTrade(@Schema(description = "主线策略", example = "LV3（板块-月多2 -> 板块RPS红 + 月多 + SSF多）")
                                             @RequestParam(defaultValue = "LV3") TopBlockStrategyEnum topBlockStrategyEnum,

                                             @Schema(description = "B策略", example = "N100日新高,月多,RPS一线红")
                                             @RequestParam(defaultValue = "N100日新高,月多,RPS一线红") String buyConList,

                                             @Schema(description = "S策略", example = "月空_MA20空,SSF空,高位爆量上影大阴,C_SSF_偏离率>25%")
                                             @RequestParam(required = false, defaultValue = "月空_MA20空,SSF空,高位爆量上影大阴,C_SSF_偏离率>25%")
                                             String sellConList,

                                             @Schema(description = "交易日期", example = "2025-08-15")
                                             @RequestParam(required = false) LocalDate tradeDate) {



        // 主线策略： LV3（板块-月多2 -> 板块RPS红 + 月多 + SSF多）

        // B策略：   N100日新高,月多
        // S策略：   个股S,板块S,主线S




        // 2019-01-01 ~ 2025-08-18          1606天
        // 12.6991          1169.91%            年化49%

        // 胜率   65.35%
        // 盈亏比 1.269

        // 最大回测：-13.83%
        // 盈利天数：48.57%
        // 夏普比率：1.803

        // 25830笔
        // 7_7524_4020.04元


        List<String> _buyConList = Arrays.stream(buyConList.split(",")).map(String::trim).collect(Collectors.toList());
        List<String> _sellConList = Arrays.stream(sellConList.split(",")).map(String::trim).collect(Collectors.toList());

        return Result.SUC(strategyService.bsTrade(topBlockStrategyEnum, _buyConList, _sellConList, tradeDate));
    }


    @Operation(summary = "策略交易 - 人工审核后，再买入", description = "策略交易 - 人工审核后，再买入")
    @GetMapping(value = "/bsTrade/read")
    public Result<BSStrategyInfoDTO> bsTradeRead() {
        return Result.SUC(strategyService.bsTradeRead());
    }


    @Deprecated
    @Operation(summary = "持仓策略", description = "持仓策略")
    @GetMapping(value = "/holdingStockRule")
    public Result<Object> holdingStockRule(@RequestParam(required = false) String stockCode) {
        strategyService.holdingStockRule(stockCode);
        return Result.SUC();
    }


    @Deprecated
    @Operation(summary = "破位卖出", description = "破位卖出")
    @GetMapping(value = "/breakSell")
    public Result<Object> breakSell(@RequestParam(required = false) String stockCode) {
        strategyService.breakSell();
        return Result.SUC();
    }

}
