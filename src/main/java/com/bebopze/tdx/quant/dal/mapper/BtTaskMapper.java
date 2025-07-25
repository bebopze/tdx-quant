package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 回测-任务 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
public interface BtTaskMapper extends BaseMapper<BtTaskDO> {

    List<BtTaskDO> listByTaskIdAndDate(@Param("taskId") Long taskId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);
}
