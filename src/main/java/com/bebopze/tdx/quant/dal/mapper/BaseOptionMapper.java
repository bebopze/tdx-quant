package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 * 期权-实时行情 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-16
 */
public interface BaseOptionMapper extends BaseMapper<BaseOptionDO> {


    Long getIdByCode(@Param("code") String code);

    BaseOptionDO getByCode(@Param("code") String code);

    List<BaseOptionDO> listAllSimple();


    int batchInsert(@Param("list") List<BaseOptionDO> list);

    int batchInsertOrUpdate(@Param("list") List<BaseOptionDO> list);


    int delAllKlineExtData();


}