package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author: bebopze
 * @date: 2025/5/6
 */
@RestController
@RequestMapping("/api/v1/trade")
@Tag(name = "Trade Operations", description = "交易相关 API，如下单、查询持仓")
public class TradeController {


    @Autowired
    private TradeService tradeService;


    /**
     * task_933
     *
     * @return
     */
    @Operation(summary = "我的持仓", description = "根据过滤条件分页查询用户当前持有的所有证券持仓信息。")
    @GetMapping(value = "/wdcc")
    public Result<Object> wdcc() {
        tradeService.wdcc();
        return Result.SUC();
    }

}