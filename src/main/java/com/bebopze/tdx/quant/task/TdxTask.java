package com.bebopze.tdx.quant.task;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.anno.DistributedLock;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BSStrategyInfoDTO;
import com.bebopze.tdx.quant.common.util.MacUtil;
import com.bebopze.tdx.quant.service.*;
import com.bebopze.tdx.quant.task.progress.TaskProgressManager;
import com.bebopze.tdx.quant.task.script.EastMoneyChromeLogin;
import com.bebopze.tdx.quant.task.script.EastMoneyLogin;
import com.bebopze.tdx.quant.task.script.TdxScript;
import com.bebopze.tdx.quant.task.script.TdxZipDownScript;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;


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


    @Autowired
    private EastMoneyLogin eastMoneyLogin;

    @Autowired
    private EastMoneyChromeLogin eastMoneyChromeLogin;


    @Autowired
    private TaskService taskService;


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
     * refreshAll  -  盘中  ->  实时行情 更新（nd）                        // nd（更新频率：1次/min）
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
        taskService.execTask__refreshKline__lastDay(true);
    }


    /**
     * refreshAll  -  盘后  ->  近N日 更新（nd）                          // nd（更新频率：1次/日）
     */
    @TotalTime
    @Async
    // @Scheduled(cron = "0 10 16 ? * 1-5", zone = "Asia/Shanghai")
    public void execTask__refreshAll__ND() {
        taskService.execTask__refreshAll(UpdateTypeEnum.N_DAY);
    }

    /**
     * refreshAll  -  盘后  ->  全量历史 更新（his + nd）                 // his（更新频率：1次/年）
     */
    @TotalTime
    @Async
    // @Scheduled(cron = "0 10 16 ? * 1-5", zone = "Asia/Shanghai")
    public void execTask__refreshAll__HIS() {
        taskService.execTask__refreshAll(UpdateTypeEnum.HIS);
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


//    /**
//     * INDEX_BLOCK-880515   当日有数据（AMO>500亿）             // 主线板块（盘中  ->  无 TDX 板块数据   ->   除非手动导出）
//     *
//     * @return
//     */
//    private boolean checkBlockLastDay() {
//
//        LdayParser.LdayDTO lastDTO = ListUtil.last(LdayParser.parseByStockCode(INDEX_BLOCK));
//
//        return lastDTO != null
//                // 当日
//                && lastDTO.getTradeDate().isEqual(LocalDate.now())
//                // 有数据（AMO>500亿）
//                && lastDTO.getAmount().doubleValue() > 500_0000_0000L;
//    }


    /**
     * 更新 回测任务（今日数据）
     */
    public void refreshBacktest() {
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


}