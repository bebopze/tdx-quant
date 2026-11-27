package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseOptionUnderlyingDO;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;
import java.util.Map;


/**
 * <p>
 * 期权标的-实时行情 服务类
 * </p>
 *
 * @author bebopze
 * @since 2026-11-28
 */
public interface IBaseOptionUnderlyingService extends IService<BaseOptionUnderlyingDO> {


    // -----------------------------------------------------------------------------------------------------------------


    Long getIdByCode(String code);

    BaseOptionUnderlyingDO getByCode(String code);


    List<BaseOptionUnderlyingDO> listSimpleByKeyword(String keyword);


    Map<String, Long> codeIdMap();


    Map<String, Long> codeIdMapNotExpired();


    // -----------------------------------------------------------------------------------------------------------------


    boolean updateById(BaseOptionUnderlyingDO entity);

    int batchInsert(List<BaseOptionUnderlyingDO> list);

    int batchInsertOrUpdate(List<BaseOptionUnderlyingDO> list);

    int saveOrUpdateBatch(List<BaseOptionUnderlyingDO> list);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * HIS 更新前     =>     先 del ALL   ->   kline_ext_data
     *
     * @return
     */
    int delAllKlineExtData();


}