package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionEtfInfoDTO;
import com.bebopze.tdx.quant.service.OptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 期权行情
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
@RestController
@RequestMapping("/api/option")
@Tag(name = "期权行情", description = "期权行情")
public class OptionController {


    @Autowired
    private OptionService optionService;


    /**
     * 期权行情
     *
     * @return
     */
    @Operation(summary = "期权行情", description = "期权行情")
    @GetMapping(value = "/info")
    public Result<OptionDTO> info(@Schema(description = "期权代码", example = "10011565.SHO")
                                  @RequestParam String optionCode) {

        return Result.SUC(optionService.info(optionCode));
    }


    /**
     * 期权-现货（指数ETF/商品现货）
     *
     * @return
     */
    @Operation(summary = "期权-现货（指数ETF/商品现货）", description = "期权-现货（指数ETF/商品现货）")
    @GetMapping(value = "/blockInfo")
    public Result<OptionEtfInfoDTO> blockInfo(@Schema(description = "期权代码", example = "10011565.SHO")
                                              @RequestParam String optionCode) {

        return Result.SUC(optionService.optionEtfInfo(optionCode));
    }


}