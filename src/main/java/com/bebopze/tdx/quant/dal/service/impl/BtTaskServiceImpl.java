package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.DBLimiter;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.bebopze.tdx.quant.dal.mapper.BtTaskMapper;
import com.bebopze.tdx.quant.dal.service.IBtDailyReturnService;
import com.bebopze.tdx.quant.dal.service.IBtPositionRecordService;
import com.bebopze.tdx.quant.dal.service.IBtTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bebopze.tdx.quant.dal.service.IBtTradeRecordService;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
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
@Slf4j
@Service
public class BtTaskServiceImpl extends ServiceImpl<BtTaskMapper, BtTaskDO> implements IBtTaskService {

    @Autowired
    private IBtTradeRecordService tradeRecordService;

    @Autowired
    private IBtPositionRecordService positionRecordService;

    @Autowired
    private IBtDailyReturnService dailyReturnService;

    @Autowired
    private ApplicationContext applicationContext;


    @Override
    public List<BtTaskDO> listByTaskId(Long taskId,
                                       List<Integer> batchNoList,
                                       LocalDateTime startCreateTime,
                                       LocalDateTime endCreateTime) {

        return baseMapper.listByTaskIdAndDate(taskId, batchNoList, startCreateTime, endCreateTime);
    }

    @Override
    public List<BtTaskDO> listByBatchNoAndStatus(Integer batchNo, Integer status) {
        return baseMapper.listByBatchNoAndStatus(batchNo, status);
    }

    @Override
    public List<Long> listIdByBatchNoAndStatus(Integer batchNo, Integer status) {
        return baseMapper.listIdByBatchNoAndStatus(batchNo, status);
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


    @Synchronized
    @TotalTime
    @Override
    // @Transactional(rollbackFor = Exception.class)
    public int delErrTaskByBatchNo(Integer batchNo) {


        // -------------------------------------------------------------------------------------------------------------


        // 未完成
        List<Long> errTaskIdList = baseMapper.listIdByBatchNoAndStatus(batchNo, 1);
        log.info("未完成     >>>     size : {} , errTaskIdList : {}", errTaskIdList.size(), errTaskIdList);


        // 异常     ->     持仓数据 中途丢失
        Integer maxTotalDays = baseMapper.getMaxTotalDaysByBatchNo(batchNo);
        List<Long> errTaskIdList2 = baseMapper.listErrTaskIdByBatchNoAndTotalDay(batchNo, maxTotalDays);
        log.info("已完成 -> 异常（持仓数据 中途丢失）    >>>     maxTotalDay : {} , size : {} , errTaskIdList2 : {}", maxTotalDays, errTaskIdList2.size(), errTaskIdList2);


        errTaskIdList.addAll(errTaskIdList2);


        if (CollectionUtils.isEmpty(errTaskIdList)) {
            return 0;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 获取当前类SpringBean，用于正确触发事务
        IBtTaskService taskService = applicationContext.getBean(IBtTaskService.class);


        return taskService.delErrTaskByTaskIds(errTaskIdList);
    }


    @TotalTime
    @Override
    // @Transactional(rollbackFor = Exception.class)       // 已分库分表  ->  已失效
    public int delErrTaskByTaskIds(List<Long> taskIdList) {


        // 总数
        int size = taskIdList.size();
        // 1次 N个
        int N = 50;


        for (int i = 0; i < size; ) {

            // 1次 N个
            List<Long> subList = taskIdList.subList(i, Math.min(i += N, size));


            // del
            tradeRecordService.deleteByTaskIds(subList);
            positionRecordService.deleteByTaskIds(subList);
            dailyReturnService.deleteByTaskIds(subList);


            baseMapper.deleteByIds(subList);
        }


        return size;
    }


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    @DBLimiter(6)
    // @Transactional(rollbackFor = Exception.class)
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 30000),   // 最大30秒延迟
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class,
                    SQLIntegrityConstraintViolationException.class}   // 排除业务异常
    )
    @Override
    public void delBacktestDataByTaskIdAndDate(Long taskId, LocalDate startDate, LocalDate endDate) {

        tradeRecordService.deleteByTaskIdAndTradeDateRange(taskId, startDate, endDate);
        positionRecordService.deleteByTaskIdAndTradeDateRange(taskId, startDate, endDate);
        dailyReturnService.deleteByTaskIdAndTradeDateRange(taskId, startDate, endDate);
    }


    // -----------------------------------------------------------------------------------------------------------------


}