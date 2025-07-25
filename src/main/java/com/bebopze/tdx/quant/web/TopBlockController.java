package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.service.impl.TopBlockServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * 主线板块
 *
 * @author: bebopze
 * @date: 2025/7/13
 */
@RestController
@RequestMapping("/api/topBlock")
@Tag(name = "主线板块", description = "1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-均线大多头；")
public class TopBlockController {


    @Autowired
    private TopBlockService topBlockService;


    /**
     * 百日新高 - 占比分布
     *
     * @return
     */
    @Operation(summary = "百日新高", description = "百日新高 - 占比分布")
    @GetMapping(value = "/task/nDayHigh")
    public Result<Void> nDayHighTask(@RequestParam(defaultValue = "100") int N) {
        topBlockService.nDayHighTask(N);
        return Result.SUC();
    }


    /**
     * 涨幅榜 - 占比分布
     *
     * @return
     */
    @Operation(summary = "涨幅榜（N日涨幅>25% / TOP100）", description = "涨幅榜（N日涨幅>25%） - 占比分布")
    @GetMapping(value = "/task/changePctTop")
    public Result<Void> changePctTopTask(@RequestParam(defaultValue = "10") int N) {
        topBlockService.changePctTopTask(N);
        return Result.SUC();
    }


    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @return
     */
    @Operation(summary = "TOP榜（主线板块） - 近N日 占比分布", description = "TOP榜（主线板块） - 近N日 占比分布")
    @GetMapping(value = "/rate")
    public Result<Map<String, Integer>> topBlockRate(@Schema(description = "1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-均线大多头；", example = "1")
                                                     @RequestParam(defaultValue = "1") int blockNewId,

                                                     @RequestParam(defaultValue = "2025-07-16") LocalDate date,

                                                     @Schema(description = "result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）", example = "2")
                                                     @RequestParam(defaultValue = "2") int resultType,

                                                     @RequestParam(defaultValue = "10") int N) {


        return Result.SUC(topBlockService.topBlockRate(blockNewId, date, resultType, N));
    }

    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @return
     */
    @Operation(summary = "TOP榜（主线板块） - 近N日 占比分布", description = "TOP榜（主线板块） - 近N日 占比分布")
    @GetMapping(value = "/info")
    public Result<List<TopBlockServiceImpl.TopBlockDTO>> topBlockRateInfo(@Schema(description = "1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-均线大多头；", example = "1")
                                                                          @RequestParam(defaultValue = "1") int blockNewId,

                                                                          @RequestParam(defaultValue = "2025-07-16") LocalDate date,

                                                                          @Schema(description = "result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）", example = "2")
                                                                          @RequestParam(defaultValue = "2") int resultType,

                                                                          @RequestParam(defaultValue = "10") int N) {


        return Result.SUC(topBlockService.topBlockRateInfo(blockNewId, date, resultType, N));
    }

}