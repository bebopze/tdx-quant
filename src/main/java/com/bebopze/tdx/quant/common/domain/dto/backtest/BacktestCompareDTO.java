package com.bebopze.tdx.quant.common.domain.dto.backtest;

import com.alibaba.fastjson2.annotation.JSONField;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;


/**
 * 回测对照（可变参数）
 *
 * @author: bebopze
 * @date: 2025/9/7
 */
@Data
public class BacktestCompareDTO {


    /**
     * 买入前N支
     */
    @Schema(description = "买入前N支", example = "100")
    private int scoreSortN = 100;


    /**
     * 单一个股 总持仓上限  <=  账户总资金 x 5%
     */
    @Schema(description = "单一个股 总持仓上限（占账户总资金比例）", example = "5")
    private int singleStockMaxPosPct = 5;


    /**
     * 单一个股 单次买入上限  <=  剩余资金 x 10%
     */
    @Schema(description = "单一个股 单次买入上限（占剩余资金比例）", example = "10")
    private int singleStockMaxBuyPct = 10;


    /**
     * 是否涨停：true-涨停（涨停个股Close 根本买不进去）；false-未涨停（Close 可正常买入）；null-不限；
     */
    @Schema(description = "是否涨停：true-涨停（涨停个股Close 根本买不进去）；false-未涨停（Close 可正常买入）；null-不限；", example = "false")
    private Boolean ztFlag = false;


    /**
     * 是否开启 大盘持仓限制：true/false
     */
    @Schema(description = "是否开启 大盘持仓限制", example = "true")
    private boolean marketPosLimitFlag = true;


    // -------------------------------------- 全局参数 上下文传递（不参与序列化）---------------------------------------------


    @JsonIgnore
    @JSONField(serialize = false)
    private Integer batchNo;

    @JsonIgnore
    @JSONField(serialize = false)
    private TopBlockStrategyEnum topBlockStrategyEnum;

    @JsonIgnore
    @JSONField(serialize = false)
    private Set<String> buyConSet;

    @JsonIgnore
    @JSONField(serialize = false)
    private Set<String> sellConSet;

    @JsonIgnore
    @JSONField(serialize = false)
    private LocalDate startDate;

    @JsonIgnore
    @JSONField(serialize = false)
    private LocalDate endDate;


    // -------------------------------------- 控制参数 ------------------------------------------------------------------


    /**
     * 是否检查 交易记录：true-检查；false-不检查；
     */
    @JsonIgnore
    @JSONField(serialize = false)
    private boolean checkTradeFlag = false;
}