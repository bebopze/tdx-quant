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


    /**
     * 回测task - 汇总结果
     */
    private BtTaskDO task;


    /**
     * 交易记录
     */
    private List<BtTradeRecordDO> tradeRecordList;

    /**
     * 持仓记录
     */
    private List<BtPositionRecordDO> positionRecordList;

    /**
     * 收益记录
     */
    private List<BtDailyReturnDO> dailyReturnList;
}
