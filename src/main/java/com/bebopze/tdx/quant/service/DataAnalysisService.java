package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.analysis.TopNAnalysisDTO;
import com.bebopze.tdx.quant.common.domain.dto.analysis.TopPoolAnalysisDTO;
import com.bebopze.tdx.quant.common.domain.dto.analysis.TopPoolSumReturnDTO;
import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;

import java.time.LocalDate;
import java.util.List;

/**
 * @author: bebopze
 * @date: 2025/10/28
 */
public interface DataAnalysisService {


    TopPoolAnalysisDTO topListAnalysis(LocalDate startDate, LocalDate endDate, Integer topPoolType, Integer type);

    TopNAnalysisDTO top100(LocalDate startDate, LocalDate endDate, Integer topPoolType, Integer type);


    List<BtDailyReturnDO> calcDailyReturn(List<BtDailyReturnDO> dailyReturnDOList);

    TopPoolSumReturnDTO sumReturn(List<BtDailyReturnDO> dailyReturnDOList,
                                  List<BtTradeRecordDO> tradeRecordList,
                                  List<BtPositionRecordDO> positionRecordList);
}
