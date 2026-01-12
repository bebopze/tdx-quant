package com.bebopze.tdx.quant.task;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.common.config.anno.DistributedLock;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BSStrategyInfoDTO;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.MacUtil;
import com.bebopze.tdx.quant.parser.tdxdata.LdayParser;
import com.bebopze.tdx.quant.service.*;
import com.bebopze.tdx.quant.task.progress.TaskProgress;
import com.bebopze.tdx.quant.task.progress.TaskProgressManager;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.Executors;

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
    @Qualifier("extDataServiceImpl")
    private ExtDataService extDataService;

    @Autowired
    private InitDataService initDataService;


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockService topBlockService;


    @Autowired
    private BacktestService backtestService;


    @Autowired
    private StrategyService strategyService;


    @Autowired
    private TaskProgressManager taskProgressManager;


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

    @TotalTime
    @Async
    @Scheduled(cron = "0 10 16 ? * 1-5", zone = "Asia/Shanghai")
    @DistributedLock(value = 600, autoRenew = true, renewInterval = 100, keyPrefix = "execTask__refreshAll")
    public void execTask__refreshAll() {


        String taskId = "refreshAll_" + System.currentTimeMillis();
        TaskProgress taskProgress = taskProgressManager.createAndStartTask(taskId, "盘后-全量更新");


        log.info("---------------------------- 任务 [refreshAll - 盘后-全量更新 入库]   执行 start");


        // ---------------------------------------------------------------------------------------------------------


        Executors.newSingleThreadExecutor().execute(() -> {
            String asyncSubTask = "板块/个股/ETF/自定义板块/关联关系（需至少每周更新1次[每天都会变,尤其是 当前主线 概念板块]）";
            try {
                taskProgressManager.updateProgress(taskId, 10, asyncSubTask, false);
                tdxDataParserService.importAll__blockRelaStock();
                taskProgressManager.completeSubTask(taskId, asyncSubTask, "SUC");
            } catch (Exception e) {
                taskProgressManager.failSubTask(taskId, asyncSubTask, "FAIL");
            }
        });


        // ---------------------------------------------------------------------------------------------------------


        try {
            // 更新进度
            taskProgressManager.updateProgress(taskId, 20, "行情数据");
            tdxDataParserService.refreshKlineAll(UpdateTypeEnum.ALL);


            taskProgressManager.updateProgress(taskId, 30, "扩展（指标）计算");
            extDataService.refreshExtDataAll(null);


            taskProgressManager.updateProgress(taskId, 50, "主线板块");
            topBlockService.refreshAll(UpdateTypeEnum.ALL);


            taskProgressManager.updateProgress(taskId, 70, "大盘量化");
            marketService.importMarketMidCycle();


            taskProgressManager.updateProgress(taskId, 90, "个股/板块 - 行情/指标 Cache");
            initDataService.refreshCache();


            taskProgressManager.completeTask(taskId, "任务执行完成");
            log.info("---------------------------- 任务 [refreshAll - 盘后-全量更新 入库]   执行 end");


            // TODO   更新 回测任务（今日数据）
            refreshBacktest();


        } catch (Exception e) {
            taskProgressManager.failTask(taskId, "任务执行失败: " + e.getMessage());
            log.error("任务执行失败", e);
        }


        // 返回taskId 供前端查询
        // return taskId;
    }


    /**
     * 行情数据   盘中-增量更新   ->   DB
     */
    @Async
    @TotalTime
    @DistributedLock(value = 300, autoRenew = true, renewInterval = 100, keyPrefix = "execTask__refreshAll")

    // 交易时段1：上午（周一 ~ 周五   9:30 ~ 11:35）
    @Scheduled(cron = "0 30,35,40,45,50,55 9 * * 1-5")
    @Scheduled(cron = "0 0/5 10 * * 1-5")
    @Scheduled(cron = "0 0,5,10,15,20,25,30,35 11 * * 1-5")
    // 交易时段2：下午（周一 ~ 周五   13:00 ~ 15:05）
    @Scheduled(cron = "0 0/5 13,14 * * 1-5")
    @Scheduled(cron = "0 0,5 15 * * 1-5")
    public void execTask__refreshKline__lastDay() {
        execTask__refreshKline__lastDay(true);
    }

    @Async
    @TotalTime
    @DistributedLock(value = 300, autoRenew = true, renewInterval = 100, keyPrefix = "execTask__refreshAll")
    public void execTask__refreshKline__lastDay(boolean check) {
        log.info("---------------------------- 任务 [refreshKline__lastDay - 盘中-增量更新 入库]   执行 start");


        // -------------------------------------------- CHECK ----------------------------------------------------------


        // 本机（Mac系统），跳过执行（仅 服务器端 执行）
        if (check && (!isTradeDateTime() || SystemUtils.IS_OS_MAC)) {
            log.info("execTask__refreshKline__lastDay     >>>     非交易日/非交易时间段，跳过执行");
            return;
        }


        // --------------------------------------------


        String taskId = "refreshKline_lastDay_" + System.currentTimeMillis();
        TaskProgress taskProgress = taskProgressManager.createAndStartTask(taskId, "盘中-增量更新");


        // -------------------------------------------------------------------------------------------------------------


        Executors.newSingleThreadExecutor().execute(() -> {
            String asyncSubTask = "板块-个股 关联关系（每天都会变,尤其是[当前主线 概念板块]）";
            try {
                taskProgressManager.updateProgress(taskId, 10, asyncSubTask, false);
                tdxDataParserService.importBlockReport();
                taskProgressManager.completeSubTask(taskId, asyncSubTask, "SUC");
            } catch (Exception e) {
                taskProgressManager.failSubTask(taskId, asyncSubTask, "FAIL");
            }
        });


        // -------------------------------------------- KLINE ----------------------------------------------------------


        try {
            taskProgressManager.updateProgress(taskId, 30, "个股实时 行情数据");
            // 全量个股 - 盘中实时行情   ->   API（东财/同花顺/雪球/新浪） 定时 循环拉取
            tdxDataParserService.fillStockKlineAll(UpdateTypeEnum.INCR);


            taskProgressManager.updateProgress(taskId, 40, "板块实时 行情数据");
            // 根据 板块-个股列表   ->   实时计算   板块指数 涨跌幅/成交额/成交量/...
            tdxDataParserService.calcAndFillBlockKlineAll();


            // -------------------------------------------- EXT_DATA -------------------------------------------------------


            taskProgressManager.updateProgress(taskId, 50, "板块实时 扩展数据");
            extDataService.calcBlockExtData(10);


            taskProgressManager.updateProgress(taskId, 60, "ETF实时 扩展数据");
            extDataService.calcStockExtData(10, StockTypeEnum.ETF.type);


            taskProgressManager.updateProgress(taskId, 75, "个股实时 扩展数据");
            extDataService.calcStockExtData(10, StockTypeEnum.A_STOCK.type);


            // -------------------------------------------- 主线板块 --------------------------------------------------------


            taskProgressManager.updateProgress(taskId, 90, "主线板块");
            topBlockService.refreshAll(UpdateTypeEnum.INCR);


            taskProgressManager.completeSubTask(taskId, "主线板块", "任务执行完成");
            taskProgressManager.completeTask(taskId, "任务执行完成");
            log.info("---------------------------- 任务 [refreshKline__lastDay - 盘中-增量更新 入库]   执行 end");


        } catch (Exception e) {
            taskProgressManager.failTask(taskId, "任务执行失败: " + e.getMessage());
            log.error("任务执行失败", e);
        }


        // return taskId;
    }


//    /**
//     * 行情数据   盘中-增量更新   ->   DB
//     */
//    @TotalTime
//    // @Async
//    // @Scheduled(cron = "0 0/30 13-14 * * 1-5", zone = "Asia/Shanghai")
//    public String execTask__refreshAll__lastDay() {
//
//
//        String taskId = "refreshAll_lastay_" + System.currentTimeMillis();
//        TaskProgress taskProgress = taskProgressManager.createTask(taskId, "盘中-增量更新");
//
//
//        Executors.newSingleThreadExecutor().execute(() -> {
//
//
//            try {
//                taskProgressManager.startTask(taskId);
//                log.info("---------------------------- 任务 [refreshAll - 盘中-增量更新 入库]   执行 start");
//
//
//                taskProgressManager.updateProgress(taskId, 10, "行情数据");
//                tdxDataParserService.refreshKlineAll(UpdateTypeEnum.INCR);
//
//
//                taskProgressManager.updateProgress(taskId, 30, "扩展数据");
//                extDataService.refreshExtDataAll(10);
//
//
//                taskProgressManager.updateProgress(taskId, 50, "主线板块");
//                if (checkBlockLastDay()) {
//                    topBlockService.refreshAll(UpdateTypeEnum.INCR);
//                }
//
//
//                taskProgressManager.updateProgress(taskId, 70, "大盘量化");
//                marketService.importMarketMidCycle();
//
//
//                taskProgressManager.updateProgress(taskId, 90, "个股/板块 - 行情/指标 Cache");
//                initDataService.refreshCache();
//
//
//                taskProgressManager.completeTask(taskId, "任务执行完成");
//                log.info("---------------------------- 任务 [refreshAll - 盘中-增量更新 入库]   执行 end");
//
//
//                // TODO   更新 回测任务（今日数据）
//                refreshBacktest();
//
//
//            } catch (Exception e) {
//                taskProgressManager.failTask(taskId, "任务执行失败: " + e.getMessage());
//                log.error("任务执行失败", e);
//            }
//
//        });
//
//
//        return taskId;
//    }


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
    @Scheduled(cron = "0 15 0/1 ? * *", zone = "Asia/Shanghai")
//    @Scheduled(fixedRateString = "PT1H", zone = "Asia/Shanghai")
//    @Scheduled(cron = "0/15 * * * * ? ", zone = "Asia/Shanghai")
    public void refreshEastmoneyCookie() {
        log.info("---------------------------- 任务 [refresh cookie - 交易账户 Cookie Expires]   执行 start");


        String chromeAppName = "Google Chrome";
        String url = "https://jywg.18.cn/MarginTrade/Buy";


        MacUtil.openChrome(chromeAppName, url);


        MacUtil.closeChrome(chromeAppName, url);


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

        return lastDTO != null
                // 当日
                && lastDTO.getTradeDate().isEqual(LocalDate.now())
                // 有数据（AMO>500亿）
                && lastDTO.getAmount().doubleValue() > 500_0000_0000L;
    }


    /**
     * 更新 回测任务（今日数据）
     */
    private void refreshBacktest() {
        // backtestService.execBacktestUpdate(19, null, LocalDate.now().minusDays(30), LocalDate.now());
    }


    /**
     * 自动 BS策略（工作日 14:50 执行）         - 打板BS策略（涨停,SSF多,月多）
     */
//    @Scheduled(cron = "0 50 14 * * 1-5")
    @TotalTime
    public BSStrategyInfoDTO bsStrategyTradeDaily___涨停_SSF多_月多() {
        log.info("---------------------------- 任务 [bsStrategyTradeDaily - BS策略 自动BS]   执行 start");


        TopBlockStrategyEnum topBlockStrategyEnum = TopBlockStrategyEnum.LV3;
        LocalDate tradeDate = LocalDate.now();

        // SSF多,月多,C_MA60_偏离率<5
        Set<String> buyConSet = Sets.newHashSet("SSF多", "月多", "C_MA60_偏离率<5");
        Set<String> sellConSet = Sets.newHashSet("个股S", "板块S", "主线S");   // 暂无意义（此参数）


        BSStrategyInfoDTO result = strategyService.bsTrade(topBlockStrategyEnum, buyConSet, sellConSet, tradeDate);

        log.info("bsStrategyTradeDaily - bsTrade end     >>>     topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , tradeDate : {} , result : {}",
                 topBlockStrategyEnum.getDesc(), buyConSet, sellConSet, tradeDate, JSON.toJSONString(result));


        log.info("---------------------------- 任务 [bsStrategyTradeDaily - BS策略 自动BS]   执行 end");


        return result;
    }


    /**
     * 自动 BS策略（工作日 14:50 执行）         - 趋势BS策略（SSF多,月多,C_MA60_偏离率<5）
     */
//    @Scheduled(cron = "0 50 14 * * 1-5")
    @TotalTime
    public BSStrategyInfoDTO bsStrategyTradeDaily___SSF多_月多__C_MA60_偏离率() {
        log.info("---------------------------- 任务 [bsStrategyTradeDaily - BS策略 自动BS]   执行 start");


        TopBlockStrategyEnum topBlockStrategyEnum = TopBlockStrategyEnum.LV3;
        LocalDate tradeDate = LocalDate.now();

        // SSF多,月多,C_MA60_偏离率<5
        Set<String> buyConSet = Sets.newHashSet("SSF多", "月多", "C_MA60_偏离率<5");
        Set<String> sellConSet = Sets.newHashSet("个股S", "板块S", "主线S");   // 暂无意义（此参数）


        BSStrategyInfoDTO result = strategyService.bsTrade(topBlockStrategyEnum, buyConSet, sellConSet, tradeDate);

        log.info("bsStrategyTradeDaily - bsTrade end     >>>     topBlockStrategyEnum : {} , buyConSet : {} , sellConSet : {} , tradeDate : {} , result : {}",
                 topBlockStrategyEnum.getDesc(), buyConSet, sellConSet, tradeDate, JSON.toJSONString(result));


        log.info("---------------------------- 任务 [bsStrategyTradeDaily - BS策略 自动BS]   执行 end");


        return result;
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