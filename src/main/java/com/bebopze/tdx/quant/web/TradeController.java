package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosV2Resp;
import com.bebopze.tdx.quant.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


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
    public Result<QueryCreditNewPosV2Resp> queryCreditNewPosV2() {
        return Result.SUC(tradeService.queryCreditNewPosV2());
    }


    @Operation(summary = "买入/卖出", description = "买入/卖出")
    @PostMapping(value = "/bs")
    public Result<Integer> bs(@RequestBody TradeBSParam param) {
        return Result.SUC(tradeService.bs(param));
    }

}
