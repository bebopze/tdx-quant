package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.base.BlockNewBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.BlockNewStockDTO;
import com.bebopze.tdx.quant.service.BlockNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 自定义板块 - 个股池子/板块池子
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@RestController
@RequestMapping("/api/blockNew")
@Tag(name = "自定义板块", description = "个股池子/板块池子")
public class BlockNewkController {


    @Autowired
    private BlockNewService blockNewService;


    /**
     * 自定义板块 - 个股列表（个股池子）
     *
     * @return
     */
    @Operation(summary = "自定义板块 - 个股列表", description = "自定义板块 - 个股池子")
    @GetMapping(value = "/stockList")
    public Result<List<BlockNewStockDTO>> stockList(@RequestParam String blockNewCode) {
        return Result.SUC(blockNewService.stockList(blockNewCode));
    }


    /**
     * 自定义板块 - 板块列表（板块池子）
     *
     * @return
     */
    @Operation(summary = "自定义板块 - 板块列表", description = "自定义板块 - 板块池子")
    @GetMapping(value = "/blockList")
    public Result<List<BlockNewBlockDTO>> blockList(@RequestParam String blockNewCode) {
        return Result.SUC(blockNewService.blockList(blockNewCode));
    }

}