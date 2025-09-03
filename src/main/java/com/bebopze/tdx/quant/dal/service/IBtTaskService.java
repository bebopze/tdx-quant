package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
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

    /**
     * listBy   任务批次号、任务状态
     *
     * @param batchNo 任务批次号
     * @param status  任务状态（用于每日 更新至最新交易日）：1-进行中（新开任务）；2-已完成（已更新至 最新交易日）；3-待更新至 最新交易日（之前已完成过）；
     * @return
     */
    List<BtTaskDO> listByBatchNoAndStatus(Integer batchNo, Integer status);

    List<Long> listIdByBatchNoAndStatus(Integer batchNo, Integer status);


    Integer getLastBatchNo();

    BtTaskDO getLastBatchNoEntity();

    BtTaskDO getBatchNoEntityByBatchNo(Integer batchNo);

    int delErrTaskByBatchNo(Integer batchNo);

    int delErrTaskByTaskIds(List<Long> taskIdList);


    /**
     * DEL     ->     待更新区间 old 回测数据
     *
     * @param taskId
     * @param startDate
     * @param endDate
     */
    void delBacktestDataByTaskIdAndDate(Long taskId, LocalDate startDate, LocalDate endDate);

}