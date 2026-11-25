package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.ObtTaskLogDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 期权BS回测-运行日志 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
public interface ObtTaskLogMapper extends BaseMapper<ObtTaskLogDO> {


    int deleteByTaskId(@Param("taskId") Long taskId);


    List<ObtTaskLogDO> listByTaskId(@Param("taskId") Long taskId,
                                    @Param("limit") int limit);

}
