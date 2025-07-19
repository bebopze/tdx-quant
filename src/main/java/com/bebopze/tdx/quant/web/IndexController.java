package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.service.IndexService;
import com.bebopze.tdx.quant.service.impl.IndexServiceImpl;
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
     * 百日新高 - 占比分布
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
     * 涨幅榜 - 占比分布
     *
     * @return
     */
    @Operation(summary = "涨幅榜（N日涨幅>25% / TOP100）", description = "涨幅榜（N日涨幅>25%） - 占比分布")
    @GetMapping(value = "/changePctTopTask")
    public Result<Void> changePctTopTask(@RequestParam(defaultValue = "10") int N) {
        indexService.changePctTopTask(N);
        return Result.SUC();
    }


    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @return
     */
    @Operation(summary = "TOP榜（主线板块） - 近N日 占比分布", description = "TOP榜（主线板块） - 近N日 占比分布")
    @GetMapping(value = "/topBlock/rate")
    public Result<Map<String, Integer>> topBlockRate(@Schema(description = "1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-均线大多头；", example = "1")
                                                     @RequestParam(defaultValue = "1") int blockNewId,

                                                     @RequestParam(defaultValue = "2025-07-16") LocalDate date,

                                                     @Schema(description = "result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）", example = "2")
                                                     @RequestParam(defaultValue = "2") int resultType,

                                                     @RequestParam(defaultValue = "10") int N) {


        return Result.SUC(indexService.topBlockRate(blockNewId, date, resultType, N));
    }

    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @return
     */
    @Operation(summary = "TOP榜（主线板块） - 近N日 占比分布", description = "TOP榜（主线板块） - 近N日 占比分布")
    @GetMapping(value = "/topBlockInfo/rate")
    public Result<List<IndexServiceImpl.TopBlockDTO>> topBlockRateInfo(@Schema(description = "1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-均线大多头；", example = "1")
                                                                       @RequestParam(defaultValue = "1") int blockNewId,

                                                                       @RequestParam(defaultValue = "2025-07-16") LocalDate date,

                                                                       @Schema(description = "result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）", example = "2")
                                                                       @RequestParam(defaultValue = "2") int resultType,

                                                                       @RequestParam(defaultValue = "10") int N) {


        return Result.SUC(indexService.topBlockRateInfo(blockNewId, date, resultType, N));
    }

}