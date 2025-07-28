package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


/**
 * 大盘量化         -           MA50占比 / 月多占比 / 板块-月多占比 / 新高-新低 / 板块-BS占比（左侧试仓/左侧买/右侧买/强势卖出/左侧卖/右侧卖） / ... / 大盘顶底
 *
 * @author: bebopze
 * @date: 2025/7/13
 */
@RestController
@RequestMapping("/api/market")
@Tag(name = "大盘量化", description = "MA50占比 / 月多占比 / 板块-月多占比 / 新高-新低 / 板块-BS占比（左侧试仓/左侧买/右侧买/强势卖出/左侧卖/右侧卖） / ... / 大盘顶底")
public class MarketController {


    @Autowired
    private MarketService marketService;


    /**
     * 大盘量化 - 导入
     *
     * @return
     */
    @Operation(summary = "大盘量化 - 导入", description = "大盘量化 - 导入")
    @GetMapping(value = "/marketMidCycle/import")
    public Result<Void> importMA50Rate() {
        marketService.importMarketMidCycle();
        return Result.SUC();
    }


    /**
     * 大盘量化 - info
     *
     * @return
     */
    @Operation(summary = "大盘量化 - info", description = "大盘量化 - info")
    @GetMapping(value = "/marketMidCycle/info")
    public Result<QaMarketMidCycleDO> marketInfo(@RequestParam(defaultValue = "2025-07-21", required = false) LocalDate date) {
        date = date == null ? LocalDate.now() : date;
        return Result.SUC(marketService.marketInfo(date));
    }

}