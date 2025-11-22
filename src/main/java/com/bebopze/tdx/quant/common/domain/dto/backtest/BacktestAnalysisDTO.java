package com.bebopze.tdx.quant.common.domain.dto.backtest;

import com.bebopze.tdx.quant.common.domain.dto.analysis.TopPoolSumReturnDTO;
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
     * 清仓记录
     */
    private List<BtPositionRecordDO> clearPositionRecordList;

    /**
     * 收益记录
     */
    private List<BtDailyReturnDO> dailyReturnList;


    /**
     * 收益汇总结果（胜率/盈亏比、最大回撤、夏普比率、年化收益率、...）
     */
    private TopPoolSumReturnDTO sumReturnDTO;


    // ------------------------------------------------------------------- 回撤记录


    /**
     * 每日对应 -> 最大回撤
     */
    private List<MaxDrawdownPctDTO> dailyDrawdownPctList;


    // ------------------------------------------------------------------- 持仓主线记录


    private List<PositionTopBlockDTO> positionTopBlockList;


}