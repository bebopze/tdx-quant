package com.bebopze.tdx.quant.common.domain.dto.trade;

import com.bebopze.tdx.quant.common.constant.StockLimitEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.util.NumUtil;
import lombok.Data;


/**
 * 个股 - 行情快照（批量拉取 全A 实时行情）
 *
 * @author: bebopze
 * @date: 2025/8/3
 */
@Data
public class StockSnapshotKlineDTO extends KlineDTO {


    private String stockCode;

    private String stockName;


    /**
     * 涨跌幅 限制
     */
    // private int changePctLimit;


    /**
     * 昨日 收盘价
     */
    private double preClose;
    /**
     * 今日 涨停价
     */
    private double ztPrice;
    /**
     * 今日 跌停价
     */
    private double dtPrice;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 涨跌幅 限制
     *
     * @return
     */
    public int getChangePctLimit() {
        return StockLimitEnum.getChangePctLimit(stockCode, stockName);
    }


    public double getZtPrice() {
        return NumUtil.of(preClose * (1 + getChangePctLimit() * 0.01), 3);
    }

    public double getDtPrice() {
        return NumUtil.of(preClose * (1 - getChangePctLimit() * 0.01), 3);
    }


}