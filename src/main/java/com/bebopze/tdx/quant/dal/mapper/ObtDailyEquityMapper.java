package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.ObtDailyEquityDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 期权BS回测-每日净值 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
public interface ObtDailyEquityMapper extends BaseMapper<ObtDailyEquityDO> {


    int deleteByTaskId(@Param("taskId") Long taskId);


    int batchInsert(@Param("list") List<ObtDailyEquityDO> list);


    List<ObtDailyEquityDO> listByTaskIdAndRange(@Param("taskId") Long taskId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);


    int countByTaskId(@Param("taskId") Long taskId);

}
