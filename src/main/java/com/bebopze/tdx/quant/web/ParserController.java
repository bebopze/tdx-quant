package com.bebopze.tdx.quant.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.TdxDataParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


/**
 * 通达信 - 数据解析
 *
 * @author: bebopze
 * @date: 2025/5/7
 */
@RestController
@RequestMapping("/api/tdxdata/parser")
@Tag(name = "tdx parser", description = "tdx解析器 API，如 tdxhy.cfg")
public class ParserController {


    @Autowired
    private TdxDataParserService tdxDataParserService;


    /**
     * 通达信 - 数据解析 入库
     *
     * @return
     */
    @Operation(summary = "通达信 - 数据解析 入库", description = "通达信 - 数据解析 入库")
    @GetMapping(value = "/tdxData")
    public Result<Object> tdxData() {
        tdxDataParserService.tdxData();
        return Result.SUC();
    }


    /**
     * 通达信 - 数据解析 入库
     *
     * @return
     */
    @Operation(summary = "通达信 - 数据解析 入库", description = "通达信 - 数据解析 入库")
    @GetMapping(value = "/xgcz")
    public Result<Object> xgcz() {
        tdxDataParserService.xgcz();
        return Result.SUC();
    }


    /**
     * 交易所 - 股票代码 前缀
     *
     * @return
     */
    @Operation(summary = "交易所 - 股票代码 前缀", description = "交易所 - 股票代码 前缀")
    @GetMapping(value = "/market-stockCodePrefixList")
    public Result<Map<String, List<String>>> market_stockCodePrefixList() {
        return Result.SUC(tdxDataParserService.marketRelaStockCodePrefixList());
    }


    /**
     * 交易所 - 股票代码 前缀
     *
     * @return
     */
    @Operation(summary = "交易所 - 股票代码 前缀", description = "交易所 - 股票代码 前缀")
    @GetMapping(value = "/check")
    public Result<JSONObject> check() {
        return Result.SUC(tdxDataParserService.check());
    }

}
