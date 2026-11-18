package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;


/**
 * task
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
public interface TaskService {


    /**
     * 刷新 全量行情（nd -> 近N日  /  his -> 全量历史）
     *
     * @param updateTypeEnum
     */
    void execTask__refreshAll(UpdateTypeEnum updateTypeEnum);


    /**
     * 刷新 实时行情（nd -> 近1日）
     *
     * @param check
     */
    void execTask__refreshKline__lastDay(boolean check);

}