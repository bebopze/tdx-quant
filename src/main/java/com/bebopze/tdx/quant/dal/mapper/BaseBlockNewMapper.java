package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * tdx - 自定义板块 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-11
 */
public interface BaseBlockNewMapper extends BaseMapper<BaseBlockNewDO> {

    BaseBlockNewDO getByCode(@Param("code") String code);
}
