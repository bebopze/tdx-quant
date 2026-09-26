package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.tq.TdxRealtimeQuoteDTO;
import com.bebopze.tdx.quant.common.util.ConvertUtil;
import com.bebopze.tdx.quant.service.TdxTqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


/**
 * 通达信 tqcenter API
 *
 * @author bebopze
 * @date 2026/9/26
 */
@RestController
@RequestMapping("/api/tdx/tq")
@Tag(name = "通达信 - TQ 数据", description = "通达信 tqcenter API")
public class TdxTqController {


    @Autowired
    private TdxTqService tdxTqService;


    @Operation(summary = "调用 TQ 方法", description = "请求体 对应 tqcenter 方法的 params")
    @PostMapping(value = "/call/{method}")
    public Result<Object> call(@PathVariable String method,
                               @RequestBody(required = false) Map<String, Object> params) {

        return Result.SUC(tdxTqService.call(method, params));
    }


    @Operation(summary = "读取单个实时行情")
    @GetMapping("/snapshot/quote")
    public Result<TdxRealtimeQuoteDTO> get(@Schema(description = "通达信 代码（股票/ETF/板块，推荐带市场后缀）", example = "600519.SH")
                                           @RequestParam String code) {

        return Result.SUC(tdxTqService.get(code));
    }


    @Operation(summary = "批量读取实时行情", description = "代码列表（逗号分隔）")
    @GetMapping("/snapshot/quotes")
    public Result<List<TdxRealtimeQuoteDTO>> getBatch(@Schema(description = "通达信 代码（股票/ETF/板块，推荐带市场后缀）列表（逗号分隔）", example = "000001,000002,000003")
                                                      @RequestParam String codes) {

        List<String> codeList = ConvertUtil.str2List(codes);
        return Result.SUC(tdxTqService.getBatch(codeList));
    }


    @Operation(summary = "按 股票类型（A股/ETF/港股/美股/板块），读取全部实时行情", description = "默认读取：个股、ETF、板块 和 重点大盘指数")
    @GetMapping("/snapshot/quote/all")
    public Result<List<TdxRealtimeQuoteDTO>> getAll(@Schema(description = "可选 股票类型，例如：A_STOCK,ETF,TDX_BLOCK（传空 -> 拉取全部类型）", example = "A_STOCK", implementation = StockTypeEnum.class)
                                                    @RequestParam(required = false) StockTypeEnum stockTypeEnum) {

        return Result.SUC(tdxTqService.getAll(stockTypeEnum));
    }

}