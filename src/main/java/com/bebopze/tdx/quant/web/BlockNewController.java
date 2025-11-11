package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.base.BlockNewBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.BlockNewStockDTO;
import com.bebopze.tdx.quant.common.util.ConvertUtil;
import com.bebopze.tdx.quant.service.BlockNewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;


/**
 * 自定义板块 - 个股池子/板块池子
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@RestController
@RequestMapping("/api/blockNew")
@Tag(name = "自定义板块", description = "个股池子/板块池子")
public class BlockNewController {


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


    /**
     * DB -> 通达信
     *
     * @return
     */
    @Operation(summary = "DB -> 通达信", description = "DB -> 通达信")
    @GetMapping(value = "/importTdx")
    public Result<Void> importTdx(@Schema(description = "通达信-自定义板块", example = "ZXBK（JX）")
                                  @RequestParam String blockNewCode,

                                  @Schema(description = "板块/个股 code列表", example = "1,2,3（逗号分隔）")
                                  @RequestParam String codeList) {


        Set<String> codeSet = ConvertUtil.str2Set(codeList);

        blockNewService.importTdx(blockNewCode, codeSet);
        return Result.SUC();
    }


//    /**
//     * 通达信 -> DB
//     *
//     * @return
//     */
//    @Operation(summary = "通达信 -> DB", description = "通达信 -> DB")
//    @GetMapping(value = "/exportTdx")
//    public Result<Void> exportTdx(@Schema(description = "通达信-自定义板块 -> CODE", example = "ZXBK（JX）")
//                                  @RequestParam String blockNewCode,
//
//                                  @Schema(description = "板块/个股 code列表", example = "1,2,3（逗号分隔）")
//                                  @RequestParam String codeList) {
//
//
//        Set<String> codeSet = Arrays.stream(codeList.split(",")).collect(Collectors.toSet());
//
//        blockNewService.exportTdx(blockNewCode);
//        return Result.SUC();
//    }


}