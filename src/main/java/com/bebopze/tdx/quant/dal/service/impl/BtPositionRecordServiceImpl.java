package com.bebopze.tdx.quant.dal.service.impl;

import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.bebopze.tdx.quant.dal.mapper.BtPositionRecordMapper;
import com.bebopze.tdx.quant.dal.service.IBtPositionRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 回测-每日持仓记录 服务实现类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
@Service
public class BtPositionRecordServiceImpl extends ServiceImpl<BtPositionRecordMapper, BtPositionRecordDO> implements IBtPositionRecordService {


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

}
