package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.ConfDistributedLockDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 分布式锁 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-01-05
 */
public interface ConfDistributedLockMapper extends BaseMapper<ConfDistributedLockDO> {


    /**
     * 尝试获取锁 - 原子操作
     */
    int tryAcquireLock(@Param("lockKey") String lockKey,
                       @Param("lockValue") String lockValue,
                       @Param("expire") long expire,
                       @Param("expireTimestamp") long expireTimestamp,
                       @Param("machineUniqueId") String machineUniqueId);

    /**
     * 释放锁 - 只有持有者才能释放
     */
    int releaseLock(@Param("lockKey") String lockKey,
                    @Param("lockValue") String lockValue);

    /**
     * 续期锁
     */
    int renewLock(@Param("lockKey") String lockKey,
                  @Param("lockValue") String lockValue,
                  @Param("newExpire") long newExpire,
                  @Param("newExpireTimestamp") long newExpireTimestamp);

    /**
     * 查询过期的锁
     */
    List<ConfDistributedLockDO> selectExpiredLocks(@Param("currentTime") long currentTime);

    /**
     * 批量清理过期锁
     */
    int batchDeleteExpiredLocks(@Param("lockIds") List<Long> lockIds);

    /**
     * 清理本地锁
     *
     * @param machineUniqueId 本机唯一标识
     * @return
     */
    int cleanLocalLocks(@Param("machineUniqueId") String machineUniqueId);
}