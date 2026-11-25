package com.bebopze.tdx.quant.dal.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 期权BS回测-元数据查询 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-26
 */
public interface ObtMetaMapper {


    List<Map<String, Object>> listUnderlyings();

}
