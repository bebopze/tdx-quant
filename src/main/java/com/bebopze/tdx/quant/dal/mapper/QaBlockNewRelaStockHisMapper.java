package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.QaBlockNewRelaStockHisDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 量化分析 - 每日 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-07-14
 */
public interface QaBlockNewRelaStockHisMapper extends BaseMapper<QaBlockNewRelaStockHisDO> {

    int deleteAll(@Param("blockNewId") Long blockNewId,
                  @Param("date") LocalDate date);

    List<QaBlockNewRelaStockHisDO> listByDateAndLimit(@Param("date") LocalDate date,
                                                      @Param("limit") int limit);

}
