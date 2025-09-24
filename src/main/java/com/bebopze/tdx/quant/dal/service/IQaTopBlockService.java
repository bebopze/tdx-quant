package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.QaTopBlockDO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 量化分析 - LV3主线板块（板块-月多2） 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-09-24
 */
public interface IQaTopBlockService extends IService<QaTopBlockDO> {


    Map<LocalDate, Long> dateIdMap();

    QaTopBlockDO getByDate(LocalDate date);

    List<QaTopBlockDO> lastN(LocalDate date, int N);

}