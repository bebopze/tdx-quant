package com.bebopze.tdx.quant.optionbs;

import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.mapper.*;

import java.time.LocalDate;


/**
 * 命令行实跑入口（不启动 Spring，不触发任何交易任务）
 *
 * <pre>
 * java -cp ... com.bebopze.tdx.quant.optionbs.OptionBsCli 2017-01-01 2026-11-25 1000000
 * </pre>
 *
 * @author bebopze
 * @date 2026/11/26
 */
public class OptionBsCli {


    public static void main(String[] args) throws Exception {

        LocalDate start = LocalDate.parse(args.length > 0 ? args[0] : "2017-01-01");
        LocalDate end = LocalDate.parse(args.length > 1 ? args[1] : "2026-11-25");
        double capital = Double.parseDouble(args.length > 2 ? args[2] : "1000000");


        OptionBsParams p = new OptionBsParams();
        p.setStartDate(start);
        p.setEndDate(end);
        p.setInitialCapital(capital);
        p.setTaskName("中期信号 · 全标的 · 当月/次月/当季");
        p.setDataVersion("base_option @ " + LocalDate.now());


        OptionBsRepository repo = MybatisPlusUtil.getOptionBsRepository();
        String taskNo = repo.nextTaskNo();
        long taskId = repo.createTask(p, taskNo);
        repo.updateTask(taskId, 1, 1, null);
        repo.log(taskId, "INFO", "任务启动 " + taskNo + " capital=" + capital);


        long st = System.currentTimeMillis();
        try {
            OptionBsDataLoader loader = new OptionBsDataLoader(
                    MybatisPlusUtil.getMapper(QaMarketMidCycleMapper.class),
                    MybatisPlusUtil.getMapper(BaseStockMapper.class),
                    MybatisPlusUtil.getMapper(BaseOptionMapper.class),
                    MybatisPlusUtil.getMapper(ObtIndexKlineMapper.class));

            OptionBsData data = loader.load(p);
            repo.log(taskId, "INFO", "数据加载完成 underlyings=" + data.underlyings.size()
                    + " contracts=" + data.contracts.values().stream().mapToInt(java.util.List::size).sum()
                    + " calendar=" + data.calendar.size());
            repo.updateProgress(taskId, 40);


            OptionBsEngine engine = new OptionBsEngine(p, data);
            OptionBsModels.Result r = engine.run();
            r.taskId = taskId;
            r.taskNo = taskNo;
            repo.updateProgress(taskId, 80);
            repo.saveResult(taskId, r);
            repo.log(taskId, "INFO", "回测完成 final=" + String.format("%.2f", r.finalEquity)
                    + " total=" + String.format("%.2f%%", r.totalReturnPct)
                    + " trades=" + r.totalTrades + " closes=" + r.closeRecords.size());


            System.out.println("=================================================");


            System.out.println("TASK       : " + taskNo + "  (id=" + taskId + ")");
            System.out.println("PERIOD     : " + r.actualStartDate + " ~ " + r.actualEndDate);
            System.out.println("CAPITAL    : " + capital);
            System.out.println("FINAL      : " + String.format("%.2f", r.finalEquity));
            System.out.println("TOTAL      : " + String.format("%.2f%%", r.totalReturnPct));
            System.out.println("ANNUAL     : " + String.format("%.2f%%", r.annualReturnPct));
            System.out.println("MAX DD     : " + String.format("%.2f%%", r.maxDrawdownPct)
                                       + "  " + r.maxDrawdownStart + " ~ " + r.maxDrawdownEnd);
            System.out.println("SHARPE     : " + String.format("%.2f", r.sharpe)
                                       + "  CALMAR " + String.format("%.2f", r.calmar));
            System.out.println("TRADES     : " + r.totalTrades + "  (buy=" + r.buyTrades + ", sell=" + r.sellTrades + ")");
            System.out.println("CLOSES     : " + r.closeRecords.size() + "  SIGNALS=" + r.signalCount);
            System.out.println("DAYS       : " + r.totalDays + "  ELAPSED=" + (System.currentTimeMillis() - st) + "ms");


            System.out.println("=================================================");


        } catch (Exception e) {
            e.printStackTrace();
            repo.updateTask(taskId, 3, 100, e.toString());
            repo.log(taskId, "ERROR", e.toString());
            throw e;
        }
    }


}