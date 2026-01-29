package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestAnalysisDTO;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestCompareDTO;
import com.bebopze.tdx.quant.common.util.ConvertUtil;
import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import com.bebopze.tdx.quant.service.BacktestService;
import com.bebopze.tdx.quant.service.DataService;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * 策略 - 回测
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
@RestController
@RequestMapping("/api/backtest")
@Tag(name = "回测", description = "交易策略 -> 回测")
public class BacktestController {


    @Autowired
    private BacktestService backTestService;

    @Autowired
    private DataService dataService;


    @Operation(summary = "创建 -> 执行回测", description = "创建 -> 执行   回测task")
    @PostMapping("/exec")
    public Result<Void> execBacktest(@Schema(description = "回测-开始时间", example = "2025-01-01")
                                     @RequestParam(defaultValue = "2025-01-01") LocalDate startDate,

                                     @Schema(description = "回测-结束时间", example = "2100-12-31")
                                     @RequestParam(defaultValue = "2100-12-31") LocalDate endDate,

                                     @Schema(description = "回测-是否支持 中断恢复（true：接着上次处理进度，继续执行； false：重开一局【batchNo+1】 ）", example = "false")
                                     @RequestParam(defaultValue = "false") boolean resume,

                                     @Schema(description = "任务批次号（resume=true 生效）", example = "1")
                                     @RequestParam(required = false) Integer batchNo,


                                     @Schema(description = "回测-对照组 可变参数", example = "{\"stockType\":\"1\",\"buyStrategyKey\":\"D\",\"scoreSortN\":100,\"singleStockMaxPosPct\":20,\"singleStockMinBuyPosPct\":0,\"singleStockMaxBuyAvlPct\":100,\"ztFlag\":false,\"marketPosLimitFlag\":true,\"failFastFlag\":true,\"checkTradeFlag\":false,\"top1TopBlockFlag\":false,\"taskListFlag\":false}")
                                     @RequestBody BacktestCompareDTO btCompareDTO) {


        backTestService.execBacktest(startDate, endDate(endDate), resume, batchNo, btCompareDTO);
        return Result.SUC();
    }


    @Operation(summary = "回测task  ->  批量更新   指定时间段  回测数据", description = "回测task  ->  批量（by batchNo/taskIdList）更新   指定时间段  回测数据")
    @GetMapping("/update")
    public Result<Void> execBacktestUpdate(@Schema(description = "任务批次号（更新整个批次，[batchNo]和[taskIdList] 只能2选1），batchNo优先级 高于 taskIdList", example = "null")
                                           @RequestParam(required = false) Integer batchNo,

                                           @Schema(description = "任务ID列表（更新指定任务，[batchNo]和[taskIdList] 只能2选1），batchNo优先级 高于 taskIdList", example = "1,2,3")
                                           @RequestParam(required = false, defaultValue = "-1") String taskIdList,

                                           @Schema(description = "更新-开始时间", example = "2025-01-01")
                                           @RequestParam(defaultValue = "2025-01-01") LocalDate startDate,

                                           @Schema(description = "更新-结束时间", example = "2100-12-31")
                                           @RequestParam(defaultValue = "2100-12-31") LocalDate endDate) {


        List<Long> _taskIdList = ConvertUtil.str2LongList(taskIdList);

        backTestService.execBacktestUpdate(batchNo, _taskIdList, startDate, endDate);
        return Result.SUC();
    }


    @Operation(summary = "回测", description = "回测task")
    @PostMapping("/exec2")
    public Result<Long> backtest2(@Schema(description = "主线策略", example = "LV3")
                                  @RequestParam(defaultValue = "LV3") TopBlockStrategyEnum topBlockStrategyEnum,

                                  @Schema(description = "回测-B策略", example = "SSF多,月多")
                                  @RequestParam(defaultValue = "SSF多,月多") String buyConList,

                                  @Schema(description = "回测-开始时间", example = "2025-01-01")
                                  @RequestParam(defaultValue = "2025-01-01") LocalDate startDate,

                                  @Schema(description = "回测-结束时间", example = "2100-12-31")
                                  @RequestParam(defaultValue = "2025-07-01") LocalDate endDate,

                                  @Schema(description = "回测-是否支持 中断恢复（true：接着上次处理进度，继续执行； false：不支持，每次重头开始 ）", example = "true")
                                  @RequestParam(defaultValue = "true") boolean resume,

                                  @Schema(description = "任务批次号（resume=true 生效）", example = "0")
                                  @RequestParam(required = false) Integer batchNo,


                                  @Schema(description = "回测-对照组 可变参数", example = "{\"stockType\":\"1\",\"buyStrategyKey\":\"D\",\"scoreSortN\":100,\"singleStockMaxPosPct\":20,\"singleStockMinBuyPosPct\":0,\"singleStockMaxBuyAvlPct\":100,\"ztFlag\":false,\"marketPosLimitFlag\":true,\"failFastFlag\":true,\"checkTradeFlag\":false,\"top1TopBlockFlag\":false,\"taskListFlag\":false}")
                                  @RequestBody BacktestCompareDTO btCompareDTO) {


        Set<String> _buyConSet = ConvertUtil.str2Set(buyConList);


        if (btCompareDTO.isTaskListFlag()) {

            Lists.newArrayList(null, true, false).forEach(ztFlag -> {
                Lists.newArrayList(true, false).forEach(marketPosLimitFlag -> {
                    Lists.newArrayList(5, 10, 15, 20, 25, 30).forEach(posPct -> {


                        if (btCompareDTO.getBuyStrategyKey().equals("ETF")) {
                            // ETF 不考虑 涨停板
                            if (ztFlag != null) {
                                return;
                            }

                            // ETF 持仓上限 翻倍
                            posPct *= 2;
                        }


                        btCompareDTO.setZtFlag(ztFlag);
                        btCompareDTO.setMarketPosLimitFlag(marketPosLimitFlag);
                        btCompareDTO.setSingleStockMaxPosPct(posPct);


                        backTestService.backtest2(topBlockStrategyEnum, _buyConSet, startDate, endDate(endDate), resume, batchNo, btCompareDTO);
                    });
                });
            });


        } else {
            backTestService.backtest2(topBlockStrategyEnum, _buyConSet, startDate, endDate(endDate), resume, batchNo, btCompareDTO);
        }


        // return Result.SUC(backTestService.backtest2(topBlockStrategyEnum, _buyConList, startDate, endDate(endDate), resume, batchNo, btCompareDTO));
        return Result.SUC();
    }


    @Operation(summary = "回测 - 实战交易", description = "回测task - 实战交易")
    @GetMapping("/exec/trade")
    public Result<Long> backtestTrade(@Schema(description = "主线策略", example = "LV3")
                                      @RequestParam(defaultValue = "LV3") TopBlockStrategyEnum topBlockStrategyEnum,

                                      @Schema(description = "回测-开始时间", example = "2022-01-01")
                                      @RequestParam(defaultValue = "2022-01-01") LocalDate startDate,

                                      @Schema(description = "回测-结束时间", example = "2100-12-31")
                                      @RequestParam(defaultValue = "2025-07-01") LocalDate endDate,


                                      @Schema(description = "回测-对照组 可变参数", example = "{\"stockType\":\"1\",\"buyStrategyKey\":\"D\",\"scoreSortN\":100,\"singleStockMaxPosPct\":20,\"singleStockMinBuyPosPct\":0,\"singleStockMaxBuyAvlPct\":100,\"ztFlag\":false,\"marketPosLimitFlag\":true,\"failFastFlag\":true,\"checkTradeFlag\":false,\"top1TopBlockFlag\":false,\"taskListFlag\":false}")
                                      @RequestBody BacktestCompareDTO btCompareDTO) {


        return Result.SUC(backTestService.backtestTrade(topBlockStrategyEnum, startDate, endDate(endDate), false, 0, btCompareDTO));
    }


    @Operation(summary = "check - 回测数据", description = "逐日 交叉check - 回测数据")
    @GetMapping("/check")
    public Result<Void> checkBacktest(@RequestParam(defaultValue = "1") Long taskId) {
        backTestService.checkBacktest(taskId);
        return Result.SUC();
    }


    @Operation(summary = "回测 - task列表", description = "回测 - task列表")
    @GetMapping("/task/list")
    public Result<List<BtTaskDO>> listTask(@RequestParam(required = false) Long taskId,
                                           @RequestParam(required = false, defaultValue = "") String batchNoList,
                                           @RequestParam(required = false) LocalDateTime startCreateTime,
                                           @RequestParam(required = false) LocalDateTime endCreateTime) {


        List<Integer> _batchNoList = ConvertUtil.str2IntList(batchNoList);

        return Result.SUC(backTestService.listTask(taskId, _batchNoList, startCreateTime, endCreateTime));
    }


    @Operation(summary = "回测 - 结果分析", description = "回测 - 结果分析")
    @GetMapping("/analysis")
    public Result<BacktestAnalysisDTO> analysis(@RequestParam(defaultValue = "1") Long taskId,

                                                @Schema(description = "交易日", example = "2017-01-01")
                                                @RequestParam(defaultValue = "2017-01-01") LocalDate startDate,

                                                @Schema(description = "交易日", example = "2025-10-31")
                                                @RequestParam(required = false) LocalDate endDate) {


        endDate = endDate == null ? LocalDate.now() : endDate;
        return Result.SUC(backTestService.analysis(taskId, startDate, endDate));
    }


    @Operation(summary = "回测 - 异常task删除（by任务批次号）", description = "回测 - 异常task删除（by任务批次号）")
    @GetMapping("/task/delErrTaskByBatchNo")
    public Result<Integer> delErrTaskByBatchNo(@Schema(description = "任务批次号", example = "12")
                                               @RequestParam Integer batchNo) {

        return Result.SUC(backTestService.delErrTaskByBatchNo(batchNo));
    }


    @Operation(summary = "回测 - 批量删除", description = "回测 - 批量删除 异常task")
    @GetMapping("/task/delete")
    public Result<Integer> deleteByTaskIds(@Schema(description = "任务批次号（[batchNo]和[taskIdList] 2选1，有batchNo时，taskIdList无效）", example = "12")
                                           @RequestParam(required = false) Integer batchNo,

                                           @Schema(description = "taskId列表（逗号分隔）", example = "1,2,3")
                                           @RequestParam(required = false) String taskIdList) {


        List<Long> taskIds = ConvertUtil.str2LongList(taskIdList);

        return Result.SUC(backTestService.deleteByTaskIds(batchNo, taskIds));
    }


    @Operation(summary = "回测 - 个股交易记录", description = "回测 - 个股交易记录")
    @GetMapping("/tradeRecord/stock")
    public Result<List<BtTradeRecordDO>> stockTradeRecordList(@RequestParam(defaultValue = "1") Long taskId,
                                                              @RequestParam(defaultValue = "300587") String stockCode) {

        return Result.SUC(backTestService.stockTradeRecordList(taskId, stockCode));
    }


    private LocalDate endDate(LocalDate endDate) {
        try {
            LocalDate dataDate = dataService.dataInfo().getBlock_extDataDTO().getDate();
            return dataDate.isBefore(endDate) ? dataDate : endDate;
        } catch (Exception e) {
            LocalDate lastTradeDate = KlineAPI.lastTradeDate();
            return endDate.isAfter(lastTradeDate) ? lastTradeDate : endDate;
        }
    }


}