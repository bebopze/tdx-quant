package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.dal.entity.BtTaskDO;
import com.bebopze.tdx.quant.dal.mapper.BtTaskMapper;
import com.bebopze.tdx.quant.dal.service.IBtDailyReturnService;
import com.bebopze.tdx.quant.dal.service.IBtPositionRecordService;
import com.bebopze.tdx.quant.dal.service.IBtTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bebopze.tdx.quant.dal.service.IBtTradeRecordService;
import com.google.common.collect.Lists;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


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
        Integer maxTotalDay = baseMapper.getMaxTotalDayByBatchNo(batchNo);
        List<Long> errTaskIdList2 = baseMapper.listErrTaskIdByBatchNoAndTotalDay(batchNo, maxTotalDay);
        log.info("已完成 -> 异常（持仓数据 中途丢失）    >>>     maxTotalDay : {} , size : {} , errTaskIdList2 : {}", maxTotalDay, errTaskIdList2.size(), errTaskIdList2);


        errTaskIdList.addAll(errTaskIdList2);


        if (CollectionUtils.isEmpty(errTaskIdList)) {
            return 0;
        }


        // -------------------------------------------------------------------------------------------------------------


        int delTotal = 0;


        // 分批处理   ->   1次 N个
        int N = 10;
        int size = errTaskIdList.size();


        // 获取当前类SpringBean，用于正确触发事务
        IBtTaskService taskService = applicationContext.getBean(IBtTaskService.class);


//        // del
//        for (int i = 0; i < size; ) {
//
//            List<Long> subList = errTaskIdList.subList(i, Math.min(i += N, size));
//
//
//            try {
//                delTotal += taskService.delErrTaskByTaskIds(subList);
//                log.info("delErrTaskByTaskIds suc     >>>     size : {} , i : {} , delTotal : {} , taskIdList : {}", size, i, delTotal, subList);
//
//            } catch (Exception e) {
//                log.error("delErrTaskByTaskIds fail     >>>     size : {} , i : {} , delTotal : {} , taskIdList : {} , errMsg : {}", size, i, delTotal, subList, e.getMessage(), e);
//            }
//        }


        AtomicInteger del_total = new AtomicInteger();
        errTaskIdList.parallelStream().forEach(taskId -> {
            try {
                int count = taskService.delErrTaskByTaskIds(Lists.newArrayList(taskId));
                del_total.addAndGet(count);
                log.info("delErrTaskByTaskIds suc     >>>     size : {} , delTotal : {} , taskIdList : {}", size, del_total.get(), taskId);

            } catch (Exception e) {
                log.info("delErrTaskByTaskIds fail     >>>     size : {} , delTotal : {} , taskIdList : {} , errMsg : {}", size, del_total.get(), taskId, e.getMessage(), e);
            }
        });


        return del_total.get();
    }


    @TotalTime
    @Override
    // @Transactional(rollbackFor = Exception.class)       // 已分库分表  ->  已失效
    public int delErrTaskByTaskIds(List<Long> taskIdList) {


        // del
        tradeRecordService.deleteByTaskIds(taskIdList);
        positionRecordService.deleteByTaskIds(taskIdList);
        dailyReturnService.deleteByTaskIds(taskIdList);


        return baseMapper.deleteByIds(taskIdList);
    }

}