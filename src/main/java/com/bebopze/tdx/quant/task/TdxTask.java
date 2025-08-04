package com.bebopze.tdx.quant.task;

import com.bebopze.tdx.quant.service.ExtDataService;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.TdxDataParserService;
import com.bebopze.tdx.quant.service.TopBlockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * 定时任务
 *
 * @author: bebopze
 * @date: 2024/9/27
 */
@Slf4j
@Component
public class TdxTask {


    @Autowired
    private TdxDataParserService tdxDataParserService;

    @Autowired
    private ExtDataService extDataService;


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockService topBlockService;


    /**
     * 通达信 盘后数据更新 -> 扩展数据计算 -> 自动选股
     */
    @Async
    // @Scheduled(cron = "0 50 15 ? * 1-5", zone = "Asia/Shanghai")
    public void execTask__933_902_921() {


        // .933   -   [盘后数据下载]
        log.info("---------------------------- 任务 [task_933 - 盘后数据下载]   执行 start");
        TdxScript.task_933();
        log.info("---------------------------- 任务 [task_933 - 盘后数据下载]   执行 end");


        // .902   -   [扩展数据管理器]
        log.info("---------------------------- 任务 [task_902 - 扩展数据管理器]   执行 start");
        TdxScript.task_902();
        log.info("---------------------------- 任务 [task_902 - 扩展数据管理器]   执行 end");


        // .921   -   [自动选股]
        log.info("---------------------------- 任务 [task_921 - 自动选股]   执行 start");
        TdxScript.task_921();
        log.info("---------------------------- 任务 [task_921 - 自动选股]   执行 end");


    }


    /**
     * 通达信 盘后数据更新
     */
    @Async
    // @Scheduled(cron = "0 50 15 ? * 1-5", zone = "Asia/Shanghai")
    public void execTask__refreshTdxLdayTask_refreshTdxCwTask() {


        log.info("---------------------------- 任务 [refreshTdxLdayTask - 盘后数据下载]   执行 start");
        TdxZipDownScript.refreshTdxLdayTask();
        log.info("---------------------------- 任务 [refreshTdxLdayTask - 盘后数据下载]   执行 end");


        log.info("---------------------------- 任务 [refreshTdxCwTask - 财务数据下载]   执行 start");
        TdxZipDownScript.refreshTdxCwTask();
        log.info("---------------------------- 任务 [refreshTdxCwTask - 财务数据下载]   执行 end");


    }


    /**
     * 行情数据   盘后-全量更新   ->   DB
     */
    @Async
    @Scheduled(cron = "0 10 16 ? * 1-5", zone = "Asia/Shanghai")
    public void execTask__refreshKlineAll() {
        log.info("---------------------------- 任务 [refreshKlineAll - 盘后-全量更新 入库]   执行 start");


        // 行情  ->  kline_his
        tdxDataParserService.refreshKlineAll(1);

        // 扩展（指标）  ->  ext_data_his
        extDataService.refreshExtDataAll(null);


        // 主线板块
        topBlockService.refreshAll();

        // 大盘量化
        marketService.importMarketMidCycle();


        log.info("---------------------------- 任务 [refreshKlineAll - 盘后-全量更新 入库]   执行 end");
    }


    /**
     * 行情数据   盘中-增量更新   ->   DB
     */
    @Async
    @Scheduled(cron = "0 30 11 * * 1-5", zone = "Asia/Shanghai")
    @Scheduled(cron = "0 15/15 13-14 * * 1-5", zone = "Asia/Shanghai")
    public void execTask__refreshKlineAll__lataDay() {
        log.info("---------------------------- 任务 [refreshKlineAll - 盘中-增量更新 入库]   执行 start");


        // ------------------ 增量更新   ->   只需控制源头 kline  ->  [起始日期]


        // 行情  ->  kline_his
        tdxDataParserService.refreshKlineAll(2);

        // 扩展（指标）  ->  ext_data_his
        extDataService.refreshExtDataAll(10);


        // 主线板块
        // topBlockService.refreshAll();

        // 大盘量化
        marketService.importMarketMidCycle();


        log.info("---------------------------- 任务 [refreshKlineAll - 盘中-增量更新 入库]   执行 end");
    }


    /**
     * 初始化数据 更新 -> DB
     */
    @Async
    @Scheduled(cron = "0 00 17 ? * 7", zone = "Asia/Shanghai")
    public void execTask__importAll() {


        log.info("---------------------------- 任务 [importAll - 初始化数据 更新入库]   执行 start");
        tdxDataParserService.importAll();
        log.info("---------------------------- 任务 [importAll - 初始化数据 更新入库]   执行 end");


    }


}