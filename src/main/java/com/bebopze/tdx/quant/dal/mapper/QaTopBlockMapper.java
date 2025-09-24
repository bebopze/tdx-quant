package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.QaTopBlockDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 量化分析 - LV3主线板块（板块-月多2） Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-09-24
 */
public interface QaTopBlockMapper extends BaseMapper<QaTopBlockDO> {


    List<QaTopBlockDO> listAllSimple();

    QaTopBlockDO getByDate(@Param("date") LocalDate date);

    List<QaTopBlockDO> lastN(@Param("date") LocalDate date,
                             @Param("N") int N);
}