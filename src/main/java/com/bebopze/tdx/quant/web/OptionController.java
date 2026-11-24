package com.bebopze.tdx.quant.web;

import com.bebopze.tdx.quant.common.domain.Result;
import com.bebopze.tdx.quant.common.domain.dto.option.MonthOptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionEtfInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionKlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionUnderlyingDTO;
import com.bebopze.tdx.quant.common.domain.dto.option.OptionUnderlyingKlineDTO;
import com.bebopze.tdx.quant.service.OptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;


/**
 * 期权行情
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
@RestController
@RequestMapping("/api/option")
@Tag(name = "期权行情", description = "期权行情")
public class OptionController {


    @Autowired
    private OptionService optionService;


    /**
     * 期权 T型报价（指定日期 -> 合约分组列表：合约月份 - 认购列表/认沽列表）
     *
     * @param underlyingCode 标的code（不传 = 全部标的；T型报价 建议指定，如 510050 / 510300）
     * @param date           指定日期（不传 = 今天）
     * @return 月份分组列表
     */
    @Operation(summary = "期权 T型报价（指定日期 -> 合约月份 - 认购列表/认沽列表）",
            description = "行情取自 base_option.kline_his 中该日期当日（或之前最近一根）的日K；" +
                    "含 行权价对齐列表、月度统计（成交量/成交额/PCR）、派生指标（内在价值/时间价值/平值/杠杆）")
    @GetMapping(value = "/listOptionInDate")
    public Result<List<MonthOptionDTO>> listOptionInDate(@Schema(description = "标的证券code（空 = 全部标的）", example = "510300")
                                                         @RequestParam(required = false, defaultValue = "") String underlyingCode,

                                                         @Schema(description = "日期", example = "2026-10-01")
                                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        return Result.SUC(optionService.listOptionInDate(underlyingCode, date));
    }


    /**
     * 期权标的列表（指定日期 存续合约的标的概况 + 标的ETF行情）
     *
     * @param date 指定日期（不传 = 今天）
     * @return 标的列表
     */
    @Operation(summary = "期权标的列表（指定日期）", description = "指定日期 存续合约的标的概况 + 标的ETF行情（收盘价/涨跌幅）")
    @GetMapping(value = "/underlyingList")
    public Result<List<OptionUnderlyingDTO>> underlyingList(@Schema(description = "日期", example = "2026-10-01")
                                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        return Result.SUC(optionService.listUnderlyingInDate(date));
    }


    /**
     * 单合约 K线（合约详情 / 合约K线弹窗）
     *
     * @param code 合约编码
     * @return 合约K线
     */
    @Operation(summary = "单合约 K线", description = "合约要素 + 全历史日K（用于 合约K线弹窗）")
    @GetMapping(value = "/kline")
    public Result<OptionKlineDTO> kline(@Schema(description = "期权合约编码", example = "10012853")
                                        @RequestParam String code) {

        return Result.SUC(optionService.optionKline(code));
    }


    /**
     * 标的ETF K线（所选日期 前后 N 个月）
     *
     * @param underlyingCode 标的code
     * @param date           所选日期（不传 = 今天）
     * @param months         前后月份数（默认 3）
     * @return 标的K线
     */
    @Operation(summary = "标的ETF K线（所选日期 前后N个月）", description = "T型报价 右上角 标的K线图；默认 前后 3 个月")
    @GetMapping(value = "/underlyingKline")
    public Result<OptionUnderlyingKlineDTO> underlyingKline(@Schema(description = "标的证券code", example = "510300")
                                                            @RequestParam String underlyingCode,

                                                            @Schema(description = "所选日期", example = "2026-10-01")
                                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,

                                                            @Schema(description = "前后月份数", example = "3")
                                                            @RequestParam(required = false, defaultValue = "3") Integer months) {

        return Result.SUC(optionService.underlyingKline(underlyingCode, date, months));
    }


    /**
     * 交易日列表（取自 标的ETF 日K，用于 日期翻页/校验）
     *
     * @param underlyingCode 标的code（不传 = 510300）
     * @param from           起始日期
     * @param to             结束日期
     * @return 交易日列表（升序）
     */
    @Operation(summary = "交易日列表", description = "取自 标的ETF 日K，用于 T型报价 的 上一交易日/下一交易日 与 日期校验")
    @GetMapping(value = "/tradeDates")
    public Result<List<LocalDate>> tradeDates(@Schema(description = "标的证券code", example = "510300")
                                              @RequestParam(required = false, defaultValue = "") String underlyingCode,

                                              @Schema(description = "起始日期", example = "2026-01-01")
                                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,

                                              @Schema(description = "结束日期", example = "2026-12-31")
                                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to) {

        return Result.SUC(optionService.tradeDates(underlyingCode, from, to));
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 期权行情
     *
     * @return
     */
    @Operation(summary = "期权行情", description = "期权行情")
    @GetMapping(value = "/info")
    public Result<OptionDTO> info(@Schema(description = "期权代码", example = "10011565.SHO")
                                  @RequestParam String optionCode) {

        return Result.SUC(optionService.info(optionCode));
    }


    /**
     * 期权-现货（指数ETF/商品现货）
     *
     * @return
     */
    @Operation(summary = "期权-现货（指数ETF/商品现货）", description = "期权-现货（指数ETF/商品现货）")
    @GetMapping(value = "/blockInfo")
    public Result<OptionEtfInfoDTO> blockInfo(@Schema(description = "期权代码", example = "10011565.SHO")
                                              @RequestParam String optionCode) {

        return Result.SUC(optionService.optionEtfInfo(optionCode));
    }


}