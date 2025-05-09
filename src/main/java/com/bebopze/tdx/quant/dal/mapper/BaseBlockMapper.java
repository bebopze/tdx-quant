package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 板块/指数-实时行情（以 tdx 为准） Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
public interface BaseBlockMapper extends BaseMapper<BaseBlockDO> {

    BaseBlockDO getByCode(@Param("code") String code);
}
