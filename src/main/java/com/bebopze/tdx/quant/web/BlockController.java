package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.base.BlockDTO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


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
    public Result<BlockDTO> info(@Schema(description = "板块代码", example = "880515")
                                 @RequestParam String blockCode,

                                 @Schema(description = "行情字段 读取类型：1-近N日更新（nd）；2-历史更新（his）；3-全部更新（nd+his）", example = "3")
                                 @RequestParam(required = false, defaultValue = "3") int updateType) {

        return Result.SUC(blockService.info(blockCode, updateType));
    }


    /**
     * 个股行情
     *
     * @return
     */
    @Operation(summary = "板块-个股", description = "板块-个股")
    @GetMapping(value = "/listStock")
    public Result<List<BaseStockDO>> blockInfo(@RequestParam String blockCode) {
        return Result.SUC(blockService.listStock(blockCode));
    }

}