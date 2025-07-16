package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.BlockDTO;
import com.bebopze.tdx.quant.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


/**
 * 板块行情
 *
 * @author: bebopze
 * @date: 2025/6/8
 */
@RestController
@RequestMapping("/api/block")
@Tag(name = "板块行情", description = "板块行情")
public class BlockController {


    @Autowired
    private BlockService blockService;


    /**
     * 个股行情
     *
     * @return
     */
    @Operation(summary = "板块行情", description = "板块行情")
    @GetMapping(value = "/info")
    public Result<BlockDTO> info(@RequestParam String blockCode) {
        return Result.SUC(blockService.info(blockCode));
    }


    /**
     * 个股行情
     *
     * @return
     */
    @Operation(summary = "板块-个股", description = "板块-个股")
    @GetMapping(value = "/listStock")
    public Result<Object> blockInfo(@RequestParam String blockCode) {
        return Result.SUC(blockService.listStock(blockCode));
    }


    /**
     * 板块-百日新高
     *
     * @return
     */
    @Operation(summary = "板块-百日新高", description = "参考：开盘啦APP-百日新高")
    @GetMapping(value = "/100DayHigh")
    public Result<Object> _100DayHigh(@Schema(description = "日期", example = "2025-07-01")
                                      @RequestParam(defaultValue = "2025-07-01") LocalDate date) {

        return Result.SUC(blockService._100DayHigh(date));
    }

}