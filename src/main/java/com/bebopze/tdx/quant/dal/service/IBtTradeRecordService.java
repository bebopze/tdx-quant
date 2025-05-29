package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BtTradeRecordDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 回测-BS交易记录 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
public interface IBtTradeRecordService extends IService<BtTradeRecordDO> {

    List<BtTradeRecordDO> listByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate);
}
