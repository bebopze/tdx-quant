package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 股票-实时行情 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
public interface BaseStockMapper extends BaseMapper<BaseStockDO> {

    BaseStockDO getByCode(@Param("code") String code);

    List<BaseStockDO> listSimpleByCodeList(@Param("codeList") Collection<String> stockCodeList);

    List<BaseStockDO> listBaseByCodeList(@Param("codeList") Collection<String> stockCodeList);
}
