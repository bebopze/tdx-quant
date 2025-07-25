package com.bebopze.tdx.quant.common.domain.dto;

import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import lombok.Data;

import java.util.List;


/**
 * 回测 - 分析结果
 *
 * @author: bebopze
 * @date: 2025/7/26
 */
@Data
public class BacktestAnalysisDTO {

    private Long taskId;


    private BtTaskDO task;


    private List<BtTradeRecordDO> tradeRecordList;

    private List<BtPositionRecordDO> positionRecordList;

    private List<BtDailyReturnDO> dailyReturnList;
}
