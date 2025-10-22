package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.topblock.*;
import com.bebopze.tdx.quant.common.util.ConvertUtil;
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
import java.util.Set;


/**
 * 主线板块
 *
 * @author: bebopze
 * @date: 2025/7/13
 */
@RestController
@RequestMapping("/api/topBlock")
@Tag(name = "主线板块", description = "1-百日新高；2-涨幅榜；3-RPS红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；6-均线大多头；11-板块AMO-TOP1")
public class TopBlockController {


    @Autowired
    private TopBlockService topBlockService;


    /**
     * refreshAll
     *
     * @return
     */
    @Operation(summary = "refreshAll", description = "refreshAll")
    @GetMapping(value = "/task/refreshAll")
    public Result<Void> refreshAllTask(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                       @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum) {

        topBlockService.refreshAll(updateTypeEnum);
        return Result.SUC();
    }


    /**
     * 1-百日新高 - 占比分布
     *
     * @return
     */
    @Operation(summary = "百日新高", description = "百日新高 - 占比分布")
    @GetMapping(value = "/task/nDayHigh")
    public Result<Void> nDayHighTask(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                     @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum,

                                     @Schema(description = "N日新高", example = "100")
                                     @RequestParam(defaultValue = "100") int N) {

        topBlockService.nDayHighTask(updateTypeEnum, N);
        return Result.SUC();
    }


    /**
     * 2-涨幅榜 - 占比分布
     *
     * @return
     */
    @Operation(summary = "涨幅榜（N日涨幅>25% / TOP100）", description = "涨幅榜（N日涨幅>25%）- 占比分布")
    @GetMapping(value = "/task/changePctTop")
    public Result<Void> changePctTopTask(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                         @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum,

                                         @Schema(description = "N日涨幅", example = "10")
                                         @RequestParam(defaultValue = "10") int N) {

        topBlockService.changePctTopTask(updateTypeEnum, N);
        return Result.SUC();
    }

    /**
     * 3-RPS红（一线95/双线90/三线85） -  占比分布
     *
     * @return
     */
    @Operation(summary = "RPS红", description = "RPS红（一线95/双线90/三线85）- 占比分布")
    @GetMapping(value = "/task/rpsRed")
    public Result<Void> changePctTopTask(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                         @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum,

                                         @Schema(description = "RPS三线红", example = "85")
                                         @RequestParam(defaultValue = "85") double RPS) {

        topBlockService.rpsRedTask(updateTypeEnum, RPS);
        return Result.SUC();
    }

    /**
     * 4-二阶段 - 占比分布
     *
     * @return
     */
    @Operation(summary = "二阶段", description = "Stage 2 – Advancing Phase（上升阶段）- 占比分布")
    @GetMapping(value = "/task/stage2")
    public Result<Void> stage2Task(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                   @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum) {

        topBlockService.stage2Task(updateTypeEnum);
        return Result.SUC();
    }

    /**
     * 5-大均线多头 - 占比分布
     *
     * @return
     */
    @Operation(summary = "大均线多头", description = "大均线多头 - 占比分布")
    @GetMapping(value = "/task/longTermMABullStack")
    public Result<Void> longTermMABullStack(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                            @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum) {

        topBlockService.longTermMABullStackTask(updateTypeEnum);
        return Result.SUC();
    }

    /**
     * 6-均线大多头 - 占比分布
     *
     * @return
     */
    @Operation(summary = "均线大多头", description = "均线大多头 - 占比分布")
    @GetMapping(value = "/task/bullMAStack")
    public Result<Void> bullMAStackTask(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                        @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum) {

        topBlockService.bullMAStackTask(updateTypeEnum);
        return Result.SUC();
    }

    /**
     * 7-均线极多头 - 占比分布
     *
     * @return
     */
    @Operation(summary = "均线极多头", description = "均线极多头 - 占比分布")
    @GetMapping(value = "/task/extremeBullMAStack")
    public Result<Void> extremeBullMAStackTask(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                               @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum) {

        topBlockService.extremeBullMAStackTask(updateTypeEnum);
        return Result.SUC();
    }

    /**
     * 11-板块AMO - TOP1
     *
     * @return
     */
    @Operation(summary = "板块AMO - TOP1", description = "板块AMO - TOP1")
    @GetMapping(value = "/task/blockAmoTop")
    public Result<Void> blockAmoTopTask(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                        @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum) {

        topBlockService.blockAmoTopTask(updateTypeEnum);
        return Result.SUC();
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 主线板块（板块-月多2）
     */
    @Operation(summary = "主线板块（板块-月多2）  ->   V1", description = "主线板块（板块-月多2）  ->   V1")
    @GetMapping(value = "/task/v1/bk-yd2")
    public Result<Void> topBlock_bkyd2_v1(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                          @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum) {

        topBlockService.bkyd2Task_v1(updateTypeEnum);
        return Result.SUC();
    }


    // ---------------------------------------


    /**
     * 主线板块（板块-月多2）  Task
     */
    @Operation(summary = "主线板块（板块-月多2）", description = "主线板块（板块-月多2）")
    @GetMapping(value = "/task/bk-yd2")
    public Result<Void> topBlock_bkyd2(@Schema(description = "数据更新类型：ALL-全量更新；INCR-增量更新；", example = "ALL", implementation = UpdateTypeEnum.class)
                                       @RequestParam(defaultValue = "ALL") UpdateTypeEnum updateTypeEnum) {

        topBlockService.bkyd2Task(updateTypeEnum);
        return Result.SUC();
    }


    /**
     * 主线板块 列表
     */
    @Operation(summary = "主线板块 列表（板块-月多2）", description = "主线板块 列表（板块-月多2）")
    @GetMapping(value = "/bk-yd2/topBlockList")
    public Result<TopBlockPoolDTO> topBlockList(@Schema(description = "交易日", example = "2025-09-30")
                                                @RequestParam(required = false) LocalDate date,

                                                @Schema(description = "列表类型：1-机选；2-人选；", example = "1")
                                                @RequestParam(defaultValue = "1") Integer type) {

        date = date == null ? LocalDate.now() : date;
        return Result.SUC(topBlockService.topBlockList(date, type));
    }

    /**
     * 主线个股 列表
     */
    @Operation(summary = "主线个股 列表（板块-月多2）", description = "主线个股 列表（板块-月多2）")
    @GetMapping(value = "/bk-yd2/topStockList")
    public Result<TopStockPoolDTO> topStockList(@Schema(description = "交易日", example = "2025-09-30")
                                                @RequestParam(required = false) LocalDate date,

                                                @Schema(description = "列表类型：1-机选；2-人选；", example = "1")
                                                @RequestParam(defaultValue = "1") Integer type) {

        date = date == null ? LocalDate.now() : date;
        return Result.SUC(topBlockService.topStockList(date, type));
    }


    /**
     * 主线个股 - add
     */
    @Operation(summary = "主线个股  -  批量 add", description = "主线个股  -  批量 add")
    @GetMapping(value = "/bk-yd2/topStockList/add")
    public Result<Integer> addTopStockList(@Schema(description = "交易日", example = "2025-09-30")
                                           @RequestParam LocalDate date,

                                           @Schema(description = "新增 个股code列表（逗号分隔）", example = "1,2,3")
                                           @RequestParam String stockCodeList) {


        Set<String> stockCodeSet = ConvertUtil.str2Set(stockCodeList);

        return Result.SUC(topBlockService.addTopStockSet(date, stockCodeSet));
    }

    /**
     * 主线个股 - DEL
     */
    @Operation(summary = "主线个股  -  批量 DEL", description = "主线个股  -  批量 DEL")
    @GetMapping(value = "/bk-yd2/topStockList/delete")
    public Result<Integer> delTopStockList(@Schema(description = "交易日", example = "2025-09-30")
                                           @RequestParam LocalDate date,

                                           @Schema(description = "新增 个股code列表（逗号分隔）", example = "1,2,3")
                                           @RequestParam String stockCodeList) {


        Set<String> stockCodeSet = ConvertUtil.str2Set(stockCodeList);

        return Result.SUC(topBlockService.delTopStockSet(date, stockCodeSet));
    }


    /**
     * 主线个股列表 - 收益率分析
     */
    @Operation(summary = "主线个股列表 - 收益率分析（指定时间段）", description = "主线个股列表 - 收益率分析（指定时间段）")
    @GetMapping(value = "/bk-yd2/topStockList/analysis")
    public Result<TopStockPoolAnalysisDTO> topStockListAnalysis(@Schema(description = "交易日", example = "2017-01-01")
                                                                @RequestParam(defaultValue = "2017-01-01") LocalDate startDate,

                                                                @Schema(description = "交易日", example = "2025-10-31")
                                                                @RequestParam(required = false) LocalDate endDate,

                                                                @Schema(description = "主线类型：1-板块；2-ETF；3-个股；", example = "1")
                                                                @RequestParam(defaultValue = "3") Integer topPoolType,

                                                                @Schema(description = "列表类型：1-机选；2-人选；", example = "1")
                                                                @RequestParam(defaultValue = "1") Integer type) {

        endDate = endDate == null ? LocalDate.now() : endDate;
        return Result.SUC(topBlockService.topStockListAnalysis(startDate, endDate, topPoolType, type));
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @return
     */
    @Operation(summary = "TOP榜（主线板块） - 近N日 占比分布", description = "TOP榜（主线板块） - 近N日 占比分布")
    @GetMapping(value = "/rate")
    public Result<Map<String, Integer>> topBlockRate(@Schema(description = "1-百日新高；2-涨幅榜；3-RPS红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；6-均线大多头；11-板块AMO-TOP1", example = "1")
                                                     @RequestParam(defaultValue = "1") int blockNewId,

                                                     @Schema(description = "交易日")
                                                     @RequestParam(required = false) LocalDate date,

                                                     @Schema(description = "result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）", example = "2")
                                                     @RequestParam(defaultValue = "2") int resultType,

                                                     @Schema(description = "行业level：1-一级行业；2-二级行业；3-三级行业；", example = "2")
                                                     @RequestParam(required = false) Integer hyLevel,

                                                     @RequestParam(defaultValue = "10") int N) {


        date = date == null ? LocalDate.now() : date;
        return Result.SUC(topBlockService.topBlockRate(blockNewId, date, resultType, hyLevel, N));
    }


    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @return
     */
    @Operation(summary = "TOP榜（主线板块） - 近N日 占比分布", description = "TOP榜（主线板块） - 近N日 占比分布")
    @GetMapping(value = "/rateAll")
    public Result<List<TopBlockServiceImpl.ResultTypeLevelRateDTO>> topBlockRateAll(@Schema(description = "1-百日新高；2-涨幅榜；3-RPS红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；6-均线大多头；11-板块AMO-TOP1", example = "1")
                                                                                    @RequestParam(defaultValue = "1") int blockNewId,

                                                                                    @Schema(description = "交易日")
                                                                                    @RequestParam(required = false) LocalDate date,

                                                                                    @RequestParam(defaultValue = "10") int N) {


        date = date == null ? LocalDate.now() : date;
        return Result.SUC(topBlockService.topBlockRateAll(blockNewId, date, N));
    }


    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @return
     */
    @Operation(summary = "TOP榜（主线板块） - 近N日 占比分布", description = "TOP榜（主线板块） - 近N日 占比分布")
    @GetMapping(value = "/info")
    public Result<List<TopBlock2DTO>> topBlockRateInfo(@Schema(description = "1-百日新高；2-涨幅榜；3-RPS红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；6-均线大多头；11-板块AMO-TOP1", example = "1")
                                                       @RequestParam(defaultValue = "1") int blockNewId,

                                                       @Schema(description = "交易日")
                                                       @RequestParam(required = false) LocalDate date,

                                                       @Schema(description = "result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）", example = "2")
                                                       @RequestParam(defaultValue = "2") int resultType,

                                                       @RequestParam(defaultValue = "10") int N) {


        date = date == null ? LocalDate.now() : date;
        return Result.SUC(topBlockService.topBlockRateInfo(blockNewId, date, resultType, N));
    }


}