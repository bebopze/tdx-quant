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
     * 回测个股类型：1-A股；2-ETF；
     */
    @Schema(description = "回测个股类型：1-A股；2-ETF；", example = "1")
    private int stockType = 1;


    /**
     * 买入前N支
     */
    @Schema(description = "买入前N支", example = "100")
    private int scoreSortN = 100;


    /**
     * 单一个股 总持仓上限  <=  账户总资金 x 5%
     */
    @Schema(description = "单一个股 总持仓上限（占账户总资金比例）", example = "5")
    private double singleStockMaxPosPct = 5;


    /**
     * 单一个股 单次买入下限  >=  账户总资金 x 0.5%
     */
    @Schema(description = "单一个股 单次买入下限（占账户总资金比例）", example = "0.5")
    private double singleStockMinBuyPosPct = 0.5;


    /**
     * 单一个股 单次买入上限  <=  剩余资金 x 10%
     */
    @Schema(description = "单一个股 单次买入上限（占剩余资金比例）", example = "10")
    private double singleStockMaxBuyAvlPct = 10;


    /**
     * 是否涨停：true-涨停（涨停个股Close 根本买不进去）；false-未涨停（Close 可正常买入）；null-不限；
     */
    @Schema(description = "是否涨停：true-涨停（涨停个股Close 根本买不进去）；false-未涨停（Close 可正常买入）；null-不限；", example = "false")
    private Boolean ztFlag = false;

    public boolean ztFlag_true() {
        // 涨停
        return ztFlag != null && ztFlag;
    }


    /**
     * 是否开启 大盘持仓限制：true/false
     */
    @Schema(description = "是否开启 大盘持仓限制", example = "true")
    private boolean marketPosLimitFlag = true;


    /**
     * 是否开启 TOP1主线板块（板块-月多2 + 涨停TOP1 + 百日新高TOP1）：true/false
     */
    @Schema(description = "是否开启 TOP1主线板块（板块-月多2 + 涨停TOP1 + 百日新高TOP1）", example = "true")
    private boolean top1TopBlockFlag = true;


    /**
     * B策略 持仓占比（占账户总资金比例：0~1）
     */
    @Schema(description = "B策略 持仓占比（占账户总资金比例：0~1）", example = "1.00")
    private double strategyPosRatio = 1.00;


    // -------------------------------------- 全局参数 上下文传递（不参与序列化）---------------------------------------------


    @JsonIgnore
    @JSONField(serialize = false)
    private transient Integer batchNo;

    @JsonIgnore
    @JSONField(serialize = false)
    private transient TopBlockStrategyEnum topBlockStrategyEnum;

    @JsonIgnore
    @JSONField(serialize = false)
    private transient Set<String> buyConSet;

    @JsonIgnore
    @JSONField(serialize = false)
    private transient Set<String> sellConSet;

    @JsonIgnore
    @JSONField(serialize = false)
    private transient LocalDate startDate;

    @JsonIgnore
    @JSONField(serialize = false)
    private transient LocalDate endDate;


    // -------------------------------------- 控制参数 ------------------------------------------------------------------


    /**
     * 是否开启 快速失败：true-是；false-否；
     */
    @JSONField(serialize = false)
    @Schema(description = "是否开启 快速失败：true-开启；false-关闭", example = "true")
    private transient boolean failFastFlag = true;


    /**
     * 是否检查 交易记录：true-检查；false-不检查；
     */
    @JSONField(serialize = false)
    private transient boolean checkTradeFlag = false;


    /**
     * B策略：A/B/C/D/E/F
     */
    @JSONField(serialize = false)
    private transient String buyStrategyKey = "D";


    /**
     * 是否开启 taskList：true-是；false-否；
     */
    @JSONField(serialize = false)
    @Schema(description = "是否开启 taskList：true-开启；false-关闭", example = "false")
    private transient boolean taskListFlag = false;


    // -------------------------------------- temp（TEST） --------------------------------------------------------------


    @JSONField(serialize = false)
    private transient int con2_num_1 = 1;

    @JSONField(serialize = false)
    private transient int con2_num_2 = 1;
}