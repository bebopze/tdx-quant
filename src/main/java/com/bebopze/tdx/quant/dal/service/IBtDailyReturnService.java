package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BtDailyReturnDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 回测-每日收益率 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-28
 */
public interface IBtDailyReturnService extends IService<BtDailyReturnDO> {


    BtDailyReturnDO getByTaskIdAndTradeDate(Long taskId, LocalDate tradeDate);

    List<BtDailyReturnDO> listByTaskIdAndTradeDateRange(Long taskId, LocalDate startDate, LocalDate endDate);

    List<BtDailyReturnDO> listByTaskId(Long taskId);
}
