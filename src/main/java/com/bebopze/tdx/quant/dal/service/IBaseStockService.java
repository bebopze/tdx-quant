package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * <p>
 * 股票-实时行情 服务类
 * </p>
 *
 * @author bebopze
 * @since 2025-05-09
 */
public interface IBaseStockService extends IService<BaseStockDO> {

    Long getIdByCode(String code);

    BaseStockDO getByCode(String code);

    BaseStockDO getSimpleByCode(String code);


    Map<String, Set<String>> market_stockCodePrefixList_map(int type, int N);


    List<BaseStockDO> listByCodeList(Collection<String> codeList);

    List<BaseStockDO> listSimpleByCodeList(Collection<String> codeList);

    Map<String, Long> codeIdMap(Collection<String> codeList);


    List<BaseStockDO> listAllRpsKline(UpdateTypeEnum updateTypeEnum, Integer stockType);

    List<BaseStockDO> listAllKline(UpdateTypeEnum updateTypeEnum, Integer stockType);

    List<BaseStockDO> listAllKline(UpdateTypeEnum updateTypeEnum, Set<Integer> stockTypeSet);

    /**
     * 从 Cache 中获取   全部个股/ETF
     *
     * @param updateTypeEnum
     * @param stockTypeSet   股票类型：null-全部；1-A股；2-ETF；11-港股；12-美股；
     * @param refresh        是否刷新缓存：true/false
     * @return
     */
    List<BaseStockDO> listAllKline(UpdateTypeEnum updateTypeEnum, Set<Integer> stockTypeSet, boolean refresh);

    List<BaseStockDO> listAllETFKline();


    /**
     * 游标分页（非 OFFSET 分页）-> 流式读取（因子挖掘 窗口加载）
     *
     * @param updateTypeEnum K线数据类型
     * @param stockTypeSet   股票类型
     * @param lastId         上一页最后一条 id（首页传 0）
     * @param pageSize       页大小
     */
    List<BaseStockDO> listByCursor(UpdateTypeEnum updateTypeEnum, Set<Integer> stockTypeSet, Long lastId, int pageSize);


    List<BaseStockDO> listAllSimple();

    Map<String, Long> codeIdMap(Integer type);

    Map<Long, String> idCodeMap();


    // -----------------------------------------------------------------------------------------------------------------


    boolean updateById(BaseStockDO entity);

    int batchInsert(List<BaseStockDO> list);

    int batchInsertOrUpdate(List<BaseStockDO> list);

    int saveOrUpdateBatch(List<BaseStockDO> list);
}
