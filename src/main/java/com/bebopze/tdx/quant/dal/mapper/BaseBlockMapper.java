package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 板块/指数-实时行情（以 tdx 为准） Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
public interface BaseBlockMapper extends BaseMapper<BaseBlockDO> {


    Long getIdByCode(@Param("code") String code);

    BaseBlockDO getByCode(@Param("code") String code);

    BaseBlockDO getSimpleById(@Param("id") Long id);


    List<BaseBlockDO> listAllSimple();

    /**
     * 全量板块   =>   2-普通行业   +   4-概念板块   +   12-研究行业
     *
     * @return
     */
    List<BaseBlockDO> listAllKline();

    /**
     * 细分行业 + 概念板块     =>     板块指数RPS
     *
     * @return
     */
    List<BaseBlockDO> listAllRpsKline();


    List<BaseBlockDO> listSimpleByCodeList(@Param("codeList") Collection<String> codeList);
}