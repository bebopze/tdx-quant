package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseStockRelaBlockNewDO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 股票-自定义板块 关联 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-11
 */
public interface BaseStockRelaBlockNewMapper extends BaseMapper<BaseStockRelaBlockNewDO> {


    int delByBlockNewId(@Param("blockNewId") Long blockNewId);

    int deleteAll();
}
