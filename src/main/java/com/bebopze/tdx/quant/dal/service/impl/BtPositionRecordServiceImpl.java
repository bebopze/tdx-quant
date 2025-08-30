package com.bebopze.tdx.quant.dal.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.bebopze.tdx.quant.dal.mapper.BtPositionRecordMapper;
import com.bebopze.tdx.quant.dal.service.IBtPositionRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Semaphore;


/**
 * <p>
 * 回测-每日持仓记录 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Slf4j
@Service
public class BtPositionRecordServiceImpl extends ServiceImpl<BtPositionRecordMapper, BtPositionRecordDO> implements IBtPositionRecordService {


    @TotalTime
    @Override
    public List<BtPositionRecordDO> listByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate) {
        return baseMapper.listByTaskIdAndTradeDate(taskId, tradeDate);
    }

    @Override
    public List<BtPositionRecordDO> listByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate) {
        return baseMapper.listByTaskIdAndTradeDateRange(taskId, startDate, endDate);
    }

    @Override
    public List<BtPositionRecordDO> listByTaskId(Long taskId) {
        return listByTaskIdAndTradeDateRange(taskId, null, null);
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
    public boolean retryBatchSave(List<BtPositionRecordDO> entityList) {
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

            log.error("positionRecord saveBatch - err     >>>     taskId : {} , tradeDate : {} , size : {} , entityList : {} , errMsg : {}",
                      taskId, tradeDate, size, JSON.toJSONString(entityList), ex.getMessage(), ex);

            throw ex;


        } finally {

            // 确保在任何情况下都释放信号量许可
            dbWriteSemaphore.release();

            log.debug("数据库写入许可 - release     >>>     taskId : {} , tradeDate : {} , size : {}", taskId, tradeDate, size);
        }

    }



    // -----------------------------------------------------------------------------------------------------------------


}