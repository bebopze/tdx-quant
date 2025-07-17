package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BtPositionRecordDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 回测-每日持仓记录 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
public interface IBtPositionRecordService extends IService<BtPositionRecordDO> {


    List<BtPositionRecordDO> listByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate);

    List<BtPositionRecordDO> listByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate);

}
