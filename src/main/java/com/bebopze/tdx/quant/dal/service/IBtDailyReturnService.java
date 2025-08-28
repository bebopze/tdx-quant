package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;


/**
 * <p>
 * 回测-每日收益率 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
public interface IBtDailyReturnService extends IService<BtDailyReturnDO> {


    BtDailyReturnDO getByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate);

    List<BtDailyReturnDO> listByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate);

    List<BtDailyReturnDO> listByTaskId(Long taskId);


    /**
     * 异常 TaskId     ->     单日下跌 -10%以上（bug：某日 持仓数据 中途丢失）
     *
     * @param batchNo
     * @param totalDays
     * @param dailyReturn
     * @return
     */
    @Deprecated
    List<Long> listTaskIdByBatchNoAndTotalDaysAndLeDailyReturn(Integer batchNo, int totalDays, Double dailyReturn);

    int deleteByTaskIds(List<Long> taskIdList);


    boolean retrySave(BtDailyReturnDO entity);
}