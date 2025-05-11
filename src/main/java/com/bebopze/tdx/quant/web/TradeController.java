package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.RevokeOrderResultDTO;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.TradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.GetOrdersDataResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 交易 - BS / 撤单 / 持仓 / ...
 *
 * @author: bebopze
 * @date: 2025/5/6
 */
@RestController
@RequestMapping("/api/trade")
@Tag(name = "交易 - BS/撤单/持仓/...", description = "交易 - BS/撤单/持仓/...")
public class TradeController {


    @Autowired
    private TradeService tradeService;


    @Operation(summary = "我的持仓", description = "我的持仓")
    @GetMapping(value = "/queryCreditNewPosV2")
    public Result<QueryCreditNewPosResp> queryCreditNewPosV2() {
        return Result.SUC(tradeService.queryCreditNewPosV2());
    }


    @Operation(summary = "实时行情：买5/卖5", description = "实时行情：买5/卖5")
    @GetMapping(value = "/SHSZQuoteSnapshot")
    public Result<SHSZQuoteSnapshotResp> SHSZQuoteSnapshot(@Schema(description = "证券代码", example = "300059")
                                                           @RequestParam String stockCode) {

        return Result.SUC(tradeService.SHSZQuoteSnapshot(stockCode));
    }


    @Operation(summary = "买入/卖出", description = "买入/卖出")
    @PostMapping(value = "/bs")
    public Result<Integer> bs(@RequestBody TradeBSParam param) {
        return Result.SUC(tradeService.bs(param));
    }


    @Operation(summary = "当日 委托单列表", description = "当日 委托单列表")
    @GetMapping(value = "/getOrdersData")
    public Result<List<GetOrdersDataResp>> getOrdersData() {
        return Result.SUC(tradeService.getOrdersData());
    }


    @Operation(summary = "全部 委托单列表   -   仅返回 可撤列表", description = "全部 委托单列表   -   仅返回 可撤列表")
    @PostMapping(value = "/getRevokeList")
    @Deprecated
    public Result<Integer> getRevokeList() {
        // https://jywg.18.cn/MarginTrade/GetRevokeList?validatekey=e0a3e79f-5868-4668-946a-bfd33a70801d
        // tradeService.getRevokeList();
        return Result.SUC();
    }


    @Operation(summary = "批量撤单", description = "批量撤单")
    @PostMapping(value = "/revokeOrders")
    public Result<List<RevokeOrderResultDTO>> revokeOrders(@RequestBody List<TradeRevokeOrdersParam> paramList) {
        return Result.SUC(tradeService.revokeOrders(paramList));
    }

}