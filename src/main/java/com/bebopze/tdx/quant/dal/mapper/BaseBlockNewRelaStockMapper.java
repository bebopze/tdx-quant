package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewRelaStockDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 股票-自定义板块 关联 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-11
 */
public interface BaseBlockNewRelaStockMapper extends BaseMapper<BaseBlockNewRelaStockDO> {


    int delByBlockNewId(@Param("blockNewId") Long blockNewId);

    int deleteAll();


    List<BaseBlockNewDO> listByStockCode(@Param("stockCode") String stockCode,
                                         @Param("type") Integer type);


    List<BaseStockDO> listStockByBlockNewCodeList(@Param("blockNewCodeList") List<String> blockNewCodeList,
                                                  @Param("type") Integer type);

    List<BaseBlockDO> listBlockByBlockNewCodeList(@Param("blockNewCodeList") List<String> blockNewCodeList,
                                                  @Param("type") Integer type);

}
