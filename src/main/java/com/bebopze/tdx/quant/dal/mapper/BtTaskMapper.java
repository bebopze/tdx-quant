package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
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
                                       @Param("batchNoList") List<Integer> batchNoList,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);


    List<BtTaskDO> listByBatchNo(@Param("batchNo") Integer batchNo,
                                 @Param("finish") Boolean finish);

    List<Long> listIdByBatchNo(@Param("batchNo") Integer batchNo,
                               @Param("finish") Boolean finish);


    Integer lastBatchNo();

    BtTaskDO getLastBatchNoEntity();

    BtTaskDO getBatchNoEntityByBatchNo(@Param("batchNo") Integer batchNo);


    Integer getMaxTotalDayByBatchNo(@Param("batchNo") Integer batchNo);

    /**
     * 异常     ->     持仓数据 中途丢失
     *
     * @param batchNo  任务批次号
     * @param totalDay 实际总天数（正常task = totalDay     /     异常task < totalDay）
     * @return
     */
    List<Long> listErrTaskIdByBatchNoAndTotalDay(@Param("batchNo") Integer batchNo,
                                                 @Param("totalDay") Integer totalDay);

    BtTaskDO getMaxFinalNavTaskByBatchNo(@Param("batchNo") Integer batchNo);
}
