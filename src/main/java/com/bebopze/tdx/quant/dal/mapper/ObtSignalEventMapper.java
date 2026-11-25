package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.ObtSignalEventDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 期权BS回测-信号事件 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
public interface ObtSignalEventMapper extends BaseMapper<ObtSignalEventDO> {


    int deleteByTaskId(@Param("taskId") Long taskId);


    int batchInsert(@Param("list") List<ObtSignalEventDO> list);


    List<ObtSignalEventDO> listByTaskId(@Param("taskId") Long taskId);

}
