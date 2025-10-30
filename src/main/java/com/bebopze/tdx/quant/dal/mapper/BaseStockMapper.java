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


    Long getIdByCode(@Param("code") String code);

    BaseStockDO getByCode(@Param("code") String code);

    BaseStockDO getSimpleByCode(@Param("code") String code);


    List<BaseStockDO> listAllSimple();

    List<BaseStockDO> listAllKline();

    List<BaseStockDO> listAllETFKline();

    /**
     * 游标分页（非 OFFSET分页）
     *
     * @param lastId
     * @param pageSize
     * @return
     */
    List<BaseStockDO> listByCursor(@Param("lastId") Long lastId,
                                   @Param("pageSize") int pageSize);


    List<BaseStockDO> listByCodeList(@Param("codeList") Collection<String> codeList);

    List<BaseStockDO> listSimpleByCodeList(@Param("codeList") Collection<String> codeList);

    List<BaseStockDO> listBaseByCodeList(@Param("codeList") Collection<String> codeList);
}