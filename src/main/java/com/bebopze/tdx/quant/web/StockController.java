package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.base.BaseStockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.StockBlockInfoDTO;
import com.bebopze.tdx.quant.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 个股行情
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@RestController
@RequestMapping("/api/stock")
@Tag(name = "个股行情", description = "个股行情")
public class StockController {


    @Autowired
    private StockService stockService;


    /**
     * 个股行情
     *
     * @return
     */
    @Operation(summary = "个股行情", description = "个股行情")
    @GetMapping(value = "/info")
    public Result<BaseStockDTO> info(@RequestParam String stockCode) {
        return Result.SUC(stockService.info(stockCode));
    }


    /**
     * 个股行情
     *
     * @return
     */
    @Operation(summary = "个股-板块", description = "个股-板块")
    @GetMapping(value = "/blockInfo")
    public Result<StockBlockInfoDTO> blockInfo(@RequestParam String stockCode) {
        return Result.SUC(stockService.blockInfo(stockCode));
    }

}