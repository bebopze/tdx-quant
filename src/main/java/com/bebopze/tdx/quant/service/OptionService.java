package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.option.MonthOptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionEtfInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionKlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionUnderlyingDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionUnderlyingKlineDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 期权
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
public interface OptionService {


    /**
     * 某日期 存续合约 分组列表（合约月份 - 认购列表/认沽列表）
     *
     * @param date 日期
     * @return 月份分组列表（全部标的）
     */
    List<MonthOptionDTO> listOptionInDate(LocalDate date);


    /**
     * 某日期 存续合约 分组列表（T型报价）
     *
     * @param underlyingCode 标的code（null/空 = 全部标的）
     * @param date           日期
     * @return 月份分组列表（按 标的 + 合约月份 升序）
     */
    List<MonthOptionDTO> listOptionInDate(String underlyingCode, LocalDate date);


    /**
     * 某日期 存续合约 的 标的列表（含标的ETF行情）
     *
     * @param date 日期
     * @return 标的列表
     */
    List<OptionUnderlyingDTO> listUnderlyingInDate(LocalDate date);


    /**
     * 单合约 K线（合约详情 / 合约K线弹窗）
     *
     * @param optionCode 合约编码
     * @return 合约K线
     */
    OptionKlineDTO optionKline(String optionCode);


    /**
     * 标的ETF K线（所选日期 前后 months 个月）
     *
     * @param underlyingCode 标的code
     * @param date           所选日期（null = 今天）
     * @param months         前后月份数（null/<=0 = 3）
     * @return 标的K线
     */
    OptionUnderlyingKlineDTO underlyingKline(String underlyingCode, LocalDate date, Integer months);


    /**
     * 交易日列表（取自 标的ETF 日K，用于 T型报价 日期翻页/校验）
     *
     * @param underlyingCode 标的code（null = 默认 510300）
     * @param from           起始日期（null = 不限）
     * @param to             结束日期（null = 不限）
     * @return 交易日列表（升序）
     */
    List<LocalDate> tradeDates(String underlyingCode, LocalDate from, LocalDate to);


    // -----------------------------------------------------------------------------------------------------------------


    OptionDTO info(String optionCode);

    OptionEtfInfoDTO optionEtfInfo(String optionCode);
}
