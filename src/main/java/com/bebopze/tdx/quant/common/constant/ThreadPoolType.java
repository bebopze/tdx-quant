package com.bebopze.tdx.quant.common.constant;


/**
 * 线程池 type
 *
 * @author: bebopze
 * @date: 2025/8/11
 */
public enum ThreadPoolType {


    /**
     * 量化计算、技术指标
     */
    CPU_INTENSIVE,
    CPU_INTENSIVE_2,


    /**
     * 回测 task（CPU + 分配密集，非纯 IO）
     *
     * <p>历史遗留问题：{@code /api/backtest/exec} 原先用 {@link #IO_INTENSIVE} 跑 867 个回测 task，
     * 而该池 {@code corePoolSize = 核数 × 15}（8 核 = 120 线程，max 160）。
     * 回测 task 里 conMap 每股每日分配 58 KB、每天 316 MB，属于典型的「CPU + 分配」型负载，
     * 120 线程抢 8 个核会让 GC 线程与业务线程互相饿死。
     */
    BACKTEST,


    /**
     * HTTP调用、外部API
     */
    IO_INTENSIVE,
    IO_INTENSIVE_2,


    /**
     * 数据库读写
     */
    DATABASE,


    /**
     * 文件读写
     */
    FILE_IO,


    /**
     * 异步通知、推送
     */
    ASYNC_TASK
}