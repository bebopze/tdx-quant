package com.bebopze.tdx.quant.common.domain.dto.backtest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


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
}