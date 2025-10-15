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


    /**
     * 指定日期  倒序N条
     *
     * @param date 指定日期
     * @param N    倒序N条
     * @return
     */
    List<QaTopBlockDO> lastN(LocalDate date, int N);


    /**
     * 以 date 为中心，前后各取 N 条（共 2N+1 条），按日期倒序排列
     *
     * @param date 指定日期
     * @param N    前后 N条
     * @return
     */
    List<QaTopBlockDO> beforeAfterN(LocalDate date, int N);

}