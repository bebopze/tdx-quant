package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BacktestAnalysisDTO;
import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


/**
 * @author: bebopze
 * @date: 2025/5/20
 */
public interface BacktestService {

    Long backtest(LocalDate startDate, LocalDate endDate, boolean resume, Integer batchNo);

    Long backtest2(TopBlockStrategyEnum topBlockStrategyEnum,
                   List<String> buyConList, LocalDate startDate, LocalDate endDate, boolean resume, Integer batchNo);

    Long backtestTrade(TopBlockStrategyEnum topBlockStrategyEnum,
                       LocalDate startDate, LocalDate endDate, boolean resume, Integer batchNo);

    void checkBacktest(Long taskId);


    List<BtTaskDO> listTask(Long taskId,
                            List<Integer> batchNo,
                            LocalDateTime startCreateTime,
                            LocalDateTime endCreateTime);

    BacktestAnalysisDTO analysis(Long taskId);


    /**
     * 回测 - 异常task删除（by任务批次号）
     *
     * @param batchNo 任务批次号
     * @return
     */
    int delErrTaskByBatchNo(Integer batchNo);

    /**
     * 回测 - task删除
     *
     * @param taskIdList taskId列表
     * @return
     */
    int deleteByTaskIds(List<Long> taskIdList);


    /**
     * 个股 - 交易记录列表
     *
     * @param taskId
     * @param stockCode
     * @return
     */
    List<BtTradeRecordDO> stockTradeRecordList(Long taskId, String stockCode);


    @Deprecated
    void holdingStockRule(String stockCode);
}
