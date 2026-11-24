package com.bebopze.tdx.quant.dal.service;

import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.baomidou.mybatisplus.spring.service.IService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 期权-实时行情 服务类
 * </p>
 *
 * @author bebopze
 * @since 2026-11-16
 */
public interface IBaseOptionService extends IService<BaseOptionDO> {


    List<BaseOptionDO> listOptionInDate(LocalDate date);

    /**
     * 某日期 存续合约（可按 标的 过滤）
     *
     * @param date           日期
     * @param underlyingCode 标的code（null = 全部）
     * @return 合约列表（简单列，不含 kline_his）
     */
    List<BaseOptionDO> listOptionInDate(LocalDate date, String underlyingCode);


    /**
     * 某日期 存续合约 的 kline_his（解压后 JSON）
     *
     * @param date           日期
     * @param underlyingCode 标的code（null = 全部）
     * @return code -> kline_his JSON 字符串
     */
    Map<String, String> klineHisMapInDate(LocalDate date, String underlyingCode);


    /**
     * 标的name（期权表口径，如 50ETF / 300ETF）
     *
     * @param underlyingCode 标的code
     * @return 标的name
     */
    String getUnderlyingName(String underlyingCode);


    // -----------------------------------------------------------------------------------------------------------------


    Long getIdByCode(String code);

    BaseOptionDO getByCode(String code);


    List<BaseOptionDO> listSimpleByKeyword(String keyword);


    Map<String, Long> codeIdMap();


    // -----------------------------------------------------------------------------------------------------------------


    boolean updateById(BaseOptionDO entity);

    int batchInsert(List<BaseOptionDO> list);

    int batchInsertOrUpdate(List<BaseOptionDO> list);

    int saveOrUpdateBatch(List<BaseOptionDO> list);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * HIS 更新前     =>     先 del ALL   ->   kline_ext_data
     *
     * @return
     */
    int delAllKlineExtData();
}