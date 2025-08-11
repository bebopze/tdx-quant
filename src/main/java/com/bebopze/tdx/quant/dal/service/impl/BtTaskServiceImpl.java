package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.bebopze.tdx.quant.dal.mapper.BtTaskMapper;
import com.bebopze.tdx.quant.dal.service.IBtDailyReturnService;
import com.bebopze.tdx.quant.dal.service.IBtPositionRecordService;
import com.bebopze.tdx.quant.dal.service.IBtTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bebopze.tdx.quant.dal.service.IBtTradeRecordService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 回测-任务 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Service
public class BtTaskServiceImpl extends ServiceImpl<BtTaskMapper, BtTaskDO> implements IBtTaskService {

    @Autowired
    private IBtTradeRecordService tradeRecordService;

    @Autowired
    private IBtPositionRecordService positionRecordService;

    @Autowired
    private IBtDailyReturnService dailyReturnService;


    @Override
    public List<BtTaskDO> listByTaskId(Long taskId, LocalDateTime startCreateTime, LocalDateTime endCreateTime) {
        return baseMapper.listByTaskIdAndDate(taskId, startCreateTime, endCreateTime);
    }

    @Override
    public List<BtTaskDO> listByBatchNo(Integer batchNo, Boolean finish) {
        return baseMapper.listByBatchNo(batchNo, finish);
    }

    @Override
    public Integer getLastBatchNo() {
        return baseMapper.lastBatchNo();
    }

    @Override
    public BtTaskDO getLastBatchNoEntity() {
        return baseMapper.getLastBatchNoEntity();
    }

    @Override
    public BtTaskDO getBatchNoEntityByBatchNo(Integer batchNo) {
        return baseMapper.getBatchNoEntityByBatchNo(batchNo);
    }

    @Override
    public int delErrTaskByBatchNo(Integer batchNo) {


        // 未完成
        List<Long> taskIdList = baseMapper.listIdByBatchNo(batchNo, false);
        if (CollectionUtils.isEmpty(taskIdList)) {
            return 0;
        }


        // del
        tradeRecordService.deleteByTaskIds(taskIdList);
        positionRecordService.deleteByTaskIds(taskIdList);
        dailyReturnService.deleteByTaskIds(taskIdList);


        return baseMapper.deleteByIds(taskIdList);
    }

}