package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.constant.BSStrategyTypeEnum;
import com.bebopze.tdx.quant.common.constant.TopTypeEnum;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.analysis.TopNAnalysisDTO;
import com.bebopze.tdx.quant.common.domain.dto.analysis.TopPoolAnalysisDTO;
import com.bebopze.tdx.quant.service.DataAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


/**
 * 数据分析
 *
 * @author: bebopze
 * @date: 2025/10/28
 */
@RestController
@RequestMapping("/api/data/analysis")
@Tag(name = "数据分析", description = "数据分析")
public class DataAnalysisController {


    @Autowired
    private DataAnalysisService dataAnalysisService;


    /**
     * 主线列表 - 收益率分析
     */
    @Operation(summary = "主线列表 - 收益率分析（指定时间段）", description = "主线列表 - 收益率分析（指定时间段）")
    @GetMapping(value = "/bk-yd2/topList")
    public Result<TopPoolAnalysisDTO> topListAnalysis(@Schema(description = "交易日", example = "2017-01-01")
                                                      @RequestParam(defaultValue = "2019-01-01") LocalDate startDate,

                                                      @Schema(description = "交易日", example = "2025-10-31")
                                                      @RequestParam(required = false) LocalDate endDate,

                                                      @Schema(description = "主线类型：1-板块；2-ETF；3-个股；", example = "3")
                                                      @RequestParam(defaultValue = "3") Integer topPoolType,

                                                      @Schema(description = "主线策略：1-机选；2-精选（TOP50）；3-历史新高；4-极多头；5-RPS三线红；6-10亿；7-首次三线红；8-口袋支点；9-T0；10-涨停（打板）；", example = "1",
                                                              implementation = TopTypeEnum.class)
                                                      @RequestParam(defaultValue = "1") Integer topStrategyType,

                                                      @Schema(description = "BS策略：1-高抛低吸（C_SSF）；2-高抛低吸（C_MA5）；3-高抛低吸（C_MA10）；4-高抛低吸（C_MA15）；5-高抛低吸（C_MA20）；6-高抛低吸（C_MA25）；7-高抛低吸（C_MA30）；8-高抛低吸（C_MA40）；9-高抛低吸（C_MA50）；10-高抛低吸（C_MA60）；11-高抛低吸（C_MA100）；12-高抛低吸（C_MA120）；13-高抛低吸（C_MA150）；14-高抛低吸（C_MA200）；15-高抛低吸（C_MA250）；", example = "1",
                                                              implementation = BSStrategyTypeEnum.class)
                                                      @RequestParam(required = false) Integer bsStrategyType,

                                                      @Schema(description = "是否涨停：true-是；false-否；", example = "false")
                                                      @RequestParam(required = false) Boolean ztFlag) {

        endDate = endDate == null ? LocalDate.now() : endDate;
        return Result.SUC(dataAnalysisService.topListAnalysis(startDate, endDate, topPoolType, topStrategyType, bsStrategyType, ztFlag));
    }


    /**
     * 主线列表 - 收益率分析
     */
    @Operation(summary = "主线列表 - 收益率分析（指定时间段）", description = "主线列表 - 收益率分析（指定时间段）")
    @GetMapping(value = "/bk-yd2/topList/top100")
    public Result<TopNAnalysisDTO> top100(@Schema(description = "交易日", example = "2017-01-01")
                                          @RequestParam(defaultValue = "2017-01-01") LocalDate startDate,

                                          @Schema(description = "交易日", example = "2025-10-31")
                                          @RequestParam(required = false) LocalDate endDate,

                                          @Schema(description = "主线类型：1-板块；2-ETF；3-个股；", example = "3")
                                          @RequestParam(defaultValue = "3") Integer topPoolType,

                                          @Schema(description = "主线策略：1-机选；2-精选（TOP50）；3-历史新高；4-极多头；5-RPS三线红；6-10亿；7-首次三线红；8-口袋支点；9-T0；10-涨停（打板）；", example = "1",
                                                  implementation = TopTypeEnum.class)
                                          @RequestParam(defaultValue = "1") Integer topStrategyType) {

        endDate = endDate == null ? LocalDate.now() : endDate;
        return Result.SUC(dataAnalysisService.top100(startDate, endDate, topPoolType, topStrategyType));
    }


}