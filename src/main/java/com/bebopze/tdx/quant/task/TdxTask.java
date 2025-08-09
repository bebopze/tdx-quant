package com.bebopze.tdx.quant.task;

import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.parser.tdxdata.LdayParser;
import com.bebopze.tdx.quant.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.bebopze.tdx.quant.common.constant.TdxConst.INDEX_BLOCK;


/**
 * 定时任务
 *
 * @author: bebopze
 * @date: 2024/9/27
 */
@Slf4j
@Component
public class TdxTask {


    @Lazy
    @Autowired
    private TdxDataParserService tdxDataParserService;

    @Autowired
    private ExtDataService extDataService;

    @Autowired
    InitDataService initDataService;


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockService topBlockService;


    @Autowired
    private TradeService tradeService;


    /**
     * 通达信 盘后数据更新 -> 扩展数据计算 -> 自动选股
     */
    @Async
    @TotalTime
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
    @TotalTime
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
    @TotalTime
    // @Scheduled(cron = "0 10 16 ? * 1-5", zone = "Asia/Shanghai")
    public void execTask__refreshAll() {
        log.info("---------------------------- 任务 [refreshAll - 盘后-全量更新 入库]   执行 start");


        // 行情  ->  kline_his
        tdxDataParserService.refreshKlineAll(1);
        // refresh Cache
        initDataService.refreshCache();


        // 扩展（指标）  ->  ext_data_his
        extDataService.refreshExtDataAll(null);
        // refresh Cache
        initDataService.refreshCache();


        // 主线板块
        topBlockService.refreshAll();


        // 大盘量化
        marketService.importMarketMidCycle();


        log.info("---------------------------- 任务 [refreshAll - 盘后-全量更新 入库]   执行 end");
    }


    /**
     * 行情数据   盘中-增量更新   ->   DB
     */
    @Async
    @TotalTime
    // @Scheduled(cron = "0 30 11 * * 1-5", zone = "Asia/Shanghai")
    @Scheduled(cron = "0 0/15 13-14 * * 1-5", zone = "Asia/Shanghai")
    public void execTask__refreshAll__lataDay() {
        log.info("---------------------------- 任务 [refreshAll - 盘中-增量更新 入库]   执行 start");


        // ------------------ 增量更新   ->   只需控制源头 kline  ->  [起始日期]


        // 行情  ->  kline_his
        tdxDataParserService.refreshKlineAll(2);
        // refresh Cache
        initDataService.refreshCache();


        // 扩展（指标）  ->  ext_data_his
        extDataService.refreshExtDataAll(10);
        // refresh Cache
        initDataService.refreshCache();


        // 主线板块       =>       INDEX_BLOCK-880515   当日有数据（AMO>500亿）
        if (checkBlockLastDay()) {
            topBlockService.refreshAll();
        } else {
            log.warn("topBlock - refreshAll     >>>     当日[板块数据] - 未更新！");
        }


        // 大盘量化
        marketService.importMarketMidCycle();


        log.info("---------------------------- 任务 [refreshAll - 盘中-增量更新 入库]   执行 end");
    }


    /**
     * 初始化数据 更新 -> DB
     */
    @Async
    @TotalTime
    // @Scheduled(cron = "0 00 17 ? * 7", zone = "Asia/Shanghai")
    public void execTask__importAll() {


        log.info("---------------------------- 任务 [importAll - 初始化数据 更新入库]   执行 start");
        tdxDataParserService.importAll();
        log.info("---------------------------- 任务 [importAll - 初始化数据 更新入库]   执行 end");


    }


    @Async
    @TotalTime
    // @Scheduled(cron = "0 0/10 * ? * *", zone = "Asia/Shanghai")
    public void queryCreditNewPosV2() {


        log.info("---------------------------- 任务 [refresh cookie - 交易账户 Cookie Expires]   执行 start");
        tradeService.queryCreditNewPosV2(false);
        log.info("---------------------------- 任务 [refresh cookie - 交易账户 Cookie Expires]   执行 end");


    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * INDEX_BLOCK-880515   当日有数据（AMO>500亿）             // 主线板块（盘中  ->  无 TDX 板块数据   ->   除非手动导出）
     *
     * @return
     */
    private boolean checkBlockLastDay() {
        LdayParser.LdayDTO lastDTO = ListUtil.last(LdayParser.parseByStockCode(INDEX_BLOCK));
        return lastDTO != null && lastDTO.getAmount().doubleValue() > 500_0000_0000L;
    }


}
