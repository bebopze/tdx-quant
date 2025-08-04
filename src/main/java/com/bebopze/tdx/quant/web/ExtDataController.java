package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.ExtDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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


    @Operation(summary = "个股/板块 - 扩展数据 计算", description = "（RPS/中期涨幅/高位爆量上影大阴/月多/RPS三线红/N日新高/均线预萌出/均线萌出/大均线多头/MA20多/MA20空/SSF多/SSF空）")
    @GetMapping(value = "/refreshExtDataAll")
    public Result<Void> refreshExtDataAll(@RequestParam(required = false) Integer N) {
        extDataService.refreshExtDataAll(N);
        return Result.SUC();
    }


    @Operation(summary = "个股 - 扩展数据 计算", description = "（RPS/中期涨幅/高位爆量上影大阴/月多/RPS三线红/N日新高/均线预萌出/均线萌出/大均线多头/MA20多/MA20空/SSF多/SSF空）")
    @GetMapping(value = "/stock/calc")
    public Result<Void> calcStockExtData(@RequestParam(required = false) Integer N) {
        extDataService.calcStockExtData(N);
        return Result.SUC();
    }


    @Operation(summary = "板块 - 扩展数据 计算", description = "（RPS/中期涨幅/高位爆量上影大阴/月多/RPS三线红/N日新高/均线预萌出/均线萌出/大均线多头/MA20多/MA20空/SSF多/SSF空）")
    @GetMapping(value = "/block/calc")
    public Result<Void> calcBlockExtData(@RequestParam(required = false) Integer N) {
        extDataService.calcBlockExtData(N);
        return Result.SUC();
    }

}