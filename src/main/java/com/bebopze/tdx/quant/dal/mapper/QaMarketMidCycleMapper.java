package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * <p>
 * 量化分析 - 大盘中期顶底 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-07-21
 */
public interface QaMarketMidCycleMapper extends BaseMapper<QaMarketMidCycleDO> {

    int deleteAll();

    QaMarketMidCycleDO getByDate(@Param("date") LocalDate date);
}
