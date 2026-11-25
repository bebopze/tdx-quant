package com.bebopze.tdx.quant.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
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


    /**
     * 某日期 存续合约（简单列，不含 kline_his）
     *
     * @param date           日期（start_date <= date <= end_date）
     * @param underlyingCode 标的code（null = 全部）
     * @return 合约列表
     */
    List<BaseOptionDO> listSimpleInDate(@Param("date") LocalDate date,
                                        @Param("underlyingCode") String underlyingCode);


    /**
     * 某日期 存续合约 的 kline_his（仅 code + blob）
     *
     * @param date           日期
     * @param underlyingCode 标的code（null = 全部）
     * @return 合约列表（仅 code、kline_his、kline_his_raw_len）
     */
    List<BaseOptionDO> listKlineHisInDate(@Param("date") LocalDate date,
                                          @Param("underlyingCode") String underlyingCode);


    /**
     * 回测用：指定标的、存续区间的合约（含 kline_his）
     */
    List<BaseOptionDO> listForBacktest(@Param("codes") List<String> codes,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);


    /**
     * 标的name（期权表口径，如 50ETF / 300ETF）
     *
     * @param underlyingCode 标的code
     * @return 标的name
     */
    String getUnderlyingName(@Param("underlyingCode") String underlyingCode);


    Long getIdByCode(@Param("code") String code);

    BaseOptionDO getByCode(@Param("code") String code);


    List<BaseOptionDO> listAllSimple();

    List<BaseOptionDO> listSimpleByKeyword(@Param("keyword") String keyword);


    int batchInsert(@Param("list") List<BaseOptionDO> list);

    int batchInsertOrUpdate(@Param("list") List<BaseOptionDO> list);


    int delAllKlineExtData();
}