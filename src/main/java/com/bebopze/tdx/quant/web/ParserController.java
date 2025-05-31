package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.TdxDataParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


/**
 * 通达信 - 数据初始化（个股-板块-大盘   关联关系 / 行情）   ->   解析入库
 *
 * @author: bebopze
 * @date: 2025/5/7
 */
@RestController
@RequestMapping("/api/parser/tdxdata")
@Tag(name = "通达信 - 数据初始化", description = "（个股-板块-大盘   关联关系 / 行情）   ->   解析入库")
public class ParserController {


    @Autowired
    private TdxDataParserService tdxDataParserService;


    /**
     * 通达信 - （股票/板块/自定义板块）数据初始化   一键导入
     *
     * @return
     */
    @Operation(summary = "（股票/板块/自定义板块）数据初始化 - 一键导入", description = "（股票/板块/自定义板块）数据初始化 - 一键导入")
    @GetMapping(value = "/importAll")
    public Result<Void> importAll() {
        tdxDataParserService.importAll();
        return Result.SUC();
    }


    /**
     * 通达信 - 行情数据（个股/板块）   一键更新
     *
     * @return
     */
    @Operation(summary = "行情数据（个股/板块） - 一键刷新", description = "行情数据（个股/板块） - 一键刷新")
    @GetMapping(value = "/refresh/klineAll")
    public Result<Void> refreshKlineAll() {
        tdxDataParserService.refreshKlineAll();
        return Result.SUC();
    }


    /**
     * 通达信 - 数据解析 入库
     *
     * @return
     */
    @Operation(summary = "通达信（cfg + dat） - 解析入库", description = "通达信 - 解析入库")
    @GetMapping(value = "/import/blockCfg")
    public Result<Void> importTdxBlockCfg() {
        tdxDataParserService.importTdxBlockCfg();
        return Result.SUC();
    }


    @Operation(summary = "通达信（板块导出 - 系统板块） - 解析入库", description = "通达信（板块导出 - 系统板块） - 解析入库")
    @GetMapping(value = "/import/blockReport")
    public Result<Void> importBlockReport() {
        tdxDataParserService.importBlockReport();
        return Result.SUC();
    }

    @Operation(summary = "通达信（板块导出 - 自定义板块） - 解析入库", description = "通达信（板块导出 - 自定义板块） - 解析入库")
    @GetMapping(value = "/import/blockNewReport")
    public Result<Object> importBlockNewReport() {
        tdxDataParserService.importBlockNewReport();
        return Result.SUC();
    }


    @Operation(summary = "板块行情（指定） - 解析入库", description = "板块行情（指定） - 解析入库")
    @GetMapping(value = "/fill/blockKline")
    public Result<Void> fillBlockKline(@RequestParam String blockCode) {
        tdxDataParserService.fillBlockKline(blockCode);
        return Result.SUC();
    }

    @Operation(summary = "板块行情（全部） - 解析入库", description = "板块行情（全部） - 解析入库")
    @GetMapping(value = "/fill/blockKlineAll")
    public Result<Void> fillBlockKlineAll() {
        tdxDataParserService.fillBlockKlineAll();
        return Result.SUC();
    }


    @Operation(summary = "个股行情（指定） - 拉取解析入库", description = "个股行情（指定） - 拉取解析入库")
    @GetMapping(value = "/fill/stockKline")
    public Result<Void> fillStockKline(@RequestParam String stockCode) {
        tdxDataParserService.fillStockKline(stockCode);
        return Result.SUC();
    }

    @Operation(summary = "个股行情（全部） - 拉取解析入库 ", description = "个股行情（全部） - 拉取解析入库")
    @GetMapping(value = "/fill/stockKlineAll")
    public Result<Void> fillStockKlineAll() {
        tdxDataParserService.fillStockKlineAll();
        return Result.SUC();
    }


    /**
     * 通达信 - 数据解析 入库
     *
     * @return
     */
    @Deprecated
    @Operation(summary = "通达信 - 数据解析 入库", description = "通达信 - 数据解析 入库")
    @GetMapping(value = "/xgcz")
    public Result<Void> xgcz() {
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

}