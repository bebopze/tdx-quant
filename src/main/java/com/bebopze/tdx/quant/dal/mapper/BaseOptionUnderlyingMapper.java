package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseOptionUnderlyingDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 * 期权标的-实时行情 Mapper 接口
 * </p>
 *
 * @author bebopze
 * @since 2026-11-28
 */
public interface BaseOptionUnderlyingMapper extends BaseMapper<BaseOptionUnderlyingDO> {


    Long getIdByCode(@Param("code") String code);

    BaseOptionUnderlyingDO getByCode(@Param("code") String code);


    List<BaseOptionUnderlyingDO> listAllSimple();

    List<BaseOptionUnderlyingDO> listSimpleByKeyword(@Param("keyword") String keyword);


    /**
     * 指定标的 的 kline_his（日更：按 code 批量读取历史K线）
     *
     * @param codes 合约编码（建议每批 ≤ 500）
     * @return 仅 code、kline_his、kline_his_raw_len
     */
    List<BaseOptionUnderlyingDO> listKlineHisByCodes(@Param("codes") List<String> codes);


    int batchInsert(@Param("list") List<BaseOptionUnderlyingDO> list);

    int batchInsertOrUpdate(@Param("list") List<BaseOptionUnderlyingDO> list);


    int delAllKlineExtData();
}