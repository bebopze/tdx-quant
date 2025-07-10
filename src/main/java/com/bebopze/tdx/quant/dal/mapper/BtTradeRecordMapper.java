package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 回测-BS交易记录 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
public interface BtTradeRecordMapper extends BaseMapper<BtTradeRecordDO> {


    List<BtTradeRecordDO> listByTaskIdAndTradeDate(@Param("taskId") Long taskId,
                                                   @Param("tradeDate") LocalDate tradeDate);


    List<BtTradeRecordDO> listByTaskIdAndTradeDateRange(@Param("taskId") Long taskId,
                                                        @Param("startTradeDate") LocalDate startTradeDate,
                                                        @Param("endTradeDate") LocalDate endTradeDate);
}
