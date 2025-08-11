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