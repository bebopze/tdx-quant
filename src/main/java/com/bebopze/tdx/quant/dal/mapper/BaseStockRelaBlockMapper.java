package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseStockRelaBlockDO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 股票-板块 关联 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-08
 */
public interface BaseStockRelaBlockMapper extends BaseMapper<BaseStockRelaBlockDO> {

    int deleteByStockId(@Param("stockId") Long stockId);
}
