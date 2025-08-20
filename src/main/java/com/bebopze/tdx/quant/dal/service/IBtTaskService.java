package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 回测-任务 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
public interface IBtTaskService extends IService<BtTaskDO> {

    List<BtTaskDO> listByTaskId(Long taskId,
                                List<Integer> batchNo,
                                LocalDateTime startCreateTime,
                                LocalDateTime endCreateTime);

    List<BtTaskDO> listByBatchNo(Integer batchNo, Boolean finish);

    Integer getLastBatchNo();

    BtTaskDO getLastBatchNoEntity();

    BtTaskDO getBatchNoEntityByBatchNo(Integer batchNo);

    int delErrTaskByBatchNo(Integer batchNo);

    int delErrTaskByTaskIds(List<Long> taskIdList);
}