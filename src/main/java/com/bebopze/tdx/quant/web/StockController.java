package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.base.BaseStockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.StockBlockInfoDTO;
import com.bebopze.tdx.quant.common.domain.tq.SimpleStockDTO;
import com.bebopze.tdx.quant.service.StockService;
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
 * 股票行情
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@RestController
@RequestMapping("/api/stock")
@Tag(name = "股票行情", description = "股票行情")
public class StockController {


    @Autowired
    private StockService stockService;


    /**
     * 关键词 模糊搜索
     *
     * @param keyword
     * @param limit
     * @return
     */
    @Operation(summary = "关键词 模糊搜索（支持 证券code/证券name）", description = "关键词 模糊搜索（支持 证券code/证券name）")
    @GetMapping(value = "/search")
    public Result<List<SimpleStockDTO>> search(@Schema(description = "关键词（支持 证券code/证券name）", example = "300059/东方财富")
                                               @RequestParam(required = false, defaultValue = "") String keyword,

                                               @Schema(description = "搜索结果数量限制", example = "10")
                                               @RequestParam(required = false, defaultValue = "10") int limit) {

        return Result.SUC(stockService.search(keyword, limit));
    }


    /**
     * 股票行情（kline + ext_data）
     *
     * @param stockCode
     * @param updateType
     * @return
     */
    @Operation(summary = "股票行情（kline + ext_data） ->  支持 股票/板块/期权", description = "股票行情（kline + ext_data） ->  支持 股票/板块/期权")
    @GetMapping(value = "/info")
    public Result<BaseStockDTO> info(@Schema(description = "证券代码", example = "300059")
                                     @RequestParam String stockCode,

                                     @Schema(description = "行情字段 读取类型：1-近N日更新（nd）；2-历史更新（his）；3-全部更新（nd+his）", example = "3")
                                     @RequestParam(required = false, defaultValue = "3") int updateType) {

        return Result.SUC(stockService.info(stockCode, updateType));
    }


    /**
     * 个股-板块
     *
     * @param stockCode
     * @return
     */
    @Operation(summary = "个股-板块", description = "个股-板块")
    @GetMapping(value = "/blockInfo")
    public Result<StockBlockInfoDTO> blockInfo(@Schema(description = "证券代码", example = "300059")
                                               @RequestParam String stockCode) {

        return Result.SUC(stockService.blockInfo(stockCode));
    }


}