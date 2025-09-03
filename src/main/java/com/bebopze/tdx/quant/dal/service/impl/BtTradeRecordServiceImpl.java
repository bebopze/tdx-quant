package com.bebopze.tdx.quant.dal.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import com.bebopze.tdx.quant.dal.mapper.BtTradeRecordMapper;
import com.bebopze.tdx.quant.dal.service.IBtTradeRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Semaphore;


/**
 * <p>
 * 回测-BS交易记录 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Slf4j
@Service
public class BtTradeRecordServiceImpl extends ServiceImpl<BtTradeRecordMapper, BtTradeRecordDO> implements IBtTradeRecordService {


    @TotalTime
    @Override
    public List<BtTradeRecordDO> listByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate) {
        return baseMapper.listByTaskIdAndTradeDate(taskId, tradeDate);
    }


    @Override
    public List<BtTradeRecordDO> listByTaskIdAndTradeDateRange(Long taskId,
                                                               LocalDate startTradeDate,
                                                               LocalDate endTradeDate) {

        return baseMapper.listByTaskIdAndTradeDateRange(taskId, startTradeDate, endTradeDate);
    }

    @Override
    public List<BtTradeRecordDO> listByTaskId(Long taskId) {
        return listByTaskIdAndTradeDateRange(taskId, null, null);
    }

    @Override
    public List<BtTradeRecordDO> listByTaskIdAndStockCode(Long taskId, String stockCode) {
        return baseMapper.listByTaskIdAndStockCode(taskId, stockCode);
    }

    @Override
    public int deleteByTaskIds(List<Long> taskIdList) {
        return baseMapper.deleteByTaskIds(taskIdList);
    }

    public int deleteByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.deleteByTaskIdAndTradeDateRange(taskId, startDate, endDate);
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 定义一个静态的信号量，用于限制并发写入数据库的线程数
    private static final Semaphore dbWriteSemaphore = new Semaphore(6);


    @TotalTime
    @Transactional(rollbackFor = Exception.class)
    @Retryable(
            value = {Exception.class, RecoverableDataAccessException.class},   // 包含可恢复的数据库异常
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 90000),   // 最大90秒延迟
            exclude = {IllegalArgumentException.class, IllegalStateException.class}              // 排除业务异常
    )
    @Override
    public boolean retryBatchSave(List<BtTradeRecordDO> entityList) {
        if (CollectionUtils.isEmpty(entityList)) {
            return true;
        }


        Long taskId = entityList.get(0).getTaskId();
        LocalDate tradeDate = entityList.get(0).getTradeDate();
        int size = entityList.size();


        try {
            // 尝试获取信号量许可，获取不到会阻塞等待
            dbWriteSemaphore.acquire();
            log.info("数据库写入许可 - acquire     >>>     taskId : {}, tradeDate : {} , size : {} , 队列中等待的线程数 : {}",
                     taskId, tradeDate, size, dbWriteSemaphore.getQueueLength());


            // 获取许可后，执行实际的数据库写入操作
            return this.saveBatch(entityList);


        } catch (InterruptedException e) {

            // 恢复中断状态
            Thread.currentThread().interrupt();


            String errMsg = String.format("数据库写入许可 - 被中断     >>>     taskId : %s , tradeDate : %s , size : %s", taskId, tradeDate, size);
            log.error(errMsg, e);


            throw new RuntimeException(errMsg, e);


        } catch (Exception ex) {

            String errMsg = String.format("tradeRecord saveBatch - err     >>>     taskId : %s , tradeDate : %s , size : %s , entityList : %s , errMsg : %s",
                                          taskId, tradeDate, size, JSON.toJSONString(entityList), ex.getMessage());
            log.error(errMsg, ex);


            // 重新抛出异常，以便触发 @Retryable 机制
            // throw ex;
            throw new RecoverableDataAccessException(errMsg, ex);


        } finally {

            // 确保在任何情况下都释放信号量许可
            dbWriteSemaphore.release();

            log.debug("数据库写入许可 - release     >>>     taskId : {} , tradeDate : {} , size : {}", taskId, tradeDate, size);
        }

    }


    // 可选的恢复方法，当重试超过 maxAttempts 次数后调用
    @Recover
    public boolean recover(Exception e, List<BtTradeRecordDO> entityList) {

        Long taskId = entityList.get(0).getTaskId();
        LocalDate tradeDate = entityList.get(0).getTradeDate();
        int size = entityList.size();


        log.error("tradeRecord saveBatch - 最终失败     >>>     taskId : {} , tradeDate : {} , size : {} , entityList : {} , 最终异常 : {}",
                  taskId, tradeDate, size, JSON.toJSONString(entityList), e.getMessage(), e);


        // 根据业务需求   决定 返回值 或 抛出特定异常


        // false   ->   表示保存失败（异常回测Task   ->   继续正常执行）
        // return false;


        // 抛出异常   ->   中断 异常回测Task
        throw new RuntimeException("数据库写入失败，回测任务中断", e);
    }


    // -----------------------------------------------------------------------------------------------------------------


}