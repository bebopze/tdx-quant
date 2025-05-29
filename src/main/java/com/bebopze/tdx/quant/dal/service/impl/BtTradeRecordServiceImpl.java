package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import com.bebopze.tdx.quant.dal.mapper.BtTradeRecordMapper;
import com.bebopze.tdx.quant.dal.service.IBtTradeRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 回测-BS交易记录 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Service
public class BtTradeRecordServiceImpl extends ServiceImpl<BtTradeRecordMapper, BtTradeRecordDO> implements IBtTradeRecordService {

    @Override
    public List<BtTradeRecordDO> listByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate) {
        return baseMapper.listByTaskIdAndTradeDate(taskId, tradeDate);
    }
}
