package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.QaBlockNewRelaStockHisDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 量化分析 - 每日 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-07-14
 */
public interface IQaBlockNewRelaStockHisService extends IService<QaBlockNewRelaStockHisDO> {

    int deleteAll(Integer blockNewId, LocalDate date);

    List<QaBlockNewRelaStockHisDO> listByBlockNewIdDateAndLimit(Integer blockNewId, LocalDate date, int limit);

    QaBlockNewRelaStockHisDO last();
}
