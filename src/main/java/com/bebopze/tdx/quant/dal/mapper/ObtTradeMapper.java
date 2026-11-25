package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.ObtTradeDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 期权BS回测-交易明细 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
public interface ObtTradeMapper extends BaseMapper<ObtTradeDO> {


    int deleteByTaskId(@Param("taskId") Long taskId);


    int batchInsert(@Param("list") List<ObtTradeDO> list);


    long countByTaskId(@Param("taskId") Long taskId,
                       @Param("tradeType") Integer tradeType,
                       @Param("reasonCode") String reasonCode,
                       @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);


    List<ObtTradeDO> pageByTaskId(@Param("taskId") Long taskId,
                                  @Param("tradeType") Integer tradeType,
                                  @Param("reasonCode") String reasonCode,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("offset") int offset,
                                  @Param("size") int size);


    List<Map<String, Object>> listSellReasons(@Param("taskId") Long taskId);


    List<ObtTradeDO> listAllByTaskId(@Param("taskId") Long taskId);

}
