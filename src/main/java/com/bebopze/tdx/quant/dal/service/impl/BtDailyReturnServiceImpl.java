package com.bebopze.tdx.quant.dal.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.bebopze.tdx.quant.dal.mapper.BtDailyReturnMapper;
import com.bebopze.tdx.quant.dal.service.IBtDailyReturnService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Semaphore;


/**
 * <p>
 * 回测-每日收益率 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Slf4j
@Service
public class BtDailyReturnServiceImpl extends ServiceImpl<BtDailyReturnMapper, BtDailyReturnDO> implements IBtDailyReturnService {


    @TotalTime
    @Override
    public BtDailyReturnDO getByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate) {
        return baseMapper.getByTaskIdAndTradeDate(taskId, tradeDate);
    }

    @Override
    public List<BtDailyReturnDO> listByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.listByTaskIdAndTradeDateRange(taskId, startDate, endDate);
    }

    @Override
    public List<BtDailyReturnDO> listByTaskId(Long taskId) {
        return listByTaskIdAndTradeDateRange(taskId, null, null);
    }


    @Deprecated
    @Override
    public List<Long> listTaskIdByBatchNoAndTotalDaysAndLeDailyReturn(Integer batchNo,
                                                                      int totalDays,
                                                                      Double dailyReturn) {

        return baseMapper.listTaskIdByBatchNoAndTotalDaysAndLeDailyReturn(batchNo, totalDays, dailyReturn);
    }

    @Override
    public int deleteByTaskIds(List<Long> taskIdList) {
        return baseMapper.deleteByTaskIds(taskIdList);
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 定义一个静态的信号量，用于限制并发写入数据库的线程数
    private static final Semaphore dbWriteSemaphore = new Semaphore(6);


    @TotalTime
    @Transactional(rollbackFor = Exception.class)
    @Retryable(
            value = {Exception.class},
            maxAttempts = 5,   // 重试次数
            backoff = @Backoff(delay = 5000, multiplier = 2, random = true, maxDelay = 90000),   // 最大90秒延迟
            exclude = {IllegalArgumentException.class, IllegalStateException.class}              // 排除业务异常
    )
    @Override
    public boolean retrySave(BtDailyReturnDO entity) {
        Long taskId = entity.getTaskId();
        LocalDate tradeDate = entity.getTradeDate();


        try {
            // 尝试获取信号量许可，获取不到会阻塞等待
            dbWriteSemaphore.acquire();
            log.info("数据库写入许可 - acquire     >>>     taskId : {}, tradeDate : {} , 队列中等待的线程数 : {}",
                     taskId, tradeDate, dbWriteSemaphore.getQueueLength());


            return this.save(entity);


        } catch (InterruptedException e) {

            // 恢复中断状态
            Thread.currentThread().interrupt();


            String errMsg = String.format("数据库写入许可 - 被中断     >>>     taskId : %s , tradeDate : %s", taskId, tradeDate);
            log.error(errMsg, e);


            throw new RuntimeException(errMsg, e);


        } catch (Exception ex) {

            log.error("dailyReturn save - err     >>>     taskId : {} , tradeDate : {} , entity : {} , errMsg : {}",
                      taskId, tradeDate, JSON.toJSONString(entity), ex.getMessage(), ex);

            throw ex;


        } finally {

            // 确保在任何情况下都释放信号量许可
            dbWriteSemaphore.release();

            log.debug("数据库写入许可 - release     >>>     taskId : {} , tradeDate : {}", taskId, tradeDate);
        }

    }


    // -----------------------------------------------------------------------------------------------------------------


}