package com.bebopze.tdx.quant.task;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.client.TdxIncrClient;
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
import com.bebopze.tdx.quant.task.script.EastMoneyChromeLogin;
import com.bebopze.tdx.quant.task.script.EastMoneyLogin;
import com.bebopze.tdx.quant.task.script.TdxScript;
import com.bebopze.tdx.quant.task.script.TdxZipDownScript;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

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


    /**
     * 增量更新是否附带 topBlockService.refreshAll(INCR)。
     * 该步骤极耗时（约 10min），盘中实时链路默认关闭。
     */
    @Value("${tdx.task.incr.top-block-enabled:false}")
    private boolean incrTopBlockEnabled;


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

    @Autowired
    private TdxIncrClient tdxIncrClient;


    @Autowired
    private EastMoneyLogin eastMoneyLogin;

    @Autowired
    private EastMoneyChromeLogin eastMoneyChromeLogin;


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
    // @Scheduled(cron = "0 10 16 ? * 1-5", zone = "Asia/Shanghai")
    @DistributedLock(value = 600, autoRenew = true, renewInterval = 100, keyPrefix = "execTask__refreshAll")
    public void execTask__refreshAll() {


        String taskId = "refreshAll_" + System.currentTimeMillis();
        TaskProgress taskProgress = taskProgressManager.createAndStartTask(taskId, "盘后-全量更新");


        log.info("---------------------------- 任务 [refreshAll - 盘后-全量更新 入库]   执行 start");


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
            tdxDataParserService.refreshKlineAll(UpdateTypeEnum.ALL);


            taskProgressManager.updateProgress(taskId, 50, "扩展（指标）计算");
            extDataService.refreshExtDataAll(null);


            taskProgressManager.updateProgress(taskId, 70, "主线板块 计算");
            topBlockService.refreshAll(UpdateTypeEnum.ALL);


            taskProgressManager.updateProgress(taskId, 90, "个股/板块 - 行情/指标 Cache 刷新");
            initDataService.refreshDiskCache();


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
     *
     * <p>实时快链路：每 15s 拉取 TQ 批量快照，只更新 base_stock 的实时行情字段
     * （开/高/低/收/量/额/涨跌），不读写全历史 kline_his。
     * K 线历史由 5min 慢链路、最近 N 日补偿或盘后全量链路持久化。</p>
     */
    @Async
    @TotalTime
    @DistributedLock(value = 120, autoRenew = true, renewInterval = 30, keyPrefix = "execTask__refreshKline__kline")
    // 交易时段：9:00~11:59 / 13:00~14:59，每 15 秒；另补 15:00~15:04
    @Scheduled(cron = "0/15 * 9-11,13-14 * * 1-5")
    @Scheduled(cron = "0 0-4 15 * * 1-5")
    public void execTask__refreshKline__lastDay() {
        refreshKlineIncr(1, false, false, true, false, false);
    }

    /**
     * 手动触发：最后 1 日实时增量（check=false 时不受交易时段限制）。
     */
    @Async
    @TotalTime
    @DistributedLock(value = 300, autoRenew = true, renewInterval = 30, keyPrefix = "execTask__refreshKline__kline")
    public void execTask__refreshKline__lastDay(boolean check) {
        refreshKlineIncr(1, false, false, check, false, false);
    }

    /**
     * 自定义：更新最近 N 个交易日（1~250）。
     *
     * <p>N=1 走 TQ 实时快照；N&gt;1 走 TQ 批量日线，并用当日快照覆盖最后 1 日。
     * 会顺带计算板块 K 线、扩展数据；topBlock 作为可选开关。</p>
     */
    @Async
    @TotalTime
    @DistributedLock(value = 1800, autoRenew = true, renewInterval = 100, keyPrefix = "execTask__refreshKline__kline")
    public void execTask__refreshKline__lastNDays(int days, boolean withTopBlock) {
        Assert.isTrue(days >= 1 && days <= 250, "增量更新天数必须在 1~250 之间：" + days);

        refreshKlineIncr(days, true, withTopBlock, false, true, true);
    }

    /**
     * 5min 慢链路：持久化最后 1 日 K 线历史 + 计算板块/扩展数据；
     * topBlockService.refreshAll(INCR) 约 10min，按配置可选。
     */
    @Async
    @TotalTime
    @DistributedLock(value = 900, autoRenew = true, renewInterval = 100, keyPrefix = "execTask__refreshKline__ext")
    @Scheduled(cron = "0 0/5 9-11,13-14 * * 1-5")
    @Scheduled(cron = "0 0,5 15 * * 1-5")
    public void execTask__refreshKline__ext() {
        // 5min 慢链路：持久化最后 1 日 K 线历史 + 板块/扩展数据；topBlock 可选
        refreshKlineIncr(1, true, incrTopBlockEnabled, true, true, true);
    }


    /**
     * 增量更新统一编排。
     *
     * @param days         最近 N 个交易日；0 表示本链路不更新行情
     * @param withExt      是否计算板块/ETF/个股扩展数据
     * @param withTopBlock 是否执行极耗时的 topBlockService.refreshAll(INCR)
     * @param check        是否校验交易日/交易时段
     * @param progress     是否创建 TaskProgress（15s 快链路不创建，避免任务刷屏）
     * @param persistKline 是否持久化 kline_his；false=仅刷新 base_stock 实时字段（15s 快链路）
     */
    private void refreshKlineIncr(int days, boolean withExt, boolean withTopBlock,
                                  boolean check, boolean progress, boolean persistKline) {

        if (check && !isTradeDateTime()) {
            log.info("refreshKlineIncr     >>>     非交易日/非交易时间段，跳过执行     days : {} , withExt : {} , withTopBlock : {}",
                     days, withExt, withTopBlock);
            return;
        }
        if (days <= 0 && !withExt && !withTopBlock) {
            return;
        }

        long start = System.currentTimeMillis();
        String taskId = "refreshKline_incr_" + System.currentTimeMillis();
        TaskProgress taskProgress = progress
                ? taskProgressManager.createAndStartTask(taskId, days > 0 ? "盘中-增量更新" : "盘中-增量扩展")
                : null;

        log.info("---------------------------- 任务 [refreshKlineIncr - 盘中增量]   执行 start     days : {} , withExt : {} , withTopBlock : {} , progress : {}",
                 days, withExt, withTopBlock, progress);

        try {
            int p = 10;

            if (days > 0) {
                String subTask = persistKline
                        ? (days == 1 ? "个股/ETF K线历史（最后 1 日）" : "个股/ETF K线历史（最近 " + days + " 日）")
                        : "个股/ETF 实时快照（最后 1 日）";
                if (taskProgress != null) {
                    taskProgressManager.updateProgress(taskId, p, subTask);
                }
                if (persistKline) {
                    tdxDataParserService.fillStockKlineLastNDays(days);
                } else {
                    tdxDataParserService.fillStockRealtimeSnapshot();
                }
                p = 40;
                if (taskProgress != null) {
                    taskProgressManager.completeSubTask(taskId, subTask, "SUC");
                    taskProgressManager.updateProgress(taskId, p, "行情增量更新完成");
                }
            }

            if (withExt) {
                String blockKlineTask = "板块实时 行情数据";
                if (taskProgress != null) {
                    taskProgressManager.updateProgress(taskId, p + 5, blockKlineTask);
                }
                tdxDataParserService.calcAndFillBlockKlineAll();
                if (taskProgress != null) {
                    taskProgressManager.completeSubTask(taskId, blockKlineTask, "SUC");
                    taskProgressManager.updateProgress(taskId, 55, "板块实时 扩展数据");
                }

                extDataService.calcBlockExtData(10);

                if (taskProgress != null) {
                    taskProgressManager.updateProgress(taskId, 65, "ETF实时 扩展数据");
                }
                extDataService.calcStockExtData(10, StockTypeEnum.ETF.type);

                if (taskProgress != null) {
                    taskProgressManager.updateProgress(taskId, 80, "个股实时 扩展数据");
                }
                extDataService.calcStockExtData(10, StockTypeEnum.A_STOCK.type);

                p = 80;
            }

            if (withTopBlock) {
                // 该步骤约 10min，只有显式开启时才执行
                if (taskProgress != null) {
                    taskProgressManager.updateProgress(taskId, 85, "板块-个股 关联关系（可选）");
                }
                tdxDataParserService.importBlockReport();

                if (taskProgress != null) {
                    taskProgressManager.updateProgress(taskId, 90, "主线板块（可选，极耗时）");
                }
                topBlockService.refreshAll(UpdateTypeEnum.INCR);
            }

            if (taskProgress != null) {
                taskProgressManager.completeTask(taskId, "任务执行完成");
            }
            log.info("---------------------------- 任务 [refreshKlineIncr - 盘中增量]   执行 end     days : {} , cost : {}",
                     days, (System.currentTimeMillis() - start) + "ms");

        } catch (Exception e) {
            if (taskProgress != null) {
                taskProgressManager.failTask(taskId, "任务执行失败: " + e.getMessage());
            }
            log.error("refreshKlineIncr 任务执行失败     >>>     days : {} , withExt : {} , withTopBlock : {}",
                      days, withExt, withTopBlock, e);
        }
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
    @Scheduled(cron = "0 15 0/1 ? * *", zone = "Asia/Shanghai")
//    @Scheduled(fixedRateString = "PT1H", zone = "Asia/Shanghai")
//    @Scheduled(cron = "0/15 * * * * ? ", zone = "Asia/Shanghai")
    public void refreshEastmoneyCookie__chrome() {
        log.info("---------------------------- 任务 [refresh cookie - 交易账户 Cookie Expires]   执行 start");


        String chromeAppName = "Google Chrome";
        String url = "https://jywg.18.cn/MarginTrade/Buy";


        MacUtil.openChrome(chromeAppName, url);


        MacUtil.closeChrome(chromeAppName, url);


        log.info("---------------------------- 任务 [refresh cookie - 交易账户 Cookie Expires]   执行 end");
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Async
    @TotalTime
    @Scheduled(cron = "0 17 0/2 ? * *", zone = "Asia/Shanghai")
    public void refreshEastmoneyCookie() {
        refreshEastmoneyCookie(1);
    }


    @Async
    @TotalTime
    public void refreshEastmoneyCookie(int loginType) {
        log.info("---------------------------- 任务 [refresh cookie - 交易账户 Cookie Expires]   执行 start     >>>     loginType : {}", loginType);


        // 登录方案（1-API登录；2-Chrome登录；）
        if (loginType == 1) {
            eastMoneyLogin.login();
        } else {
            eastMoneyChromeLogin.runLoginWorkflow();
        }


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


        // 最新交易日：优先 TQ 交易日历，TQ 不可用时回退东财快照
        LocalDate lastTradeDate = null;
        try {
            lastTradeDate = tdxIncrClient.tradingDates(LocalDate.now(), 1).getLast();
        } catch (Exception e) {
            try {
                lastTradeDate = KlineAPI.lastTradeDate();
            } catch (Exception ex) {
                log.warn("isTradeDateTime     >>>     获取最新交易日失败，跳过执行 : {}", ex.getMessage());
                return false;
            }
        }


        // 非交易日
        if (lastTradeDate == null || !lastTradeDate.isEqual(LocalDate.now())) {
            return false;
        }


        // 交易时间段：9:25~11:35 / 13:00~15:05（不含午休）
        LocalTime nowTime = LocalTime.now();
        boolean morning = !nowTime.isBefore(LocalTime.of(9, 25)) && !nowTime.isAfter(LocalTime.of(11, 35));
        boolean afternoon = !nowTime.isBefore(LocalTime.of(13, 0)) && !nowTime.isAfter(LocalTime.of(15, 5));
        return morning || afternoon;
    }


}