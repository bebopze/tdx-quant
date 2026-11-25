package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.ObtTaskDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 * 期权BS回测-任务 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
public interface ObtTaskMapper extends BaseMapper<ObtTaskDO> {


    String lastTaskNo(@Param("prefix") String prefix);


    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("progress") Integer progress,
                     @Param("errorMsg") String errorMsg);


    int updateProgress(@Param("id") Long id,
                       @Param("progress") Integer progress);


    int updateFavorite(@Param("id") Long id,
                       @Param("flag") Integer flag);


    int logicDelete(@Param("id") Long id);


    long countPage(@Param("keyword") String keyword,
                   @Param("status") Integer status,
                   @Param("signalMode") String signalMode,
                   @Param("underlyingCode") String underlyingCode,
                   @Param("startDate") LocalDate startDate,
                   @Param("endDate") LocalDate endDate);


    List<ObtTaskDO> pageList(@Param("keyword") String keyword,
                             @Param("status") Integer status,
                             @Param("signalMode") String signalMode,
                             @Param("underlyingCode") String underlyingCode,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             @Param("sortBy") String sortBy,
                             @Param("sortAsc") boolean sortAsc,
                             @Param("offset") int offset,
                             @Param("size") int size);

}
