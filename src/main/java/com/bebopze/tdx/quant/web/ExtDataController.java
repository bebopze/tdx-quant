package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.ExtDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 扩展数据（自定义指标）计算  -  个股/板块
 *
 * @author: bebopze
 * @date: 2025/5/24
 */
@RestController
@RequestMapping("/api/extData")
@Tag(name = "扩展数据（自定义指标） 计算", description = "自定义指标 - 个股/板块")
public class ExtDataController {


    @Autowired
    private ExtDataService extDataService;


    @Operation(summary = "个股 - RPS计算", description = "个股 - RPS计算")
    @GetMapping(value = "/stock/calcRps")
    public Result<Void> calcStockRps() {
        extDataService.calcStockRps();
        return Result.SUC();
    }


    @Operation(summary = "板块 - RPS计算", description = "板块 - RPS计算")
    @GetMapping(value = "/block/calcRps")
    public Result<Void> calcBlockRps() {
        extDataService.calcBlockRps();
        return Result.SUC();
    }

}