package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.ObtIndexKlineDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 期权BS回测-指数K线缓存 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
public interface ObtIndexKlineMapper extends BaseMapper<ObtIndexKlineDO> {


    List<ObtIndexKlineDO> listByCodeAndRange(@Param("code") String code,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);


    int batchUpsert(@Param("list") List<ObtIndexKlineDO> list);

}
