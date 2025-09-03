package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 回测-每日收益率 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
public interface BtDailyReturnMapper extends BaseMapper<BtDailyReturnDO> {


    BtDailyReturnDO getByTaskIdAndTradeDate(@Param("taskId") Long taskId,
                                            @Param("tradeDate") LocalDate tradeDate);

    BtDailyReturnDO lastByTaskId(@Param("taskId") Long taskId);

    List<BtDailyReturnDO> listByTaskIdAndTradeDateRange(@Param("taskId") Long taskId,
                                                        @Param("startTradeDate") LocalDate startTradeDate,
                                                        @Param("endTradeDate") LocalDate endTradeDate);


    @Deprecated
    List<LocalDate> listTradeDateByTaskId(@Param("taskId") Long taskId);


    @Deprecated
    List<Long> listTaskIdByBatchNoAndTotalDaysAndLeDailyReturn(@Param("batchNo") Integer batchNo,
                                                               @Param("totalDays") int totalDays,
                                                               @Param("dailyReturn") Double dailyReturn);


    int deleteByTaskIds(@Param("taskIdList") List<Long> taskIdList);


    int deleteByTaskIdAndTradeDateRange(@Param("taskId") Long taskId,
                                        @Param("startTradeDate") LocalDate startTradeDate,
                                        @Param("endTradeDate") LocalDate endTradeDate);

}