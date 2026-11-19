package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.common.config.anno.DistributedLock;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.service.*;
import com.bebopze.tdx.quant.task.TdxTask;
import com.bebopze.tdx.quant.task.progress.TaskProgress;
import com.bebopze.tdx.quant.task.progress.TaskProgressManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;


/**
 * task
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
@Slf4j
@Service
public class TaskServiceImpl implements TaskService {


    @Lazy
    @Autowired
    private TdxDataParserService tdxDataParserService;


    @Lazy
    @Autowired
    private TdxTask tdxTask;


    @Autowired
    @Qualifier("extDataServiceImpl")
    private ExtDataService extDataService;

    @Autowired
    private InitDataService initDataService;


    @Autowired
    private MarketService marketService;

    @Autowired
    private KlineService klineService;

    @Autowired
    private TopBlockService topBlockService;


    @Autowired
    private TaskProgressManager taskProgressManager;


    /**
     * 盘后 行情数据                  盘后-全量更新   ->   DB
     */
    @TotalTime
    @Async
    @DistributedLock(value = 600, autoRenew = true, renewInterval = 100, keyPrefix = "execTask__refreshAll")
    public void execTask__refreshAll(UpdateTypeEnum updateTypeEnum) {


        String taskId = "refreshAll_" + System.currentTimeMillis();
        TaskProgress taskProgress = taskProgressManager.createAndStartTask(taskId, "盘后-全量更新 " + updateTypeEnum);


        log.info("---------------------------- 任务 [refreshAll - [{}]  -  盘后-全量更新 入库]   执行 start", updateTypeEnum);


        // -------------------------------------------- 板块-个股 -------------------------------------------------------


        String subTask = "板块/个股/ETF/自定义板块/关联关系（需至少每周更新1次[每天都会变,尤其是 当前主线 概念板块]）";
        try {
            taskProgressManager.updateProgress(taskId, 10, subTask);
            tdxDataParserService.importAll__blockRelaStock();
            taskProgressManager.completeSubTask(taskId, subTask, "SUC");
        } catch (Exception e) {
            taskProgressManager.failSubTask(taskId, subTask, "FAIL");
        }


        // -------------------------------------------------------------------------------------------------------------


        try {
            taskProgressManager.updateProgress(taskId, 10, "大盘量化 导入（更新）");
            marketService.importMarketMidCycle();


            taskProgressManager.updateProgress(taskId, 30, "行情数据 导入（更新）");
            klineService.refreshKlineAll(updateTypeEnum);


            taskProgressManager.updateProgress(taskId, 50, "扩展（指标）计算");
            extDataService.refreshExtDataAll(updateTypeEnum);


            taskProgressManager.updateProgress(taskId, 70, "主线板块 计算");
            topBlockService.refreshAll(updateTypeEnum);


            taskProgressManager.updateProgress(taskId, 90, "个股/板块 - 行情/指标 Cache 刷新");
            initDataService.refreshDiskCache();


            taskProgressManager.completeTask(taskId, "任务执行完成");
            log.info("---------------------------- 任务 [refreshAll - [{}]  -  盘后-全量更新 入库]   执行 end", updateTypeEnum);


            // TODO   更新 回测任务（今日数据）
            tdxTask.refreshBacktest();


        } catch (Exception e) {
            taskProgressManager.failTask(taskId, "任务执行失败: " + e.getMessage());
            log.error("任务执行失败", e);
        }


        // 返回taskId 供前端查询
        // return taskId;
    }


    /**
     * 盘中 实时 行情数据                   盘中 实时拉取-全量更新   ->   DB
     *
     * @param check
     */
    @TotalTime
    @Async
    @DistributedLock(value = 300, autoRenew = true, renewInterval = 100, keyPrefix = "execTask__refreshKline__lastDay")
    @Override
    public void execTask__refreshKline__lastDay(boolean check) {


        // 本机（Mac系统），跳过执行（仅 服务器端 执行）
        if (check && (!isTradeDateTime() || SystemUtils.IS_OS_MAC)) {
            log.info("execTask__refreshKline__lastDay     >>>     非交易日/非交易时间段，跳过执行");
            return;
        }


        // -------------------------------------------------------------------------------------------------------------


        String taskId = "refreshAll_" + System.currentTimeMillis();
        TaskProgress taskProgress = taskProgressManager.createAndStartTask(taskId, "盘中 实时 行情数据 - 全量更新");


        log.info("---------------------------- 任务 [refreshAll - 盘中 实时 - 全量更新 入库]   执行 start");


        // -------------------------------------------- 板块-个股 -------------------------------------------------------


//        String subTask = "板块/个股/ETF/自定义板块/关联关系（需至少每周更新1次[每天都会变,尤其是 当前主线 概念板块]）";
//        try {
//            taskProgressManager.updateProgress(taskId, 10, subTask);
//            tdxDataParserService.importAll__blockRelaStock();
//            taskProgressManager.completeSubTask(taskId, subTask, "SUC");
//        } catch (Exception e) {
//            taskProgressManager.failSubTask(taskId, subTask, "FAIL");
//        }


        // -------------------------------------------------------------------------------------------------------------


        try {
            taskProgressManager.updateProgress(taskId, 10, "大盘量化 导入（更新）");
            // marketService.importMarketMidCycle();


            taskProgressManager.updateProgress(taskId, 30, "行情数据 导入（更新）");
            klineService.refreshKlineAll__lastDay();


            taskProgressManager.updateProgress(taskId, 50, "扩展（指标）计算");
            extDataService.refreshExtDataAll(UpdateTypeEnum.N_DAY);


            taskProgressManager.updateProgress(taskId, 70, "主线板块 计算");
            // topBlockService.refreshAll(UpdateTypeEnum.N_DAY);


            taskProgressManager.updateProgress(taskId, 90, "个股/板块 - 行情/指标 Cache 刷新");
            initDataService.refreshDiskCache();


            taskProgressManager.completeTask(taskId, "任务执行完成");
            log.info("---------------------------- 任务 [refreshAll - 盘中 实时 - 全量更新 入库]   执行 end");


            // TODO   更新 回测任务（今日数据）
            tdxTask.refreshBacktest();


        } catch (Exception e) {
            taskProgressManager.failTask(taskId, "任务执行失败: " + e.getMessage());
            log.error("任务执行失败", e);
        }


        // 返回taskId 供前端查询
        // return taskId;


        klineService.fillBlockKlineAll__lastDay();
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 校验 当前日期、时间   ->   是否 交易日、交易时间段
     *
     * @return
     */
    private boolean isTradeDateTime() {


        // 最新交易日
        LocalDate lastTradeDate = KlineAPI.lastTradeDate();


        // 非交易日
        if (!lastTradeDate.isEqual(LocalDate.now())) {
            return false;
        }


        // 交易时间段
        LocalTime nowTime = LocalTime.now();
        return nowTime.isAfter(LocalTime.of(9, 25)) && nowTime.isBefore(LocalTime.of(15, 5));
    }


}