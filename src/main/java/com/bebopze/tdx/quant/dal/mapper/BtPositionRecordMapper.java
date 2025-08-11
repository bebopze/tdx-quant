package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 回测-每日持仓记录 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
public interface BtPositionRecordMapper extends BaseMapper<BtPositionRecordDO> {


    List<BtPositionRecordDO> listByTaskIdAndTradeDate(@Param("taskId") Long taskId,
                                                      @Param("tradeDate") LocalDate tradeDate);


    List<BtPositionRecordDO> listByTaskIdAndTradeDateRange(@Param("taskId") Long taskId,
                                                           @Param("startTradeDate") LocalDate startTradeDate,
                                                           @Param("endTradeDate") LocalDate endTradeDate);


    int deleteByTaskIds(@Param("taskIdList") List<Long> taskIdList);

}