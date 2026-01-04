package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.ConfDistributedLockDO;
import com.bebopze.tdx.quant.dal.mapper.ConfDistributedLockMapper;
import com.bebopze.tdx.quant.dal.service.IConfDistributedLockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 分布式锁 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2026-01-05
 */
@Service
public class ConfDistributedLockServiceImpl extends ServiceImpl<ConfDistributedLockMapper, ConfDistributedLockDO> implements IConfDistributedLockService {


//    @Override
//    public boolean lock(String lockKey, String lockValue, Long expireSeconds) {
//        long expireTimestamp = System.currentTimeMillis() + expireSeconds * 1000;
//        return baseMapper.lock(lockKey, lockValue, expireSeconds, expireTimestamp) > 0;
//    }
//
//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public boolean releaseLock(String lockKey, String lockValue) {
////        return baseMapper.releaseLock(lockKey, lockValue);
//
//
//        try {
//            // 仅持有者才能释放：DELETE WHERE lock_key=? AND lock_value=?
//            int deleted = jdbcTemplate.update("DELETE FROM conf_distributed_lock WHERE lock_key=? AND lock_value=?", key, val);
//            if (deleted > 0) {
//                log.debug("[releaseLock] deleted by owner {} -> {}", key, val);
//                return true;
//            } else {
//                // 也有可能锁已过期并被清理，视为释放成功
//                ConfDistributedLock row = queryByKey(key);
//                if (row == null || row.getExpireTimestamp() < System.currentTimeMillis()) {
//                    log.debug("[releaseLock] no row or already expired for key={}", key);
//                    return true;
//                }
//                log.warn("[releaseLock] not owner or cannot delete: key={}, ownerInDb={}", key, row.getLockValue());
//                return false;
//            }
//        } catch (Exception ex) {
//            log.error("[releaseLock] error for key=" + key, ex);
//            return false;
//        }
//
//    }


}
