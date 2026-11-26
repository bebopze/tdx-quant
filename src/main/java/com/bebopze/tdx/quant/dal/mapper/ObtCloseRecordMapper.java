package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.ObtCloseRecordDO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 期权BS回测-平仓记录 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
public interface ObtCloseRecordMapper extends BaseMapper<ObtCloseRecordDO> {


    int deleteByTaskId(@Param("taskId") Long taskId);


    int batchInsert(@Param("list") List<ObtCloseRecordDO> list);


    long countByTaskId(@Param("taskId") Long taskId,
                       @Param("closeReason") String closeReason,
                       @Param("startDate") LocalDate startDate,
                       @Param("endDate") LocalDate endDate);


    List<ObtCloseRecordDO> pageByTaskId(@Param("taskId") Long taskId,
                                        @Param("closeReason") String closeReason,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate,
                                        @Param("offset") int offset,
                                        @Param("size") int size);


    List<Map<String, Object>> listContribution(@Param("taskId") Long taskId,
                                               @Param("startDate") java.time.LocalDate startDate,
                                               @Param("endDate") java.time.LocalDate endDate);


    BigDecimal avgHoldDays(@Param("taskId") Long taskId,
                           @Param("startDate") java.time.LocalDate startDate,
                           @Param("endDate") java.time.LocalDate endDate);


    List<ObtCloseRecordDO> listAllByTaskId(@Param("taskId") Long taskId);

}
