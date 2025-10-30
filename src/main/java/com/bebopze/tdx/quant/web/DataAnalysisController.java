package com.bebopze.tdx.quant.web;

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
                                                      @RequestParam(defaultValue = "2017-01-01") LocalDate startDate,

                                                      @Schema(description = "交易日", example = "2025-10-31")
                                                      @RequestParam(required = false) LocalDate endDate,

                                                      @Schema(description = "主线类型：1-板块；2-ETF；3-个股；", example = "3")
                                                      @RequestParam(defaultValue = "3") Integer topPoolType,

                                                      @Schema(description = "列表类型：1-机选；2-人选；", example = "1")
                                                      @RequestParam(defaultValue = "1") Integer type) {

        endDate = endDate == null ? LocalDate.now() : endDate;
        return Result.SUC(dataAnalysisService.topListAnalysis(startDate, endDate, topPoolType, type));
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

                                          @Schema(description = "列表类型：1-机选；2-人选；", example = "1")
                                             @RequestParam(defaultValue = "1") Integer type) {

        endDate = endDate == null ? LocalDate.now() : endDate;
        return Result.SUC(dataAnalysisService.top100(startDate, endDate, topPoolType, type));
    }


}