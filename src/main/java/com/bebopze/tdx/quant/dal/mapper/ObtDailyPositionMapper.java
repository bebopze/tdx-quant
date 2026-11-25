package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.ObtDailyPositionDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 期权BS回测-每日持仓 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
public interface ObtDailyPositionMapper extends BaseMapper<ObtDailyPositionDO> {


    int deleteByTaskId(@Param("taskId") Long taskId);


    int batchInsert(@Param("list") List<ObtDailyPositionDO> list);


    LocalDate maxTradeDate(@Param("taskId") Long taskId);


    int countByTaskIdAndTradeDate(@Param("taskId") Long taskId,
                                  @Param("tradeDate") LocalDate tradeDate);


    List<ObtDailyPositionDO> pageByTaskIdAndTradeDate(@Param("taskId") Long taskId,
                                                      @Param("tradeDate") LocalDate tradeDate,
                                                      @Param("offset") int offset,
                                                      @Param("size") int size);


    List<LocalDate> listTradeDates(@Param("taskId") Long taskId);


    List<ObtDailyPositionDO> listAllByTaskId(@Param("taskId") Long taskId);

}
