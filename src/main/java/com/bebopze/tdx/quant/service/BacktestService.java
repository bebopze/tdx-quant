package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.BacktestAnalysisDTO;
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

    void checkBacktest(Long taskId);


    List<BtTaskDO> listTask(Long taskId, LocalDateTime startCreateTime, LocalDateTime endCreateTime);

    BacktestAnalysisDTO analysis(Long taskId);

    /**
     * 个股 - 交易记录列表
     *
     * @param taskId
     * @param stockCode
     * @return
     */
    List<BtTradeRecordDO> stockTradeRecordList(Long taskId, String stockCode);

    void holdingStockRule(String stockCode);

}
