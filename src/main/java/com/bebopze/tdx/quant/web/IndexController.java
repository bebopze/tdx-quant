package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.BlockNewStockDTO;
import com.bebopze.tdx.quant.service.IndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;


/**
 * 大盘量化
 *
 * @author: bebopze
 * @date: 2025/7/13
 */
@RestController
@RequestMapping("/api/index")
@Tag(name = "大盘量化", description = "MA50占比/月多占比/板块-月多占比/新高-新低/百日新高/.../大盘顶底")
public class IndexController {


    @Autowired
    private IndexService indexService;


    /**
     * 自定义板块 - 个股列表（个股池子）
     *
     * @return
     */
    @Operation(summary = "百日新高", description = "百日新高 - 占比分布")
    @GetMapping(value = "/nDayHighTask")
    public Result<Void> nDayHighTask(@RequestParam(defaultValue = "100") int N) {
        indexService.nDayHighTask(N);
        return Result.SUC();
    }


    /**
     * 自定义板块 - 个股列表（个股池子）
     *
     * @return
     */
    @Operation(summary = "百日新高 - 近N日", description = "百日新高 - 近N日 占比分布")
    @GetMapping(value = "/nDayHigh/rate")
    public Result<Map> nDayHighRate(@RequestParam(defaultValue = "2025-07-16") LocalDate date,
                                    @RequestParam(defaultValue = "10") int N) {

        return Result.SUC(indexService.nDayHighRate(date, N));
    }

}
